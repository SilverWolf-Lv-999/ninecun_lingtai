package net.seraphina.ninecun_lingtai.common.behavior;

import net.seraphina.ninecun_lingtai.ModMain;
import net.seraphina.ninecun_lingtai.common.creature.CreatureCultivationData;
import net.seraphina.ninecun_lingtai.common.data.AlchemyRecipeManager;
import net.seraphina.ninecun_lingtai.common.inner_demon.MindTrialManager;
import net.seraphina.ninecun_lingtai.common.item.PillItem;
import net.seraphina.ninecun_lingtai.common.player.PlayerPillToxinData;
import net.seraphina.ninecun_lingtai.common.time.MonotonicCalendarUtility;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 行为账本事件驱动采样（心魔即己 T15，设计文档 §5.2）。全部事件驱动、不逐 tick。
 *
 * <p>采样点覆盖情况：</p>
 * <ul>
 *   <li>击杀：{@link LivingDeathEvent}，分类顺序与 {@code SoulSenseManager.classifyEntity}
 *       一致（村民 → 修士 → 玩家 → 其余野兽/敌对）。</li>
 *   <li>服丹：{@link LivingEntityUseItemEvent.Finish} 判 {@link PillItem}。注意：现状
 *       {@code PillItem.use()} 为即时消耗（不走 startUsingItem），该事件当前不会触发——
 *       真正的一行接线需加在 {@code PillItem.consumePill}（见开发日志"需集中接线"），
 *       本 hook 保留以兼容将来丹药改为持续使用式。</li>
 *   <li>施法：{@code CastSpellPacket.handle}（本系统可改文件）内采样。</li>
 *   <li>天劫失败：{@code TribulationManager.fail} 统一入口内采样。</li>
 *   <li>夺舍成功/被夺舍：不改 {@code PlayerPossessionManager}，接线清单记
 *       {@code PlayerBehaviorLedgerData.recordPossess / recordPossessedBy}。</li>
 *   <li>破境失败：不改 breakthrough 文件，接线清单记
 *       {@code PlayerBehaviorLedgerData.recordBreakthroughFail}。</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = ModMain.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BehaviorLedgerEventHandler {

    /** 嗜杀心障"7 修真日零击杀"化解的日界检查间隔（tick）。 */
    private static final int BLOODTHIRST_CHECK_INTERVAL_TICKS = 200;
    /** 嗜杀心障化解所需的零击杀修真日数（M1 简化口径）。 */
    private static final long BLOODTHIRST_RESOLVE_DAYS = 7L;

    private BehaviorLedgerEventHandler() {
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer killer)
                || killer.level().isClientSide()) {
            return;
        }
        LivingEntity victim = event.getEntity();
        if (victim == killer) {
            return;
        }
        PlayerBehaviorLedgerData.recordKill(killer, classifyKill(victim));
    }

    /**
     * 嗜杀心障化解（接线清单第 10 条，M1 简化口径）：带心障期间连续 7 个修真日
     * 零击杀即化解。低频轮询（每 {@value #BLOODTHIRST_CHECK_INTERVAL_TICKS} tick），
     * 基线 {@code LastKillCalendarDay} 由击杀采样与获得心障时写入。
     */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END
                || !(event.player instanceof ServerPlayer player)
                || player.tickCount % BLOODTHIRST_CHECK_INTERVAL_TICKS != 0
                || !PlayerBehaviorLedgerData.hasActiveMindScar(
                        player,
                        PlayerBehaviorLedgerData.MindScarIds.BLOODTHIRST)) {
            return;
        }
        long currentDay = MonotonicCalendarUtility.getCalendarDay(player.server);
        long lastKillDay = PlayerBehaviorLedgerData.getLastKillCalendarDay(player);
        if (lastKillDay < 0L) {
            // 旧档无基线：从当日起算 7 日窗口。
            PlayerBehaviorLedgerData.setLastKillCalendarDay(player, currentDay);
            return;
        }
        if (currentDay - lastKillDay >= BLOODTHIRST_RESOLVE_DAYS) {
            MindTrialManager.resolveMindScar(
                    player,
                    PlayerBehaviorLedgerData.MindScarIds.BLOODTHIRST
            );
        }
    }

    @SubscribeEvent
    public static void onUseItemFinish(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !(event.getItem().getItem() instanceof PillItem pillItem)) {
            return;
        }
        boolean soulPill = isSoulPill(pillItem);
        boolean toxinBurst =
                PlayerPillToxinData.getToxin(player) >= PlayerPillToxinData.OUTBREAK_THRESHOLD;
        PlayerBehaviorLedgerData.recordPillUse(player, soulPill, toxinBurst);
        MindTrialManager.onPillUse(player);
    }

    /** 分类逻辑与 SoulSenseManager.classifyEntity 的判断顺序一致（设计文档 §5.2）。 */
    private static PlayerBehaviorLedgerData.KillCategory classifyKill(LivingEntity victim) {
        if (victim instanceof AbstractVillager) {
            return PlayerBehaviorLedgerData.KillCategory.VILLAGER;
        }
        if (CreatureCultivationData.getStage(victim).isPresent()) {
            return PlayerBehaviorLedgerData.KillCategory.CULTIVATOR;
        }
        if (victim instanceof Player) {
            // M1 先按"玩家"计 ally（同队/同宗判定待队伍系统落地）。
            return PlayerBehaviorLedgerData.KillCategory.ALLY;
        }
        return PlayerBehaviorLedgerData.KillCategory.BEAST;
    }

    private static boolean isSoulPill(PillItem pillItem) {
        return AlchemyRecipeManager.INSTANCE
                .getRecipeForOutputItem(BuiltInRegistries.ITEM.getKey(pillItem))
                .map(recipe -> recipe.effects().stream()
                        .anyMatch(effect -> "soul_tempering".equals(effect.type())))
                .orElse(false);
    }
}
