package net.seraphina.ninecun_lingtai.common.event;

import com.mojang.brigadier.context.CommandContext;
import net.seraphina.ninecun_lingtai.ModMain;
import net.seraphina.ninecun_lingtai.common.behavior.PlayerBehaviorLedgerData;
import net.seraphina.ninecun_lingtai.common.data.MindDemonLineManager;
import net.seraphina.ninecun_lingtai.common.inner_demon.MindTrialManager;
import net.seraphina.ninecun_lingtai.common.inner_demon.MindTrialSavedData;
import net.seraphina.ninecun_lingtai.common.inner_demon.MindTrialSession;
import net.seraphina.ninecun_lingtai.common.inner_demon.ResolveChoice;
import net.seraphina.ninecun_lingtai.common.instance.MindRealmManager;
import net.seraphina.ninecun_lingtai.common.instance.MindRealmSavedData;
import net.seraphina.ninecun_lingtai.common.tribulation.TribulationSavedData;
import net.seraphina.ninecun_lingtai.utility.NotificationUtility;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 心魔即己的世界侧事件接线（FORGE 总线，全部新建、不动既有事件处理器）：
 * <ul>
 *   <li>心境权限弹出：非 owner 玩家出现在 mind_realm 且不在自己分格 → 立即弹回
 *       重生点（硬保证，指令 tp 进来也弹；旁观者豁免以便管理员观战）。</li>
 *   <li>启动扫描兜底（§4.2 / R4）：孤儿试炼会话按外劫横死兜底清理 + 登录补偿通知；
 *       孤儿分格立即释放。</li>
 *   <li>分格延迟清理队列驱动（release 后 100 tick 清平台）。</li>
 *   <li>台词库 reload listener 注册。</li>
 *   <li>{@code /mindtrial} 命令：决念三选一（聊天按钮的落点）+ debug 子命令
 *       （账本打印 / mindrealm enter / exit，P1、P3 验证用）。</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = ModMain.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class MindTrialEventHandler {
    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();

    /** debug mindrealm enter 分配的临时分格（玩家 → trialId），exit/退出时释放。 */
    private static final Map<UUID, UUID> DEBUG_TRIALS = new HashMap<>();

    private MindTrialEventHandler() {
    }

    @SubscribeEvent
    public static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(MindDemonLineManager.INSTANCE);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            MindRealmManager.tickPendingReleases(event.getServer());
        }
    }

    /** 心境权限：每 10 tick 检查一次（验收项：任何方式进入都会被立即弹出）。 */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END
                || !(event.player instanceof ServerPlayer player)
                || player.tickCount % 10 != 0
                || player.isSpectator()
                || !MindRealmManager.isMindRealm(player.level().dimension())) {
            return;
        }
        int pocketIndex = MindRealmManager.pocketIndexAt(player.blockPosition());
        boolean allowed = MindRealmSavedData.get(player.server)
                .getOccupant(pocketIndex)
                .map(occupant -> occupant.ownerUuid().equals(player.getUUID()))
                .orElse(false);
        if (!allowed) {
            MindRealmManager.ejectToRespawn(player);
            player.displayClientMessage(
                    Component.translatableWithFallback(
                            "message.ninecun_lingtai.mind_realm_no_entry",
                            "此乃他人灵台方寸之地，不容外人踏足"),
                    false
            );
        }
    }

    /** 启动扫描兜底：清理崩溃残留（R4 / §4.2）。 */
    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        MindTrialSavedData trials = MindTrialSavedData.get(server);
        TribulationSavedData tribulations = TribulationSavedData.get(server);
        MindRealmSavedData realm = MindRealmSavedData.get(server);

        // 1) 孤儿试炼会话（天劫会话已不存在）→ 移除 + 释放分格 + 登录补偿通知。
        for (MindTrialSession trial : trials.getAllSessions()) {
            if (tribulations.getSession(trial.ownerUuid()) == null) {
                LOGGER.warn(
                        "[mind_trial] startup scan: orphan trial {} of {} settled as collapse",
                        trial.trialId(),
                        trial.ownerUuid()
                );
                trials.removeSession(trial.ownerUuid());
                trials.addPendingCompensation(trial.ownerUuid());
                realm.findPocketByTrial(trial.trialId())
                        .ifPresent(pocketIndex -> MindRealmManager.releaseImmediately(server, pocketIndex));
            }
        }
        // 2) 孤儿分格（没有对应试炼会话）→ 立即释放（含平台清空）。
        for (int pocketIndex : realm.getOccupiedPocketIndices()) {
            boolean orphan = realm.getOccupant(pocketIndex)
                    .map(occupant -> trials.getByTrialId(occupant.trialId()) == null
                            && !DEBUG_TRIALS.containsValue(occupant.trialId()))
                    .orElse(true);
            if (orphan) {
                LOGGER.warn("[mind_trial] startup scan: releasing orphan pocket {}", pocketIndex);
                MindRealmManager.releaseImmediately(server, pocketIndex);
            }
        }
    }

    /** 信箱式补偿说明（§4.2）：孤儿会话 owner 登录时告知兜底结算。 */
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (MindTrialSavedData.get(player.server).consumePendingCompensation(player.getUUID())) {
            NotificationUtility.send(
                    player,
                    Component.translatableWithFallback(
                            "message.ninecun_lingtai.mind_trial_collapse_title", "心境崩塌"),
                    Component.translatableWithFallback(
                            "message.ninecun_lingtai.mind_trial_compensation",
                            "服务器异常中断了你的心魔试炼，本次已按兜底结算，未另行惩罚"),
                    20 * 10
            );
        }
    }

    // ------------------------------------------------------------------ 命令

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("mindtrial")
                .then(Commands.literal("resolve")
                        .then(Commands.literal("sever")
                                .executes(context -> resolve(context, ResolveChoice.SEVER)))
                        .then(Commands.literal("hold")
                                .executes(context -> resolve(context, ResolveChoice.HOLD)))
                        .then(Commands.literal("accept")
                                .executes(context -> resolve(context, ResolveChoice.ACCEPT))))
                .then(Commands.literal("debug")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("ledger").executes(MindTrialEventHandler::debugLedger))
                        .then(Commands.literal("mindrealm")
                                .then(Commands.literal("enter").executes(MindTrialEventHandler::debugEnter))
                                .then(Commands.literal("exit").executes(MindTrialEventHandler::debugExit))))
        );
    }

    private static int resolve(CommandContext<CommandSourceStack> context, ResolveChoice choice) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) {
            return 0;
        }
        if (!MindTrialManager.submitResolve(player, choice)) {
            context.getSource().sendFailure(Component.translatableWithFallback(
                    "message.ninecun_lingtai.mind_trial_not_resolving",
                    "当下并非决念之时"));
            return 0;
        }
        return 1;
    }

    /** P1 验证：debug 命令打印账本。 */
    private static int debugLedger(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) {
            return 0;
        }
        context.getSource().sendSuccess(
                () -> Component.literal(PlayerBehaviorLedgerData.describe(player)),
                false
        );
        return 1;
    }

    /** P3 验证：无心魔，仅进出心境分格。 */
    private static int debugEnter(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) {
            return 0;
        }
        UUID trialId = UUID.randomUUID();
        MindRealmManager.MindArena arena =
                MindRealmManager.allocate(player.server, trialId, player.getUUID());
        if (!MindRealmManager.enter(player, arena)) {
            MindRealmManager.release(player.server, trialId);
            context.getSource().sendFailure(Component.literal("心境维度不可用"));
            return 0;
        }
        DEBUG_TRIALS.put(player.getUUID(), trialId);
        context.getSource().sendSuccess(
                () -> Component.literal("已入心境分格 #" + arena.pocketIndex()),
                false
        );
        return 1;
    }

    private static int debugExit(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) {
            return 0;
        }
        MindRealmManager.ejectToRespawn(player);
        UUID trialId = DEBUG_TRIALS.remove(player.getUUID());
        if (trialId != null) {
            MindRealmManager.release(player.server, trialId);
        }
        context.getSource().sendSuccess(() -> Component.literal("已出心境"), false);
        return 1;
    }
}
