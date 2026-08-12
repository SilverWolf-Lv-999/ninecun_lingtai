package net.seraphina.ninecun_lingtai.common.spell;

import net.seraphina.ninecun_lingtai.api.spell.Spell;
import net.seraphina.ninecun_lingtai.api.spell.SpellCastContext;
import net.minecraft.server.level.ServerPlayer;

/**
 * 御剑术的神通实现；名称、品级与消耗见 data/ninecun_lingtai/techniques/sword_flight_spell.json。
 * 施展即登剑：须手持剑、立于地面并蹲身。条件不足时返回 false，灵力由施法管线退还。
 */
public final class SwordFlightSpell extends Spell {
    @Override
    public boolean cast(ServerPlayer player, SpellCastContext context) {
        return SwordFlightManager.tryMount(player, context);
    }
}
