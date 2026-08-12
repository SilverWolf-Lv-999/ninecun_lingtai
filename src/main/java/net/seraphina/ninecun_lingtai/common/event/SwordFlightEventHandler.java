package net.seraphina.ninecun_lingtai.common.event;

import net.seraphina.ninecun_lingtai.ModMain;
import net.seraphina.ninecun_lingtai.common.network.ModNetwork;
import net.seraphina.ninecun_lingtai.common.player.PlayerSwordFlightData;
import net.seraphina.ninecun_lingtai.common.player.PlayerTechniqueData;
import net.seraphina.ninecun_lingtai.common.spell.SwordFlightManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** 御剑术的事件入口：蹲下右键登剑、每刻维持、锁剑防丢与生死进退的善后。 */
@Mod.EventBusSubscriber(modid = ModMain.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SwordFlightEventHandler {

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (tryMountFromInteraction(event.getEntity(), event.getHand())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (tryMountFromInteraction(event.getEntity(), event.getHand())) {
            event.setCanceled(true);
        }
    }

    /** 手持剑、立于地面且蹲身的玩家右键即尝试登剑；灵力与参悟检查由施法管线完成。 */
    private static boolean tryMountFromInteraction(Player entity, InteractionHand hand) {
        if (!(entity instanceof ServerPlayer player) || hand != InteractionHand.MAIN_HAND) {
            return false;
        }
        if (!player.isShiftKeyDown()
                || !player.onGround()
                || PlayerSwordFlightData.isFlying(player)
                || !(player.getMainHandItem().getItem() instanceof SwordItem)) {
            return false;
        }
        if (!PlayerTechniqueData.hasTechnique(player, SwordFlightManager.TECHNIQUE_ID)) {
            return false;
        }
        return SwordFlightManager.requestMount(player, false);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player) {
            SwordFlightManager.serverTick(player);
        }
    }

    /** 御剑之时，足下之剑不可丢弃：拦下丢出的剑并放回原槽。 */
    @SubscribeEvent
    public static void onItemToss(ItemTossEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player) || !PlayerSwordFlightData.isFlying(player)) {
            return;
        }
        ItemStack tossedStack = event.getEntity().getItem();
        ItemStack lockedSword = PlayerSwordFlightData.getSword(player);
        if (!tossedStack.is(lockedSword.getItem())) {
            return;
        }
        int slot = PlayerSwordFlightData.getLockedSlot(player);
        // 原槽还留着同类剑，说明丢的是另一把，随它去。
        if (slot >= 0 && player.getInventory().getItem(slot).is(lockedSword.getItem())) {
            return;
        }
        event.setCanceled(true);
        if (slot >= 0 && player.getInventory().getItem(slot).isEmpty()) {
            player.getInventory().setItem(slot, tossedStack);
        } else {
            player.getInventory().add(tossedStack);
        }
        player.displayClientMessage(Component.literal("御剑之时，剑不可离身"), true);
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && PlayerSwordFlightData.isFlying(player)) {
            SwordFlightManager.dismount(player);
        }
    }

    /** 御剑状态存于 persistent NBT，下线再上线时人仍在剑上，须重授飞行能力并补同步。 */
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (PlayerSwordFlightData.isFlying(player)) {
            player.getAbilities().mayfly = true;
            player.getAbilities().flying = true;
            player.getAbilities().setFlyingSpeed(SwordFlightManager.getFlightSpeed(player));
            player.onUpdateAbilities();
        }
        ModNetwork.sendSwordFlightState(player);
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && PlayerSwordFlightData.isFlying(player)) {
            player.getAbilities().mayfly = true;
            player.getAbilities().flying = true;
            player.getAbilities().setFlyingSpeed(SwordFlightManager.getFlightSpeed(player));
            player.onUpdateAbilities();
            ModNetwork.sendSwordFlightState(player);
        }
    }

    /** 让后来的观察者也看见别人脚下的飞剑。 */
    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer observer
                && event.getTarget() instanceof ServerPlayer subject) {
            ModNetwork.sendSwordFlightStateTo(observer, subject);
        }
    }
}
