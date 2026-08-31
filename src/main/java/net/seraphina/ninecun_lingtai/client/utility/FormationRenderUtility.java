package net.seraphina.ninecun_lingtai.client.utility;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.seraphina.ninecun_lingtai.client.player.ClientFormationOverlayData;
import net.seraphina.ninecun_lingtai.common.formation.FormationFlowPacket;
import net.seraphina.ninecun_lingtai.common.formation.FormationGuidePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

import java.util.Map;

/**
 * 阵法覆盖层渲染（照 SoulSenseRenderUtility 的 DEBUG_LINES 口径）：
 * <ul>
 *   <li>旗位幻影：期望点线框盒——对齐变绿、未对齐按旗位五行着色（灰 = 不限）；
 *       神识开启且有实旗时画"期望↔实旗"偏差连线；</li>
 *   <li>旗间灵力流：暗色基线 + 三段游走亮痕；生门旗出流段的亮痕反向游走——
 *       不做任何高亮标记，逆流全凭肉眼（阵禁篇 7.2）。</li>
 * </ul>
 */
public final class FormationRenderUtility {
    private static final int[] ELEMENT_COLORS = {0xFFD75E, 0x4FC46A, 0x4FA3E8, 0xE85F45, 0xC9973F};
    private static final int NEUTRAL_COLOR = 0xB0B4BF;
    private static final int ALIGNED_COLOR = 0x53E077;
    private static final int FLOW_BASE_COLOR = 0x3D74B8;
    private static final int FLOW_BRIGHT_COLOR = 0x9BD7FF;
    /** 亮痕游走周期（tick）。 */
    private static final float FLOW_CYCLE_TICKS = 50.0F;

    private FormationRenderUtility() {
    }

    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        FormationGuidePacket guide = ClientFormationOverlayData.getActiveGuide();
        Map<Long, ClientFormationOverlayData.FlowFrame> flows =
                ClientFormationOverlayData.getActiveFlows();
        if (guide == null && flows.isEmpty()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }

        Vec3 cameraPosition = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(-cameraPosition.x, -cameraPosition.y, -cameraPosition.z);
        Matrix4f pose = poseStack.last().pose();

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.lineWidth(2.0F);

        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);

        if (guide != null) {
            renderGuide(buffer, pose, guide);
        }
        float animationTime = minecraft.level.getGameTime() + event.getPartialTick();
        for (ClientFormationOverlayData.FlowFrame frame : flows.values()) {
            renderFlow(buffer, pose, frame.packet(), animationTime);
        }

        BufferUploader.drawWithShader(buffer.end());

        RenderSystem.lineWidth(1.0F);
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        poseStack.popPose();
    }

    private static void renderGuide(BufferBuilder buffer, Matrix4f pose, FormationGuidePacket guide) {
        for (FormationGuidePacket.Entry entry : guide.entries()) {
            int color = entry.aligned() ? ALIGNED_COLOR
                    : entry.elementIndex() >= 0 && entry.elementIndex() < ELEMENT_COLORS.length
                            ? ELEMENT_COLORS[entry.elementIndex()]
                            : NEUTRAL_COLOR;
            BlockPos expected = entry.expectedPos();
            AABB ghostBox = new AABB(expected).inflate(-0.12D);
            drawBox(buffer, pose, ghostBox, color, entry.aligned() ? 0.95F : 0.6F);
            // 中柱竖线：远处也认得出这是"旗"的幻影
            addLine(buffer, pose,
                    new Vec3(expected.getX() + 0.5D, expected.getY(), expected.getZ() + 0.5D),
                    new Vec3(expected.getX() + 0.5D, expected.getY() + 1.4D, expected.getZ() + 0.5D),
                    color, 0.8F);
            if (entry.actualPos() != null) {
                // 神识开启时的精确偏差连线（服务端才会带 actualPos）
                addLine(buffer, pose,
                        Vec3.atCenterOf(expected),
                        Vec3.atCenterOf(entry.actualPos()),
                        0xE8D44F, 0.9F);
            }
        }
    }

    private static void renderFlow(
            BufferBuilder buffer,
            Matrix4f pose,
            FormationFlowPacket packet,
            float animationTime
    ) {
        for (FormationFlowPacket.Segment segment : packet.segments()) {
            Vec3 from = new Vec3(segment.fromX(), segment.fromY(), segment.fromZ());
            Vec3 to = new Vec3(segment.toX(), segment.toY(), segment.toZ());
            addLine(buffer, pose, from, to, FLOW_BASE_COLOR, 0.35F);
            for (int dash = 0; dash < 3; dash++) {
                float phase = Mth.frac(animationTime / FLOW_CYCLE_TICKS + dash / 3.0F);
                if (segment.reversed()) {
                    phase = 1.0F - phase;
                }
                float head = Math.min(1.0F, phase + 0.08F);
                addLine(buffer, pose,
                        from.lerp(to, phase),
                        from.lerp(to, head),
                        FLOW_BRIGHT_COLOR, 0.9F);
            }
        }
    }

    private static void drawBox(BufferBuilder buffer, Matrix4f pose, AABB box, int color, float alpha) {
        Vec3[] corners = {
                new Vec3(box.minX, box.minY, box.minZ), new Vec3(box.maxX, box.minY, box.minZ),
                new Vec3(box.maxX, box.minY, box.maxZ), new Vec3(box.minX, box.minY, box.maxZ),
                new Vec3(box.minX, box.maxY, box.minZ), new Vec3(box.maxX, box.maxY, box.minZ),
                new Vec3(box.maxX, box.maxY, box.maxZ), new Vec3(box.minX, box.maxY, box.maxZ)
        };
        int[][] edges = {
                {0, 1}, {1, 2}, {2, 3}, {3, 0},
                {4, 5}, {5, 6}, {6, 7}, {7, 4},
                {0, 4}, {1, 5}, {2, 6}, {3, 7}
        };
        for (int[] edge : edges) {
            addLine(buffer, pose, corners[edge[0]], corners[edge[1]], color, alpha);
        }
    }

    private static void addLine(
            BufferBuilder buffer,
            Matrix4f pose,
            Vec3 start,
            Vec3 end,
            int color,
            float alpha
    ) {
        float red = (color >> 16 & 0xFF) / 255.0F;
        float green = (color >> 8 & 0xFF) / 255.0F;
        float blue = (color & 0xFF) / 255.0F;
        buffer.vertex(pose, (float) start.x, (float) start.y, (float) start.z)
                .color(red, green, blue, alpha).endVertex();
        buffer.vertex(pose, (float) end.x, (float) end.y, (float) end.z)
                .color(red, green, blue, alpha).endVertex();
    }
}
