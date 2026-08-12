package net.seraphina.ninecun_lingtai.client.player;

import net.seraphina.ninecun_lingtai.api.cultivation.CultivationApi;
import net.seraphina.ninecun_lingtai.client.input.ModKeyMappings;
import net.seraphina.ninecun_lingtai.common.entity.ability.PushingMountainsCollisionEntity;
import net.seraphina.ninecun_lingtai.common.entity.ability.PushingMountainsEntity;
import net.seraphina.ninecun_lingtai.common.network.ModNetwork;
import net.seraphina.ninecun_lingtai.common.network.packet.PushingMountainsClearSelectionPacket;
import net.seraphina.ninecun_lingtai.common.network.packet.PushingMountainsEntityizePacket;
import net.seraphina.ninecun_lingtai.common.network.packet.PushingMountainsRestorePacket;
import net.seraphina.ninecun_lingtai.common.network.packet.PushingMountainsSelectPointPacket;
import net.seraphina.ninecun_lingtai.common.network.packet.PushingMountainsThrowPacket;
import net.seraphina.ninecun_lingtai.utility.PushingMountainsUtility;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.InputEvent;
import org.lwjgl.glfw.GLFW;

public final class ClientPushingMountainsManager {
    private static final int MINIMUM_DISTANCE = 4;
    private static final int DISTANCE_STEP = 4;
    private static final long DOUBLE_RIGHT_CLICK_MILLISECONDS = 280L;

    private static BlockPos firstPoint;
    private static BlockPos secondPoint;
    private static int selectionDistance = 16;
    private static int lastRightClickedEntityId = -1;
    private static long lastRightClickTime;
    private static boolean rightClickHandledWhileDown;

    private ClientPushingMountainsManager() {
    }

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (!minecraft.options.keyUse.isDown()) {
            rightClickHandledWhileDown = false;
        }
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.screen != null || !isSelectionMode()) {
            return;
        }
        int maximumDistance = (int) Math.ceil(
                CultivationApi.getStage(player)
                        .map(PushingMountainsUtility::getMaximumSelectionDistance)
                        .orElse(0.0D)
        );
        selectionDistance = Mth.clamp(selectionDistance, MINIMUM_DISTANCE, Math.max(MINIMUM_DISTANCE, maximumDistance));
    }

    public static void onKeyInput(int key, int action) {
        if (action != GLFW.GLFW_PRESS || key != GLFW.GLFW_KEY_DELETE || !isSelectionMode()) {
            return;
        }
        clearSelection(true);
    }

    public static void onMouseScroll(double scrollDelta) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null || !isSelectionMode()
                || !PushingMountainsUtility.canUse(CultivationApi.getStage(minecraft.player))) {
            return;
        }
        int maximumDistance = (int) Math.ceil(
                CultivationApi.getStage(minecraft.player)
                        .map(PushingMountainsUtility::getMaximumSelectionDistance)
                        .orElse((double) MINIMUM_DISTANCE)
        );
        int direction = scrollDelta > 0.0D ? 1 : -1;
        selectionDistance = Mth.clamp(
                selectionDistance + direction * DISTANCE_STEP,
                MINIMUM_DISTANCE,
                Math.max(MINIMUM_DISTANCE, maximumDistance)
        );
    }

    public static boolean handleInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.screen != null
                || !PushingMountainsUtility.canUse(CultivationApi.getStage(player))) {
            return false;
        }

        boolean selectionMode = isSelectionMode();
        if (event.isAttack()) {
            if (!selectionMode) {
                return false;
            }
            selectPoint(getPreviewPoint());
            cancel(event);
            return true;
        }
        if (!event.isUseItem()) {
            return false;
        }

        if (firstPoint != null && secondPoint != null) {
            if (rightClickHandledWhileDown) {
                cancel(event);
                return true;
            }
            rightClickHandledWhileDown = true;
            ModNetwork.sendToServer(PushingMountainsEntityizePacket.INSTANCE);
            clearSelection(false);
            cancel(event);
            return true;
        }

        PushingMountainsEntity region = findTargetRegion();
        if (region != null) {
            if (rightClickHandledWhileDown) {
                cancel(event);
                return true;
            }
            rightClickHandledWhileDown = true;
            long now = System.currentTimeMillis();
            if (selectionMode) {
                ModNetwork.sendToServer(new PushingMountainsRestorePacket(region.getId()));
                lastRightClickedEntityId = -1;
            } else if (region.getId() == lastRightClickedEntityId
                    && now - lastRightClickTime <= DOUBLE_RIGHT_CLICK_MILLISECONDS) {
                ModNetwork.sendToServer(new PushingMountainsThrowPacket(region.getId()));
                lastRightClickedEntityId = -1;
            } else {
                lastRightClickedEntityId = region.getId();
                lastRightClickTime = now;
            }
            cancel(event);
            return true;
        }

        if (!selectionMode) {
            return false;
        }
        if (rightClickHandledWhileDown) {
            cancel(event);
            return true;
        }
        rightClickHandledWhileDown = true;
        cancel(event);
        return true;
    }

    public static boolean isSelectionMode() {
        return ModKeyMappings.PUSHING_MOUNTAINS.isDown();
    }

    public static SelectionPreview getSelectionPreview() {
        if (!isSelectionMode()) {
            return null;
        }
        BlockPos previewPoint = getPreviewPoint();
        AABB previewBox = new AABB(
                previewPoint.getX(),
                previewPoint.getY(),
                previewPoint.getZ(),
                previewPoint.getX() + 1.0D,
                previewPoint.getY() + 1.0D,
                previewPoint.getZ() + 1.0D
        );
        AABB selectionBox = firstPoint == null
                ? previewBox
                : secondPoint == null
                ? PushingMountainsUtility.createSelectionBox(firstPoint, previewPoint)
                : PushingMountainsUtility.createSelectionBox(firstPoint, secondPoint);
        return new SelectionPreview(firstPoint, secondPoint, previewPoint, selectionBox, selectionDistance);
    }

    public static void reset() {
        firstPoint = null;
        secondPoint = null;
        selectionDistance = 16;
        lastRightClickedEntityId = -1;
        lastRightClickTime = 0L;
        rightClickHandledWhileDown = false;
    }

    private static void selectPoint(BlockPos point) {
        if (firstPoint == null || secondPoint != null) {
            firstPoint = point;
            secondPoint = null;
        } else {
            secondPoint = point;
        }
        ModNetwork.sendToServer(new PushingMountainsSelectPointPacket(point));
    }

    private static void clearSelection(boolean notifyServer) {
        firstPoint = null;
        secondPoint = null;
        if (notifyServer) {
            ModNetwork.sendToServer(PushingMountainsClearSelectionPacket.INSTANCE);
        }
    }

    private static BlockPos getPreviewPoint() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            return BlockPos.ZERO;
        }
        Vec3 eyePosition = player.getEyePosition(1.0F);
        Vec3 endPosition = eyePosition.add(player.getViewVector(1.0F).scale(selectionDistance));
        return BlockPos.containing(endPosition);
    }

    private static PushingMountainsEntity findTargetRegion() {
        Minecraft minecraft = Minecraft.getInstance();
        Entity target = minecraft.crosshairPickEntity;
        if (target instanceof PushingMountainsEntity region) {
            return region;
        }
        if (!(target instanceof PushingMountainsCollisionEntity)) {
            return null;
        }
        AABB searchBox = target.getBoundingBox().inflate(1.0D);
        return minecraft.level.getEntitiesOfClass(
                        PushingMountainsEntity.class,
                        searchBox,
                        region -> !region.isThrown()
                )
                .stream()
                .findFirst()
                .orElse(null);
    }

    private static void cancel(InputEvent.InteractionKeyMappingTriggered event) {
        event.setSwingHand(false);
        event.setCanceled(true);
    }

    public record SelectionPreview(
            BlockPos firstPoint,
            BlockPos secondPoint,
            BlockPos previewPoint,
            AABB selectionBox,
            int selectionDistance
    ) {
    }
}
