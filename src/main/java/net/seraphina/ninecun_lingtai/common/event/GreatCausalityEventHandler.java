package net.seraphina.ninecun_lingtai.common.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.BlockPos;
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
import net.seraphina.ninecun_lingtai.utility.DimensionNameUtility;
import net.seraphina.ninecun_lingtai.utility.GreatCausalityUtility;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = ModMain.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class GreatCausalityEventHandler {
    private static final String ENHANCED_LOOT_KEY = ModMain.MOD_ID + ":causality_enhanced";
    private static final double MAXIMUM_LOOT_BONUS_CHANCE = 0.75D;
    private static final Map<UUID, BlockPos> LAST_MOVEMENT_RECORDS = new HashMap<>();

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CausalityHistoryManager.record(
                    player,
                    "combat",
                    "使用 " + player.getMainHandItem().getHoverName().getString()
                            + " 攻击了 " + event.getTarget().getName().getString()
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
                        + " 发生了交互；使用物品："
                        + event.getItemStack().getHoverName().getString()
                        + "；结果：" + (event.isCanceled() ? "未完成" : "已提交")
        );
        if (GreatCausalityUtility.hasTechnique(player)) {
            enhanceContainerLoot(player, event.getPos());
        }
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CausalityHistoryManager.record(
                    player,
                    "item",
                    "使用了物品 " + event.getItemStack().getHoverName().getString()
                            + "；结果：" + (event.isCanceled() ? "未完成" : "已提交")
            );
        }
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CausalityHistoryManager.record(
                    player,
                    "interaction",
                    "与实体 " + event.getTarget().getName().getString()
                            + " 发生了交互；使用物品："
                            + event.getItemStack().getHoverName().getString()
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
                            + "；工具：" + player.getMainHandItem().getHoverName().getString()
                            + "；坐标：" + formatPosition(event.getPos())
            );
        }
    }

    @SubscribeEvent
    public static void onPlaceBlock(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CausalityHistoryManager.record(
                    player,
                    "block",
                    "放置了方块 " + event.getPlacedBlock().getBlock().getName().getString()
                            + "；坐标：" + formatPosition(event.getPos())
                            + "；手持物品：" + player.getMainHandItem().getHoverName().getString()
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
                            + " ×" + event.getItem().getItem().getCount()
                            + "；物品实体坐标：" + formatPosition(event.getItem().blockPosition())
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
                            + " ×" + event.getEntity().getItem().getCount()
            );
        }
    }

    @SubscribeEvent
    public static void onItemFished(ItemFishedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (GreatCausalityUtility.hasTechnique(player)) {
            int treasureCount = GreatCausalityUtility.getMasteryStage(player).ordinal() >= 2 ? 2 : 1;
            event.getDrops().clear();
            StringBuilder result = new StringBuilder();
            for (int index = 0; index < treasureCount; index++) {
                ItemStack treasure = new ItemStack(getFishingTreasure(player));
                event.getDrops().add(treasure);
                if (result.length() > 0) {
                    result.append("、");
                }
                result.append(treasure.getHoverName().getString());
            }
            CausalityHistoryManager.record(
                    player,
                    "fishing",
                    "完成了一次垂钓；获得：" + result
                            + "；鱼钩坐标：" + formatPosition(player.blockPosition())
            );
            return;
        }

        String result = event.getDrops().stream()
                .map(stack -> stack.getHoverName().getString() + " ×" + stack.getCount())
                .reduce((first, second) -> first + "、" + second)
                .orElse("无物品");
        CausalityHistoryManager.record(
                player,
                "fishing",
                "完成了一次垂钓；获得：" + result
                        + "；鱼钩坐标：" + formatPosition(player.blockPosition())
        );
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Entity killer = event.getSource().getEntity();
            CausalityHistoryManager.record(
                    player,
                    "death",
                    "死亡；原因：" + event.getSource().getMsgId()
                            + "；致死者：" + (killer == null ? "环境" : killer.getName().getString())
            );
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            LAST_MOVEMENT_RECORDS.put(player.getUUID(), player.blockPosition());
            CausalityHistoryManager.record(
                    player,
                    "login",
                    "进入了世界；维度：" + DimensionNameUtility.getDisplayName(
                            player.server,
                            player.level().dimension().location()
                    )
            );
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CausalityHistoryManager.record(
                    player,
                    "logout",
                    "离开了世界；最后位置：" + formatPosition(player.blockPosition())
            );
            CausalityHistoryManager.flush(player);
            LAST_MOVEMENT_RECORDS.remove(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CausalityHistoryManager.record(
                    player,
                    "death",
                    "复活；当前位置：" + formatPosition(player.blockPosition())
            );
            LAST_MOVEMENT_RECORDS.put(player.getUUID(), player.blockPosition());
        }
    }

    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CausalityHistoryManager.record(
                    player,
                    "crafting",
                    "制作了 " + event.getCrafting().getHoverName().getString()
                            + " ×" + event.getCrafting().getCount()
            );
        }
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CausalityHistoryManager.record(
                player,
                "travel",
                "从 " + event.getFrom().location() + " 进入了 " + event.getTo().location()
                        + "；当前位置：" + formatPosition(player.blockPosition())
            );
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END
                || !(event.player instanceof ServerPlayer player)
                || player.tickCount % 20 != 0) {
            return;
        }
        BlockPos currentPosition = player.blockPosition();
        BlockPos previousPosition = LAST_MOVEMENT_RECORDS.put(player.getUUID(), currentPosition);
        if (previousPosition != null && !previousPosition.equals(currentPosition)) {
            CausalityHistoryManager.record(
                    player,
                    "travel",
                    "从 " + formatPosition(previousPosition)
                            + " 移动到 " + formatPosition(currentPosition)
            );
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
            CausalityHistoryManager.record(
                    player,
                    "loot",
                    "开启容器并使 " + bestStack.getHoverName().getString()
                            + " 的数量增加 1；容器坐标：" + formatPosition(position)
            );
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

    private static String formatPosition(BlockPos position) {
        return "[" + position.getX() + ", " + position.getY() + ", " + position.getZ() + "]";
    }
}
