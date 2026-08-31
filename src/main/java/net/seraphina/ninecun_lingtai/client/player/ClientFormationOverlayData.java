package net.seraphina.ninecun_lingtai.client.player;

import net.seraphina.ninecun_lingtai.common.formation.FormationFlowPacket;
import net.seraphina.ninecun_lingtai.common.formation.FormationGuidePacket;
import net.minecraft.client.Minecraft;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * 客户端阵法覆盖层数据（纯渲染缓存，无任何判定）：
 * 幻影指引帧（持阵盘潜行时服务端推送）+ 旗间灵力流帧（神识开启时推送）。
 * 均带过期时间，服务端停发即自然消隐。
 */
public final class ClientFormationOverlayData {
    private static final int GUIDE_LIFETIME_TICKS = 15;

    @Nullable
    private static GuideFrame guideFrame;
    private static final Map<Long, FlowFrame> FLOW_FRAMES = new HashMap<>();

    public record GuideFrame(FormationGuidePacket packet, long expiryGameTime) {
    }

    public record FlowFrame(FormationFlowPacket packet, long expiryGameTime) {
    }

    private ClientFormationOverlayData() {
    }

    public static void acceptGuide(FormationGuidePacket packet) {
        long gameTime = currentGameTime();
        guideFrame = new GuideFrame(packet, gameTime + GUIDE_LIFETIME_TICKS);
    }

    public static void acceptFlow(FormationFlowPacket packet) {
        long gameTime = currentGameTime();
        FLOW_FRAMES.put(packet.discPos().asLong(),
                new FlowFrame(packet, gameTime + packet.durationTicks()));
    }

    @Nullable
    public static FormationGuidePacket getActiveGuide() {
        long gameTime = currentGameTime();
        if (guideFrame == null || gameTime > guideFrame.expiryGameTime()) {
            guideFrame = null;
            return null;
        }
        return guideFrame.packet();
    }

    public static Map<Long, FlowFrame> getActiveFlows() {
        long gameTime = currentGameTime();
        Iterator<Map.Entry<Long, FlowFrame>> iterator = FLOW_FRAMES.entrySet().iterator();
        while (iterator.hasNext()) {
            if (gameTime > iterator.next().getValue().expiryGameTime()) {
                iterator.remove();
            }
        }
        return FLOW_FRAMES;
    }

    public static void clear() {
        guideFrame = null;
        FLOW_FRAMES.clear();
    }

    private static long currentGameTime() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.level == null ? 0L : minecraft.level.getGameTime();
    }
}
