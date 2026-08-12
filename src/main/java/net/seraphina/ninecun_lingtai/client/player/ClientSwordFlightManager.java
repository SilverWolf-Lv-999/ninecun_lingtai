package net.seraphina.ninecun_lingtai.client.player;

import net.seraphina.ninecun_lingtai.common.network.ModNetwork;
import net.seraphina.ninecun_lingtai.common.network.packet.DismountSwordFlightPacket;
import net.seraphina.ninecun_lingtai.common.network.packet.MountSwordFlightPacket;
import net.seraphina.ninecun_lingtai.common.spell.SwordFlightManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.InputEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 客户端御剑状态：记录谁在御剑（用于脚下渲染飞剑），并对本地玩家做两件事——
 * 贴地时把人托在方块上方 0.2 格内不再下坠；贴地时双击潜行发包收剑。
 */
public final class ClientSwordFlightManager {
    /** 与 SwordFlightManager.HOVER_GAP 一致。 */
    private static final double HOVER_GAP = 0.2D;
    /** 允许双击收剑的离地高度（贴地悬浮时约为 HOVER_GAP，稍加余量）。 */
    private static final double DISMOUNT_GROUND_DISTANCE = 0.35D;
    private static final long DOUBLE_TAP_WINDOW_MILLISECONDS = 300L;
    /** 御剑时身体相对视线侧转的角度。 */
    private static final float BODY_SIDE_ANGLE = 30.0F;

    private static final Map<UUID, ItemStack> FLYING_SWORDS = new ConcurrentHashMap<>();

    private static long lastShiftPressTime;

    private ClientSwordFlightManager() {
    }

    public static void setFlying(UUID playerId, boolean flying, ItemStack sword) {
        if (flying) {
            FLYING_SWORDS.put(playerId, sword);
        } else {
            FLYING_SWORDS.remove(playerId);
        }
    }

    public static boolean isFlying(UUID playerId) {
        return FLYING_SWORDS.containsKey(playerId);
    }

    public static ItemStack getSword(UUID playerId) {
        return FLYING_SWORDS.getOrDefault(playerId, ItemStack.EMPTY);
    }

    /**
     * 使用客户端原始潜行键处理登剑交互，避免依赖可能被其他客户端模组改写的移动输入状态。
     */
    public static boolean handleInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null
                || minecraft.screen != null
                || !event.isUseItem()
                || event.getHand() != InteractionHand.MAIN_HAND
                || !minecraft.options.keyShift.isDown()
                || !player.onGround()
                || isFlying(player.getUUID())
                || !(player.getMainHandItem().getItem() instanceof SwordItem)
                || !ClientTechniqueManager.isInitialized()
                || !ClientTechniqueManager.isLearned(SwordFlightManager.TECHNIQUE_ID)) {
            return false;
        }
        ModNetwork.sendToServer(MountSwordFlightPacket.INSTANCE);
        event.setSwingHand(false);
        event.setCanceled(true);
        return true;
    }

    public static boolean isLocalPlayerFlying() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null && isFlying(player.getUUID());
    }

    /** 每个客户端刻末维护御剑姿态与本地玩家的御剑手感。 */
    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null && !FLYING_SWORDS.isEmpty()) {
            for (Player flyingPlayer : minecraft.level.players()) {
                if (isFlying(flyingPlayer.getUUID())) {
                    enforceFlightPose(flyingPlayer);
                }
            }
        }
        LocalPlayer player = minecraft.player;
        if (player == null || !isFlying(player.getUUID())) {
            return;
        }
        // 御剑期间飞行不落：贴地时剑气托身，被原版判定落地也强行续飞。
        if (!player.getAbilities().flying && player.getAbilities().mayfly) {
            player.getAbilities().flying = true;
        }
        double groundDistance = getGroundDistance(player);
        if (groundDistance < HOVER_GAP) {
            Vec3 movement = player.getDeltaMovement();
            player.setPos(player.getX(), player.getY() + (HOVER_GAP - groundDistance), player.getZ());
            player.setDeltaMovement(movement.x, Math.max(0.0D, movement.y), movement.z);
        }
    }

    /**
     * 御剑者永远持站立之姿：不下蹲、腿不摆动，身体微微侧转而头朝正前，如剑仙临风。
     * 每刻在实体逻辑之后覆写渲染所用的姿态字段，对本地与远端玩家一并生效。
     */
    private static void enforceFlightPose(Player player) {
        player.setPose(Pose.STANDING);
        player.yBodyRot = player.yHeadRot + BODY_SIDE_ANGLE;
        player.yBodyRotO = player.yHeadRotO + BODY_SIDE_ANGLE;
        // 连清两次，把行走动画的当前速度与上一刻速度都归零，双腿便纹丝不动。
        player.walkAnimation.update(0.0F, 1.0F);
        player.walkAnimation.update(0.0F, 1.0F);
    }

    /** 御剑贴地时双击潜行收剑；由按键事件调用，只记录按下沿。 */
    public static void onShiftPressed() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !isFlying(player.getUUID())) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastShiftPressTime <= DOUBLE_TAP_WINDOW_MILLISECONDS
                && getGroundDistance(player) <= DISMOUNT_GROUND_DISTANCE) {
            lastShiftPressTime = 0L;
            ModNetwork.sendToServer(DismountSwordFlightPacket.INSTANCE);
            return;
        }
        lastShiftPressTime = now;
    }

    public static void removePlayer(UUID playerId) {
        FLYING_SWORDS.remove(playerId);
    }

    public static void reset() {
        FLYING_SWORDS.clear();
        lastShiftPressTime = 0L;
    }

    private static double getGroundDistance(LocalPlayer player) {
        Vec3 feet = player.position();
        HitResult hit = player.level().clip(new ClipContext(
                feet,
                feet.add(0.0D, -3.0D, 0.0D),
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                player
        ));
        if (hit.getType() == HitResult.Type.MISS) {
            return Double.MAX_VALUE;
        }
        return Mth.clamp(feet.y - hit.getLocation().y, 0.0D, Double.MAX_VALUE);
    }
}
