package net.seraphina.ninecun_lingtai.common.spell.palm_buddha;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.seraphina.ninecun_lingtai.api.cultivation.CultivationApi;
import net.seraphina.ninecun_lingtai.api.cultivation.CultivationRealm;
import net.seraphina.ninecun_lingtai.api.cultivation.CultivationStage;
import net.seraphina.ninecun_lingtai.api.spell.Spell;
import net.seraphina.ninecun_lingtai.api.spell.SpellCastContext;
import net.seraphina.ninecun_lingtai.common.realm.ThreeThousandRealmManager;
import net.minecraft.network.chat.Component;

import java.util.Objects;

/**
 * 掌中佛国的第一阶段施法实现：准心单选在线玩家。
 *
 * <p>服务器会重新解析目标、验证距离与视线，并再次执行境界限制，客户端只负责
 * 提交准心锁定的 UUID，不能直接指定迁移位置或伪造目标境界。</p>
 */
public final class PalmBuddhaSpell extends Spell {
    private static final double MAXIMUM_TARGET_DISTANCE = 32.0D;
    private static final double MAXIMUM_TARGET_DISTANCE_SQUARED =
            MAXIMUM_TARGET_DISTANCE * MAXIMUM_TARGET_DISTANCE;

    @Override
    public boolean cast(ServerPlayer player, SpellCastContext context) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(context, "context");
        if (context.targetPlayerId() == null) {
            player.displayClientMessage(Component.literal("掌中佛国须先以神念锁定一人"), true);
            return false;
        }

        ServerPlayer target = player.getServer().getPlayerList().getPlayer(context.targetPlayerId());
        if (!isValidTarget(player, target)) {
            player.displayClientMessage(Component.literal("目标不在掌中佛国的摄取范围内"), true);
            return false;
        }
        if (!PalmBuddhaCultivationUtility.canAffect(player, target)) {
            player.displayClientMessage(
                    Component.literal(PalmBuddhaCultivationUtility.getRestrictionMessage(player, target)),
                    true
            );
            return false;
        }

        return PalmBuddhaRealmManager.get(player.server).imprisonPlayer(player, target);
    }

    private static boolean isValidTarget(ServerPlayer player, ServerPlayer target) {
        if (target == null || target == player || !target.isAlive()) {
            return false;
        }
        if (target.serverLevel() != player.serverLevel()
                || ThreeThousandRealmManager.get(player.server)
                .isManagedRealm(player.serverLevel().dimension())) {
            return false;
        }
        return player.distanceToSqr(target) <= MAXIMUM_TARGET_DISTANCE_SQUARED
                && player.hasLineOfSight(target);
    }
}
