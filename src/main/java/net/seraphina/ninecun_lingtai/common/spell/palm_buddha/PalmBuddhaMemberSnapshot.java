package net.seraphina.ninecun_lingtai.common.spell.palm_buddha;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.seraphina.ninecun_lingtai.api.cultivation.CultivationRealm;
import net.seraphina.ninecun_lingtai.api.cultivation.CultivationStage;
import net.seraphina.ninecun_lingtai.common.player.PlayerCultivationData;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * 发送给施法者的佛国成员摘要。
 *
 * <p>这是一个按需同步的轻量数据对象，只描述成员当前的服务端状态，
 * 不携带区块、实体渲染数据，也不创建或缓存其他玩家的摄像机画面。</p>
 */
public record PalmBuddhaMemberSnapshot(
        UUID memberId,
        String displayName,
        boolean online,
        Optional<ResourceLocation> currentDimension,
        boolean inExpectedRealm,
        Optional<BlockPos> currentPosition,
        Optional<CultivationStage> cultivationStage
) {
    public static final int MAXIMUM_DISPLAY_NAME_LENGTH = 64;

    public PalmBuddhaMemberSnapshot {
        Objects.requireNonNull(memberId, "memberId");
        displayName = Objects.requireNonNull(displayName, "displayName").strip();
        if (displayName.isEmpty()) {
            displayName = memberId.toString();
        }
        if (displayName.length() > MAXIMUM_DISPLAY_NAME_LENGTH) {
            displayName = displayName.substring(0, MAXIMUM_DISPLAY_NAME_LENGTH);
        }
        currentDimension = currentDimension == null ? Optional.empty() : currentDimension;
        currentPosition = currentPosition == null ? Optional.empty() : currentPosition;
        cultivationStage = cultivationStage == null ? Optional.empty() : cultivationStage;
        if (!online && (currentDimension.isPresent() || currentPosition.isPresent() || inExpectedRealm)) {
            throw new IllegalArgumentException("Offline palm-buddha members cannot expose live state");
        }
        if (currentPosition.isPresent() && currentDimension.isEmpty()) {
            throw new IllegalArgumentException("A current position requires a current dimension");
        }
    }

    public static PalmBuddhaMemberSnapshot from(
            MinecraftServer server,
            PalmBuddhaRealmManager realmManager,
            PalmBuddhaMemberState.MemberEntry memberEntry
    ) {
        Objects.requireNonNull(server, "server");
        Objects.requireNonNull(realmManager, "realmManager");
        Objects.requireNonNull(memberEntry, "memberEntry");

        UUID memberId = memberEntry.memberId();
        ServerPlayer player = server.getPlayerList().getPlayer(memberId);
        String displayName;
        if (player != null) {
            displayName = player.getGameProfile().getName();
        } else if (server.getProfileCache() != null) {
            displayName = server.getProfileCache().get(memberId)
                    .map(com.mojang.authlib.GameProfile::getName)
                    .orElse(memberId.toString());
        } else {
            displayName = memberId.toString();
        }
        if (player == null) {
            return new PalmBuddhaMemberSnapshot(
                    memberId,
                    displayName,
                    false,
                    Optional.empty(),
                    false,
                    Optional.empty(),
                    Optional.empty()
            );
        }

        return new PalmBuddhaMemberSnapshot(
                memberId,
                displayName,
                true,
                Optional.of(player.serverLevel().dimension().location()),
                realmManager.isMemberInExpectedRealm(player),
                Optional.of(BlockPos.containing(player.position())),
                PlayerCultivationData.getStage(player)
        );
    }

    public String cultivationRealmName() {
        return this.cultivationStage
                .map(CultivationStage::realm)
                .map(CultivationRealm::getChineseName)
                .orElse("");
    }
}
