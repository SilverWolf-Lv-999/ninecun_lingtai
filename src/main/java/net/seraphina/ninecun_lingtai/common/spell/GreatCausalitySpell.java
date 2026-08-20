package net.seraphina.ninecun_lingtai.common.spell;

import net.minecraft.server.level.ServerPlayer;
import net.seraphina.ninecun_lingtai.api.spell.Spell;
import net.seraphina.ninecun_lingtai.api.spell.SpellCastContext;
import net.seraphina.ninecun_lingtai.common.causality.GreatCausalityQueryService;

/**
 * 大因果术的推衍入口。目标来自本次真正的施法上下文：
 * 对着玩家施法查询该玩家，对空气施法查询当前界域。
 */
public final class GreatCausalitySpell extends Spell {
    @Override
    public boolean cast(ServerPlayer player, SpellCastContext context) {
        GreatCausalityQueryService.query(player, context.targetPlayerId());
        return true;
    }
}
