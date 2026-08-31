package net.seraphina.ninecun_lingtai.common.registry;

import net.seraphina.ninecun_lingtai.ModMain;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;

public final class ModDamageTypes {
    public static final ResourceKey<DamageType> REALM_EXECUTION = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(ModMain.MOD_ID, "realm_execution")
    );
    public static final ResourceKey<DamageType> PRESSURE_EXECUTION = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(ModMain.MOD_ID, "pressure_execution")
    );
    public static final ResourceKey<DamageType> SPELL_METAL = createSpellKey("spell_metal");
    public static final ResourceKey<DamageType> SPELL_WOOD = createSpellKey("spell_wood");
    public static final ResourceKey<DamageType> SPELL_WATER = createSpellKey("spell_water");
    public static final ResourceKey<DamageType> SPELL_FIRE = createSpellKey("spell_fire");
    public static final ResourceKey<DamageType> SPELL_EARTH = createSpellKey("spell_earth");
    public static final ResourceKey<DamageType> SPELL_THUNDER = createSpellKey("spell_thunder");
    public static final ResourceKey<DamageType> SPELL_YIN_YANG = createSpellKey("spell_yin_yang");
    public static final ResourceKey<DamageType> SPELL_CHAOS = createSpellKey("spell_chaos");
    public static final ResourceKey<DamageType> SOUL_PIERCE = createSpellKey("soul_pierce");

    private static ResourceKey<DamageType> createSpellKey(String name) {
        return ResourceKey.create(
                Registries.DAMAGE_TYPE,
                ResourceLocation.fromNamespaceAndPath(ModMain.MOD_ID, name)
        );
    }
}
