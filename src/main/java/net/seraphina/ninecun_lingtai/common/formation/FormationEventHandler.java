package net.seraphina.ninecun_lingtai.common.formation;

import net.seraphina.ninecun_lingtai.ModMain;
import net.seraphina.ninecun_lingtai.common.block.FormationFlagBlock;
import net.seraphina.ninecun_lingtai.common.time.MonotonicCalendarUtility;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 阵法事件挂钩（自动发现，无需 ModMain 接线）：
 * 数据包 reload、服务端 tick、护罩吸伤（HIGH，先于防御法术结算）、
 * 阵旗破坏保护（拔错旗反冲——生门玩法的另一半）、白名单授权交互。
 */
@Mod.EventBusSubscriber(modid = ModMain.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class FormationEventHandler {

    private FormationEventHandler() {
    }

    @SubscribeEvent
    public static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(FormationManager.INSTANCE);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            FormationRuntime.tick(event.getServer());
        }
    }

    /** 护府阵吸伤：HIGH 优先级，先于防御法术（施工图 6.4）。 */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingHurt(LivingHurtEvent event) {
        FormationRuntime.absorbDamage(event);
    }

    /**
     * 阵旗破坏保护（阵禁篇 7.2 生门）：运转中的阵，非阵主拔旗——
     * 拔中生门放行（onRemove 里瘫痪全阵）；拔错旗取消破坏并灵力反冲 + 惊扰。
     * 阵主与创造模式放行（onRemove 里正常散阵）。
     */
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !(event.getState().getBlock() instanceof FormationFlagBlock)) {
            return;
        }
        FormationInstance instance = FormationRuntime.findInstanceByFlag(level, event.getPos());
        if (instance == null || instance.isParalyzed(level.getGameTime())) {
            return;
        }
        ServerPlayer player = event.getPlayer() instanceof ServerPlayer serverPlayer
                ? serverPlayer : null;
        if (player == null || player.getUUID().equals(instance.ownerUuid())
                || player.getAbilities().instabuild) {
            return;
        }
        int flagIndex = instance.flagPositions().indexOf(event.getPos());
        if (flagIndex == instance.lifeGateIndex()) {
            player.displayClientMessage(Component.translatableWithFallback(
                    "message.ninecun_lingtai.formation.life_gate_hit",
                    "这面旗应手而断——全阵灵机轰然溃散，六十息内规则皆失！"
            ), false);
            return;
        }
        event.setCanceled(true);
        applyWrongFlagBacklash(level, event.getPos(), player, instance);
    }

    private static void applyWrongFlagBacklash(
            ServerLevel level,
            BlockPos flagPos,
            ServerPlayer breaker,
            FormationInstance instance
    ) {
        breaker.hurt(level.damageSources().magic(), FormationTuning.WRONG_FLAG_BACKLASH_DAMAGE);
        breaker.knockback(
                FormationTuning.WRONG_FLAG_KNOCKBACK,
                flagPos.getX() + 0.5D - breaker.getX(),
                flagPos.getZ() + 0.5D - breaker.getZ()
        );
        breaker.hurtMarked = true;
        level.playSound(null, flagPos, SoundEvents.LIGHTNING_BOLT_IMPACT,
                SoundSource.BLOCKS, 0.8F, 1.6F);
        breaker.displayClientMessage(Component.translatableWithFallback(
                "message.ninecun_lingtai.formation.wrong_flag",
                "拔错了——旗上灵力喷薄反冲，将你掀了出去！"
        ), true);
        int disturb = instance.disturbCount() + 1;
        if (disturb >= 3) {
            instance.setDisturbCount(0);
            instance.setFlowHiddenUntilCalendarTick(
                    MonotonicCalendarUtility.getCalendarTick(level.getServer())
                            + FormationTuning.DISTURB_HIDE_FLOW_DAYS
                            * MonotonicCalendarUtility.TICKS_PER_DAY);
            breaker.displayClientMessage(Component.translatableWithFallback(
                    "message.ninecun_lingtai.formation.alert_mode",
                    "阵法察觉了反复的试探——灵力流转彻底隐没，巧破之路暂时断绝。"
            ), false);
        } else {
            instance.setDisturbCount(disturb);
        }
        FormationSavedData.get(level).setDirty();
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(instance.ownerUuid());
        if (owner != null) {
            owner.displayClientMessage(Component.translatableWithFallback(
                    "message.ninecun_lingtai.formation.alarm_flag_tamper",
                    "心头一悸——有人在试探你的阵旗（惊扰 %s/3）。",
                    disturb >= 3 ? 3 : disturb
            ), false);
            owner.playNotifySound(SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 0.75F, 1.1F);
        }
    }

    /** 白名单授权：阵主在自家灵阵域内潜行空手右键道友。 */
    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer owner)
                || !(event.getTarget() instanceof ServerPlayer target)
                || !owner.isShiftKeyDown()
                || !owner.getItemInHand(event.getHand()).isEmpty()) {
            return;
        }
        if (FormationInteractionService.toggleWhitelist(owner, target)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            FormationInteractionService.onPlayerLoggedOut(player);
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        FormationRuntime.clear();
        FormationInteractionService.clear();
    }
}
