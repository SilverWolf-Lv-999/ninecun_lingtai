package net.seraphina.ninecun_lingtai.common.network.packet;

import net.seraphina.ninecun_lingtai.common.player.PlayerSoulRoamingManager;
import net.seraphina.ninecun_lingtai.common.spell.SpellCastManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class CastSpellPacket {
    public static final CastSpellPacket INSTANCE = new CastSpellPacket();

    private CastSpellPacket() {
    }

    public static void encode(CastSpellPacket packet, FriendlyByteBuf buffer) {
    }

    public static CastSpellPacket decode(FriendlyByteBuf buffer) {
        return INSTANCE;
    }

    public static void handle(CastSpellPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        ServerPlayer player = contextSupplier.get().getSender();
        if (player != null
                && !PlayerSoulRoamingManager.isSoulRoaming(player)) {
            SpellCastManager.castSelectedSpell(player);
        }
    }
}
