package com.seraphina.ninecun_lingtai.common.network.packet;

import com.seraphina.ninecun_lingtai.api.technique.TechniqueClientData;
import com.seraphina.ninecun_lingtai.client.player.ClientTechniqueManager;
import com.seraphina.ninecun_lingtai.utility.TechniqueNetworkUtility;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

public record SyncTechniqueLibraryPacket(
        List<TechniqueClientData> techniques,
        Set<ResourceLocation> imprintedTechniqueIds
) {
    public SyncTechniqueLibraryPacket {
        techniques = List.copyOf(techniques);
        imprintedTechniqueIds = Set.copyOf(imprintedTechniqueIds);
    }

    public static void encode(SyncTechniqueLibraryPacket packet, FriendlyByteBuf buffer) {
        TechniqueNetworkUtility.writeTechniques(buffer, packet.techniques);
        if (packet.imprintedTechniqueIds.size() > TechniqueNetworkUtility.getMaximumTechniques()) {
            throw new IllegalArgumentException("Too many imprinted techniques to synchronize");
        }
        buffer.writeVarInt(packet.imprintedTechniqueIds.size());
        packet.imprintedTechniqueIds.stream()
                .sorted()
                .forEach(buffer::writeResourceLocation);
    }

    public static SyncTechniqueLibraryPacket decode(FriendlyByteBuf buffer) {
        List<TechniqueClientData> techniques = TechniqueNetworkUtility.readTechniques(buffer);
        int imprintedTechniqueCount = buffer.readVarInt();
        if (imprintedTechniqueCount < 0
                || imprintedTechniqueCount > TechniqueNetworkUtility.getMaximumTechniques()) {
            throw new IllegalArgumentException(
                    "Invalid synchronized imprint count: " + imprintedTechniqueCount
            );
        }
        Set<ResourceLocation> imprintedTechniqueIds = new HashSet<>();
        for (int techniqueIndex = 0; techniqueIndex < imprintedTechniqueCount; techniqueIndex++) {
            imprintedTechniqueIds.add(buffer.readResourceLocation());
        }
        return new SyncTechniqueLibraryPacket(techniques, imprintedTechniqueIds);
    }

    public static void handle(
            SyncTechniqueLibraryPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientTechniqueManager.acceptLibrary(
                        packet.techniques,
                        packet.imprintedTechniqueIds
                )
        );
    }
}
