package com.seraphina.ninecun_lingtai.common.network.packet;

import com.seraphina.ninecun_lingtai.api.aptitude.SoulStrength;
import com.seraphina.ninecun_lingtai.api.aptitude.SpiritualRoot;
import com.seraphina.ninecun_lingtai.api.cultivation.CultivationRealm;
import com.seraphina.ninecun_lingtai.api.cultivation.CultivationStage;
import com.seraphina.ninecun_lingtai.api.element.ElementType;
import com.seraphina.ninecun_lingtai.client.player.ClientCultivationInfoManager;
import com.seraphina.ninecun_lingtai.common.player.PlayerCultivationInfo;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public record SyncCultivationInfoPacket(PlayerCultivationInfo cultivationInfo) {
    public static void encode(SyncCultivationInfoPacket packet, FriendlyByteBuf buffer) {
        PlayerCultivationInfo cultivationInfo = packet.cultivationInfo;
        buffer.writeBoolean(cultivationInfo.spiritualRoot().isPresent());
        cultivationInfo.spiritualRoot().ifPresent(spiritualRoot -> {
            buffer.writeVarInt(spiritualRoot.getElementMask());
            buffer.writeFloat(spiritualRoot.lengthInches());
        });
        buffer.writeVarInt(cultivationInfo.soulStrength()
                .map(SoulStrength::illuminatedStars)
                .orElse(0));

        for (ElementType element : ElementType.values()) {
            CultivationStage stage = cultivationInfo.cultivationStages().get(element);
            buffer.writeBoolean(stage != null);
            if (stage != null) {
                buffer.writeVarInt(stage.realm().ordinal());
                buffer.writeVarInt(stage.layer());
            }
            buffer.writeDouble(cultivationInfo.getSpiritualEnergy(element));
        }
    }

    public static SyncCultivationInfoPacket decode(FriendlyByteBuf buffer) {
        Optional<SpiritualRoot> spiritualRoot = Optional.empty();
        if (buffer.readBoolean()) {
            spiritualRoot = SpiritualRoot.fromElementMask(buffer.readVarInt(), buffer.readFloat());
        }
        Optional<SoulStrength> soulStrength = SoulStrength.fromIlluminatedStars(buffer.readVarInt());
        Map<ElementType, CultivationStage> stages = new EnumMap<>(ElementType.class);
        Map<ElementType, Double> spiritualEnergy = new EnumMap<>(ElementType.class);
        CultivationRealm[] realms = CultivationRealm.values();
        for (ElementType element : ElementType.values()) {
            if (buffer.readBoolean()) {
                int realmIndex = buffer.readVarInt();
                int layer = buffer.readVarInt();
                if (realmIndex >= 0 && realmIndex < realms.length) {
                    try {
                        stages.put(element, new CultivationStage(realms[realmIndex], layer));
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }
            double energy = buffer.readDouble();
            if (Double.isFinite(energy) && energy > 0.0D) {
                spiritualEnergy.put(element, energy);
            }
        }
        return new SyncCultivationInfoPacket(new PlayerCultivationInfo(
                spiritualRoot,
                soulStrength,
                stages,
                spiritualEnergy
        ));
    }

    public static void handle(
            SyncCultivationInfoPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientCultivationInfoManager.accept(packet.cultivationInfo)
        );
    }
}
