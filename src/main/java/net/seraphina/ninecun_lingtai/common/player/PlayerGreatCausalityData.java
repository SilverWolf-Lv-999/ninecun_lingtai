package net.seraphina.ninecun_lingtai.common.player;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.seraphina.ninecun_lingtai.ModMain;
import net.seraphina.ninecun_lingtai.utility.GreatCausalityUtility;

import java.util.UUID;

public final class PlayerGreatCausalityData {
    private static final String DATA_KEY = ModMain.MOD_ID + ":great_causality";
    private static final String TRANSFER_ATTACK_KEY = "TransferAttack";
    private static final String TRANSFER_HATE_KEY = "TransferHate";
    private static final String TRANSFER_TRIBULATION_KEY = "TransferTribulation";
    private static final String TRANSFER_NEGATIVE_EFFECTS_KEY = "TransferNegativeEffects";
    private static final String TRANSFER_VILLAGER_PRICE_KEY = "TransferVillagerPrice";
    private static final String TRANSFER_DAMAGE_KEY = "TransferDamage";
    private static final String TRANSFER_TARGET_MODE_KEY = "TransferTargetMode";
    private static final String TRANSFER_TARGET_ID_KEY = "TransferTargetId";
    private static final String RANDOM_ITEM_KEY = "RandomItem";
    private static final String CORRUPT_BENEFICIAL_EFFECTS_KEY = "CorruptBeneficialEffects";
    private static final String TRANSFER_ITEMS_KEY = "TransferItems";
    private static final String TRANSFER_ADVANCEMENTS_KEY = "TransferAdvancements";
    private static final String VOID_CRAFTING_KEY = "VoidCrafting";
    private static final String TAMPER_TARGET_MODE_KEY = "TamperTargetMode";
    private static final String TAMPER_TARGET_ID_KEY = "TamperTargetId";

    private PlayerGreatCausalityData() {
    }

    public static GreatCausalitySettings getSettings(Player player) {
        if (!GreatCausalityUtility.isPerfection(player)) {
            return GreatCausalitySettings.defaultSettings();
        }
        CompoundTag data = getData(player);
        GreatCausalitySettings defaults = GreatCausalitySettings.defaultSettings();
        return new GreatCausalitySettings(
                data.getBoolean(TRANSFER_ATTACK_KEY),
                data.getBoolean(TRANSFER_HATE_KEY),
                data.getBoolean(TRANSFER_TRIBULATION_KEY),
                data.getBoolean(TRANSFER_NEGATIVE_EFFECTS_KEY),
                data.getBoolean(TRANSFER_VILLAGER_PRICE_KEY),
                data.getBoolean(TRANSFER_DAMAGE_KEY),
                readTargetMode(data, TRANSFER_TARGET_MODE_KEY, defaults.transferTargetMode()),
                readUuid(data, TRANSFER_TARGET_ID_KEY),
                data.getBoolean(RANDOM_ITEM_KEY),
                data.getBoolean(CORRUPT_BENEFICIAL_EFFECTS_KEY),
                data.getBoolean(TRANSFER_ITEMS_KEY),
                data.getBoolean(TRANSFER_ADVANCEMENTS_KEY),
                data.getBoolean(VOID_CRAFTING_KEY),
                readTargetMode(data, TAMPER_TARGET_MODE_KEY, defaults.tamperTargetMode()),
                readUuid(data, TAMPER_TARGET_ID_KEY)
        );
    }

    public static void setSettings(Player player, GreatCausalitySettings settings) {
        GreatCausalitySettings sanitizedSettings = sanitize(player, settings);
        CompoundTag data = new CompoundTag();
        data.putBoolean(TRANSFER_ATTACK_KEY, sanitizedSettings.transferAttack());
        data.putBoolean(TRANSFER_HATE_KEY, sanitizedSettings.transferHate());
        data.putBoolean(TRANSFER_TRIBULATION_KEY, sanitizedSettings.transferTribulation());
        data.putBoolean(TRANSFER_NEGATIVE_EFFECTS_KEY, sanitizedSettings.transferNegativeEffects());
        data.putBoolean(TRANSFER_VILLAGER_PRICE_KEY, sanitizedSettings.transferVillagerPrice());
        data.putBoolean(TRANSFER_DAMAGE_KEY, sanitizedSettings.transferDamage());
        data.putString(TRANSFER_TARGET_MODE_KEY, sanitizedSettings.transferTargetMode().getSerializedName());
        writeUuid(data, TRANSFER_TARGET_ID_KEY, sanitizedSettings.transferTargetId());
        data.putBoolean(RANDOM_ITEM_KEY, sanitizedSettings.randomItem());
        data.putBoolean(
                CORRUPT_BENEFICIAL_EFFECTS_KEY,
                sanitizedSettings.corruptBeneficialEffects()
        );
        data.putBoolean(TRANSFER_ITEMS_KEY, sanitizedSettings.transferItems());
        data.putBoolean(TRANSFER_ADVANCEMENTS_KEY, sanitizedSettings.transferAdvancements());
        data.putBoolean(VOID_CRAFTING_KEY, sanitizedSettings.voidCrafting());
        data.putString(TAMPER_TARGET_MODE_KEY, sanitizedSettings.tamperTargetMode().getSerializedName());
        writeUuid(data, TAMPER_TARGET_ID_KEY, sanitizedSettings.tamperTargetId());
        player.getPersistentData().put(DATA_KEY, data);
    }

    public static boolean isFeatureEnabled(Player player, boolean enabled) {
        return GreatCausalityUtility.isPerfection(player) && enabled;
    }

    private static GreatCausalitySettings sanitize(Player player, GreatCausalitySettings settings) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return GreatCausalitySettings.defaultSettings();
        }
        return GreatCausalityUtility.sanitizeSettings(serverPlayer, settings);
    }

    private static CompoundTag getData(Player player) {
        CompoundTag persistentData = player.getPersistentData();
        if (!persistentData.contains(DATA_KEY, Tag.TAG_COMPOUND)) {
            return new CompoundTag();
        }
        return persistentData.getCompound(DATA_KEY);
    }

    private static GreatCausalityTargetMode readTargetMode(
            CompoundTag data,
            String key,
            GreatCausalityTargetMode fallback
    ) {
        return GreatCausalityTargetMode.fromSerializedName(data.getString(key)).orElse(fallback);
    }

    private static UUID readUuid(CompoundTag data, String key) {
        return data.hasUUID(key) ? data.getUUID(key) : null;
    }

    private static void writeUuid(CompoundTag data, String key, UUID uuid) {
        if (uuid != null) {
            data.putUUID(key, uuid);
        }
    }
}
