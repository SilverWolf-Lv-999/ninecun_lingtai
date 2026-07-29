package com.seraphina.ninecun_lingtai.common.player;

import com.seraphina.ninecun_lingtai.ModMain;
import com.seraphina.ninecun_lingtai.common.spell.defense.DefenseSpellKind;
import com.seraphina.ninecun_lingtai.common.spell.defense.DefenseSpellShape;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;
import java.util.UUID;

public final class PlayerDefenseSpellData {
    private static final String BODY_SHIELD_KEY = ModMain.MOD_ID + ":body_defense_spell";
    private static final String GUARDIAN_KEY = ModMain.MOD_ID + ":guardian_defense_spell";
    private static final String KIND_KEY = "kind";
    private static final String CURRENT_KEY = "current";
    private static final String MAXIMUM_KEY = "maximum";
    private static final String ELEMENT_MASK_KEY = "element_mask";
    private static final String REMAINING_TICKS_KEY = "remaining_ticks";
    private static final String LAST_HURT_TICK_KEY = "last_hurt_tick";
    private static final String REPAIR_DISABLED_UNTIL_TICK_KEY = "repair_disabled_until_tick";
    private static final String ENTITY_UUID_KEY = "entity_uuid";
    private static final int REPAIR_DELAY_TICKS = 20;

    public static Optional<DefenseSpellSnapshot> getBodyShield(Player player) {
        CompoundTag persistentData = player.getPersistentData();
        if (!persistentData.contains(BODY_SHIELD_KEY, Tag.TAG_COMPOUND)) {
            return Optional.empty();
        }
        CompoundTag shieldData = persistentData.getCompound(BODY_SHIELD_KEY);
        Optional<DefenseSpellKind> kind = DefenseSpellKind.fromSerializedName(shieldData.getString(KIND_KEY));
        if (kind.isEmpty()
                || kind.get().getShape() != DefenseSpellShape.BODY_SHIELD
                || !shieldData.contains(CURRENT_KEY, Tag.TAG_ANY_NUMERIC)
                || !shieldData.contains(MAXIMUM_KEY, Tag.TAG_ANY_NUMERIC)
                || !shieldData.contains(REMAINING_TICKS_KEY, Tag.TAG_ANY_NUMERIC)) {
            clearBodyShield(player);
            return Optional.empty();
        }
        double maximum = shieldData.getDouble(MAXIMUM_KEY);
        int remainingTicks = shieldData.getInt(REMAINING_TICKS_KEY);
        if (!Double.isFinite(maximum) || maximum <= 0.0D || remainingTicks <= 0) {
            clearBodyShield(player);
            return Optional.empty();
        }
        DefenseSpellSnapshot snapshot = new DefenseSpellSnapshot(
                kind.get(),
                shieldData.getDouble(CURRENT_KEY),
                maximum,
                shieldData.getInt(ELEMENT_MASK_KEY),
                remainingTicks
        );
        if (snapshot.current() <= 0.0D) {
            clearBodyShield(player);
            return Optional.empty();
        }
        if (snapshot.current() != shieldData.getDouble(CURRENT_KEY)) {
            shieldData.putDouble(CURRENT_KEY, snapshot.current());
        }
        return Optional.of(snapshot);
    }

    public static DefenseSpellSnapshot setBodyShield(
            Player player,
            DefenseSpellKind kind,
            double maximum,
            int elementMask,
            int durationTicks
    ) {
        if (kind.getShape() != DefenseSpellShape.BODY_SHIELD) {
            throw new IllegalArgumentException("Cannot store a guardian as a body shield");
        }
        DefenseSpellSnapshot snapshot = new DefenseSpellSnapshot(
                kind,
                maximum,
                maximum,
                elementMask,
                durationTicks
        );
        CompoundTag shieldData = new CompoundTag();
        shieldData.putString(KIND_KEY, kind.getSerializedName());
        shieldData.putDouble(CURRENT_KEY, snapshot.current());
        shieldData.putDouble(MAXIMUM_KEY, snapshot.maximum());
        shieldData.putInt(ELEMENT_MASK_KEY, snapshot.elementMask());
        shieldData.putInt(REMAINING_TICKS_KEY, snapshot.remainingTicks());
        shieldData.putInt(LAST_HURT_TICK_KEY, player.tickCount);
        shieldData.putInt(REPAIR_DISABLED_UNTIL_TICK_KEY, player.tickCount);
        player.getPersistentData().put(BODY_SHIELD_KEY, shieldData);
        return snapshot;
    }

    public static boolean tickBodyShield(Player player) {
        Optional<DefenseSpellSnapshot> snapshotOptional = getBodyShield(player);
        if (snapshotOptional.isEmpty()) {
            return false;
        }
        int remainingTicks = snapshotOptional.get().remainingTicks() - 1;
        if (remainingTicks <= 0) {
            clearBodyShield(player);
            return false;
        }
        player.getPersistentData().getCompound(BODY_SHIELD_KEY).putInt(REMAINING_TICKS_KEY, remainingTicks);
        return true;
    }

    public static double consumeBodyShield(Player player, double amount) {
        Optional<DefenseSpellSnapshot> snapshotOptional = getBodyShield(player);
        if (snapshotOptional.isEmpty() || amount <= 0.0D) {
            return 0.0D;
        }
        DefenseSpellSnapshot snapshot = snapshotOptional.get();
        double consumed = Math.min(snapshot.current(), amount);
        double remaining = snapshot.current() - consumed;
        if (remaining <= 1.0E-6D) {
            clearBodyShield(player);
        } else {
            player.getPersistentData().getCompound(BODY_SHIELD_KEY).putDouble(CURRENT_KEY, remaining);
        }
        return consumed;
    }

    public static double repairBodyShield(Player player, double amount) {
        Optional<DefenseSpellSnapshot> snapshotOptional = getBodyShield(player);
        if (snapshotOptional.isEmpty() || !Double.isFinite(amount) || amount <= 0.0D) {
            return 0.0D;
        }
        DefenseSpellSnapshot snapshot = snapshotOptional.get();
        double repaired = Math.min(snapshot.maximum() - snapshot.current(), amount);
        if (repaired <= 0.0D) {
            return 0.0D;
        }
        player.getPersistentData().getCompound(BODY_SHIELD_KEY).putDouble(
                CURRENT_KEY,
                snapshot.current() + repaired
        );
        return repaired;
    }

    public static void recordBodyShieldHit(Player player, boolean chaosDamage) {
        if (getBodyShield(player).isEmpty()) {
            return;
        }
        CompoundTag shieldData = player.getPersistentData().getCompound(BODY_SHIELD_KEY);
        shieldData.putInt(LAST_HURT_TICK_KEY, player.tickCount);
        if (chaosDamage) {
            shieldData.putInt(REPAIR_DISABLED_UNTIL_TICK_KEY, player.tickCount + 100);
        }
    }

    public static boolean canRepairBodyShield(Player player) {
        if (getBodyShield(player).isEmpty()) {
            return false;
        }
        CompoundTag shieldData = player.getPersistentData().getCompound(BODY_SHIELD_KEY);
        return player.tickCount - shieldData.getInt(LAST_HURT_TICK_KEY) >= REPAIR_DELAY_TICKS
                && player.tickCount >= shieldData.getInt(REPAIR_DISABLED_UNTIL_TICK_KEY);
    }

    public static boolean clearBodyShield(Player player) {
        boolean present = player.getPersistentData().contains(BODY_SHIELD_KEY);
        player.getPersistentData().remove(BODY_SHIELD_KEY);
        return present;
    }

    public static void setGuardian(Player player, DefenseSpellKind kind, UUID entityUuid) {
        if (kind.getShape() != DefenseSpellShape.GUARDIAN) {
            throw new IllegalArgumentException("Cannot store a body shield as a guardian");
        }
        CompoundTag guardianData = new CompoundTag();
        guardianData.putString(KIND_KEY, kind.getSerializedName());
        guardianData.putUUID(ENTITY_UUID_KEY, entityUuid);
        player.getPersistentData().put(GUARDIAN_KEY, guardianData);
    }

    public static Optional<UUID> getGuardianUuid(Player player) {
        CompoundTag persistentData = player.getPersistentData();
        if (!persistentData.contains(GUARDIAN_KEY, Tag.TAG_COMPOUND)) {
            return Optional.empty();
        }
        CompoundTag guardianData = persistentData.getCompound(GUARDIAN_KEY);
        if (!guardianData.hasUUID(ENTITY_UUID_KEY)) {
            clearGuardian(player);
            return Optional.empty();
        }
        return Optional.of(guardianData.getUUID(ENTITY_UUID_KEY));
    }

    public static boolean clearGuardian(Player player) {
        boolean present = player.getPersistentData().contains(GUARDIAN_KEY);
        player.getPersistentData().remove(GUARDIAN_KEY);
        return present;
    }

    public static void clearGuardian(Player player, UUID expectedUuid) {
        getGuardianUuid(player).filter(expectedUuid::equals).ifPresent(uuid -> clearGuardian(player));
    }
}
