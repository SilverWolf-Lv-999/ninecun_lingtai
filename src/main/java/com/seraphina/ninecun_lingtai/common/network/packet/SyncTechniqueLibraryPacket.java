package com.seraphina.ninecun_lingtai.common.network.packet;

import com.seraphina.ninecun_lingtai.api.technique.LearnedTechniqueClientData;
import com.seraphina.ninecun_lingtai.api.technique.TechniqueClientData;
import com.seraphina.ninecun_lingtai.client.player.ClientTechniqueManager;
import com.seraphina.ninecun_lingtai.utility.TechniqueNetworkUtility;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

public record SyncTechniqueLibraryPacket(
        List<TechniqueClientData> techniques,
        Set<ResourceLocation> imprintedTechniqueIds,
        List<LearnedTechniqueClientData> learnedTechniques
) {
    public SyncTechniqueLibraryPacket {
        techniques = List.copyOf(techniques);
        imprintedTechniqueIds = Set.copyOf(imprintedTechniqueIds);
        learnedTechniques = List.copyOf(learnedTechniques);
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

        if (packet.learnedTechniques.size() > TechniqueNetworkUtility.getMaximumTechniques()) {
            throw new IllegalArgumentException("Too many learned techniques to synchronize");
        }
        buffer.writeVarInt(packet.learnedTechniques.size());
        for (LearnedTechniqueClientData learnedTechnique : packet.learnedTechniques) {
            buffer.writeResourceLocation(learnedTechnique.id());
            buffer.writeVarInt(learnedTechnique.comprehensionScore());
            buffer.writeVarLong(learnedTechnique.proficiency());
            buffer.writeResourceLocation(learnedTechnique.mastery());
        }
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

        int learnedTechniqueCount = buffer.readVarInt();
        if (learnedTechniqueCount < 0
                || learnedTechniqueCount > TechniqueNetworkUtility.getMaximumTechniques()) {
            throw new IllegalArgumentException(
                    "Invalid synchronized learned technique count: " + learnedTechniqueCount
            );
        }
        List<LearnedTechniqueClientData> learnedTechniques = new ArrayList<>(learnedTechniqueCount);
        for (int techniqueIndex = 0; techniqueIndex < learnedTechniqueCount; techniqueIndex++) {
            learnedTechniques.add(new LearnedTechniqueClientData(
                    buffer.readResourceLocation(),
                    buffer.readVarInt(),
                    buffer.readVarLong(),
                    buffer.readResourceLocation()
            ));
        }
        return new SyncTechniqueLibraryPacket(techniques, imprintedTechniqueIds, learnedTechniques);
    }

    public static void handle(
            SyncTechniqueLibraryPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientTechniqueManager.acceptLibrary(
                        packet.techniques,
                        packet.imprintedTechniqueIds,
                        packet.learnedTechniques
                )
        );
    }
}
