package net.seraphina.ninecun_lingtai.common.registry;

import net.seraphina.ninecun_lingtai.ModMain;
import net.seraphina.ninecun_lingtai.api.element.ElementType;
import net.seraphina.ninecun_lingtai.common.block.FormationDiscBlock;
import net.seraphina.ninecun_lingtai.common.block.FormationFlagBlock;
import net.seraphina.ninecun_lingtai.common.block.entity.FormationDiscBlockEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 阵法内容独立注册类（M4 并发约束：不动 ModBlocks/ModItems/ModBlockEntityTypes）。
 * RegistryObject 全部懒加载，未接 ModMain 时可通过编译；
 * <b>运行期挂载需主控在 ModMain 构造器加一行：{@code ModFormationContent.register(modEventBus);}</b>
 */
public final class ModFormationContent {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(
            ForgeRegistries.BLOCKS,
            ModMain.MOD_ID
    );
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(
            ForgeRegistries.ITEMS,
            ModMain.MOD_ID
    );
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(
            ForgeRegistries.BLOCK_ENTITY_TYPES,
            ModMain.MOD_ID
    );
    // ==================== 方块 ====================

    public static final RegistryObject<Block> FORMATION_DISC = BLOCKS.register(
            "formation_disc",
            FormationDiscBlock::new
    );

    public static final RegistryObject<Block> FORMATION_FLAG_METAL = BLOCKS.register(
            "formation_flag_metal",
            () -> new FormationFlagBlock(ElementType.METAL)
    );

    public static final RegistryObject<Block> FORMATION_FLAG_WOOD = BLOCKS.register(
            "formation_flag_wood",
            () -> new FormationFlagBlock(ElementType.WOOD)
    );

    public static final RegistryObject<Block> FORMATION_FLAG_WATER = BLOCKS.register(
            "formation_flag_water",
            () -> new FormationFlagBlock(ElementType.WATER)
    );

    public static final RegistryObject<Block> FORMATION_FLAG_FIRE = BLOCKS.register(
            "formation_flag_fire",
            () -> new FormationFlagBlock(ElementType.FIRE)
    );

    public static final RegistryObject<Block> FORMATION_FLAG_EARTH = BLOCKS.register(
            "formation_flag_earth",
            () -> new FormationFlagBlock(ElementType.EARTH)
    );

    // ==================== 物品（带中文回退名，无 lang 也可跑） ====================

    public static final RegistryObject<Item> FORMATION_DISC_ITEM = ITEMS.register(
            "formation_disc",
            () -> new FallbackNameBlockItem(FORMATION_DISC.get(), "阵盘")
    );

    public static final RegistryObject<Item> FORMATION_FLAG_METAL_ITEM = ITEMS.register(
            "formation_flag_metal",
            () -> new FallbackNameBlockItem(FORMATION_FLAG_METAL.get(), "阵旗·金")
    );

    public static final RegistryObject<Item> FORMATION_FLAG_WOOD_ITEM = ITEMS.register(
            "formation_flag_wood",
            () -> new FallbackNameBlockItem(FORMATION_FLAG_WOOD.get(), "阵旗·木")
    );

    public static final RegistryObject<Item> FORMATION_FLAG_WATER_ITEM = ITEMS.register(
            "formation_flag_water",
            () -> new FallbackNameBlockItem(FORMATION_FLAG_WATER.get(), "阵旗·水")
    );

    public static final RegistryObject<Item> FORMATION_FLAG_FIRE_ITEM = ITEMS.register(
            "formation_flag_fire",
            () -> new FallbackNameBlockItem(FORMATION_FLAG_FIRE.get(), "阵旗·火")
    );

    public static final RegistryObject<Item> FORMATION_FLAG_EARTH_ITEM = ITEMS.register(
            "formation_flag_earth",
            () -> new FallbackNameBlockItem(FORMATION_FLAG_EARTH.get(), "阵旗·土")
    );

    // ==================== 方块实体 ====================

    public static final RegistryObject<BlockEntityType<FormationDiscBlockEntity>> FORMATION_DISC_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register(
                    "formation_disc",
                    () -> BlockEntityType.Builder
                            .of(FormationDiscBlockEntity::new, FORMATION_DISC.get())
                            .build(null)
            );

    private ModFormationContent() {
    }

    /** 主控接线点：ModMain 构造器中调用一次。 */
    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITY_TYPES.register(modEventBus);
    }

    /** 物品名带中文回退（lang 缺失时不显示裸键，M4 文案约束）。 */
    private static final class FallbackNameBlockItem extends BlockItem {
        private final String fallbackName;

        private FallbackNameBlockItem(Block block, String fallbackName) {
            super(block, new Item.Properties());
            this.fallbackName = fallbackName;
        }

        @Override
        public Component getName(ItemStack itemStack) {
            return Component.translatableWithFallback(getDescriptionId(itemStack), this.fallbackName);
        }
    }
}
