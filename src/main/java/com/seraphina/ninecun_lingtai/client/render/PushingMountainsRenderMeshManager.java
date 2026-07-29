package com.seraphina.ninecun_lingtai.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.seraphina.ninecun_lingtai.common.entity.ability.PushingMountainsEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bakes the blocks of a Pushing Mountains entity into static GPU meshes.
 *
 * <p>The mesh is built once on the render thread. Rendering then only changes
 * the render state and submits one vertex buffer per block render layer.</p>
 */
public final class PushingMountainsRenderMeshManager {
    private static final Map<UUID, BakedMesh> MESHES = new ConcurrentHashMap<>();
    private static final Object MESH_LOCK = new Object();
    private static final List<RenderType> RENDER_TYPE_ORDER = List.of(
            Sheets.solidBlockSheet(),
            Sheets.cutoutBlockSheet(),
            Sheets.translucentCullBlockSheet(),
            Sheets.translucentItemSheet()
    );

    private PushingMountainsRenderMeshManager() {
    }

    public static void render(
            PushingMountainsEntity entity,
            BlockRenderDispatcher blockRenderDispatcher,
            PoseStack poseStack,
            int packedLight
    ) {
        BakedMesh mesh = getOrBake(entity, blockRenderDispatcher, packedLight);
        if (mesh == null || mesh.buffers.isEmpty()) {
            return;
        }

        Matrix4f modelViewMatrix = poseStack.last().pose();
        Matrix4f projectionMatrix = RenderSystem.getProjectionMatrix();
        for (Map.Entry<RenderType, VertexBuffer> entry : mesh.buffers.entrySet()) {
            RenderType renderType = entry.getKey();
            renderType.setupRenderState();
            try {
                VertexBuffer vertexBuffer = entry.getValue();
                vertexBuffer.bind();
                ShaderInstance shader = RenderSystem.getShader();
                if (shader != null) {
                    vertexBuffer.drawWithShader(modelViewMatrix, projectionMatrix, shader);
                }
            } finally {
                VertexBuffer.unbind();
                renderType.clearRenderState();
            }
        }
    }

    public static void clearEntity(UUID entityUuid) {
        BakedMesh mesh;
        synchronized (MESH_LOCK) {
            mesh = MESHES.remove(entityUuid);
        }
        closeOnRenderThread(mesh);
    }

    public static void clearAll() {
        List<BakedMesh> meshes;
        synchronized (MESH_LOCK) {
            meshes = new ArrayList<>(MESHES.values());
            MESHES.clear();
        }
        meshes.forEach(PushingMountainsRenderMeshManager::closeOnRenderThread);
    }

    private static BakedMesh getOrBake(
            PushingMountainsEntity entity,
            BlockRenderDispatcher blockRenderDispatcher,
            int packedLight
    ) {
        UUID entityUuid = entity.getUUID();
        synchronized (MESH_LOCK) {
            BakedMesh cachedMesh = MESHES.get(entityUuid);
            if (cachedMesh != null) {
                return cachedMesh;
            }
            BakedMesh bakedMesh = bake(entity, blockRenderDispatcher, packedLight);
            MESHES.put(entityUuid, bakedMesh);
            return bakedMesh;
        }
    }

    private static BakedMesh bake(
            PushingMountainsEntity entity,
            BlockRenderDispatcher blockRenderDispatcher,
            int packedLight
    ) {
        Map<RenderType, BufferBuilder> builders = new LinkedHashMap<>();
        MultiBufferSource bufferSource = renderType -> getBuilder(builders, renderType);
        PoseStack localPoseStack = new PoseStack();

        for (PushingMountainsEntity.StoredBlock storedBlock : entity.getBlocks()) {
            BlockState blockState = storedBlock.state();
            localPoseStack.pushPose();
            localPoseStack.translate(storedBlock.x(), storedBlock.y(), storedBlock.z());
            blockRenderDispatcher.renderSingleBlock(
                    blockState,
                    localPoseStack,
                    bufferSource,
                    packedLight,
                    OverlayTexture.NO_OVERLAY
            );
            localPoseStack.popPose();
        }

        Map<RenderType, VertexBuffer> vertexBuffers = new LinkedHashMap<>();
        builders.entrySet().stream()
                .sorted(Comparator.comparingInt(entry -> getRenderTypeOrder(entry.getKey())))
                .forEach(entry -> upload(entry.getKey(), entry.getValue(), vertexBuffers));
        return new BakedMesh(vertexBuffers);
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
            Map<RenderType, VertexBuffer> vertexBuffers
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
            vertexBuffers.put(renderType, vertexBuffer);
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
