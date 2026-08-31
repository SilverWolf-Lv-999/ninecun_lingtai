package net.seraphina.ninecun_lingtai.common.registry;

import net.seraphina.ninecun_lingtai.ModMain;
import net.seraphina.ninecun_lingtai.common.item.TombRelicItem;
import net.seraphina.ninecun_lingtai.common.tomb.CultivatorTombFeature;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 修士墓穴切片（M4）的独立注册类：墓穴产出物品 + 墓穴雕刻 feature。
 *
 * <p>并发约束：本波次禁改 {@code ModMain}/{@code ModItems}/{@code ModFeatures}，
 * 故墓穴内容自带 {@link DeferredRegister}。运行期挂载由主控在 {@code ModMain} 构造器中
 * 统一补一行 {@code ModTombContent.register(modEventBus);}（见开发日志集中接线清单）。
 * 接线前所有 {@link RegistryObject} 均未注册，使用方一律先判 {@code isPresent()}。</p>
 */
public final class ModTombContent {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(
            ForgeRegistries.ITEMS,
            ModMain.MOD_ID
    );
    private static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(
            ForgeRegistries.FEATURES,
            ModMain.MOD_ID
    );

    /** 墓主残魂：击杀墓主掉落的魂道机缘之物（6→7 星封顶开关的钥匙之一）。 */
    public static final RegistryObject<Item> TOMB_MASTER_REMNANT_SOUL = ITEMS.register(
            "tomb_master_remnant_soul",
            () -> new TombRelicItem(
                    new Item.Properties().stacksTo(16).rarity(Rarity.EPIC),
                    "墓主残魂",
                    "上古修士兵解后不散的一缕真灵，可为魂桥引路，点亮第七星",
                    true
            )
    );

    /** 魂泉水：高阶墓穴宝箱的稀有机缘之物，效用与墓主残魂等同。 */
    public static final RegistryObject<Item> SOUL_SPRING_WATER = ITEMS.register(
            "soul_spring_water",
            () -> new TombRelicItem(
                    new Item.Properties().stacksTo(16).rarity(Rarity.RARE),
                    "魂泉水",
                    "自九幽魂泉汲出的一捧幽光，饮之魂魄清明，可代残魂架起魂桥",
                    true
            )
    );

    /** 法宝残料：法宝体系落地前的素材占位（炼器/器修远期日志承接）。 */
    public static final RegistryObject<Item> ARTIFACT_MATERIAL_FRAGMENT = ITEMS.register(
            "artifact_material_fragment",
            () -> new TombRelicItem(
                    new Item.Properties().stacksTo(64).rarity(Rarity.UNCOMMON),
                    "法宝残料",
                    "陪葬法宝崩解后的残片，灵性未泯，来日炼器可作胚料",
                    false
            )
    );

    /** 修士墓穴雕刻 feature（挂 minecraft:is_overworld 的 underground_structures 步骤）。 */
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> CULTIVATOR_TOMB = FEATURES.register(
            "cultivator_tomb",
            () -> new CultivatorTombFeature(NoneFeatureConfiguration.CODEC)
    );

    private ModTombContent() {
    }

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
        FEATURES.register(modEventBus);
    }
}
