package com.seraphina.ninecun_lingtai.common.spell.defense;

import com.seraphina.ninecun_lingtai.api.spell.Spell;
import com.seraphina.ninecun_lingtai.api.spell.SpellCastContext;
import com.seraphina.ninecun_lingtai.utility.DefenseSpellUtility;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class GuardianDefenseSpell extends Spell {
    private final DefenseSpellKind kind;

    public GuardianDefenseSpell(DefenseSpellKind kind) {
        if (kind.getShape() != DefenseSpellShape.GUARDIAN) {
            throw new IllegalArgumentException("Guardian defense spell requires a guardian kind");
        }
        this.kind = kind;
    }

    @Override
    public boolean cast(ServerPlayer player, SpellCastContext context) {
        if (!DefenseSpellUtility.canUse(player, this.kind)) {
            player.displayClientMessage(Component.literal("当前灵根无法维持《" + this.kind.getChineseName() + "》"), true);
            return false;
        }
        return DefenseSpellUtility.summonGuardian(player, this.kind, context);
    }
}
