package com.seraphina.ninecun_lingtai.common.registry;

import com.seraphina.ninecun_lingtai.ModMain;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;

public final class ModDamageTypes {
    public static final ResourceKey<DamageType> REALM_EXECUTION = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(ModMain.MOD_ID, "realm_execution")
    );
}
