package net.seraphina.ninecun_lingtai.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.EnderChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;

import java.util.Calendar;

public final class StorageRingChestProjectionRenderer {
    private static ModelPart thisSingleLid;
    private static ModelPart thisSingleBottom;
    private static ModelPart thisSingleLock;
    private static ModelPart thisDoubleLeftLid;
    private static ModelPart thisDoubleLeftBottom;
    private static ModelPart thisDoubleLeftLock;
    private static ModelPart thisDoubleRightLid;
    private static ModelPart thisDoubleRightBottom;
    private static ModelPart thisDoubleRightLock;

    private StorageRingChestProjectionRenderer() {
    }

    public static boolean isSupported(BlockState blockState) {
        return blockState.getBlock() instanceof ChestBlock
                || blockState.getBlock() instanceof EnderChestBlock;
    }

    public static void render(
            BlockState blockState,
            BlockPos position,
            PoseStack poseStack,
            MultiBufferSource bufferSource
    ) {
        initializeModels();
        ChestType chestType = blockState.hasProperty(ChestBlock.TYPE)
                ? blockState.getValue(ChestBlock.TYPE)
                : ChestType.SINGLE;
        Direction facing = blockState.hasProperty(ChestBlock.FACING)
                ? blockState.getValue(ChestBlock.FACING)
                : Direction.SOUTH;
        poseStack.translate(position.getX(), position.getY(), position.getZ());
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
        poseStack.translate(-0.5F, -0.5F, -0.5F);

        VertexConsumer vertexConsumer = getMaterial(blockState, chestType).buffer(
                bufferSource,
                RenderType::entityCutout
        );
        if (chestType == ChestType.LEFT) {
            renderParts(
                    poseStack,
                    vertexConsumer,
                    thisDoubleLeftLid,
                    thisDoubleLeftLock,
                    thisDoubleLeftBottom
            );
        } else if (chestType == ChestType.RIGHT) {
            renderParts(
                    poseStack,
                    vertexConsumer,
                    thisDoubleRightLid,
                    thisDoubleRightLock,
                    thisDoubleRightBottom
            );
        } else {
            renderParts(
                    poseStack,
                    vertexConsumer,
                    thisSingleLid,
                    thisSingleLock,
                    thisSingleBottom
            );
        }
    }

    private static void initializeModels() {
        if (thisSingleLid != null) {
            return;
        }

        ModelPart singleChest = Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.CHEST);
        thisSingleBottom = singleChest.getChild("bottom");
        thisSingleLid = singleChest.getChild("lid");
        thisSingleLock = singleChest.getChild("lock");

        ModelPart doubleLeftChest = Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.DOUBLE_CHEST_LEFT);
        thisDoubleLeftBottom = doubleLeftChest.getChild("bottom");
        thisDoubleLeftLid = doubleLeftChest.getChild("lid");
        thisDoubleLeftLock = doubleLeftChest.getChild("lock");

        ModelPart doubleRightChest = Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.DOUBLE_CHEST_RIGHT);
        thisDoubleRightBottom = doubleRightChest.getChild("bottom");
        thisDoubleRightLid = doubleRightChest.getChild("lid");
        thisDoubleRightLock = doubleRightChest.getChild("lock");
    }

    private static Material getMaterial(BlockState blockState, ChestType chestType) {
        if (blockState.getBlock() instanceof EnderChestBlock) {
            return Sheets.ENDER_CHEST_LOCATION;
        }
        if (isChristmas()) {
            return selectMaterial(
                    chestType,
                    Sheets.CHEST_XMAS_LOCATION,
                    Sheets.CHEST_XMAS_LOCATION_LEFT,
                    Sheets.CHEST_XMAS_LOCATION_RIGHT
            );
        }
        if (blockState.is(Blocks.TRAPPED_CHEST)) {
            return selectMaterial(
                    chestType,
                    Sheets.CHEST_TRAP_LOCATION,
                    Sheets.CHEST_TRAP_LOCATION_LEFT,
                    Sheets.CHEST_TRAP_LOCATION_RIGHT
            );
        }
        return selectMaterial(
                chestType,
                Sheets.CHEST_LOCATION,
                Sheets.CHEST_LOCATION_LEFT,
                Sheets.CHEST_LOCATION_RIGHT
        );
    }

    private static Material selectMaterial(
            ChestType chestType,
            Material singleMaterial,
            Material leftMaterial,
            Material rightMaterial
    ) {
        return switch (chestType) {
            case LEFT -> leftMaterial;
            case RIGHT -> rightMaterial;
            case SINGLE -> singleMaterial;
        };
    }

    private static boolean isChristmas() {
        Calendar calendar = Calendar.getInstance();
        return calendar.get(Calendar.MONTH) == Calendar.DECEMBER
                && calendar.get(Calendar.DAY_OF_MONTH) >= 24
                && calendar.get(Calendar.DAY_OF_MONTH) <= 26;
    }

    private static void renderParts(
            PoseStack poseStack,
            VertexConsumer vertexConsumer,
            ModelPart lid,
            ModelPart lock,
            ModelPart bottom
    ) {
        lid.xRot = 0.0F;
        lock.xRot = 0.0F;
        lid.render(poseStack, vertexConsumer, 0x00F000F0, OverlayTexture.NO_OVERLAY);
        lock.render(poseStack, vertexConsumer, 0x00F000F0, OverlayTexture.NO_OVERLAY);
        bottom.render(poseStack, vertexConsumer, 0x00F000F0, OverlayTexture.NO_OVERLAY);
    }
}
