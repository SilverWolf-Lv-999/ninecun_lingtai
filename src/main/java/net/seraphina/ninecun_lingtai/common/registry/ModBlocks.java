package net.seraphina.ninecun_lingtai.common.registry;

import net.seraphina.ninecun_lingtai.ModMain;
import net.seraphina.ninecun_lingtai.common.block.AlchemyFurnaceBlock;
import net.seraphina.ninecun_lingtai.common.block.AlchemyFurnacePartBlock;
import net.seraphina.ninecun_lingtai.common.block.plant.EarthMarrowGinsengBlock;
import net.seraphina.ninecun_lingtai.common.block.plant.FireLingzhiBlock;
import net.seraphina.ninecun_lingtai.common.block.plant.JadeMindFlowerBlock;
import net.seraphina.ninecun_lingtai.common.block.plant.MoonDewOrchidBlock;
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
    private static final int SPIRITUAL_STONE_LIGHT_LEVEL = 12;
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(
            ForgeRegistries.BLOCKS,
            ModMain.MOD_ID
    );

    public static final RegistryObject<Block> SPIRITUAL_STONE_ORE = BLOCKS.register(
            "spiritual_stone_ore",
            () -> new DropExperienceBlock(
                    BlockBehaviour.Properties.copy(Blocks.DIAMOND_ORE)
                            .lightLevel(blockState -> SPIRITUAL_STONE_LIGHT_LEVEL),
                    UniformInt.of(2, 5)
            )
    );

    public static final RegistryObject<Block> DEEPSLATE_SPIRITUAL_STONE_ORE = BLOCKS.register(
            "deepslate_spiritual_stone_ore",
            () -> new DropExperienceBlock(
                    BlockBehaviour.Properties.copy(Blocks.DEEPSLATE_DIAMOND_ORE)
                            .lightLevel(blockState -> SPIRITUAL_STONE_LIGHT_LEVEL),
                    UniformInt.of(2, 5)
            )
    );

    public static final RegistryObject<Block> ALCHEMY_FURNACE = BLOCKS.register(
            "alchemy_furnace",
            AlchemyFurnaceBlock::new
    );

    // 丹炉五阶（炼丹 M4）：同一 Block 类注册五次，阶别属性走
    // data/ninecun_lingtai/alchemy_furnaces/*.json 数据驱动，按注册名查表。
    public static final RegistryObject<Block> RED_COPPER_ALCHEMY_FURNACE = BLOCKS.register(
            "red_copper_alchemy_furnace",
            AlchemyFurnaceBlock::new
    );

    public static final RegistryObject<Block> COLD_JADE_ALCHEMY_FURNACE = BLOCKS.register(
            "cold_jade_alchemy_furnace",
            AlchemyFurnaceBlock::new
    );

    public static final RegistryObject<Block> DARK_CRYSTAL_ALCHEMY_FURNACE = BLOCKS.register(
            "dark_crystal_alchemy_furnace",
            AlchemyFurnaceBlock::new
    );

    public static final RegistryObject<Block> NINE_DRAGON_ALCHEMY_FURNACE = BLOCKS.register(
            "nine_dragon_alchemy_furnace",
            AlchemyFurnaceBlock::new
    );

    public static final RegistryObject<Block> ALCHEMY_FURNACE_PART = BLOCKS.register(
            "alchemy_furnace_part",
            AlchemyFurnacePartBlock::new
    );

    public static final RegistryObject<Block> FIRE_LINGZHI = BLOCKS.register(
            "fire_lingzhi",
            FireLingzhiBlock::new
    );

    public static final RegistryObject<Block> MOON_DEW_ORCHID = BLOCKS.register(
            "moon_dew_orchid",
            MoonDewOrchidBlock::new
    );

    public static final RegistryObject<Block> EARTH_MARROW_GINSENG = BLOCKS.register(
            "earth_marrow_ginseng",
            EarthMarrowGinsengBlock::new
    );

    public static final RegistryObject<Block> JADE_MIND_FLOWER = BLOCKS.register(
            "jade_mind_flower",
            JadeMindFlowerBlock::new
    );

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
    }
}
