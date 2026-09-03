package net.seraphina.ninecun_lingtai.client.player;

import net.minecraft.client.Minecraft;
import net.seraphina.ninecun_lingtai.common.network.ModNetwork;
import net.seraphina.ninecun_lingtai.common.network.packet.RequestPalmBuddhaMembersPacket;
import net.seraphina.ninecun_lingtai.common.spell.palm_buddha.PalmBuddhaMemberPageSnapshot;
import net.seraphina.ninecun_lingtai.common.spell.palm_buddha.PalmBuddhaMemberPacketLimits;
import net.seraphina.ninecun_lingtai.common.spell.palm_buddha.PalmBuddhaMemberSnapshot;

import java.util.Optional;
import java.util.UUID;

/**
 * 佛国成员摘要的客户端缓存。
 *
 * <p>这里只保存服务端主动返回的一页数据，不保存实体副本、区块副本或其他玩家视角。
 * 后续水墨界面可以直接读取这个缓存，不需要创建第二个摄像机页面。</p>
 */
public final class ClientPalmBuddhaMemberManager {
    private static PalmBuddhaMemberPageSnapshot thisCurrentPage =
            PalmBuddhaMemberPageSnapshot.empty();
    private static int thisRequestedPageIndex;

    private ClientPalmBuddhaMemberManager() {
    }

    public static void requestFirstPage() {
        requestPage(0);
    }

    public static void requestPage(int pageIndex) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        thisRequestedPageIndex = Math.max(
                0,
                Math.min(PalmBuddhaMemberPacketLimits.MAXIMUM_PAGE_COUNT - 1, pageIndex)
        );
        ModNetwork.sendToServer(new RequestPalmBuddhaMembersPacket(thisRequestedPageIndex));
    }

    public static void accept(PalmBuddhaMemberPageSnapshot pageSnapshot) {
        if (pageSnapshot == null) {
            return;
        }
        thisCurrentPage = pageSnapshot;
    }

    public static PalmBuddhaMemberPageSnapshot getCurrentPage() {
        return thisCurrentPage;
    }

    public static int getRequestedPageIndex() {
        return thisRequestedPageIndex;
    }

    public static Optional<PalmBuddhaMemberSnapshot> findMember(UUID memberId) {
        if (memberId == null) {
            return Optional.empty();
        }
        return thisCurrentPage.members().stream()
                .filter(member -> member.memberId().equals(memberId))
                .findFirst();
    }

    public static void reset() {
        thisCurrentPage = PalmBuddhaMemberPageSnapshot.empty();
        thisRequestedPageIndex = 0;
    }
}
