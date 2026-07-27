package com.seraphina.ninecun_lingtai.common.spell;

import com.seraphina.ninecun_lingtai.api.spell.Spell;
import com.seraphina.ninecun_lingtai.api.spell.SpellCastContext;
import net.minecraft.server.level.ServerPlayer;

/** 缩地成寸的神通实现；名称、品级与消耗见 data/ninecun_lingtai/techniques/shrink_earth_spell.json。 */
public final class ShrinkEarthSpell extends Spell {
    @Override
    public boolean cast(ServerPlayer player, SpellCastContext context) {
        return ShrinkEarthManager.start(player, context);
    }
}
