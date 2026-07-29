package com.seraphina.ninecun_lingtai.common.registry;

import com.seraphina.ninecun_lingtai.ModMain;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(
            ForgeRegistries.BLOCKS,
            ModMain.MOD_ID
    );

    public static final RegistryObject<Block> SPIRITUAL_STONE_ORE = BLOCKS.register(
            "spiritual_stone_ore",
            () -> new DropExperienceBlock(
                    BlockBehaviour.Properties.copy(Blocks.DIAMOND_ORE),
                    UniformInt.of(2, 5)
            )
    );

    public static final RegistryObject<Block> DEEPSLATE_SPIRITUAL_STONE_ORE = BLOCKS.register(
            "deepslate_spiritual_stone_ore",
            () -> new DropExperienceBlock(
                    BlockBehaviour.Properties.copy(Blocks.DEEPSLATE_DIAMOND_ORE),
                    UniformInt.of(2, 5)
            )
    );


    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
    }
}
