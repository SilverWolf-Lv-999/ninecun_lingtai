package com.seraphina.ninecun_lingtai.common.network.packet;

import com.seraphina.ninecun_lingtai.common.spell.SpellCastManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SelectSpellPacket(ResourceLocation spellId) {
    public static void encode(SelectSpellPacket packet, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(packet.spellId);
    }

    public static SelectSpellPacket decode(FriendlyByteBuf buffer) {
        return new SelectSpellPacket(buffer.readResourceLocation());
    }

    public static void handle(SelectSpellPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        ServerPlayer player = contextSupplier.get().getSender();
        if (player != null) {
            SpellCastManager.selectSpell(player, packet.spellId);
        }
    }
}
