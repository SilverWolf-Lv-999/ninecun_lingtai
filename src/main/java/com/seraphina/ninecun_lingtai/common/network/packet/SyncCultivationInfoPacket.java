package com.seraphina.ninecun_lingtai.common.network.packet;

import com.seraphina.ninecun_lingtai.api.aptitude.SoulStrength;
import com.seraphina.ninecun_lingtai.api.aptitude.SpiritualRoot;
import com.seraphina.ninecun_lingtai.api.cultivation.CultivationRealm;
import com.seraphina.ninecun_lingtai.api.cultivation.CultivationStage;
import com.seraphina.ninecun_lingtai.api.element.ElementType;
import com.seraphina.ninecun_lingtai.client.player.ClientCultivationInfoManager;
import com.seraphina.ninecun_lingtai.common.lifespan.LifespanSnapshot;
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

        buffer.writeBoolean(cultivationInfo.cultivationStage().isPresent());
        cultivationInfo.cultivationStage().ifPresent(stage -> {
            buffer.writeVarInt(stage.realm().ordinal());
            buffer.writeVarInt(stage.layer());
        });

        for (ElementType element : ElementType.values()) {
            buffer.writeDouble(cultivationInfo.getSpiritualEnergy(element));
        }
        buffer.writeVarLong(cultivationInfo.lifespan().ageTicks());
        buffer.writeVarInt(cultivationInfo.lifespan().maximumLifespanYears());
        buffer.writeBoolean(cultivationInfo.lifespan().immortal());
    }

    public static SyncCultivationInfoPacket decode(FriendlyByteBuf buffer) {
        Optional<SpiritualRoot> spiritualRoot = Optional.empty();
        if (buffer.readBoolean()) {
            spiritualRoot = SpiritualRoot.fromElementMask(buffer.readVarInt(), buffer.readFloat());
        }
        Optional<SoulStrength> soulStrength = SoulStrength.fromIlluminatedStars(buffer.readVarInt());
        Optional<CultivationStage> cultivationStage = Optional.empty();
        CultivationRealm[] realms = CultivationRealm.values();
        if (buffer.readBoolean()) {
            int realmIndex = buffer.readVarInt();
            int layer = buffer.readVarInt();
            if (realmIndex >= 0 && realmIndex < realms.length) {
                try {
                    cultivationStage = Optional.of(new CultivationStage(realms[realmIndex], layer));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }

        Map<ElementType, Double> spiritualEnergy = new EnumMap<>(ElementType.class);
        for (ElementType element : ElementType.values()) {
            double energy = buffer.readDouble();
            if (Double.isFinite(energy) && energy > 0.0D) {
                spiritualEnergy.put(element, energy);
            }
        }
        LifespanSnapshot lifespan = new LifespanSnapshot(
                buffer.readVarLong(),
                buffer.readVarInt(),
                buffer.readBoolean()
        );
        return new SyncCultivationInfoPacket(new PlayerCultivationInfo(
                spiritualRoot,
                soulStrength,
                cultivationStage,
                spiritualEnergy,
                lifespan
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
