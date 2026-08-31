package net.seraphina.ninecun_lingtai.common.network.packet;

import net.seraphina.ninecun_lingtai.client.player.ClientSoulInjuryData;
import net.seraphina.ninecun_lingtai.common.player.PlayerSoulInjuryData;
import net.seraphina.ninecun_lingtai.utility.SoulInjuryUtility;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * S→C 魂伤定性同步：只发分档 + 7 字节星图明暗，不发精确 injury 值。
 * 星图每魄带随机相位扰动，客户端无法反推数值（设计文档 §6）。
 * 需集中接线：本包尚未在 ModNetwork 注册（注册链末尾 PLAY_TO_CLIENT）。
 */
public record SyncSoulInjuryPacket(byte tierOrdinal, byte[] starDimming) {
    public static final int STAR_COUNT = 7;
    /** 每魄随机相位扰动幅度（±），保证星图只作定性反馈。 */
    private static final int STAR_PHASE_JITTER = 24;

    /** 服务端取样：由发送方（登录补发/变档/±5 变动）调用。 */
    public static SyncSoulInjuryPacket capture(ServerPlayer player) {
        double injury = PlayerSoulInjuryData.getInjury(player);
        byte[] stars = new byte[STAR_COUNT];
        for (int index = 0; index < STAR_COUNT; index++) {
            int jitter = player.getRandom().nextInt(STAR_PHASE_JITTER * 2 + 1) - STAR_PHASE_JITTER;
            int brightness = (int) Math.round(255.0D - injury * 2.55D) + jitter;
            stars[index] = (byte) Math.min(255, Math.max(0, brightness));
        }
        return new SyncSoulInjuryPacket(
                (byte) SoulInjuryUtility.getTier(player).ordinal(),
                stars
        );
    }

    public static void encode(SyncSoulInjuryPacket packet, FriendlyByteBuf buffer) {
        buffer.writeByte(packet.tierOrdinal);
        for (int index = 0; index < STAR_COUNT; index++) {
            buffer.writeByte(index < packet.starDimming.length ? packet.starDimming[index] : 0);
        }
    }

    public static SyncSoulInjuryPacket decode(FriendlyByteBuf buffer) {
        byte tierOrdinal = buffer.readByte();
        byte[] stars = new byte[STAR_COUNT];
        for (int index = 0; index < STAR_COUNT; index++) {
            stars[index] = buffer.readByte();
        }
        return new SyncSoulInjuryPacket(tierOrdinal, stars);
    }

    public static void handle(SyncSoulInjuryPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientSoulInjuryData.applySync(packet.tierOrdinal, packet.starDimming)
        );
    }
}
