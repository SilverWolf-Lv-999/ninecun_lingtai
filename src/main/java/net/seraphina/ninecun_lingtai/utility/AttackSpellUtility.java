package net.seraphina.ninecun_lingtai.utility;

import net.seraphina.ninecun_lingtai.api.element.ElementType;
import net.seraphina.ninecun_lingtai.common.insight.InsightEffectUtility;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public final class AttackSpellUtility {
    private static final double MAXIMUM_DAMAGE = 1.0E6D;
    private static final double BOLT_HIT_INFLATION = 0.35D;

    public static float getScaledDamage(double baseDamage, int cultivationStrength, double powerEfficiency) {
        double damage = baseDamage
                * SpellPowerUtility.getPowerMultiplier(cultivationStrength, powerEfficiency);
        return (float) Math.min(MAXIMUM_DAMAGE, damage);
    }

    /**
     * 带施法者的伤害重载：玩家施法者追加悟道印记"剑心通明"乘区
     * （经 {@link InsightEffectUtility} 单点读取，R9）。心魔等实体施法者走基准值，
     * 不继承印记效果（心魔即己 R9：镜像不吃印记）。
     */
    public static float getScaledDamage(
            LivingEntity caster,
            double baseDamage,
            int cultivationStrength,
            double powerEfficiency
    ) {
        float damage = getScaledDamage(baseDamage, cultivationStrength, powerEfficiency);
        if (!(caster instanceof ServerPlayer player)) {
            return damage;
        }
        double insightBonus = InsightEffectUtility.query(
                player,
                InsightEffectUtility.EFFECT_ATTACK_SPELL_DAMAGE_BONUS
        );
        return insightBonus <= 0.0D
                ? damage
                : (float) Math.min(MAXIMUM_DAMAGE, damage * (1.0D + insightBonus));
    }

    public static DamageSource createDamageSource(ServerPlayer player, ResourceKey<DamageType> damageTypeKey) {
        return createDamageSource((LivingEntity) player, damageTypeKey);
    }

    /**
     * 施法主体泛化重载（心魔即己 §6.4）：以任意活体为施法者构建法术伤害源。
     * 玩家版委托本方法，行为不变；心魔等实体施法者经此复用同一伤害链路。
     */
    public static DamageSource createDamageSource(LivingEntity caster, ResourceKey<DamageType> damageTypeKey) {
        return new DamageSource(
                caster.level()
                        .registryAccess()
                        .registryOrThrow(Registries.DAMAGE_TYPE)
                        .getHolderOrThrow(damageTypeKey),
                caster,
                caster
        );
    }

    public static Optional<LivingEntity> findBoltTarget(ServerPlayer player, double range) {
        return findBoltTarget((LivingEntity) player, range);
    }

    /** 施法主体泛化重载（心魔即己 §6.4）：沿施法者视线选取单体目标。 */
    public static Optional<LivingEntity> findBoltTarget(LivingEntity caster, double range) {
        Vec3 origin = caster.getEyePosition();
        Vec3 direction = caster.getLookAngle();
        Vec3 reach = origin.add(direction.scale(range));
        BlockHitResult blockHit = caster.level().clip(new ClipContext(
                origin,
                reach,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                caster
        ));
        double blockedDistanceSquared = blockHit.getType() == HitResult.Type.MISS
                ? range * range
                : blockHit.getLocation().distanceToSqr(origin);
        Vec3 end = blockHit.getType() == HitResult.Type.MISS ? reach : blockHit.getLocation();
        AABB searchBox = caster.getBoundingBox().expandTowards(direction.scale(range)).inflate(1.0D);
        Predicate<Entity> filter = entity -> entity instanceof LivingEntity
                && entity != caster
                && entity.isPickable()
                && !entity.isAlliedTo(caster);
        EntityHitResult entityHit = clipEntities(caster, origin, end, searchBox, filter);
        if (entityHit == null
                || entityHit.getLocation().distanceToSqr(origin) > blockedDistanceSquared) {
            return Optional.empty();
        }
        return Optional.of((LivingEntity) entityHit.getEntity());
    }

    public static Vec3 findStrikePoint(ServerPlayer player, double range) {
        return findStrikePoint((LivingEntity) player, range);
    }

    /** 施法主体泛化重载（心魔即己 §6.4）：沿施法者视线选取落点。 */
    public static Vec3 findStrikePoint(LivingEntity caster, double range) {
        Vec3 origin = caster.getEyePosition();
        Vec3 direction = caster.getLookAngle();
        Vec3 reach = origin.add(direction.scale(range));
        AABB searchBox = caster.getBoundingBox().expandTowards(direction.scale(range)).inflate(1.0D);
        Predicate<Entity> filter = entity -> entity instanceof LivingEntity
                && entity != caster
                && entity.isPickable();
        EntityHitResult entityHit = clipEntities(caster, origin, reach, searchBox, filter);
        BlockHitResult blockHit = caster.level().clip(new ClipContext(
                origin,
                reach,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                caster
        ));
        Vec3 blockPoint = blockHit.getType() == HitResult.Type.MISS ? reach : blockHit.getLocation();
        if (entityHit != null
                && entityHit.getLocation().distanceToSqr(origin) <= blockPoint.distanceToSqr(origin)) {
            return entityHit.getEntity().position();
        }
        return blockPoint;
    }

    public static List<LivingEntity> findBurstTargets(ServerPlayer player, Vec3 center, double radius) {
        return findBurstTargets((LivingEntity) player, center, radius);
    }

    /** 施法主体泛化重载（心魔即己 §6.4）：以施法者为敌我判定基准的范围目标搜集。 */
    public static List<LivingEntity> findBurstTargets(LivingEntity caster, Vec3 center, double radius) {
        AABB area = new AABB(center, center).inflate(radius, radius * 0.6D, radius);
        return caster.level().getEntitiesOfClass(
                LivingEntity.class,
                area,
                entity -> entity != caster
                        && entity.isAlive()
                        && !entity.isAlliedTo(caster)
                        && entity.distanceToSqr(center) <= radius * radius
        );
    }

    public static void spawnBoltTrail(ServerLevel level, Vec3 origin, Vec3 target, ElementType element) {
        Vec3 path = target.subtract(origin);
        double length = path.length();
        if (length < 0.5D) {
            return;
        }
        Vec3 step = path.scale(1.0D / length);
        ParticleOptions particle = getTrailParticle(element);
        for (double travelled = 0.5D; travelled < length; travelled += 0.5D) {
            Vec3 point = origin.add(step.scale(travelled));
            level.sendParticles(particle, point.x, point.y, point.z, 2, 0.05D, 0.05D, 0.05D, 0.01D);
        }
    }

    public static void spawnBurstParticles(ServerLevel level, Vec3 center, double radius, ElementType element) {
        ParticleOptions particle = getTrailParticle(element);
        int count = (int) Math.min(160.0D, 24.0D * radius);
        level.sendParticles(
                particle,
                center.x,
                center.y + 0.5D,
                center.z,
                count,
                radius * 0.55D,
                0.6D,
                radius * 0.55D,
                0.05D
        );
    }

    public static ParticleOptions getTrailParticle(ElementType element) {
        return switch (element) {
            case METAL -> ParticleTypes.CRIT;
            case WOOD -> ParticleTypes.COMPOSTER;
            case WATER -> ParticleTypes.SNOWFLAKE;
            case FIRE -> ParticleTypes.FLAME;
            case EARTH -> new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DIRT.defaultBlockState());
            case YIN_YANG -> ParticleTypes.SOUL_FIRE_FLAME;
            case THUNDER -> ParticleTypes.ELECTRIC_SPARK;
            case CHAOS -> ParticleTypes.REVERSE_PORTAL;
        };
    }

    private static EntityHitResult clipEntities(
            LivingEntity caster,
            Vec3 origin,
            Vec3 end,
            AABB searchBox,
            Predicate<Entity> filter
    ) {
        EntityHitResult closest = null;
        double closestDistanceSquared = Double.MAX_VALUE;
        for (Entity entity : caster.level().getEntities(caster, searchBox, filter)) {
            AABB hitBox = entity.getBoundingBox().inflate(BOLT_HIT_INFLATION);
            Optional<Vec3> clip = hitBox.clip(origin, end);
            if (clip.isEmpty()) {
                continue;
            }
            double distanceSquared = clip.get().distanceToSqr(origin);
            if (distanceSquared < closestDistanceSquared) {
                closestDistanceSquared = distanceSquared;
                closest = new EntityHitResult(entity, clip.get());
            }
        }
        return closest;
    }
}
