package com.seraphina.ninecun_lingtai.common.block.entity;

import com.seraphina.ninecun_lingtai.common.registry.ModBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

public final class AlchemyFurnaceBlockEntity extends BlockEntity implements GeoBlockEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public AlchemyFurnaceBlockEntity(BlockPos blockPosition, BlockState blockState) {
        super(ModBlockEntityTypes.ALCHEMY_FURNACE.get(), blockPosition, blockState);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(
                this.worldPosition.getX() - 1.0625D,
                this.worldPosition.getY(),
                this.worldPosition.getZ() - 1.0625D,
                this.worldPosition.getX() + 2.0625D,
                this.worldPosition.getY() + 3.0D,
                this.worldPosition.getZ() + 2.0625D
        );
    }
}
