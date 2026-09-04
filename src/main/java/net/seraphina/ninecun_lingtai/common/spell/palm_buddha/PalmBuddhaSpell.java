package net.seraphina.ninecun_lingtai.common.spell.palm_buddha;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
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
 * 掌中佛国的第一阶段施法实现：准心单选玩家或普通生物。
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
        if (context.targetEntityId() == null) {
            player.displayClientMessage(Component.literal("掌中佛国须先以神念锁定一名生灵"), true);
            return false;
        }

        LivingEntity target = findTarget(player, context.targetEntityId());
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

        PalmBuddhaRealmManager realmManager = PalmBuddhaRealmManager.get(player.server);
        return target instanceof ServerPlayer serverPlayer
                ? realmManager.imprisonPlayer(player, serverPlayer)
                : realmManager.imprisonEntity(player, target);
    }

    private static LivingEntity findTarget(ServerPlayer player, java.util.UUID targetId) {
        if (player.getServer() == null) {
            return null;
        }
        Entity target = player.serverLevel().getEntity(targetId);
        return target instanceof LivingEntity livingEntity ? livingEntity : null;
    }

    private static boolean isValidTarget(ServerPlayer player, LivingEntity target) {
        if (target == null || target == player || !target.isAlive()) {
            return false;
        }
        if (target.isPassenger()
                || !target.getPassengers().isEmpty()
                || !target.getType().canSerialize()) {
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
