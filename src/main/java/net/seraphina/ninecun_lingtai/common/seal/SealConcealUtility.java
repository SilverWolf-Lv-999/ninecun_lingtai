package net.seraphina.ninecun_lingtai.common.seal;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

/**
 * 隐匿禁判定（阵禁篇 4.1：该物品对神识探测隐形，对抗神识搜刮）。
 * 判定全部收在 seal 侧——SoulSenseManager 只在容器归类处加一行只读调用，
 * 不动 soul 包核心（M4 施工约束）。
 *
 * <p>两层效果：
 * <ol>
 *   <li>物品展示快照抹平（{@code SealManager.updateDisplaySnapshot}）——
 *       隐匿层生效时 tooltip 与常物无异，"神识都扫不到，根本不给你破的机会"；</li>
 *   <li>容器过滤：容器内全部非空物品皆带生效隐匿层时，整柜从神识方块扫描里消失
 *       （藏最值钱的那件——混一件凡物进去柜子就会重新显形，取舍自负）。</li>
 * </ol>
 */
public final class SealConcealUtility {

    private SealConcealUtility() {
    }

    /** 该物品是否带未休眠的隐匿层。 */
    public static boolean isConcealed(MinecraftServer server, ItemStack itemStack) {
        SealRecord record = SealManager.getRecord(server, itemStack).orElse(null);
        return record != null
                && !record.layers().isEmpty()
                && SealManager.hasActiveLayer(server, record, SealKind.CONCEAL);
    }

    /**
     * 神识方块扫描的容器过滤：至少一件内容物且全部内容物皆隐匿 → 整柜隐形。
     * 空容器不隐（空柜没有值得藏的东西，也犯不着骗探子）。
     */
    public static boolean isContainerFullyConcealed(ServerLevel level, Container container) {
        MinecraftServer server = level.getServer();
        boolean hasAnyItem = false;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack itemStack = container.getItem(slot);
            if (itemStack.isEmpty()) {
                continue;
            }
            hasAnyItem = true;
            if (!isConcealed(server, itemStack)) {
                return false;
            }
        }
        return hasAnyItem;
    }
}
