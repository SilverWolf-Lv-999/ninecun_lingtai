package net.seraphina.ninecun_lingtai.utility;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.seraphina.ninecun_lingtai.ModMain;
import net.seraphina.ninecun_lingtai.api.cultivation.CultivationApi;
import net.seraphina.ninecun_lingtai.api.cultivation.CultivationRealm;
import net.seraphina.ninecun_lingtai.api.cultivation.CultivationStage;
import net.seraphina.ninecun_lingtai.api.technique.TechniqueMasteryStage;
import net.seraphina.ninecun_lingtai.common.player.GreatCausalitySettings;
import net.seraphina.ninecun_lingtai.common.player.GreatCausalityTargetMode;
import net.seraphina.ninecun_lingtai.common.player.PlayerGreatCausalityData;
import net.seraphina.ninecun_lingtai.common.player.PlayerTechniqueData;
import net.seraphina.ninecun_lingtai.common.player.PlayerTechniqueProficiencyData;

import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.function.Supplier;

public final class GreatCausalityUtility {
    public static final ResourceLocation TECHNIQUE_ID = ResourceLocation.fromNamespaceAndPath(
            ModMain.MOD_ID,
            "great_causality"
    );

    private static final int ENTRY_HISTORY_LIMIT = 16;
    private static final int MINOR_MASTERY_HISTORY_LIMIT = 64;
    private static final int MAJOR_MASTERY_HISTORY_LIMIT = 256;
    private static final double PASSIVE_DETECTION_MULTIPLIER = 0.35D;
    private static final double MAJOR_DETECTION_MULTIPLIER = 0.15D;
    private static final int RANDOM_SELECTION_ATTEMPTS = 12;
    private static final ThreadLocal<Integer> CAUSALITY_REDIRECT_DEPTH = ThreadLocal.withInitial(() -> 0);

    public static boolean hasTechnique(Player player) {
        return PlayerTechniqueData.hasTechnique(player, TECHNIQUE_ID);
    }

    public static TechniqueMasteryStage getMasteryStage(Player player) {
        if (!hasTechnique(player)) {
            return TechniqueMasteryStage.ENTRY;
        }
        return PlayerTechniqueData.getTechnique(player, TECHNIQUE_ID)
                .flatMap(progress -> TechniqueMasteryStage.fromMastery(progress.mastery()))
                .orElse(TechniqueMasteryStage.ENTRY);
    }

    public static boolean isPerfection(Player player) {
        return getMasteryStage(player) == TechniqueMasteryStage.PERFECTION;
    }

    public static int getMaximumVisibleRecords(Player observer) {
        TechniqueMasteryStage masteryStage = getMasteryStage(observer);
        if (masteryStage == TechniqueMasteryStage.PERFECTION) {
            return Integer.MAX_VALUE;
        }
        int baseLimit = switch (masteryStage) {
            case ENTRY -> ENTRY_HISTORY_LIMIT;
            case MINOR_MASTERY -> MINOR_MASTERY_HISTORY_LIMIT;
            case MAJOR_MASTERY -> MAJOR_MASTERY_HISTORY_LIMIT;
            case PERFECTION -> Integer.MAX_VALUE;
        };
        int realmBonus = CultivationApi.getStage(observer)
                .map(CultivationStage::realm)
                .map(CultivationRealm::ordinal)
                .orElse(0) * 16;
        return Math.min(Integer.MAX_VALUE, baseLimit + realmBonus);
    }

    public static double getDetectionMultiplier(Player observer) {
        if (!hasTechnique(observer)) {
            return 1.0D;
        }
        return switch (getMasteryStage(observer)) {
            case ENTRY -> PASSIVE_DETECTION_MULTIPLIER;
            case MINOR_MASTERY -> PASSIVE_DETECTION_MULTIPLIER;
            case MAJOR_MASTERY -> MAJOR_DETECTION_MULTIPLIER;
            case PERFECTION -> 0.0D;
        };
    }

    public static boolean isImmortal(Player player) {
        return CultivationApi.getStage(player)
                .map(stage -> stage.realm() == CultivationRealm.IMMORTAL)
                .orElse(false);
    }

    public static boolean canQuery(ServerPlayer observer) {
        return observer != null && hasTechnique(observer);
    }

    public static long getProficiency(Player player) {
        return PlayerTechniqueProficiencyData.get(player, TECHNIQUE_ID);
    }

    public static GreatCausalitySettings sanitizeSettings(
            ServerPlayer player,
            GreatCausalitySettings requestedSettings
    ) {
        if (!isPerfection(player)) {
            return GreatCausalitySettings.defaultSettings();
        }
        GreatCausalitySettings settings = requestedSettings == null
                ? GreatCausalitySettings.defaultSettings()
                : requestedSettings;
        return new GreatCausalitySettings(
                settings.transferAttack(),
                settings.transferHate(),
                settings.transferTribulation(),
                settings.transferNegativeEffects(),
                settings.transferVillagerPrice(),
                settings.transferDamage(),
                settings.transferTargetMode(),
                sanitizeTargetId(
                        player,
                        settings.transferTargetMode(),
                        settings.transferTargetId()
                ),
                settings.randomItem(),
                settings.corruptBeneficialEffects(),
                settings.transferItems(),
                settings.transferAdvancements(),
                settings.voidCrafting(),
                settings.tamperTargetMode(),
                sanitizeTargetId(
                        player,
                        settings.tamperTargetMode(),
                        settings.tamperTargetId()
                )
        );
    }

    public static List<ServerPlayer> getEligibleTargets(ServerPlayer source) {
        return source.server.getPlayerList().getPlayers().stream()
                .filter(target -> isEligibleTarget(source, target))
                .toList();
    }

    public static ServerPlayer getTransferTarget(ServerPlayer caster) {
        GreatCausalitySettings settings = PlayerGreatCausalityData.getSettings(caster);
        return resolveTarget(
                caster,
                settings.transferTargetMode(),
                settings.transferTargetId()
        );
    }

    public static ServerPlayer findTamperingCaster(
            ServerPlayer target,
            Predicate<GreatCausalitySettings> featureEnabled
    ) {
        if (target == null || isImmortal(target)) {
            return null;
        }
        for (ServerPlayer candidate : target.server.getPlayerList().getPlayers()) {
            GreatCausalitySettings settings = PlayerGreatCausalityData.getSettings(candidate);
            if (candidate.level() == target.level()
                    && isPerfection(candidate)
                    && featureEnabled.test(settings)
                    && isConfiguredTarget(candidate, target, settings.tamperTargetMode(), settings.tamperTargetId())) {
                return candidate;
            }
        }
        return null;
    }

    public static ItemStack createRandomItemStack(ServerPlayer player, int amount) {
        return new ItemStack(getRandomRegisteredItem(player.getRandom()), Math.max(1, amount));
    }

    public static MobEffectInstance createRandomHarmfulEffect(
            ServerPlayer player,
            MobEffectInstance originalEffect
    ) {
        MobEffect harmfulEffect = getRandomHarmfulEffect(player.getRandom());
        return new MobEffectInstance(
                harmfulEffect,
                Math.max(1, originalEffect.getDuration()),
                originalEffect.getAmplifier(),
                originalEffect.isAmbient(),
                originalEffect.isVisible(),
                originalEffect.showIcon()
        );
    }

    public static boolean isCausalityRedirectInProgress() {
        return CAUSALITY_REDIRECT_DEPTH.get() > 0;
    }

    public static <T> T runWithCausalityRedirect(Supplier<T> operation) {
        CAUSALITY_REDIRECT_DEPTH.set(CAUSALITY_REDIRECT_DEPTH.get() + 1);
        try {
            return operation.get();
        } finally {
            int depth = CAUSALITY_REDIRECT_DEPTH.get() - 1;
            if (depth <= 0) {
                CAUSALITY_REDIRECT_DEPTH.remove();
            } else {
                CAUSALITY_REDIRECT_DEPTH.set(depth);
            }
        }
    }

    private static UUID sanitizeTargetId(
            ServerPlayer source,
            GreatCausalityTargetMode targetMode,
            UUID targetId
    ) {
        if (targetMode != GreatCausalityTargetMode.SELECTED || targetId == null) {
            return null;
        }
        ServerPlayer target = source.server.getPlayerList().getPlayer(targetId);
        return isEligibleTarget(source, target) ? targetId : null;
    }

    private static boolean isConfiguredTarget(
            ServerPlayer caster,
            ServerPlayer target,
            GreatCausalityTargetMode targetMode,
            UUID targetId
    ) {
        ServerPlayer configuredTarget = resolveTarget(caster, targetMode, targetId);
        return configuredTarget != null && configuredTarget.getUUID().equals(target.getUUID());
    }

    private static ServerPlayer resolveTarget(
            ServerPlayer source,
            GreatCausalityTargetMode targetMode,
            UUID targetId
    ) {
        if (targetMode == GreatCausalityTargetMode.SELECTED) {
            ServerPlayer selectedTarget = targetId == null
                    ? null
                    : source.server.getPlayerList().getPlayer(targetId);
            return isEligibleTarget(source, selectedTarget) ? selectedTarget : null;
        }

        List<ServerPlayer> targets = getEligibleTargets(source);
        return targets.isEmpty() ? null : targets.get(source.getRandom().nextInt(targets.size()));
    }

    private static boolean isEligibleTarget(ServerPlayer source, ServerPlayer target) {
        return target != null
                && target != source
                && target.isAlive()
                && !target.isSpectator()
                && target.level() == source.level()
                && !isImmortal(target);
    }

    private static Item getRandomRegisteredItem(RandomSource random) {
        for (int attempt = 0; attempt < RANDOM_SELECTION_ATTEMPTS; attempt++) {
            Item item = BuiltInRegistries.ITEM.byId(random.nextInt(BuiltInRegistries.ITEM.size()));
            if (item != null && item != net.minecraft.world.item.Items.AIR) {
                return item;
            }
        }
        return net.minecraft.world.item.Items.STONE;
    }

    private static MobEffect getRandomHarmfulEffect(RandomSource random) {
        for (int attempt = 0; attempt < RANDOM_SELECTION_ATTEMPTS; attempt++) {
            MobEffect effect = BuiltInRegistries.MOB_EFFECT.byId(
                    random.nextInt(BuiltInRegistries.MOB_EFFECT.size())
            );
            if (effect != null && effect.getCategory() == MobEffectCategory.HARMFUL) {
                return effect;
            }
        }
        return MobEffects.POISON;
    }
}
