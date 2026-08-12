package net.seraphina.ninecun_lingtai.common.event;

import net.seraphina.ninecun_lingtai.ModMain;
import net.seraphina.ninecun_lingtai.common.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ModMain.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SpiritualStoneOreEventHandler {
    private static final float SILK_TOUCH_EXPLOSION_RADIUS = 3.0F;

    @SubscribeEvent
    public static void onBreakBlock(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof Level level) || level.isClientSide()) {
            return;
        }
        if (!isSpiritualStoneOre(event.getState().getBlock())) {
            return;
        }
        if (event.getPlayer().getMainHandItem().getEnchantmentLevel(Enchantments.SILK_TOUCH) <= 0) {
            return;
        }

        BlockPos pos = event.getPos();
        event.setCanceled(true);
        level.removeBlock(pos, false);
        level.explode(
                event.getPlayer(),
                pos.getX() + 0.5D,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D,
                SILK_TOUCH_EXPLOSION_RADIUS,
                Level.ExplosionInteraction.BLOCK
        );
    }

    private static boolean isSpiritualStoneOre(Block block) {
        return block == ModBlocks.SPIRITUAL_STONE_ORE.get()
                || block == ModBlocks.DEEPSLATE_SPIRITUAL_STONE_ORE.get();
    }
}
