package net.seraphina.ninecun_lingtai.common.registry;

import net.seraphina.ninecun_lingtai.ModMain;
import net.seraphina.ninecun_lingtai.api.spell.Spell;
import net.seraphina.ninecun_lingtai.common.spell.FireballSpell;
import net.seraphina.ninecun_lingtai.common.spell.ShrinkEarthSpell;
import net.seraphina.ninecun_lingtai.common.spell.SwiftWindSpell;
import net.seraphina.ninecun_lingtai.common.spell.SwordFlightSpell;
import net.seraphina.ninecun_lingtai.common.spell.defense.BodyDefenseSpell;
import net.seraphina.ninecun_lingtai.common.spell.defense.DefenseSpellKind;
import net.seraphina.ninecun_lingtai.common.spell.defense.GuardianDefenseSpell;
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

    public static final RegistryObject<Spell> SHRINK_EARTH = SPELLS.register(
            "shrink_earth",
            ShrinkEarthSpell::new
    );

    public static final RegistryObject<Spell> METAL_BODY_SHIELD = registerBodyShield(
            DefenseSpellKind.METAL_BODY_SHIELD
    );
    public static final RegistryObject<Spell> WOOD_GUARDIAN = registerGuardian(
            DefenseSpellKind.WOOD_GUARDIAN
    );
    public static final RegistryObject<Spell> WATER_BODY_SHIELD = registerBodyShield(
            DefenseSpellKind.WATER_BODY_SHIELD
    );
    public static final RegistryObject<Spell> FIRE_GUARDIAN = registerGuardian(
            DefenseSpellKind.FIRE_GUARDIAN
    );
    public static final RegistryObject<Spell> EARTH_GUARDIAN = registerGuardian(
            DefenseSpellKind.EARTH_GUARDIAN
    );
    public static final RegistryObject<Spell> YIN_YANG_BODY_SHIELD = registerBodyShield(
            DefenseSpellKind.YIN_YANG_BODY_SHIELD
    );
    public static final RegistryObject<Spell> FIVE_ELEMENT_CYCLE = registerBodyShield(
            DefenseSpellKind.FIVE_ELEMENT_CYCLE
    );
    public static final RegistryObject<Spell> CHAOS_VOID = registerBodyShield(
            DefenseSpellKind.CHAOS_VOID
    );
    public static final RegistryObject<Spell> COMPOSITE_BODY_SHIELD = registerBodyShield(
            DefenseSpellKind.COMPOSITE_BODY_SHIELD
    );
    public static final RegistryObject<Spell> FOUR_PHASE_GUARDIAN = registerGuardian(
            DefenseSpellKind.FOUR_PHASE_GUARDIAN
    );

    public static void register(IEventBus modEventBus) {
        SPELLS.register(modEventBus);
    }

    private static RegistryObject<Spell> registerBodyShield(DefenseSpellKind kind) {
        return SPELLS.register(kind.getSerializedName(), () -> new BodyDefenseSpell(kind));
    }

    private static RegistryObject<Spell> registerGuardian(DefenseSpellKind kind) {
        return SPELLS.register(kind.getSerializedName(), () -> new GuardianDefenseSpell(kind));
    }
}
