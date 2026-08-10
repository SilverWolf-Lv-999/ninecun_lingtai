package com.seraphina.ninecun_lingtai.integration.jade;

import com.seraphina.ninecun_lingtai.utility.AlchemyFurnaceStructureUtility;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.BlockHitResult;
import snownee.jade.api.Accessor;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public final class LifespanJadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerEntityDataProvider(LifespanJadeProvider.INSTANCE, LivingEntity.class);
        registration.registerEntityDataProvider(CultivationJadeProvider.INSTANCE, LivingEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerEntityComponent(LifespanJadeProvider.INSTANCE, LivingEntity.class);
        registration.registerEntityComponent(CultivationJadeProvider.INSTANCE, LivingEntity.class);
        registration.addRayTraceCallback((hitResult, accessor, originalAccessor) ->
                redirectAlchemyFurnacePartTarget(registration, accessor));
    }

    private static Accessor<?> redirectAlchemyFurnacePartTarget(
            IWailaClientRegistration registration,
            Accessor<?> accessor
    ) {
        if (!(accessor instanceof BlockAccessor blockAccessor)) {
            return accessor;
        }

        BlockPos furnacePosition = AlchemyFurnaceStructureUtility.getMainFurnacePosition(
                blockAccessor.getLevel(),
                blockAccessor.getPosition()
        );
        if (furnacePosition == null || furnacePosition.equals(blockAccessor.getPosition())) {
            return accessor;
        }

        BlockHitResult originalHitResult = blockAccessor.getHitResult();
        return registration.blockAccessor()
                .from(blockAccessor)
                .hit(new BlockHitResult(
                        originalHitResult.getLocation(),
                        originalHitResult.getDirection(),
                        furnacePosition,
                        originalHitResult.isInside()
                ))
                .blockState(blockAccessor.getLevel().getBlockState(furnacePosition))
                .blockEntity(blockAccessor.getLevel().getBlockEntity(furnacePosition))
                .build();
    }
}
