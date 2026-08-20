package net.seraphina.ninecun_lingtai.common.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerXpEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.ItemFishedEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.seraphina.ninecun_lingtai.ModMain;
import net.seraphina.ninecun_lingtai.common.causality.CausalityHistoryManager;
import net.seraphina.ninecun_lingtai.utility.GreatCausalityUtility;

import java.util.HashMap;
import java.util.Map;

@Mod.EventBusSubscriber(modid = ModMain.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class GreatCausalityEventHandler {
    private static final String ENHANCED_LOOT_KEY = ModMain.MOD_ID + ":causality_enhanced";
    private static final double MAXIMUM_LOOT_BONUS_CHANCE = 0.75D;
    private static final Map<Integer, Long> LAST_MOVEMENT_RECORDS = new HashMap<>();

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CausalityHistoryManager.record(
                    player,
                    "combat",
                    "攻击了 " + event.getTarget().getName().getString()
            );
        }
    }

    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)
                || !GreatCausalityUtility.hasTechnique(player)) {
            return;
        }
        // LivingAttackEvent 到达这里时目标已经被判定命中，保留攻击事件可使末影人瞬移不能吞掉本次伤害。
        if (event.getEntity() != player) {
            CausalityHistoryManager.record(
                    player,
                    "combat",
                    "攻击命中了 " + event.getEntity().getName().getString()
            );
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !GreatCausalityUtility.hasTechnique(player)) {
            return;
        }
        CausalityHistoryManager.record(
                player,
                "interaction",
                "与方块 " + event.getLevel().getBlockState(event.getPos()).getBlock().getName().getString()
                        + " 发生了交互"
        );
        enhanceContainerLoot(player, event.getPos());
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CausalityHistoryManager.record(
                    player,
                    "interaction",
                    "与实体 " + event.getTarget().getName().getString() + " 发生了交互"
            );
        }
    }

    @SubscribeEvent
    public static void onBreakBlock(BlockEvent.BreakEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player) {
            CausalityHistoryManager.record(
                    player,
                    "block",
                    "破坏了方块 " + event.getState().getBlock().getName().getString()
            );
        }
    }

    @SubscribeEvent
    public static void onItemPickup(EntityItemPickupEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CausalityHistoryManager.record(
                    player,
                    "item",
                    "拾取了 " + event.getItem().getItem().getHoverName().getString()
            );
        }
    }

    @SubscribeEvent
    public static void onExperiencePickup(PlayerXpEvent.PickupXp event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CausalityHistoryManager.record(
                    player,
                    "cultivation",
                    "获得了 " + event.getOrb().getValue() + " 点经验"
            );
        }
    }

    @SubscribeEvent
    public static void onItemToss(ItemTossEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player) {
            CausalityHistoryManager.record(
                    player,
                    "item",
                    "投掷了 " + event.getEntity().getItem().getHoverName().getString()
            );
        }
    }

    @SubscribeEvent
    public static void onItemFished(ItemFishedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !GreatCausalityUtility.hasTechnique(player)) {
            return;
        }
        int treasureCount = GreatCausalityUtility.getMasteryStage(player).ordinal() >= 2 ? 2 : 1;
        event.getDrops().clear();
        for (int index = 0; index < treasureCount; index++) {
            event.getDrops().add(new ItemStack(getFishingTreasure(player)));
        }
        CausalityHistoryManager.record(player, "fishing", "垂钓得到了因果所向的宝物");
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CausalityHistoryManager.record(player, "death", "死亡");
        }
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CausalityHistoryManager.record(
                    player,
                    "travel",
                    "从 " + event.getFrom().location() + " 进入了 " + event.getTo().location()
            );
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END
                || !(event.player instanceof ServerPlayer player)
                || !GreatCausalityUtility.hasTechnique(player)
                || player.tickCount % 100 != 0) {
            return;
        }
        long packedPosition = player.blockPosition().asLong();
        Long lastPosition = LAST_MOVEMENT_RECORDS.put(player.getId(), packedPosition);
        if (lastPosition != null && lastPosition != packedPosition) {
            CausalityHistoryManager.record(player, "travel", "移动到了新的位置");
        }
    }

    @SubscribeEvent
    public static void onMobTick(LivingEvent.LivingTickEvent event) {
        if (event.getEntity().level().isClientSide()
                || !(event.getEntity() instanceof Mob mob)
                || mob.tickCount % 5 != 0
                || !(mob.getTarget() instanceof ServerPlayer player)
                || !GreatCausalityUtility.hasTechnique(player)) {
            return;
        }
        double multiplier = GreatCausalityUtility.getDetectionMultiplier(player);
        double followRange = mob.getAttributeValue(
                net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE
        );
        if (multiplier <= 0.0D || mob.distanceToSqr(player) > followRange * multiplier * multiplier) {
            mob.setTarget(null);
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        CausalityHistoryManager.flush(event.getServer());
        CausalityHistoryManager.clear(event.getServer());
        LAST_MOVEMENT_RECORDS.clear();
    }

    private static void enhanceContainerLoot(ServerPlayer player, net.minecraft.core.BlockPos position) {
        if (!(player.level().getBlockEntity(position) instanceof RandomizableContainerBlockEntity container)
                || container.getPersistentData().getBoolean(ENHANCED_LOOT_KEY)) {
            return;
        }
        container.unpackLootTable(player);
        if (player.getRandom().nextDouble() > getLootBonusChance(player)) {
            return;
        }
        ItemStack bestStack = findBestStack(container);
        if (!bestStack.isEmpty() && bestStack.getCount() < bestStack.getMaxStackSize()) {
            bestStack.grow(1);
        }
        container.getPersistentData().putBoolean(ENHANCED_LOOT_KEY, true);
    }

    private static ItemStack findBestStack(Container inventory) {
        ItemStack bestStack = ItemStack.EMPTY;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack candidate = inventory.getItem(slot);
            if (candidate.isEmpty()) {
                continue;
            }
            if (bestStack.isEmpty() || candidate.getHoverName().getString().compareTo(bestStack.getHoverName().getString()) > 0) {
                bestStack = candidate;
            }
        }
        return bestStack;
    }

    private static double getLootBonusChance(ServerPlayer player) {
        return Math.min(
                MAXIMUM_LOOT_BONUS_CHANCE,
                0.25D + GreatCausalityUtility.getMasteryStage(player).ordinal() * 0.15D
        );
    }

    private static Item getFishingTreasure(ServerPlayer player) {
        return switch (player.getRandom().nextInt(5)) {
            case 0 -> Items.NAME_TAG;
            case 1 -> Items.SADDLE;
            case 2 -> Items.NAUTILUS_SHELL;
            case 3 -> Items.HEART_OF_THE_SEA;
            default -> Items.DIAMOND;
        };
    }
}
