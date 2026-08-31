package net.seraphina.ninecun_lingtai.common.entity;

import net.seraphina.ninecun_lingtai.common.inner_demon.MindTrialManager;
import net.seraphina.ninecun_lingtai.common.registry.ModDamageTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 劫场肉身替身（心魔即己 §4.3）：玩家本体进心境后留在公开劫场的替身，
 * 可被攻击、可被护法。
 *
 * <ul>
 *   <li>受击转发（心潮震荡）：{@link #hurt} 不直接扣替身血，而是
 *       {@code MindTrialManager.onBodyHurt}——玩家在心境内受同源真实伤害
 *       × bodyEchoRatio，心魔获得小增益；替身血量每 10 tick 镜像玩家血量。</li>
 *   <li>劫雷免疫（R2）：对 {@code REALM_EXECUTION} 伤害类型在 hurt 内直接豁免，
 *       无需改 {@code ThunderTribulationEntity}。</li>
 *   <li>不掉落任何物品；渲染层用 owner 皮肤盘坐姿态（{@code MortalBodyRenderer}）。</li>
 * </ul>
 */
public final class MortalBodyEntity extends LivingEntity {
    private static final String OWNER_UUID_KEY = "OwnerUuid";
    private static final String OWNER_NAME_KEY = "OwnerName";
    private static final String TRIAL_ID_KEY = "TrialId";

    private static final EntityDataAccessor<Optional<UUID>> OWNER_UUID = SynchedEntityData.defineId(
            MortalBodyEntity.class,
            EntityDataSerializers.OPTIONAL_UUID
    );
    private static final EntityDataAccessor<String> OWNER_NAME = SynchedEntityData.defineId(
            MortalBodyEntity.class,
            EntityDataSerializers.STRING
    );

    private UUID trialId;

    public MortalBodyEntity(EntityType<? extends MortalBodyEntity> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(false);
        this.setInvulnerable(false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return LivingEntity.createLivingAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.ARMOR, 0.0D);
    }

    /** 生成时写入 owner 身份与当前面板（心魔即己 §4.3）。 */
    public void initialize(ServerPlayer owner, UUID trialId) {
        this.entityData.set(OWNER_UUID, Optional.of(owner.getUUID()));
        this.entityData.set(OWNER_NAME, owner.getGameProfile().getName());
        this.trialId = trialId;
        mirrorHealth(owner);
        this.setYRot(owner.getYRot());
        this.setYBodyRot(owner.getYRot());
        this.setYHeadRot(owner.getYRot());
    }

    /** 血量镜像：替身最大/当前血量随玩家（每 10 tick 由 MindTrialManager 调用）。 */
    public void mirrorHealth(ServerPlayer owner) {
        if (this.getAttribute(Attributes.MAX_HEALTH) != null) {
            this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(owner.getMaxHealth());
        }
        this.setHealth(Math.max(0.0F, owner.getHealth()));
    }

    public Optional<UUID> getOwnerUuid() {
        return this.entityData.get(OWNER_UUID);
    }

    public String getOwnerName() {
        return this.entityData.get(OWNER_NAME);
    }

    public UUID getTrialId() {
        return this.trialId;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.level().isClientSide || this.isRemoved()) {
            return false;
        }
        // 劫雷免疫（R2）：劫云雷用 REALM_EXECUTION 结算，替身直接豁免，防止雷伤二次回响进心境。
        if (source.is(ModDamageTypes.REALM_EXECUTION)) {
            return false;
        }
        // /kill 等绕过无敌的伤害仍然生效（替身消失 → 心境崩塌由 MindTrialManager tick 兜底）。
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return super.hurt(source, amount);
        }
        MindTrialManager.onBodyHurt(this, source, amount);
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        this.setDeltaMovement(this.getDeltaMovement().multiply(0.0D, 1.0D, 0.0D));
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void doPush(net.minecraft.world.entity.Entity entity) {
    }

    @Override
    public void dropAllDeathLoot(DamageSource damageSource) {
        // 替身绝不掉落任何东西（含经验）。
    }

    @Override
    public Vec3 getLeashOffset() {
        return Vec3.ZERO;
    }

    @Override
    public Iterable<ItemStack> getArmorSlots() {
        return List.of();
    }

    @Override
    public ItemStack getItemBySlot(EquipmentSlot slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public void setItemSlot(EquipmentSlot slot, ItemStack itemStack) {
    }

    @Override
    public HumanoidArm getMainArm() {
        return HumanoidArm.RIGHT;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compoundTag) {
        super.addAdditionalSaveData(compoundTag);
        getOwnerUuid().ifPresent(ownerUuid -> compoundTag.putUUID(OWNER_UUID_KEY, ownerUuid));
        compoundTag.putString(OWNER_NAME_KEY, getOwnerName());
        if (this.trialId != null) {
            compoundTag.putUUID(TRIAL_ID_KEY, this.trialId);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compoundTag) {
        super.readAdditionalSaveData(compoundTag);
        if (compoundTag.contains(OWNER_UUID_KEY, Tag.TAG_INT_ARRAY)) {
            this.entityData.set(OWNER_UUID, Optional.of(compoundTag.getUUID(OWNER_UUID_KEY)));
        }
        this.entityData.set(OWNER_NAME, compoundTag.getString(OWNER_NAME_KEY));
        if (compoundTag.hasUUID(TRIAL_ID_KEY)) {
            this.trialId = compoundTag.getUUID(TRIAL_ID_KEY);
        }
    }

    @Override
    public void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(OWNER_UUID, Optional.empty());
        this.entityData.define(OWNER_NAME, "");
    }
}
