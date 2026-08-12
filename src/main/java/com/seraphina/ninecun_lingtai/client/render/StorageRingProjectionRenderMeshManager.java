package com.seraphina.ninecun_lingtai.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class StorageRingProjectionRenderMeshManager {
    private static final float FIELD_OF_VIEW_RADIANS = (float) Math.toRadians(55.0D);
    private static final List<RenderType> RENDER_TYPE_ORDER = List.of(
            Sheets.solidBlockSheet(),
            Sheets.cutoutBlockSheet(),
            Sheets.chestSheet(),
            Sheets.translucentCullBlockSheet(),
            Sheets.translucentItemSheet()
    );

    private static BakedMesh thisMesh;
    private static long thisSnapshotVersion = -1L;

    private StorageRingProjectionRenderMeshManager() {
    }

    public static void render(
            BlockRenderDispatcher blockRenderDispatcher,
            Map<BlockPos, BlockState> blockStates,
            long snapshotVersion,
            Vec3 cameraPosition,
            float cameraYRot,
            float cameraXRot,
            int viewportWidth,
            int viewportHeight
    ) {
        if (viewportWidth <= 0 || viewportHeight <= 0) {
            return;
        }

        BakedMesh mesh = getOrBake(blockRenderDispatcher, blockStates, snapshotVersion);
        if (mesh.buffers.isEmpty()) {
            return;
        }

        Matrix4f previousProjection = new Matrix4f(RenderSystem.getProjectionMatrix());
        VertexSorting previousVertexSorting = RenderSystem.getVertexSorting();
        PoseStack modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.pushPose();
        try {
            modelViewStack.setIdentity();
            modelViewStack.mulPose(Axis.XP.rotationDegrees(cameraXRot));
            modelViewStack.mulPose(Axis.YP.rotationDegrees(cameraYRot + 180.0F));
            modelViewStack.translate(
                    -cameraPosition.x,
                    -cameraPosition.y,
                    -cameraPosition.z
            );
            RenderSystem.applyModelViewMatrix();
            RenderSystem.setProjectionMatrix(
                    new Matrix4f().setPerspective(
                            FIELD_OF_VIEW_RADIANS,
                            (float) viewportWidth / viewportHeight,
                            0.05F,
                            64.0F
                    ),
                    VertexSorting.DISTANCE_TO_ORIGIN
            );

            for (Map.Entry<RenderType, VertexBuffer> entry : mesh.buffers.entrySet()) {
                RenderType renderType = entry.getKey();
                renderType.setupRenderState();
                try {
                    VertexBuffer vertexBuffer = entry.getValue();
                    vertexBuffer.bind();
                    ShaderInstance shader = RenderSystem.getShader();
                    if (shader != null) {
                        vertexBuffer.drawWithShader(
                                modelViewStack.last().pose(),
                                RenderSystem.getProjectionMatrix(),
                                shader
                        );
                    }
                } finally {
                    VertexBuffer.unbind();
                    renderType.clearRenderState();
                }
            }
        } finally {
            RenderSystem.setProjectionMatrix(previousProjection, previousVertexSorting);
            modelViewStack.popPose();
            RenderSystem.applyModelViewMatrix();
        }
    }

    public static void clear() {
        BakedMesh mesh = thisMesh;
        thisMesh = null;
        thisSnapshotVersion = -1L;
        closeOnRenderThread(mesh);
    }

    private static BakedMesh getOrBake(
            BlockRenderDispatcher blockRenderDispatcher,
            Map<BlockPos, BlockState> blockStates,
            long snapshotVersion
    ) {
        if (thisMesh != null && thisSnapshotVersion == snapshotVersion) {
            return thisMesh;
        }

        BakedMesh previousMesh = thisMesh;
        thisMesh = bake(blockRenderDispatcher, blockStates);
        thisSnapshotVersion = snapshotVersion;
        closeOnRenderThread(previousMesh);
        return thisMesh;
    }

    private static BakedMesh bake(
            BlockRenderDispatcher blockRenderDispatcher,
            Map<BlockPos, BlockState> blockStates
    ) {
        Map<RenderType, BufferBuilder> builders = new LinkedHashMap<>();
        MultiBufferSource bufferSource = renderType -> getBuilder(builders, renderType);
        PoseStack poseStack = new PoseStack();

        for (Map.Entry<BlockPos, BlockState> entry : blockStates.entrySet()) {
            BlockPos position = entry.getKey();
            poseStack.pushPose();
            BlockState blockState = entry.getValue();
            if (StorageRingChestProjectionRenderer.isSupported(blockState)) {
                StorageRingChestProjectionRenderer.render(
                        blockState,
                        position,
                        poseStack,
                        bufferSource
                );
            } else {
                poseStack.translate(position.getX(), position.getY(), position.getZ());
                blockRenderDispatcher.renderSingleBlock(
                        blockState,
                        poseStack,
                        bufferSource,
                        0x00F000F0,
                        OverlayTexture.NO_OVERLAY
                );
            }
            poseStack.popPose();
        }

        Map<RenderType, VertexBuffer> buffers = new LinkedHashMap<>();
        builders.entrySet().stream()
                .sorted(Comparator.comparingInt(entry -> getRenderTypeOrder(entry.getKey())))
                .forEach(entry -> upload(entry.getKey(), entry.getValue(), buffers));
        return new BakedMesh(buffers);
    }

    private static BufferBuilder getBuilder(
            Map<RenderType, BufferBuilder> builders,
            RenderType renderType
    ) {
        BufferBuilder builder = builders.computeIfAbsent(
                renderType,
                type -> new BufferBuilder(Math.max(type.bufferSize(), 256))
        );
        if (!builder.building()) {
            builder.begin(renderType.mode(), renderType.format());
        }
        return builder;
    }

    private static void upload(
            RenderType renderType,
            BufferBuilder builder,
            Map<RenderType, VertexBuffer> buffers
    ) {
        if (!builder.building()) {
            return;
        }
        BufferBuilder.RenderedBuffer renderedBuffer = builder.endOrDiscardIfEmpty();
        if (renderedBuffer == null || renderedBuffer.isEmpty()) {
            return;
        }

        VertexBuffer vertexBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
        try {
            vertexBuffer.bind();
            vertexBuffer.upload(renderedBuffer);
            buffers.put(renderType, vertexBuffer);
        } catch (RuntimeException | Error exception) {
            vertexBuffer.close();
            throw exception;
        } finally {
            VertexBuffer.unbind();
        }
    }

    private static int getRenderTypeOrder(RenderType renderType) {
        int order = RENDER_TYPE_ORDER.indexOf(renderType);
        return order >= 0 ? order : RENDER_TYPE_ORDER.size();
    }

    private static void closeOnRenderThread(BakedMesh mesh) {
        if (mesh == null) {
            return;
        }
        Runnable close = mesh::close;
        if (RenderSystem.isOnRenderThreadOrInit()) {
            close.run();
        } else {
            RenderSystem.recordRenderCall(close::run);
        }
    }

    private static final class BakedMesh implements AutoCloseable {
        private final Map<RenderType, VertexBuffer> buffers;

        private BakedMesh(Map<RenderType, VertexBuffer> buffers) {
            this.buffers = Collections.unmodifiableMap(new LinkedHashMap<>(buffers));
        }

        @Override
        public void close() {
            this.buffers.values().forEach(VertexBuffer::close);
        }
    }
}
