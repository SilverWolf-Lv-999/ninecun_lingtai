package net.seraphina.ninecun_lingtai.common.network.packet;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * S→C："心头一悸"——传讯禁触发时给主人的方位 + 模糊距离档提示（不给坐标）。
 * directionOctant: 0=北 顺时针到 7=西北；-1 = 方位不明（跨维度）。
 * distanceTier: 0 咫尺 / 1 百里 / 2 千里之外。
 */
public record SealAlertPacket(byte directionOctant, byte distanceTier, String reasonKey) {

    public static void encode(SealAlertPacket packet, FriendlyByteBuf buffer) {
        buffer.writeByte(packet.directionOctant);
        buffer.writeByte(packet.distanceTier);
        buffer.writeUtf(packet.reasonKey);
    }

    public static SealAlertPacket decode(FriendlyByteBuf buffer) {
        return new SealAlertPacket(buffer.readByte(), buffer.readByte(), buffer.readUtf());
    }

    public static void handle(SealAlertPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        contextSupplier.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> showOnClient(packet)));
        contextSupplier.get().setPacketHandled(true);
    }

    private static void showOnClient(SealAlertPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        String directionKey = packet.directionOctant < 0
                ? "message.ninecun_lingtai.seal.alert.direction.unknown"
                : "message.ninecun_lingtai.seal.alert.direction." + packet.directionOctant;
        Component message = Component.translatable(
                packet.reasonKey,
                Component.translatable(directionKey),
                Component.translatable("message.ninecun_lingtai.seal.alert.distance." + packet.distanceTier)
        );
        minecraft.gui.setOverlayMessage(message, false);
        minecraft.player.displayClientMessage(message, false);
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(
                SoundEvents.WARDEN_HEARTBEAT, 0.75F, 1.0F
        ));
    }
}
