package net.seraphina.ninecun_lingtai.common.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * S→C：打开刻印/管理界面（潜行右键自己或未刻物品）。
 * 只发主人视角允许知道的信息：锚位、叠层上限、自己刻过的层类型序（主人认得自己的刻印）。
 */
public record OpenSealCarveScreenPacket(
        boolean mainHand,
        int usedAnchors,
        int totalAnchors,
        int availableAnchors,
        int layerCap,
        List<Byte> ownLayerKindOrdinals
) {
    public static void encode(OpenSealCarveScreenPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.mainHand);
        buffer.writeVarInt(packet.usedAnchors);
        buffer.writeVarInt(packet.totalAnchors);
        buffer.writeVarInt(packet.availableAnchors);
        buffer.writeVarInt(packet.layerCap);
        buffer.writeVarInt(packet.ownLayerKindOrdinals.size());
        for (byte ordinal : packet.ownLayerKindOrdinals) {
            buffer.writeByte(ordinal);
        }
    }

    public static OpenSealCarveScreenPacket decode(FriendlyByteBuf buffer) {
        boolean mainHand = buffer.readBoolean();
        int usedAnchors = buffer.readVarInt();
        int totalAnchors = buffer.readVarInt();
        int availableAnchors = buffer.readVarInt();
        int layerCap = buffer.readVarInt();
        int count = buffer.readVarInt();
        List<Byte> ownLayerKindOrdinals = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            ownLayerKindOrdinals.add(buffer.readByte());
        }
        return new OpenSealCarveScreenPacket(
                mainHand, usedAnchors, totalAnchors, availableAnchors, layerCap, ownLayerKindOrdinals
        );
    }

    public InteractionHand hand() {
        return this.mainHand ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
    }

    public static void handle(
            OpenSealCarveScreenPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        contextSupplier.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> net.seraphina.ninecun_lingtai.client.screen.SealCarveScreen.open(packet)
        ));
        contextSupplier.get().setPacketHandled(true);
    }
}
