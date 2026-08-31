package net.seraphina.ninecun_lingtai.common.tomb;

import net.seraphina.ninecun_lingtai.ModMain;
import net.seraphina.ninecun_lingtai.common.registry.ModCreativeModeTabs;
import net.seraphina.ninecun_lingtai.common.registry.ModTombContent;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;

/**
 * 把墓穴产出物品补进本模组创造标签（不改 {@code ModCreativeModeTabs}，
 * 走 mod 总线事件追加；未接线（物品未注册）时静默跳过）。
 */
@Mod.EventBusSubscriber(modid = ModMain.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class TombCreativeTabHandler {
    @SubscribeEvent
    public static void onBuildCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (!event.getTabKey().equals(ModCreativeModeTabs.NINECUN_LINGTAI.getKey())) {
            return;
        }
        List<RegistryObject<Item>> tombItems = List.of(
                ModTombContent.TOMB_MASTER_REMNANT_SOUL,
                ModTombContent.SOUL_SPRING_WATER,
                ModTombContent.ARTIFACT_MATERIAL_FRAGMENT
        );
        for (RegistryObject<Item> item : tombItems) {
            if (item.isPresent()) {
                event.accept(item.get());
            }
        }
    }

    private TombCreativeTabHandler() {
    }
}
