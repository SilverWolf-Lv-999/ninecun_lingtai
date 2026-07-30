package com.seraphina.ninecun_lingtai.common.network;

import com.seraphina.ninecun_lingtai.api.aptitude.SoulStrength;
import com.seraphina.ninecun_lingtai.api.aptitude.SpiritualRoot;
import com.seraphina.ninecun_lingtai.api.cultivation.CultivationRealm;
import com.seraphina.ninecun_lingtai.api.cultivation.CultivationStage;
import com.seraphina.ninecun_lingtai.api.element.ElementType;
import com.seraphina.ninecun_lingtai.common.debug.DaoScrollSnapshot;
import com.seraphina.ninecun_lingtai.common.debug.DaoScrollTechniqueData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class DaoScrollPacketUtility {
    private static final int MAXIMUM_ENTITY_NAME_LENGTH = 256;
    private static final int MAXIMUM_TECHNIQUE_NAME_LENGTH = 256;
    private static final int MAXIMUM_TECHNIQUES = 2048;

    private DaoScrollPacketUtility() {
    }

    public static void writeSnapshot(FriendlyByteBuf buffer, DaoScrollSnapshot snapshot) {
        buffer.writeVarInt(snapshot.entityId());
        buffer.writeUtf(snapshot.entityName(), MAXIMUM_ENTITY_NAME_LENGTH);
        buffer.writeBoolean(snapshot.player());

        buffer.writeBoolean(snapshot.cultivationStage().isPresent());
        snapshot.cultivationStage().ifPresent(stage -> {
            buffer.writeVarInt(stage.realm().ordinal());
            buffer.writeVarInt(stage.layer());
        });

        buffer.writeBoolean(snapshot.spiritualRoot().isPresent());
        snapshot.spiritualRoot().ifPresent(root -> {
            buffer.writeVarInt(root.getElementMask());
            buffer.writeFloat(root.lengthInches());
        });
        buffer.writeVarInt(snapshot.soulStrength()
                .map(SoulStrength::illuminatedStars)
                .orElse(0));
        buffer.writeVarLong(snapshot.ageTicks());
        buffer.writeVarInt(snapshot.defenseCurrent());
        for (ElementType element : ElementType.values()) {
            buffer.writeDouble(snapshot.getEnergy(element));
        }

        if (snapshot.techniques().size() > MAXIMUM_TECHNIQUES) {
            throw new IllegalArgumentException("Too many Dao Scroll techniques");
        }
        buffer.writeVarInt(snapshot.techniques().size());
        for (DaoScrollTechniqueData technique : snapshot.techniques()) {
            buffer.writeResourceLocation(technique.id());
            buffer.writeUtf(technique.name(), MAXIMUM_TECHNIQUE_NAME_LENGTH);
            buffer.writeBoolean(technique.learned());
            buffer.writeVarInt(technique.comprehensionScore());
            buffer.writeVarLong(technique.proficiency());
            buffer.writeBoolean(technique.imprinted());
        }
    }

    public static DaoScrollSnapshot readSnapshot(FriendlyByteBuf buffer) {
        int entityId = buffer.readVarInt();
        String entityName = buffer.readUtf(MAXIMUM_ENTITY_NAME_LENGTH);
        boolean player = buffer.readBoolean();

        Optional<CultivationStage> cultivationStage = Optional.empty();
        if (buffer.readBoolean()) {
            int realmIndex = buffer.readVarInt();
            int layer = buffer.readVarInt();
            CultivationRealm[] realms = CultivationRealm.values();
            if (realmIndex >= 0 && realmIndex < realms.length) {
                try {
                    cultivationStage = Optional.of(new CultivationStage(realms[realmIndex], layer));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }

        Optional<SpiritualRoot> spiritualRoot = Optional.empty();
        if (buffer.readBoolean()) {
            spiritualRoot = SpiritualRoot.fromElementMask(buffer.readVarInt(), buffer.readFloat());
        }
        Optional<SoulStrength> soulStrength = SoulStrength.fromIlluminatedStars(buffer.readVarInt());
        long ageTicks = buffer.readVarLong();
        int defenseCurrent = buffer.readVarInt();

        Map<ElementType, Double> spiritualEnergy = new EnumMap<>(ElementType.class);
        for (ElementType element : ElementType.values()) {
            double energy = buffer.readDouble();
            if (Double.isFinite(energy) && energy > 0.0D) {
                spiritualEnergy.put(element, energy);
            }
        }

        int techniqueCount = buffer.readVarInt();
        if (techniqueCount < 0 || techniqueCount > MAXIMUM_TECHNIQUES) {
            throw new IllegalArgumentException("Invalid Dao Scroll technique count: " + techniqueCount);
        }
        List<DaoScrollTechniqueData> techniques = new ArrayList<>(techniqueCount);
        for (int index = 0; index < techniqueCount; index++) {
            techniques.add(new DaoScrollTechniqueData(
                    buffer.readResourceLocation(),
                    buffer.readUtf(MAXIMUM_TECHNIQUE_NAME_LENGTH),
                    buffer.readBoolean(),
                    buffer.readVarInt(),
                    buffer.readVarLong(),
                    buffer.readBoolean()
            ));
        }
        return new DaoScrollSnapshot(
                entityId,
                entityName,
                player,
                cultivationStage,
                spiritualRoot,
                soulStrength,
                ageTicks,
                defenseCurrent,
                spiritualEnergy,
                techniques
        );
    }
}
