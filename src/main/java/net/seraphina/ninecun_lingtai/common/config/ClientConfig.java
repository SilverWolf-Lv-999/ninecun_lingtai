package net.seraphina.ninecun_lingtai.common.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class ClientConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.BooleanValue ENABLE_COSMIC_RENDERER;
    public static final ForgeConfigSpec.DoubleValue COSMIC_SCALE;
    public static final ForgeConfigSpec.DoubleValue COSMIC_SPEED_MULTIPLIER;
    public static final ForgeConfigSpec.ConfigValue<String> WRAPPED_COSMIC_LOCATION;
    public static final ForgeConfigSpec SPEC;

    static {
        BUILDER.push("render");
        BUILDER.push("cosmic");
        ENABLE_COSMIC_RENDERER = BUILDER
                .comment("Enable the cosmic item renderer")
                .define("enabled", true);
        COSMIC_SCALE = BUILDER
                .comment("Scale of the cosmic shader texture")
                .defineInRange("scale", 1.0D, 0.0D, 100.0D);
        COSMIC_SPEED_MULTIPLIER = BUILDER
                .comment("Animation speed multiplier of the cosmic shader")
                .defineInRange("speedMultiplier", 1.0D, 0.0D, 100.0D);
        WRAPPED_COSMIC_LOCATION = BUILDER
                .comment("Texture used by the cosmic item overlay")
                .define("wrappedTextureLocation", "minecraft:textures/misc/forcefield.png");
        BUILDER.pop();
        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    private ClientConfig() {
    }
}
