package com.seraphina.ninecun_lingtai.common.network.packet;

import com.seraphina.ninecun_lingtai.common.debug.DaoScrollSnapshot;
import com.seraphina.ninecun_lingtai.common.item.DaoScrollItem;
import com.seraphina.ninecun_lingtai.common.network.DaoScrollPacketUtility;
import com.seraphina.ninecun_lingtai.common.registry.ModItems;
import com.seraphina.ninecun_lingtai.utility.DaoScrollUtility;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SubmitDaoScrollPacket(DaoScrollSnapshot snapshot, boolean editingSelf) {
    private static final double MAXIMUM_EDIT_DISTANCE_SQUARED = 64.0D;

    public static void encode(SubmitDaoScrollPacket packet, FriendlyByteBuf buffer) {
        DaoScrollPacketUtility.writeSnapshot(buffer, packet.snapshot());
        buffer.writeBoolean(packet.editingSelf());
    }

    public static SubmitDaoScrollPacket decode(FriendlyByteBuf buffer) {
        return new SubmitDaoScrollPacket(
                DaoScrollPacketUtility.readSnapshot(buffer),
                buffer.readBoolean()
        );
    }

    public static void handle(
            SubmitDaoScrollPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        ServerPlayer editor = contextSupplier.get().getSender();
        if (editor == null || !DaoScrollItem.isHeldBy(editor)) {
            return;
        }
        LivingEntity target = getTarget(editor, packet);
        if (target == null
                || packet.snapshot().entityId() != target.getId()
                || (!packet.editingSelf()
                && target.distanceToSqr(editor) > MAXIMUM_EDIT_DISTANCE_SQUARED)) {
            editor.displayClientMessage(
                    net.minecraft.network.chat.Component.translatable(
                            "message.ninecun_lingtai.dao_scroll.invalid_target"
                    ),
                    true
            );
            return;
        }
        DaoScrollUtility.apply(editor, target, packet.snapshot()).ifPresentOrElse(
                message -> editor.displayClientMessage(message, true),
                () -> editor.displayClientMessage(
                        net.minecraft.network.chat.Component.translatable(
                                "message.ninecun_lingtai.dao_scroll.saved"
                        ),
                        true
                )
        );
    }

    private static LivingEntity getTarget(ServerPlayer editor, SubmitDaoScrollPacket packet) {
        if (packet.editingSelf()) {
            return editor;
        }
        Entity entity = editor.serverLevel().getEntity(packet.snapshot().entityId());
        return entity instanceof LivingEntity livingEntity ? livingEntity : null;
    }
}
