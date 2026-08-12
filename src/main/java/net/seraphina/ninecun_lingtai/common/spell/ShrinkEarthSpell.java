package net.seraphina.ninecun_lingtai.common.spell;

import net.seraphina.ninecun_lingtai.api.spell.Spell;
import net.seraphina.ninecun_lingtai.api.spell.SpellCastContext;
import net.minecraft.server.level.ServerPlayer;

public final class ShrinkEarthSpell extends Spell {
    @Override
    public boolean cast(ServerPlayer player, SpellCastContext context) {
        return ShrinkEarthManager.start(player, context);
    }
}
