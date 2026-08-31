package net.seraphina.ninecun_lingtai.client.event;

import net.seraphina.ninecun_lingtai.ModMain;
import net.seraphina.ninecun_lingtai.common.seal.SealItemUtility;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 封印物品的 tooltip（只读物品上的公开展示快照——层数是否存在、公开品阶、死契，
 * 不含任何层内容；信息敬畏：层数与类型外人不可知）。
 */
@Mod.EventBusSubscriber(modid = ModMain.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class SealTooltipEventHandler {

    private SealTooltipEventHandler() {
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        if (!SealItemUtility.isDisplaySealed(event.getItemStack())) {
            return;
        }
        event.getToolTip().add(Component.translatable(
                "tooltip.ninecun_lingtai.seal.sealed",
                SealItemUtility.getDisplayGrade(event.getItemStack())
        ).withStyle(ChatFormatting.DARK_PURPLE));
        if (SealItemUtility.isDisplayDeathPact(event.getItemStack())) {
            event.getToolTip().add(Component.translatable("tooltip.ninecun_lingtai.seal.death_pact")
                    .withStyle(ChatFormatting.DARK_RED));
        }
    }
}
