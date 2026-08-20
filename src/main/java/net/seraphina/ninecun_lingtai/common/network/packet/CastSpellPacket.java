package net.seraphina.ninecun_lingtai.common.network.packet;

import net.seraphina.ninecun_lingtai.common.player.PlayerSoulRoamingManager;
import net.seraphina.ninecun_lingtai.common.spell.SpellCastManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public record CastSpellPacket(UUID targetPlayerId) {
    public static final CastSpellPacket INSTANCE = new CastSpellPacket();

    public CastSpellPacket() {
        this(null);
    }

    public static void encode(CastSpellPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.targetPlayerId() != null);
        if (packet.targetPlayerId() != null) {
            buffer.writeUUID(packet.targetPlayerId());
        }
    }

    public static CastSpellPacket decode(FriendlyByteBuf buffer) {
        return new CastSpellPacket(buffer.readBoolean() ? buffer.readUUID() : null);
    }

    public static void handle(CastSpellPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        ServerPlayer player = contextSupplier.get().getSender();
        if (player != null
                && !PlayerSoulRoamingManager.isSoulRoaming(player)) {
            SpellCastManager.castSelectedSpell(player, packet.targetPlayerId());
        }
    }
}
