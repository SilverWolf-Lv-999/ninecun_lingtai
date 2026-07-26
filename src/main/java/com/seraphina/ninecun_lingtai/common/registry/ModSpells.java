package com.seraphina.ninecun_lingtai.common.registry;

import com.seraphina.ninecun_lingtai.ModMain;
import com.seraphina.ninecun_lingtai.api.spell.Spell;
import com.seraphina.ninecun_lingtai.common.spell.FireballSpell;
import com.seraphina.ninecun_lingtai.common.spell.SwiftWindSpell;
import com.seraphina.ninecun_lingtai.common.spell.SwordFlightSpell;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryBuilder;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public final class ModSpells {
    public static final ResourceKey<Registry<Spell>> SPELL_REGISTRY_KEY = ResourceKey.createRegistryKey(
            ResourceLocation.fromNamespaceAndPath(ModMain.MOD_ID, "spell")
    );

    private static final DeferredRegister<Spell> SPELLS = DeferredRegister.create(
            SPELL_REGISTRY_KEY,
            ModMain.MOD_ID
    );

    public static final Supplier<IForgeRegistry<Spell>> REGISTRY = SPELLS.makeRegistry(
            () -> new RegistryBuilder<Spell>().disableSaving().disableSync()
    );

    public static final RegistryObject<Spell> SWIFT_WIND = SPELLS.register(
            "swift_wind",
            SwiftWindSpell::new
    );

    public static final RegistryObject<Spell> FIREBALL = SPELLS.register(
            "fireball",
            FireballSpell::new
    );

    public static final RegistryObject<Spell> SWORD_FLIGHT = SPELLS.register(
            "sword_flight",
            SwordFlightSpell::new
    );

    private ModSpells() {
    }

    public static void register(IEventBus modEventBus) {
        SPELLS.register(modEventBus);
    }
}
