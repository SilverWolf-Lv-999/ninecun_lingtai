package net.seraphina.ninecun_lingtai.utility;

import net.seraphina.ninecun_lingtai.api.element.ElementType;
import net.seraphina.ninecun_lingtai.api.technique.TechniqueClientData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public final class TechniqueNetworkUtility {
    private static final int MAXIMUM_TECHNIQUES = 2048;
    private static final int MAXIMUM_NAME_LENGTH = 256;
    private static final int MAXIMUM_DESCRIPTION_LENGTH = 4096;
    private static final int MAXIMUM_CONTENT_LENGTH = 131072;

    public static void writeTechnique(FriendlyByteBuf buffer, TechniqueClientData technique) {
        buffer.writeResourceLocation(technique.id());
        buffer.writeUtf(technique.name(), MAXIMUM_NAME_LENGTH);
        buffer.writeUtf(technique.description(), MAXIMUM_DESCRIPTION_LENGTH);
        buffer.writeUtf(technique.content(), MAXIMUM_CONTENT_LENGTH);
        buffer.writeVarInt(technique.compatibleElements().size());
        technique.compatibleElements().stream()
                .sorted()
                .forEach(buffer::writeEnum);
        buffer.writeDouble(technique.breathingAbsorptionMultiplier());
        buffer.writeVarInt(technique.spellGrade());
        buffer.writeUtf(technique.spellCategory(), MAXIMUM_NAME_LENGTH);
    }

    public static TechniqueClientData readTechnique(FriendlyByteBuf buffer) {
        ResourceLocation techniqueId = buffer.readResourceLocation();
        String name = buffer.readUtf(MAXIMUM_NAME_LENGTH);
        String description = buffer.readUtf(MAXIMUM_DESCRIPTION_LENGTH);
        String content = buffer.readUtf(MAXIMUM_CONTENT_LENGTH);
        int compatibleElementCount = buffer.readVarInt();
        if (compatibleElementCount < 0 || compatibleElementCount > ElementType.values().length) {
            throw new IllegalArgumentException(
                    "Invalid synchronized compatible element count: " + compatibleElementCount
            );
        }
        Set<ElementType> compatibleElements = EnumSet.noneOf(ElementType.class);
        for (int elementIndex = 0; elementIndex < compatibleElementCount; elementIndex++) {
            compatibleElements.add(buffer.readEnum(ElementType.class));
        }
        double breathingAbsorptionMultiplier = buffer.readDouble();
        int spellGrade = buffer.readVarInt();
        String spellCategory = buffer.readUtf(MAXIMUM_NAME_LENGTH);
        return new TechniqueClientData(
                techniqueId,
                name,
                description,
                content,
                compatibleElements,
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
