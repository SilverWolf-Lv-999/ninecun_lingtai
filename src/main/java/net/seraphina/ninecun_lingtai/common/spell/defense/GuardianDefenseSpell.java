package net.seraphina.ninecun_lingtai.common.spell.defense;

import net.seraphina.ninecun_lingtai.api.spell.Spell;
import net.seraphina.ninecun_lingtai.api.spell.SpellCastContext;
import net.seraphina.ninecun_lingtai.common.network.ModNetwork;
import net.seraphina.ninecun_lingtai.utility.DefenseSpellUtility;
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
    public boolean cancelIfActive(ServerPlayer player) {
        if (DefenseSpellUtility.getGuardian(player)
                .filter(guardian -> guardian.getSpellKind() == this.kind)
                .isEmpty()) {
            return false;
        }
        DefenseSpellUtility.dismissGuardian(player);
        ModNetwork.sendDefenseSpellState(player);
        return true;
    }

    @Override
    public boolean cast(ServerPlayer player, SpellCastContext context) {
        if (!DefenseSpellUtility.canUse(player, this.kind)) {
            player.displayClientMessage(Component.literal("当前灵根无法维持《" + this.kind.getChineseName() + "》"), true);
            return false;
        }
        boolean summoned = DefenseSpellUtility.summonGuardian(player, this.kind, context);
        if (summoned) {
            ModNetwork.sendDefenseSpellState(player);
        }
        return summoned;
    }
}
