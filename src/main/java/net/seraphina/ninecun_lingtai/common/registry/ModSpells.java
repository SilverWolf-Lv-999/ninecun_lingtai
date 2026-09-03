package net.seraphina.ninecun_lingtai.common.registry;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryBuilder;
import net.minecraftforge.registries.RegistryObject;
import net.seraphina.ninecun_lingtai.ModMain;
import net.seraphina.ninecun_lingtai.api.spell.Spell;
import net.seraphina.ninecun_lingtai.common.spell.FireballSpell;
import net.seraphina.ninecun_lingtai.common.spell.GreatCausalitySpell;
import net.seraphina.ninecun_lingtai.common.spell.ShrinkEarthSpell;
import net.seraphina.ninecun_lingtai.common.spell.SwiftWindSpell;
import net.seraphina.ninecun_lingtai.common.spell.SwordFlightSpell;
import net.seraphina.ninecun_lingtai.common.spell.attack.AttackSpellKind;
import net.seraphina.ninecun_lingtai.common.spell.attack.BoltAttackSpell;
import net.seraphina.ninecun_lingtai.common.spell.attack.BurstAttackSpell;
import net.seraphina.ninecun_lingtai.common.spell.attack.StrikeAttackSpell;
import net.seraphina.ninecun_lingtai.common.spell.defense.BodyDefenseSpell;
import net.seraphina.ninecun_lingtai.common.spell.defense.DefenseSpellKind;
import net.seraphina.ninecun_lingtai.common.spell.defense.GuardianDefenseSpell;
import net.seraphina.ninecun_lingtai.common.spell.palm_buddha.PalmBuddhaSpell;
import net.seraphina.ninecun_lingtai.common.spell.soul.SoulPierceSpell;
import net.seraphina.ninecun_lingtai.common.spell.soul.SoulShockSpell;

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

    public static final RegistryObject<Spell> GREAT_CAUSALITY = SPELLS.register(
            "great_causality",
            GreatCausalitySpell::new
    );

    public static final RegistryObject<Spell> PALM_BUDDHA = SPELLS.register(
            "palm_buddha",
            PalmBuddhaSpell::new
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

    public static final RegistryObject<Spell> METAL_BLADE = registerAttack(AttackSpellKind.METAL_BLADE);
    public static final RegistryObject<Spell> WOOD_BIND = registerAttack(AttackSpellKind.WOOD_BIND);
    public static final RegistryObject<Spell> ICE_SPIKE = registerAttack(AttackSpellKind.ICE_SPIKE);
    public static final RegistryObject<Spell> YIN_FINGER = registerAttack(AttackSpellKind.YIN_FINGER);
    public static final RegistryObject<Spell> EARTH_SPIKE = registerAttack(AttackSpellKind.EARTH_SPIKE);
    public static final RegistryObject<Spell> PALM_THUNDER = registerAttack(AttackSpellKind.PALM_THUNDER);
    public static final RegistryObject<Spell> THUNDER_FALL = registerAttack(AttackSpellKind.THUNDER_FALL);
    public static final RegistryObject<Spell> FLAME_SEA = registerAttack(AttackSpellKind.FLAME_SEA);
    public static final RegistryObject<Spell> FROST_DOMAIN = registerAttack(AttackSpellKind.FROST_DOMAIN);
    public static final RegistryObject<Spell> CHAOS_REND = registerAttack(AttackSpellKind.CHAOS_REND);

    public static final RegistryObject<Spell> SOUL_PIERCE = SPELLS.register(
            "soul_pierce",
            SoulPierceSpell::new
    );
    public static final RegistryObject<Spell> SOUL_SHOCK = SPELLS.register(
            "soul_shock",
            SoulShockSpell::new
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

    private static RegistryObject<Spell> registerAttack(AttackSpellKind kind) {
        return SPELLS.register(kind.getSerializedName(), () -> switch (kind.getShape()) {
            case BOLT -> new BoltAttackSpell(kind);
            case STRIKE -> new StrikeAttackSpell(kind);
            case BURST -> new BurstAttackSpell(kind);
        });
    }
}
