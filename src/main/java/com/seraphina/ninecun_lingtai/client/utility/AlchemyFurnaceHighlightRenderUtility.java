package com.seraphina.ninecun_lingtai.client.utility;

import com.seraphina.ninecun_lingtai.utility.AlchemyFurnaceStructureUtility;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderHighlightEvent;

public final class AlchemyFurnaceHighlightRenderUtility {
    private static final double FURNACE_CENTER_COORDINATE = 0.5D;
    private static final double OCTAGON_CORNER_CUT_RATIO = 0.2928932188D;
    private static final float RED = 0.0F;
    private static final float GREEN = 0.0F;
    private static final float BLUE = 0.0F;
    private static final float ALPHA = 0.4F;

    public static void render(RenderHighlightEvent.Block event) {

        BlockPos furnacePosition = AlchemyFurnaceStructureUtility.getMainFurnacePosition(
                event.getCamera().getEntity().level(),
                event.getTarget().getBlockPos()
        );
        if (furnacePosition == null) {
            return;
        }

        Vec3 cameraPosition = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        VertexConsumer vertexConsumer = event.getMultiBufferSource().getBuffer(RenderType.lines());
        renderFurnaceOutline(poseStack, vertexConsumer, furnacePosition, cameraPosition);
        event.setCanceled(true);
    }

    private static void renderFurnaceOutline(
            PoseStack poseStack,
            VertexConsumer vertexConsumer,
            BlockPos furnacePosition,
            Vec3 cameraPosition
    ) {
        AlchemyFurnaceStructureUtility.FurnaceOutlinePoint previousPoint = null;
        for (AlchemyFurnaceStructureUtility.FurnaceOutlinePoint outlinePoint
                : AlchemyFurnaceStructureUtility.getFurnaceOutlinePoints()) {
            renderOctagonalRing(
                    poseStack,
                    vertexConsumer,
                    furnacePosition,
                    cameraPosition,
                    outlinePoint.yCoordinate(),
                    outlinePoint.horizontalRadius()
            );
            if (previousPoint != null) {
                renderConnectingEdges(
                        poseStack,
                        vertexConsumer,
                        furnacePosition,
                        cameraPosition,
                        previousPoint,
                        outlinePoint
                );
            }
            previousPoint = outlinePoint;
        }
        renderFurnaceHandleOutline(
                poseStack,
                vertexConsumer,
                furnacePosition,
                cameraPosition,
                -1.0D,
                -0.4D
        );
        renderFurnaceHandleOutline(
                poseStack,
                vertexConsumer,
                furnacePosition,
                cameraPosition,
                2.0D,
                1.4D
        );
    }

    private static void renderOctagonalRing(
            PoseStack poseStack,
            VertexConsumer vertexConsumer,
            BlockPos furnacePosition,
            Vec3 cameraPosition,
            double yCoordinate,
            double horizontalRadius
    ) {
        for (int index = 0; index < 8; index++) {
            OutlinePoint startPoint = getOctagonPoint(horizontalRadius, index);
            OutlinePoint endPoint = getOctagonPoint(horizontalRadius, (index + 1) % 8);
            renderLine(
                    poseStack,
                    vertexConsumer,
                    furnacePosition,
                    cameraPosition,
                    startPoint.xCoordinate(),
                    yCoordinate,
                    startPoint.zCoordinate(),
                    endPoint.xCoordinate(),
                    yCoordinate,
                    endPoint.zCoordinate()
            );
        }
    }

    private static void renderConnectingEdges(
            PoseStack poseStack,
            VertexConsumer vertexConsumer,
            BlockPos furnacePosition,
            Vec3 cameraPosition,
            AlchemyFurnaceStructureUtility.FurnaceOutlinePoint lowerPoint,
            AlchemyFurnaceStructureUtility.FurnaceOutlinePoint upperPoint
    ) {
        for (int index = 0; index < 8; index++) {
            OutlinePoint lowerOutlinePoint = getOctagonPoint(lowerPoint.horizontalRadius(), index);
            OutlinePoint upperOutlinePoint = getOctagonPoint(upperPoint.horizontalRadius(), index);
            renderLine(
                    poseStack,
                    vertexConsumer,
                    furnacePosition,
                    cameraPosition,
                    lowerOutlinePoint.xCoordinate(),
                    lowerPoint.yCoordinate(),
                    lowerOutlinePoint.zCoordinate(),
                    upperOutlinePoint.xCoordinate(),
                    upperPoint.yCoordinate(),
                    upperOutlinePoint.zCoordinate()
            );
        }
    }

    private static void renderFurnaceHandleOutline(
            PoseStack poseStack,
            VertexConsumer vertexConsumer,
            BlockPos furnacePosition,
            Vec3 cameraPosition,
            double outerXCoordinate,
            double innerXCoordinate
    ) {
        double minimumY = 0.75D;
        double maximumY = 2.125D;
        double minimumZ = 0.28125D;
        double maximumZ = 0.71875D;
        renderLine(
                poseStack,
                vertexConsumer,
                furnacePosition,
                cameraPosition,
                outerXCoordinate,
                minimumY,
                minimumZ,
                outerXCoordinate,
                maximumY,
                minimumZ
        );
        renderLine(
                poseStack,
                vertexConsumer,
                furnacePosition,
                cameraPosition,
                outerXCoordinate,
                minimumY,
                maximumZ,
                outerXCoordinate,
                maximumY,
                maximumZ
        );
        renderLine(
                poseStack,
                vertexConsumer,
                furnacePosition,
                cameraPosition,
                outerXCoordinate,
                minimumY,
                minimumZ,
                innerXCoordinate,
                minimumY,
                minimumZ
        );
        renderLine(
                poseStack,
                vertexConsumer,
                furnacePosition,
                cameraPosition,
                outerXCoordinate,
                minimumY,
                maximumZ,
                innerXCoordinate,
                minimumY,
                maximumZ
        );
        renderLine(
                poseStack,
                vertexConsumer,
                furnacePosition,
                cameraPosition,
                outerXCoordinate,
                maximumY,
                minimumZ,
                innerXCoordinate,
                maximumY,
                minimumZ
        );
        renderLine(
                poseStack,
                vertexConsumer,
                furnacePosition,
                cameraPosition,
                outerXCoordinate,
                maximumY,
                maximumZ,
                innerXCoordinate,
                maximumY,
                maximumZ
        );
    }

    private static OutlinePoint getOctagonPoint(double horizontalRadius, int index) {
        double cornerCut = horizontalRadius * OCTAGON_CORNER_CUT_RATIO;
        double innerRadius = horizontalRadius - cornerCut;
        return switch (index) {
            case 0 -> new OutlinePoint(FURNACE_CENTER_COORDINATE - innerRadius, FURNACE_CENTER_COORDINATE - horizontalRadius);
            case 1 -> new OutlinePoint(FURNACE_CENTER_COORDINATE + innerRadius, FURNACE_CENTER_COORDINATE - horizontalRadius);
            case 2 -> new OutlinePoint(FURNACE_CENTER_COORDINATE + horizontalRadius, FURNACE_CENTER_COORDINATE - innerRadius);
            case 3 -> new OutlinePoint(FURNACE_CENTER_COORDINATE + horizontalRadius, FURNACE_CENTER_COORDINATE + innerRadius);
            case 4 -> new OutlinePoint(FURNACE_CENTER_COORDINATE + innerRadius, FURNACE_CENTER_COORDINATE + horizontalRadius);
            case 5 -> new OutlinePoint(FURNACE_CENTER_COORDINATE - innerRadius, FURNACE_CENTER_COORDINATE + horizontalRadius);
            case 6 -> new OutlinePoint(FURNACE_CENTER_COORDINATE - horizontalRadius, FURNACE_CENTER_COORDINATE + innerRadius);
            default -> new OutlinePoint(FURNACE_CENTER_COORDINATE - horizontalRadius, FURNACE_CENTER_COORDINATE - innerRadius);
        };
    }

    private static void renderLine(
            PoseStack poseStack,
            VertexConsumer vertexConsumer,
            BlockPos furnacePosition,
            Vec3 cameraPosition,
            double startX,
            double startY,
            double startZ,
            double endX,
            double endY,
            double endZ
    ) {
        float normalX = (float) (endX - startX);
        float normalY = (float) (endY - startY);
        float normalZ = (float) (endZ - startZ);
        float normalLength = (float) Math.sqrt(normalX * normalX + normalY * normalY + normalZ * normalZ);
        normalX /= normalLength;
        normalY /= normalLength;
        normalZ /= normalLength;

        double cameraRelativeStartX = furnacePosition.getX() + startX - cameraPosition.x;
        double cameraRelativeStartY = furnacePosition.getY() + startY - cameraPosition.y;
        double cameraRelativeStartZ = furnacePosition.getZ() + startZ - cameraPosition.z;
        double cameraRelativeEndX = furnacePosition.getX() + endX - cameraPosition.x;
        double cameraRelativeEndY = furnacePosition.getY() + endY - cameraPosition.y;
        double cameraRelativeEndZ = furnacePosition.getZ() + endZ - cameraPosition.z;
        PoseStack.Pose pose = poseStack.last();
        vertexConsumer.vertex(
                        pose.pose(),
                        (float) cameraRelativeStartX,
                        (float) cameraRelativeStartY,
                        (float) cameraRelativeStartZ
                )
                .color(RED, GREEN, BLUE, ALPHA)
                .normal(pose.normal(), normalX, normalY, normalZ)
                .endVertex();
        vertexConsumer.vertex(
                        pose.pose(),
                        (float) cameraRelativeEndX,
                        (float) cameraRelativeEndY,
                        (float) cameraRelativeEndZ
                )
                .color(RED, GREEN, BLUE, ALPHA)
                .normal(pose.normal(), normalX, normalY, normalZ)
                .endVertex();
    }

    private record OutlinePoint(double xCoordinate, double zCoordinate) {
    }
}
