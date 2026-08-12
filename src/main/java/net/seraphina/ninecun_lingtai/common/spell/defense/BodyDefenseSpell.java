package net.seraphina.ninecun_lingtai.common.spell.defense;

import net.seraphina.ninecun_lingtai.api.spell.Spell;
import net.seraphina.ninecun_lingtai.api.spell.SpellCastContext;
import net.seraphina.ninecun_lingtai.common.player.PlayerAptitudeData;
import net.seraphina.ninecun_lingtai.common.player.PlayerDefenseSpellData;
import net.seraphina.ninecun_lingtai.common.network.ModNetwork;
import net.seraphina.ninecun_lingtai.utility.DefenseSpellUtility;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class BodyDefenseSpell extends Spell {
    private final DefenseSpellKind kind;

    public BodyDefenseSpell(DefenseSpellKind kind) {
        if (kind.getShape() != DefenseSpellShape.BODY_SHIELD) {
            throw new IllegalArgumentException("Body defense spell requires a body-shield kind");
        }
        this.kind = kind;
    }

    @Override
    public boolean cancelIfActive(ServerPlayer player) {
        if (PlayerDefenseSpellData.getBodyShield(player)
                .filter(snapshot -> snapshot.kind() == this.kind)
                .isEmpty()) {
            return false;
        }
        PlayerDefenseSpellData.clearBodyShield(player);
        ModNetwork.sendDefenseSpellState(player);
        return true;
    }

    @Override
    public boolean cast(ServerPlayer player, SpellCastContext context) {
        if (!DefenseSpellUtility.canUse(player, this.kind)) {
            player.displayClientMessage(Component.literal("当前灵根无法维持《" + this.kind.getChineseName() + "》"), true);
            return false;
        }
        int elementMask = PlayerAptitudeData.getSpiritualRoot(player)
                .map(root -> root.getElementMask())
                .orElse(0);
        double maximum = DefenseSpellUtility.getMaximumPoints(this.kind, context);
        PlayerDefenseSpellData.setBodyShield(
                player,
                this.kind,
                maximum,
                elementMask,
                DefenseSpellUtility.getDurationTicks(context)
        );
        ModNetwork.sendDefenseSpellState(player);
        return true;
    }
}
