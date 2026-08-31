package net.seraphina.ninecun_lingtai.common.formation;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.seraphina.ninecun_lingtai.api.cultivation.CultivationRealm;
import net.seraphina.ninecun_lingtai.api.cultivation.CultivationStage;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.block.Block;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 一张阵图（`data/ninecun_lingtai/formations/*.json`，形状见阵禁篇 11.1 /
 * 阵禁系统日志 5.3）。运行时实例只引用 {@link #id}，/reload 热更新不悬空。
 */
public final class FormationDefinition {
    private final ResourceLocation id;
    private final String name;
    private final int grade;
    private final FormationType type;
    private final FormationEnergyMode energyMode;
    private final List<FlagSlot> flagLayout;
    private final double radius;
    private final Map<String, Double> effects;
    @Nullable
    private final CultivationStage minimumCultivation;

    private FormationDefinition(
            ResourceLocation id,
            String name,
            int grade,
            FormationType type,
            FormationEnergyMode energyMode,
            List<FlagSlot> flagLayout,
            double radius,
            Map<String, Double> effects,
            @Nullable CultivationStage minimumCultivation
    ) {
        this.id = id;
        this.name = name;
        this.grade = grade;
        this.type = type;
        this.energyMode = energyMode;
        this.flagLayout = List.copyOf(flagLayout);
        this.radius = radius;
        this.effects = Map.copyOf(effects);
        this.minimumCultivation = minimumCultivation;
    }

    public ResourceLocation id() {
        return this.id;
    }

    public String name() {
        return this.name;
    }

    public int grade() {
        return this.grade;
    }

    public FormationType type() {
        return this.type;
    }

    public FormationEnergyMode energyMode() {
        return this.energyMode;
    }

    public List<FlagSlot> flagLayout() {
        return this.flagLayout;
    }

    public double radius() {
        return this.radius;
    }

    public double getEffect(String key, double fallback) {
        return this.effects.getOrDefault(key, fallback);
    }

    public Optional<CultivationStage> minimumCultivation() {
        return Optional.ofNullable(this.minimumCultivation);
    }

    /** 品阶对应的最低境界（阵禁篇 6.3：一至九阶对应境界序列，收拢到现有六大境界）。 */
    public CultivationRealm gradeRealm() {
        CultivationRealm[] realms = CultivationRealm.values();
        return realms[Math.min(realms.length - 1, Math.max(0, this.grade - 1))];
    }

    /**
     * 旗位：以阵盘为原点的方位角（度，0 = 北、顺时针）+ 水平距离（格）+ 材料方块 tag。
     * tag 用 `ninecun_lingtai:flags/any` 时不限定五行，由布阵者自选（旗序学问所在）。
     */
    public record FlagSlot(double angleDegrees, double distance, TagKey<Block> materialTag) {
    }

    /** GSON 解析（容错日志由调用方负责，本方法直接抛 JsonParseException）。 */
    public static FormationDefinition fromJson(ResourceLocation id, JsonObject jsonObject) {
        String name = GsonHelper.getAsString(jsonObject, "name");
        int grade = GsonHelper.getAsInt(jsonObject, "grade");
        if (grade < 1 || grade > 9) {
            throw new JsonParseException("grade must be 1-9, got " + grade);
        }
        FormationType type = FormationType.fromSerializedName(GsonHelper.getAsString(jsonObject, "type"))
                .orElseThrow(() -> new JsonParseException(
                        "Unknown formation type: " + GsonHelper.getAsString(jsonObject, "type")));
        FormationEnergyMode energyMode = FormationEnergyMode
                .fromSerializedName(GsonHelper.getAsString(jsonObject, "energy_mode", "stored"))
                .orElseThrow(() -> new JsonParseException(
                        "Unknown energy_mode: " + GsonHelper.getAsString(jsonObject, "energy_mode")));

        JsonArray layoutJson = GsonHelper.getAsJsonArray(jsonObject, "flag_layout");
        if (layoutJson.isEmpty()) {
            throw new JsonParseException("flag_layout must not be empty");
        }
        List<FlagSlot> flagLayout = new ArrayList<>();
        for (int index = 0; index < layoutJson.size(); index++) {
            JsonObject slotJson = GsonHelper.convertToJsonObject(layoutJson.get(index), "flag slot");
            double angle = GsonHelper.getAsDouble(slotJson, "angle");
            double distance = GsonHelper.getAsDouble(slotJson, "distance");
            if (distance <= 0.0D || !Double.isFinite(distance)) {
                throw new JsonParseException("flag slot distance must be positive");
            }
            String tagName = GsonHelper.getAsString(slotJson, "material_tag");
            ResourceLocation tagId = ResourceLocation.tryParse(tagName);
            if (tagId == null) {
                throw new JsonParseException("Invalid material_tag: " + tagName);
            }
            flagLayout.add(new FlagSlot(angle, distance, TagKey.create(Registries.BLOCK, tagId)));
        }

        double radius = GsonHelper.getAsDouble(jsonObject, "radius");
        if (radius <= 0.0D || radius > 64.0D) {
            throw new JsonParseException("radius must be in (0, 64], got " + radius);
        }

        Map<String, Double> effects = new HashMap<>();
        if (jsonObject.has("effects")) {
            JsonObject effectsJson = GsonHelper.getAsJsonObject(jsonObject, "effects");
            effectsJson.entrySet().forEach(entry -> effects.put(
                    entry.getKey(),
                    GsonHelper.convertToDouble(entry.getValue(), "effect " + entry.getKey())
            ));
        }

        CultivationStage minimumCultivation = null;
        if (jsonObject.has("requirements")) {
            JsonObject requirementsJson = GsonHelper.getAsJsonObject(jsonObject, "requirements");
            if (requirementsJson.has("minimum_cultivation")) {
                minimumCultivation = parseCultivation(
                        GsonHelper.getAsString(requirementsJson, "minimum_cultivation"));
            }
        }

        return new FormationDefinition(
                id, name, grade, type, energyMode, flagLayout, radius, effects, minimumCultivation);
    }

    /** 解析 "foundation_establishment:1" 形境界串（复用境界枚举既有序列化名）。 */
    private static CultivationStage parseCultivation(String serialized) {
        String[] parts = serialized.split(":");
        CultivationRealm realm = CultivationRealm.fromSerializedName(parts[0])
                .orElseThrow(() -> new JsonParseException("Unknown realm: " + parts[0]));
        int layer = CultivationStage.MINIMUM_LAYER;
        if (parts.length > 1) {
            try {
                layer = Integer.parseInt(parts[1]);
            } catch (NumberFormatException exception) {
                throw new JsonParseException("Invalid realm layer: " + serialized);
            }
        }
        return new CultivationStage(realm, layer);
    }
}
