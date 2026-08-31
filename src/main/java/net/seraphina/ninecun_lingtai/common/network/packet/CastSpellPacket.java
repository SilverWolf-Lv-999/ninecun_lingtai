package net.seraphina.ninecun_lingtai.common.network.packet;

import net.seraphina.ninecun_lingtai.common.behavior.PlayerBehaviorLedgerData;
import net.seraphina.ninecun_lingtai.common.inner_demon.MindTrialManager;
import net.seraphina.ninecun_lingtai.common.player.PlayerSelectedSpellData;
import net.seraphina.ninecun_lingtai.common.player.PlayerSoulRoamingManager;
import net.seraphina.ninecun_lingtai.common.spell.SpellCastManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public record CastSpellPacket(UUID targetPlayerId) {
    public static final CastSpellPacket INSTANCE = new CastSpellPacket();

    public CastSpellPacket() {
        this(null);
    }

    public static void encode(CastSpellPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.targetPlayerId() != null);
        if (packet.targetPlayerId() != null) {
            buffer.writeUUID(packet.targetPlayerId());
        }
    }

    public static CastSpellPacket decode(FriendlyByteBuf buffer) {
        return new CastSpellPacket(buffer.readBoolean() ? buffer.readUUID() : null);
    }

    public static void handle(CastSpellPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        ServerPlayer player = contextSupplier.get().getSender();
        if (player == null || PlayerSoulRoamingManager.isSoulRoaming(player)) {
            return;
        }
        ResourceLocation selectedSpellId =
                PlayerSelectedSpellData.getSelectedSpell(player).orElse(null);
        // 心魔即己 §6.3 锢空：AIR_LOCK 生效期内位移法术被封（服务端权威判定）。
        if (selectedSpellId != null
                && MindTrialManager.isMovementSpellLocked(player, selectedSpellId)) {
            player.displayClientMessage(
                    Component.literal("灵台锢空，遁法难施"),
                    true
            );
            return;
        }
        boolean castSucceeded = SpellCastManager.castSelectedSpell(player, packet.targetPlayerId());
        // 心魔即己 §5.1 施法采样：按"成功施放"计（接线清单第 11 条精确采样），
        // 冷却/灵力不足等失败请求不再进滑动窗口。
        if (castSucceeded && selectedSpellId != null) {
            PlayerBehaviorLedgerData.recordSpellCast(player, selectedSpellId);
            // REFLECT 阶段"换招"检测 + 位移法术使用标记（§7）。
            MindTrialManager.onOwnerSpellCast(player, selectedSpellId);
        }
    }
}
