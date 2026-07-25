package com.seraphina.ninecun_lingtai.utility;

import com.seraphina.ninecun_lingtai.api.element.ElementRelation;
import com.seraphina.ninecun_lingtai.api.element.ElementType;
import com.seraphina.ninecun_lingtai.api.element.ElementalPower;
import com.seraphina.ninecun_lingtai.common.config.CommonConfig;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;

public final class ElementDamageUtility {
    private ElementDamageUtility() {
    }

    public static double getDamageMultiplier(ElementalPower attackElement, ElementalPower defenseElement) {
        ElementRelation relation = attackElement.element().getRelationTo(defenseElement.element());
        return switch (relation) {
            case GENERATES -> CommonConfig.GENERATION_WEAKENING_MULTIPLIER.get();
            case OVERCOMES -> naturallySuppresses(attackElement, defenseElement)
                    ? CommonConfig.RESTRAINT_ENHANCEMENT_MULTIPLIER.get()
                    : CommonConfig.SUPPRESSED_MULTIPLIER.get();
            case OVERCOME_BY -> reverseSuppresses(attackElement, defenseElement)
                    ? CommonConfig.RESTRAINT_ENHANCEMENT_MULTIPLIER.get()
                    : CommonConfig.SUPPRESSED_MULTIPLIER.get();
            case SUPPRESSES -> CommonConfig.CHAOS_ENHANCEMENT_MULTIPLIER.get();
            case SUPPRESSED_BY -> CommonConfig.CHAOS_SUPPRESSED_MULTIPLIER.get();
            default -> 1.0D;
        };
    }

    public static void spawnHitParticles(LivingEntity target, ElementalPower attackElement) {
        if (!(target instanceof Player) || !(target.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        int particleCount = Mth.clamp((int) Math.ceil(8.0D + attackElement.strength() * 4.0D), 8, 32);
        serverLevel.sendParticles(
                getHitParticle(attackElement.element()),
                target.getX(),
                target.getY(0.5D),
                target.getZ(),
                particleCount,
                target.getBbWidth() * 0.5D,
                target.getBbHeight() * 0.35D,
                target.getBbWidth() * 0.5D,
                0.03D
        );
    }

    private static boolean naturallySuppresses(ElementalPower attackElement, ElementalPower defenseElement) {
        return attackElement.strength() * CommonConfig.NATURAL_RESTRAINT_ADVANTAGE.get()
                >= defenseElement.strength();
    }

    private static boolean reverseSuppresses(ElementalPower attackElement, ElementalPower defenseElement) {
        return attackElement.strength()
                >= defenseElement.strength() * CommonConfig.NATURAL_RESTRAINT_ADVANTAGE.get();
    }

    private static ParticleOptions getHitParticle(ElementType element) {
        return switch (element) {
            case METAL -> ParticleTypes.CRIT;
            case WOOD -> ParticleTypes.COMPOSTER;
            case WATER -> ParticleTypes.SPLASH;
            case FIRE -> ParticleTypes.FLAME;
            case EARTH -> new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DIRT.defaultBlockState());
            case YIN_YANG -> ParticleTypes.ENCHANT;
            case THUNDER -> ParticleTypes.ELECTRIC_SPARK;
            case CHAOS -> ParticleTypes.REVERSE_PORTAL;
        };
    }
}
