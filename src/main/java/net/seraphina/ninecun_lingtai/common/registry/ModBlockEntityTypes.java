package net.seraphina.ninecun_lingtai.common.registry;

import net.seraphina.ninecun_lingtai.ModMain;
import net.seraphina.ninecun_lingtai.common.block.entity.AlchemyFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlockEntityTypes {
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(
            ForgeRegistries.BLOCK_ENTITY_TYPES,
            ModMain.MOD_ID
    );

    public static final RegistryObject<BlockEntityType<AlchemyFurnaceBlockEntity>> ALCHEMY_FURNACE =
            BLOCK_ENTITY_TYPES.register(
                    "alchemy_furnace",
                    () -> BlockEntityType.Builder
                            .of(
                                    AlchemyFurnaceBlockEntity::new,
                                    ModBlocks.ALCHEMY_FURNACE.get(),
                                    ModBlocks.RED_COPPER_ALCHEMY_FURNACE.get(),
                                    ModBlocks.COLD_JADE_ALCHEMY_FURNACE.get(),
                                    ModBlocks.DARK_CRYSTAL_ALCHEMY_FURNACE.get(),
                                    ModBlocks.NINE_DRAGON_ALCHEMY_FURNACE.get()
                            )
                            .build(null)
            );

    public static void register(IEventBus modEventBus) {
        BLOCK_ENTITY_TYPES.register(modEventBus);
    }
}
