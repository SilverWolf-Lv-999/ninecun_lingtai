package net.seraphina.ninecun_lingtai.common.block.entity;

import net.seraphina.ninecun_lingtai.common.formation.FormationRuntime;
import net.seraphina.ninecun_lingtai.common.formation.FormationTuning;
import net.seraphina.ninecun_lingtai.common.registry.ModFormationContent;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/**
 * 阵盘方块实体：只承载"载入的阵图 + 灵石储能"两样慢数据；
 * 运转状态的权威在 FormationSavedData（实例存在即运转），本类不带 ticker，
 * 全部驱动来自 FormationRuntime 的服务端 tick（施工图 6.4 口径）。
 */
public class FormationDiscBlockEntity extends BlockEntity {
    private static final String FORMATION_ID_TAG = "FormationId";
    private static final String STORED_ENERGY_TAG = "StoredEnergy";

    @Nullable
    private ResourceLocation selectedFormationId;
    private double storedEnergy;

    public FormationDiscBlockEntity(BlockPos pos, BlockState state) {
        super(ModFormationContent.FORMATION_DISC_BLOCK_ENTITY.get(), pos, state);
    }

    @Nullable
    public ResourceLocation getSelectedFormationId() {
        return this.selectedFormationId;
    }

    public void setSelectedFormationId(@Nullable ResourceLocation formationId) {
        this.selectedFormationId = formationId;
        setChanged();
    }

    public double getStoredEnergy() {
        return this.storedEnergy;
    }

    /** @return 实际充入量（容量封顶）。 */
    public double addEnergy(double amount) {
        double accepted = Math.min(amount, FormationTuning.DISC_ENERGY_CAPACITY - this.storedEnergy);
        if (accepted > 0.0D) {
            this.storedEnergy += accepted;
            setChanged();
        }
        return Math.max(0.0D, accepted);
    }

    /** @return true = 储能足额扣减成功；false = 不足（分文未动）。 */
    public boolean consumeEnergy(double amount) {
        if (this.storedEnergy < amount) {
            return false;
        }
        this.storedEnergy -= amount;
        setChanged();
        return true;
    }

    /** 吸伤折算等场景的"能扣多少扣多少"。@return 实际扣减量。 */
    public double drainEnergyUpTo(double amount) {
        double drained = Math.min(amount, this.storedEnergy);
        if (drained > 0.0D) {
            this.storedEnergy -= drained;
            setChanged();
        }
        return drained;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (this.level instanceof ServerLevel serverLevel) {
            FormationRuntime.trackDisc(serverLevel, this.worldPosition);
        }
    }

    @Override
    public void setRemoved() {
        if (this.level instanceof ServerLevel serverLevel) {
            FormationRuntime.untrackDisc(serverLevel, this.worldPosition);
        }
        super.setRemoved();
    }

    @Override
    public void onChunkUnloaded() {
        if (this.level instanceof ServerLevel serverLevel) {
            FormationRuntime.untrackDisc(serverLevel, this.worldPosition);
        }
        super.onChunkUnloaded();
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.selectedFormationId = tag.contains(FORMATION_ID_TAG)
                ? ResourceLocation.tryParse(tag.getString(FORMATION_ID_TAG))
                : null;
        this.storedEnergy = tag.getDouble(STORED_ENERGY_TAG);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (this.selectedFormationId != null) {
            tag.putString(FORMATION_ID_TAG, this.selectedFormationId.toString());
        }
        tag.putDouble(STORED_ENERGY_TAG, this.storedEnergy);
    }
}
