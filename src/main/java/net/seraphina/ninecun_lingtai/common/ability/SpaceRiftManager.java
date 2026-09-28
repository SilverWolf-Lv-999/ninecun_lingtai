package net.seraphina.ninecun_lingtai.common.ability;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.seraphina.ninecun_lingtai.api.cultivation.CultivationRealm;
import net.seraphina.ninecun_lingtai.api.cultivation.CultivationStage;
import net.seraphina.ninecun_lingtai.common.entity.ability.SpaceRiftEntity;
import net.seraphina.ninecun_lingtai.common.network.ModNetwork;
import net.seraphina.ninecun_lingtai.common.network.packet.OpenSpaceRiftGuiPacket;
import net.seraphina.ninecun_lingtai.common.player.PlayerCultivationData;
import net.seraphina.ninecun_lingtai.common.registry.ModEntityTypes;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class SpaceRiftManager {
    private SpaceRiftManager() {
    }

    public static void sendDimensions(ServerPlayer player) {
        if (!canUse(player)) {
            return;
        }
        List<ResourceLocation> dimensions = new ArrayList<>();
        for (ServerLevel level : player.server.getAllLevels()) {
            if (!level.dimension().location().equals(player.level().dimension().location())) {
                dimensions.add(level.dimension().location());
            }
        }
        dimensions.sort(Comparator.comparing(ResourceLocation::toString));
        ModNetwork.sendOpenSpaceRiftGui(player, new OpenSpaceRiftGuiPacket(dimensions));
    }

    public static void create(ServerPlayer player, ResourceLocation targetId) {
        if (!canUse(player) || targetId == null) {
            return;
        }
        ResourceKey<Level> targetKey = ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, targetId);
        ServerLevel targetLevel = player.server.getLevel(targetKey);
        if (targetLevel == null || targetLevel == player.serverLevel()) {
            return;
        }
        ResourceLocation sourceId = player.serverLevel().dimension().location();
        Vec3 direction = player.getLookAngle().normalize();
        Vec3 sourcePosition = player.position().add(direction.scale(3.0D));
        Vec3 targetPosition = new Vec3(player.getX(), player.getY(), player.getZ());
        SpaceRiftEntity source = ModEntityTypes.SPACE_RIFT.get().create(player.serverLevel());
        SpaceRiftEntity target = ModEntityTypes.SPACE_RIFT.get().create(targetLevel);
        if (source == null || target == null) {
            return;
        }
        source.initialize(sourceId, targetId, sourcePosition, targetPosition, direction);
        target.initialize(targetId, sourceId, targetPosition, sourcePosition, direction);
        source.moveTo(sourcePosition.x, sourcePosition.y, sourcePosition.z, source.getYRot(), source.getXRot());
        target.moveTo(targetPosition.x, targetPosition.y, targetPosition.z, target.getYRot(), target.getXRot());
        player.serverLevel().addFreshEntity(source);
        targetLevel.addFreshEntity(target);
    }

    private static boolean canUse(ServerPlayer player) {
        CultivationStage stage = PlayerCultivationData.getStage(player).orElse(null);
        return stage != null && stage.realm().ordinal() >= CultivationRealm.SPIRIT_TRANSFORMATION.ordinal();
    }
}
