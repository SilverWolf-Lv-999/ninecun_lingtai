package com.seraphina.ninecun_lingtai.utility;

import com.seraphina.ninecun_lingtai.api.technique.TechniqueClientData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public final class TechniqueNetworkUtility {
    private static final int MAXIMUM_TECHNIQUES = 2048;
    private static final int MAXIMUM_NAME_LENGTH = 256;
    private static final int MAXIMUM_DESCRIPTION_LENGTH = 4096;
    private static final int MAXIMUM_CONTENT_LENGTH = 131072;

    private TechniqueNetworkUtility() {
    }

    public static void writeTechnique(FriendlyByteBuf buffer, TechniqueClientData technique) {
        buffer.writeResourceLocation(technique.id());
        buffer.writeUtf(technique.name(), MAXIMUM_NAME_LENGTH);
        buffer.writeUtf(technique.description(), MAXIMUM_DESCRIPTION_LENGTH);
        buffer.writeUtf(technique.content(), MAXIMUM_CONTENT_LENGTH);
        buffer.writeDouble(technique.breathingAbsorptionMultiplier());
        buffer.writeVarInt(technique.spellGrade());
        buffer.writeUtf(technique.spellCategory(), MAXIMUM_NAME_LENGTH);
    }

    public static TechniqueClientData readTechnique(FriendlyByteBuf buffer) {
        ResourceLocation techniqueId = buffer.readResourceLocation();
        String name = buffer.readUtf(MAXIMUM_NAME_LENGTH);
        String description = buffer.readUtf(MAXIMUM_DESCRIPTION_LENGTH);
        String content = buffer.readUtf(MAXIMUM_CONTENT_LENGTH);
        double breathingAbsorptionMultiplier = buffer.readDouble();
        int spellGrade = buffer.readVarInt();
        String spellCategory = buffer.readUtf(MAXIMUM_NAME_LENGTH);
        return new TechniqueClientData(
                techniqueId,
                name,
                description,
                content,
                breathingAbsorptionMultiplier,
                spellGrade,
                spellCategory
        );
    }

    public static void writeTechniques(FriendlyByteBuf buffer, List<TechniqueClientData> techniques) {
        if (techniques.size() > MAXIMUM_TECHNIQUES) {
            throw new IllegalArgumentException("Too many techniques to synchronize");
        }
        buffer.writeVarInt(techniques.size());
        techniques.forEach(technique -> writeTechnique(buffer, technique));
    }

    public static List<TechniqueClientData> readTechniques(FriendlyByteBuf buffer) {
        int techniqueCount = buffer.readVarInt();
        if (techniqueCount < 0 || techniqueCount > MAXIMUM_TECHNIQUES) {
            throw new IllegalArgumentException("Invalid synchronized technique count: " + techniqueCount);
        }
        List<TechniqueClientData> techniques = new ArrayList<>(techniqueCount);
        for (int techniqueIndex = 0; techniqueIndex < techniqueCount; techniqueIndex++) {
            techniques.add(readTechnique(buffer));
        }
        return List.copyOf(techniques);
    }

    public static int getMaximumTechniques() {
        return MAXIMUM_TECHNIQUES;
    }
}
