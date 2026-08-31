package net.seraphina.ninecun_lingtai.common.entity;

import net.seraphina.ninecun_lingtai.common.config.CommonConfig;
import net.seraphina.ninecun_lingtai.common.entity.spell.DefenseSpellGuardianEntity;
import net.seraphina.ninecun_lingtai.common.inner_demon.MindDemonCounterMove;
import net.seraphina.ninecun_lingtai.common.inner_demon.MindDemonSnapshot;
import net.seraphina.ninecun_lingtai.common.player.PlayerDefenseResourceData;
import net.seraphina.ninecun_lingtai.utility.SoulInjuryUtility;
import net.seraphina.ninecun_lingtai.common.spell.SpellCastManager;
import net.seraphina.ninecun_lingtai.common.spell.attack.AttackSpellKind;
import net.seraphina.ninecun_lingtai.utility.AttackSpellUtility;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 心魔实体（心魔即己 §6）：由行为账本锁定快照生成的暗色剪影镜像，单体
 * PathfinderMob。SPAR 阶段只用 snapshot.topSpells 复现玩家常用招（经
 * {@link AttackSpellUtility} 实体重载，不走 Spell 注册表的 cast(ServerPlayer) 链）；
 * REFLECT 起解锁反制招。
 *
 * <p>绝不复制物品/背包：无装备槽、{@code shouldDropLoot=false}、击杀无战利品；
 * 外观由 {@code MindDemonRenderer} 用 owner 皮肤暗化渲染。</p>
 *
 * <p>剧情血量下限：进入 SEVER/HOLD 之前伤害无法将其打到 1 HP 以下（决念未下，
 * 心魔不灭），保证五阶段流转完整。</p>
 */
public final class MindDemonEntity extends PathfinderMob {
    // AI 阶段（与 MindTrialPhase 对齐，由 MindTrialManager 驱动）。
    public static final byte STAGE_SPAR = 1;
    public static final byte STAGE_REFLECT = 2;
    public static final byte STAGE_PAUSED = 3;
    public static final byte STAGE_SEVER = 4;
    public static final byte STAGE_FRENZY = 5;

    private static final int SPELL_COOLDOWN_TICKS = 60;
    private static final int SHIELD_BREAK_COOLDOWN_TICKS = 200;
    private static final int AIR_LOCK_COOLDOWN_TICKS = 300;
    private static final double FRENZY_COOLDOWN_MULTIPLIER = 0.7D;
    private static final int ECHO_RAGE_DURATION_TICKS = 100;
    private static final int ECHO_RAGE_MAX_STACKS = 3;
    private static final double ECHO_RAGE_DAMAGE_BONUS = 0.10D;
    private static final double SHIELD_BREAK_HIT_RANGE = 8.0D;

    private static final EntityDataAccessor<Optional<UUID>> OWNER_UUID = SynchedEntityData.defineId(
            MindDemonEntity.class,
            EntityDataSerializers.OPTIONAL_UUID
    );
    private static final EntityDataAccessor<String> OWNER_NAME = SynchedEntityData.defineId(
            MindDemonEntity.class,
            EntityDataSerializers.STRING
    );
    private static final EntityDataAccessor<Byte> AI_STAGE = SynchedEntityData.defineId(
            MindDemonEntity.class,
            EntityDataSerializers.BYTE
    );
    private static final EntityDataAccessor<Boolean> CHARGING = SynchedEntityData.defineId(
            MindDemonEntity.class,
            EntityDataSerializers.BOOLEAN
    );

    private UUID trialId;
    private double karmaScale = MindDemonSnapshot.karmaScaleBase();
    private int cultivationStrength = 1;
    private final List<AttackSpellKind> mirroredSpells = new ArrayList<>();
    private EnumSet<MindDemonCounterMove> counterMoves = EnumSet.noneOf(MindDemonCounterMove.class);

    private int spellCooldown = SPELL_COOLDOWN_TICKS;
    private int nextSpellIndex;
    private int shieldBreakCooldown = SHIELD_BREAK_COOLDOWN_TICKS / 2;
    private int airLockCooldown = AIR_LOCK_COOLDOWN_TICKS / 2;
    private int windupRemainingTicks;
    private Vec3 windupTargetOrigin = Vec3.ZERO;
    private int airLockWarningTicks;
    private int echoRageStacks;
    private int echoRageExpireTicks;
    /** 本场试炼已施加的魂伤累计（R5：cap 防一场心魔打进 PERIL 死螺旋，上限读配置）。 */
    private double soulInjuryDealt;

    public MindDemonEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        this.setPersistenceRequired();
        this.xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D)
                .add(Attributes.FOLLOW_RANGE, 64.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6D);
    }

    /** 由 MindTrialManager 生成时调用：面板 = 玩家属性 × karmaScale（§6.1）。 */
    public void initialize(ServerPlayer owner, MindDemonSnapshot snapshot, UUID trialId, byte stage) {
        this.entityData.set(OWNER_UUID, Optional.of(snapshot.ownerUuid()));
        this.entityData.set(OWNER_NAME, snapshot.ownerName());
        this.trialId = trialId;
        this.karmaScale = snapshot.karmaScale();
        this.cultivationStrength = SpellCastManager.getCultivationStrength(snapshot.stage());
        this.mirroredSpells.clear();
        for (net.minecraft.resources.ResourceLocation spellId : snapshot.topSpells()) {
            AttackSpellKind.fromSerializedName(spellId.getPath()).ifPresent(this.mirroredSpells::add);
        }
        if (this.mirroredSpells.isEmpty()) {
            // 玩家没有攻击法术使用记录时的最小镜像：一门基础单体咒。
            this.mirroredSpells.add(AttackSpellKind.YIN_FINGER);
        }
        this.counterMoves = snapshot.counterMoves().isEmpty()
                ? EnumSet.noneOf(MindDemonCounterMove.class)
                : EnumSet.copyOf(snapshot.counterMoves());
        double maxHealth = Math.max(10.0D, owner.getMaxHealth() * this.karmaScale);
        if (this.getAttribute(Attributes.MAX_HEALTH) != null) {
            this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(maxHealth);
        }
        this.setHealth((float) maxHealth);
        this.setCustomName(Component.translatableWithFallback(
                "entity.ninecun_lingtai.mind_demon.named",
                "心魔·%s",
                snapshot.ownerName()
        ));
        this.setCustomNameVisible(true);
        setAiStage(stage);
    }

    // ------------------------------------------------------------------ 阶段控制

    public byte getAiStage() {
        return this.entityData.get(AI_STAGE);
    }

    public void setAiStage(byte stage) {
        this.entityData.set(AI_STAGE, stage);
        if (stage == STAGE_FRENZY && this.getAttribute(Attributes.MOVEMENT_SPEED) != null) {
            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.36D);
        }
    }

    public boolean isCharging() {
        return this.entityData.get(CHARGING);
    }

    public UUID getTrialId() {
        return this.trialId;
    }

    public Optional<UUID> getOwnerUuid() {
        return this.entityData.get(OWNER_UUID);
    }

    public String getOwnerName() {
        return this.entityData.get(OWNER_NAME);
    }

    public float getHealthPercent() {
        return this.getMaxHealth() <= 0.0F ? 0.0F : this.getHealth() / this.getMaxHealth();
    }

    /** 心潮震荡增益：肉身受创时 +10% 伤害 5s，可叠 {@value #ECHO_RAGE_MAX_STACKS} 层。 */
    public void addEchoRageStack() {
        this.echoRageStacks = Math.min(ECHO_RAGE_MAX_STACKS, this.echoRageStacks + 1);
        this.echoRageExpireTicks = ECHO_RAGE_DURATION_TICKS;
    }

    private boolean isKillable() {
        byte stage = getAiStage();
        return stage == STAGE_SEVER || stage == STAGE_FRENZY;
    }

    private boolean isCombatActive() {
        byte stage = getAiStage();
        return stage != STAGE_PAUSED && stage != 0;
    }

    // ------------------------------------------------------------------ 战斗

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.level().isClientSide) {
            return super.hurt(source, amount);
        }
        // 破盾蓄力可被打断：前摇内受到 ≥1 伤害立即取消（§6.3）。
        if (this.windupRemainingTicks > 0 && amount >= 1.0F) {
            cancelWindup();
        }
        // THORN_MAIL 反震甲：近战/strike 类直接命中，按比例反弹为魂伤（§6.3，已接魂伤体系）。
        if (this.counterMoves.contains(MindDemonCounterMove.THORN_MAIL)
                && getAiStage() >= STAGE_REFLECT
                && source.getDirectEntity() instanceof ServerPlayer attacker
                && getOwnerUuid().map(attacker.getUUID()::equals).orElse(false)) {
            float reflected = amount * (float) MindDemonCounterMove.THORN_MAIL_REFLECT_RATIO;
            if (reflected > 0.0F) {
                applyCappedSoulInjury(
                        attacker,
                        reflected,
                        SoulInjuryUtility.InjurySource.SPELL_BACKLASH
                );
            }
        }
        // 剧情血量下限：决念未下（SEVER/HOLD 前）心魔不灭。
        if (!isKillable() && amount >= this.getHealth()) {
            amount = Math.max(0.0F, this.getHealth() - 1.0F);
            if (amount <= 0.0F) {
                return false;
            }
        }
        return super.hurt(source, amount);
    }

    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        boolean hurt = super.doHurtTarget(target);
        if (hurt && target instanceof ServerPlayer victim) {
            applyMindDemonHitInjury(victim);
        }
        return hurt;
    }

    /** 心魔命中转魂伤（魂伤体系接入，R5：单场累计上限防死螺旋）。 */
    private void applyMindDemonHitInjury(ServerPlayer victim) {
        applyCappedSoulInjury(
                victim,
                CommonConfig.SOUL_INJURY_SOURCE_MIND_DEMON_HIT.get(),
                SoulInjuryUtility.InjurySource.MIND_DEMON_HIT
        );
    }

    private void applyCappedSoulInjury(
            ServerPlayer victim,
            double amount,
            SoulInjuryUtility.InjurySource source
    ) {
        double remaining = CommonConfig.MIND_DEMON_SOUL_INJURY_CAP_PER_TRIAL.get() - this.soulInjuryDealt;
        if (remaining <= 0.0D || amount <= 0.0D) {
            return;
        }
        double applied = Math.min(amount, remaining);
        this.soulInjuryDealt += applied;
        SoulInjuryUtility.addInjury(victim, applied, source);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15D, true) {
            @Override
            public boolean canUse() {
                return isCombatActive() && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return isCombatActive() && super.canContinueToUse();
            }
        });
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        tickEchoRage();
        if (!isCombatActive()) {
            this.setTarget(null);
            this.getNavigation().stop();
            return;
        }
        if (this.tickCount % 20 == 0) {
            this.setTarget(resolveTarget(serverLevel));
        }
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) {
            return;
        }
        if (this.windupRemainingTicks > 0) {
            tickShieldBreakWindup(serverLevel, target);
            return;
        }
        if (this.airLockWarningTicks > 0) {
            tickAirLockWarning(serverLevel, target);
            return;
        }
        tickCooldownsAndCast(serverLevel, target);
    }

    private void tickEchoRage() {
        if (this.echoRageExpireTicks > 0 && --this.echoRageExpireTicks == 0) {
            this.echoRageStacks = 0;
        }
    }

    /** FOCUS_SUMMON：优先攻击护体傀儡（§6.3），否则锁定 owner。 */
    private LivingEntity resolveTarget(ServerLevel serverLevel) {
        ServerPlayer owner = getOwnerUuid()
                .map(uuid -> serverLevel.getServer().getPlayerList().getPlayer(uuid))
                .orElse(null);
        if (this.counterMoves.contains(MindDemonCounterMove.FOCUS_SUMMON)) {
            List<DefenseSpellGuardianEntity> guardians = serverLevel.getEntitiesOfClass(
                    DefenseSpellGuardianEntity.class,
                    this.getBoundingBox().inflate(16.0D),
                    LivingEntity::isAlive
            );
            if (!guardians.isEmpty()) {
                return guardians.get(0);
            }
        }
        return owner != null && owner.serverLevel() == serverLevel ? owner : null;
    }

    private void tickCooldownsAndCast(ServerLevel serverLevel, LivingEntity target) {
        boolean frenzy = getAiStage() == STAGE_FRENZY;
        boolean reflectUnlocked = getAiStage() >= STAGE_REFLECT && getAiStage() != STAGE_PAUSED;

        if (reflectUnlocked && this.counterMoves.contains(MindDemonCounterMove.SHIELD_BREAK)) {
            if (--this.shieldBreakCooldown <= 0 && this.distanceTo(target) < SHIELD_BREAK_HIT_RANGE) {
                startShieldBreakWindup(target);
                this.shieldBreakCooldown = scaledCooldown(SHIELD_BREAK_COOLDOWN_TICKS, frenzy);
                return;
            }
        }
        if (reflectUnlocked && this.counterMoves.contains(MindDemonCounterMove.AIR_LOCK)) {
            if (--this.airLockCooldown <= 0) {
                this.airLockWarningTicks = MindDemonCounterMove.AIR_LOCK_WARNING_TICKS;
                this.airLockCooldown = scaledCooldown(AIR_LOCK_COOLDOWN_TICKS, frenzy);
                return;
            }
        }
        if (--this.spellCooldown <= 0) {
            castMirroredSpell(serverLevel, target);
            this.spellCooldown = scaledCooldown(
                    SPELL_COOLDOWN_TICKS + this.random.nextInt(30),
                    frenzy
            );
        }
    }

    private static int scaledCooldown(int baseTicks, boolean frenzy) {
        return frenzy ? (int) Math.max(10, baseTicks * FRENZY_COOLDOWN_MULTIPLIER) : baseTicks;
    }

    // ------------------------------------------------------------------ 镜像施法（§6.3：直接经实体重载结算，表现内联）

    private void castMirroredSpell(ServerLevel serverLevel, LivingEntity target) {
        AttackSpellKind kind = this.mirroredSpells.get(this.nextSpellIndex % this.mirroredSpells.size());
        this.nextSpellIndex++;
        this.getLookControl().setLookAt(target, 30.0F, 30.0F);
        float damage = mirroredDamage(kind);
        switch (kind.getShape()) {
            case BOLT -> {
                if (this.distanceTo(target) > kind.getRange() || !this.hasLineOfSight(target)) {
                    return;
                }
                AttackSpellUtility.spawnBoltTrail(
                        serverLevel,
                        this.getEyePosition(),
                        target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D),
                        kind.getElement()
                );
                target.hurt(
                        AttackSpellUtility.createDamageSource(this, kind.getDamageTypeKey()),
                        damage
                );
                if (target instanceof ServerPlayer victim) {
                    applyMindDemonHitInjury(victim);
                }
            }
            case STRIKE -> {
                if (this.distanceTo(target) > kind.getRange()) {
                    return;
                }
                Vec3 strikePoint = target.position();
                AttackSpellUtility.spawnBurstParticles(
                        serverLevel,
                        strikePoint,
                        Math.max(1.0D, kind.getRadius()),
                        kind.getElement()
                );
                for (LivingEntity victim : AttackSpellUtility.findBurstTargets(
                        this, strikePoint, Math.max(1.0D, kind.getRadius()))) {
                    victim.hurt(
                            AttackSpellUtility.createDamageSource(this, kind.getDamageTypeKey()),
                            damage
                    );
                    if (victim instanceof ServerPlayer playerVictim) {
                        applyMindDemonHitInjury(playerVictim);
                    }
                }
            }
            case BURST -> {
                if (this.distanceTo(target) > kind.getRadius()) {
                    return;
                }
                Vec3 center = this.position();
                AttackSpellUtility.spawnBurstParticles(
                        serverLevel,
                        center,
                        kind.getRadius(),
                        kind.getElement()
                );
                for (LivingEntity victim : AttackSpellUtility.findBurstTargets(
                        this, center, kind.getRadius())) {
                    victim.hurt(
                            AttackSpellUtility.createDamageSource(this, kind.getDamageTypeKey()),
                            damage
                    );
                    if (victim instanceof ServerPlayer playerVictim) {
                        applyMindDemonHitInjury(playerVictim);
                    }
                }
            }
        }
        serverLevel.playSound(
                null,
                this.blockPosition(),
                SoundEvents.EVOKER_CAST_SPELL,
                SoundSource.HOSTILE,
                0.8F,
                0.6F
        );
    }

    private float mirroredDamage(AttackSpellKind kind) {
        float base = AttackSpellUtility.getScaledDamage(
                kind.getBaseDamage(),
                this.cultivationStrength,
                1.0D
        );
        double rageBonus = 1.0D + this.echoRageStacks * ECHO_RAGE_DAMAGE_BONUS;
        return (float) (base * this.karmaScale * rageBonus);
    }

    // ------------------------------------------------------------------ SHIELD_BREAK 破盾蓄力

    private void startShieldBreakWindup(LivingEntity target) {
        this.windupRemainingTicks = MindDemonCounterMove.SHIELD_BREAK_WINDUP_TICKS;
        this.windupTargetOrigin = target.position();
        this.entityData.set(CHARGING, true);
        this.getNavigation().stop();
    }

    private void tickShieldBreakWindup(ServerLevel serverLevel, LivingEntity target) {
        // 蓝光粒子向心聚集（前摇提示）。
        for (int index = 0; index < 4; index++) {
            double angle = this.random.nextDouble() * Math.PI * 2.0D;
            double distance = 1.6D + this.random.nextDouble();
            serverLevel.sendParticles(
                    ParticleTypes.SOUL_FIRE_FLAME,
                    this.getX() + Math.cos(angle) * distance,
                    this.getY() + 1.0D + this.random.nextDouble() * 0.8D,
                    this.getZ() + Math.sin(angle) * distance,
                    1,
                    -Math.cos(angle) * 0.2D,
                    0.0D,
                    -Math.sin(angle) * 0.2D,
                    0.05D
            );
        }
        // 位移 8 格取消。
        if (target.position().distanceTo(this.windupTargetOrigin)
                > MindDemonCounterMove.SHIELD_BREAK_CANCEL_DISTANCE) {
            cancelWindup();
            return;
        }
        if (--this.windupRemainingTicks > 0) {
            return;
        }
        this.entityData.set(CHARGING, false);
        if (target instanceof ServerPlayer owner && this.distanceTo(owner) < SHIELD_BREAK_HIT_RANGE) {
            int current = PlayerDefenseResourceData.getCurrent(owner);
            int drained = Mth.ceil(current * MindDemonCounterMove.SHIELD_BREAK_DEFENSE_DRAIN_RATIO);
            if (drained > 0) {
                PlayerDefenseResourceData.consume(owner, drained);
                net.seraphina.ninecun_lingtai.common.network.ModNetwork.sendDefenseResource(owner);
                owner.displayClientMessage(
                        Component.translatableWithFallback(
                                "message.ninecun_lingtai.mind_trial_shield_break",
                                "护体灵光被破妄一击撕开三成！"),
                        true
                );
            }
            serverLevel.playSound(
                    null,
                    this.blockPosition(),
                    SoundEvents.GLASS_BREAK,
                    SoundSource.HOSTILE,
                    1.2F,
                    0.7F
            );
        }
    }

    private void cancelWindup() {
        this.windupRemainingTicks = 0;
        this.entityData.set(CHARGING, false);
    }

    // ------------------------------------------------------------------ AIR_LOCK 封空圈

    private void tickAirLockWarning(ServerLevel serverLevel, LivingEntity target) {
        // 生成前 1s 预警粒子（走出范围免疫由半径判定天然实现）。
        spawnAirLockRing(serverLevel, target.position(), ParticleTypes.END_ROD, 24);
        if (--this.airLockWarningTicks > 0) {
            return;
        }
        if (target instanceof ServerPlayer owner
                && owner.position().distanceTo(this.position()) <= MindDemonCounterMove.AIR_LOCK_RADIUS * 2.0D) {
            net.seraphina.ninecun_lingtai.common.inner_demon.MindTrialManager.applyAirLock(owner);
            spawnAirLockRing(serverLevel, owner.position(), ParticleTypes.REVERSE_PORTAL, 48);
            owner.displayClientMessage(
                    Component.translatableWithFallback(
                            "message.ninecun_lingtai.mind_trial_air_lock",
                            "困灵封空！短促真言难出，位移法术受禁"),
                    true
            );
        }
    }

    private void spawnAirLockRing(ServerLevel serverLevel, Vec3 center, net.minecraft.core.particles.SimpleParticleType particle, int count) {
        for (int index = 0; index < count; index++) {
            double angle = (Math.PI * 2.0D / count) * index;
            serverLevel.sendParticles(
                    particle,
                    center.x + Math.cos(angle) * MindDemonCounterMove.AIR_LOCK_RADIUS,
                    center.y + 0.2D,
                    center.z + Math.sin(angle) * MindDemonCounterMove.AIR_LOCK_RADIUS,
                    1,
                    0.0D,
                    0.4D,
                    0.0D,
                    0.0D
            );
        }
    }

    // ------------------------------------------------------------------ 实体通性

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public boolean requiresCustomPersistence() {
        return true;
    }

    @Override
    public boolean canBeLeashed(Player player) {
        return false;
    }

    @Override
    public boolean shouldDropLoot() {
        return false;
    }

    @Override
    public void dropEquipment() {
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compoundTag) {
        super.addAdditionalSaveData(compoundTag);
        getOwnerUuid().ifPresent(ownerUuid -> compoundTag.putUUID("OwnerUuid", ownerUuid));
        compoundTag.putString("OwnerName", getOwnerName());
        if (this.trialId != null) {
            compoundTag.putUUID("TrialId", this.trialId);
        }
        compoundTag.putByte("AiStage", getAiStage());
        compoundTag.putDouble("KarmaScale", this.karmaScale);
        compoundTag.putInt("CultivationStrength", this.cultivationStrength);
        net.minecraft.nbt.ListTag spells = new net.minecraft.nbt.ListTag();
        this.mirroredSpells.forEach(kind ->
                spells.add(net.minecraft.nbt.StringTag.valueOf(kind.getSerializedName())));
        compoundTag.put("MirroredSpells", spells);
        net.minecraft.nbt.ListTag moves = new net.minecraft.nbt.ListTag();
        this.counterMoves.forEach(move ->
                moves.add(net.minecraft.nbt.StringTag.valueOf(move.name())));
        compoundTag.put("CounterMoves", moves);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compoundTag) {
        super.readAdditionalSaveData(compoundTag);
        if (compoundTag.contains("OwnerUuid", Tag.TAG_INT_ARRAY)) {
            this.entityData.set(OWNER_UUID, Optional.of(compoundTag.getUUID("OwnerUuid")));
        }
        this.entityData.set(OWNER_NAME, compoundTag.getString("OwnerName"));
        if (compoundTag.hasUUID("TrialId")) {
            this.trialId = compoundTag.getUUID("TrialId");
        }
        setAiStage(compoundTag.getByte("AiStage"));
        this.karmaScale = Mth.clamp(
                compoundTag.getDouble("KarmaScale"),
                MindDemonSnapshot.karmaScaleBase(),
                MindDemonSnapshot.karmaScaleCap()
        );
        this.cultivationStrength = Math.max(1, compoundTag.getInt("CultivationStrength"));
        this.mirroredSpells.clear();
        for (Tag spellTag : compoundTag.getList("MirroredSpells", Tag.TAG_STRING)) {
            AttackSpellKind.fromSerializedName(spellTag.getAsString())
                    .ifPresent(this.mirroredSpells::add);
        }
        if (this.mirroredSpells.isEmpty()) {
            this.mirroredSpells.add(AttackSpellKind.YIN_FINGER);
        }
        this.counterMoves = EnumSet.noneOf(MindDemonCounterMove.class);
        for (Tag moveTag : compoundTag.getList("CounterMoves", Tag.TAG_STRING)) {
            MindDemonCounterMove move = MindDemonCounterMove.byName(moveTag.getAsString());
            if (move != null) {
                this.counterMoves.add(move);
            }
        }
    }

    @Override
    public void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(OWNER_UUID, Optional.empty());
        this.entityData.define(OWNER_NAME, "");
        this.entityData.define(AI_STAGE, (byte) 0);
        this.entityData.define(CHARGING, false);
    }
}
