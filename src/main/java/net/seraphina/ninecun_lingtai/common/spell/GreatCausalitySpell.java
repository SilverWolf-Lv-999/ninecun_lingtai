package net.seraphina.ninecun_lingtai.common.spell;

import net.minecraft.server.level.ServerPlayer;
import net.seraphina.ninecun_lingtai.api.spell.Spell;
import net.seraphina.ninecun_lingtai.api.spell.SpellCastContext;
import net.seraphina.ninecun_lingtai.common.causality.GreatCausalityQueryService;

/**
 * 推衍因果是大因果术唯一需要主动施展的部分。
 *
 * <p>查询目标由客户端交互包提供；普通施法键只打开当前界域的玩家列表。</p>
 */
public final class GreatCausalitySpell extends Spell {
    @Override
    public boolean cast(ServerPlayer player, SpellCastContext context) {
        if (GreatCausalityQueryService.hasPendingQuery(player)) {
            GreatCausalityQueryService.query(
                    player,
                    GreatCausalityQueryService.consumePendingTarget(player)
            );
            return true;
        }
        GreatCausalityQueryService.sendPlayerList(player);
        return true;
    }
}
