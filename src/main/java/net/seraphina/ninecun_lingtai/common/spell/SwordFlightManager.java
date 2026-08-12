package net.seraphina.ninecun_lingtai.common.spell;

import net.seraphina.ninecun_lingtai.ModMain;
import net.seraphina.ninecun_lingtai.api.cultivation.CultivationApi;
import net.seraphina.ninecun_lingtai.api.cultivation.CultivationRealm;
import net.seraphina.ninecun_lingtai.api.cultivation.CultivationStage;
import net.seraphina.ninecun_lingtai.api.spell.SpellCastContext;
import net.seraphina.ninecun_lingtai.api.technique.TechniqueDefinition;
import net.seraphina.ninecun_lingtai.api.technique.TechniqueMasteryStage;
import net.seraphina.ninecun_lingtai.common.data.TechniqueManager;
import net.seraphina.ninecun_lingtai.common.network.ModNetwork;
import net.seraphina.ninecun_lingtai.common.player.PlayerSwordFlightData;
import net.seraphina.ninecun_lingtai.common.player.PlayerTechniqueData;
import net.seraphina.ninecun_lingtai.common.player.PlayerTechniqueManager;
import net.seraphina.ninecun_lingtai.common.player.PlayerTechniqueProficiencyData;
import net.seraphina.ninecun_lingtai.common.player.PlayerValueControlType;
import net.seraphina.ninecun_lingtai.utility.PlayerValueControlUtility;
import net.seraphina.ninecun_lingtai.utility.PlayerValueControlUnlockUtility;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public final class SwordFlightManager {
    public static final ResourceLocation TECHNIQUE_ID = ResourceLocation.fromNamespaceAndPath(
            ModMain.MOD_ID,
            "sword_flight_spell"
    );

    public static final double HOVER_GAP = 0.2D;

    public static final double DISMOUNT_GROUND_DISTANCE = 1.0D;

    private static final float BASE_FLYING_SPEED = 0.05F;

    private static final float FOUNDATION_BASE_SPEED = 0.3F;

    private static final double REALM_SPEED_RATIO = 6.0D;

    private static final double LAYER_SPEED_RATIO = 1.12D;

    private static final long PROFICIENCY_SPEED_CAP = 3600L;

    private static final float MAXIMUM_FLYING_SPEED = 1000.0F;
    private static final int FOUNDATION_STRENGTH =
            CultivationRealm.FOUNDATION_ESTABLISHMENT.ordinal() * CultivationStage.MAXIMUM_LAYER + 1;
    private static final int PROFICIENCY_INTERVAL_TICKS = 100;
    private static final long PROFICIENCY_PER_INTERVAL = 1L;

    public static boolean requestMount(ServerPlayer player, boolean interactionTriggered) {
        Optional<TechniqueDefinition> technique =
                TechniqueManager.INSTANCE.getTechnique(TECHNIQUE_ID);
        if (technique.isEmpty()) {
            return false;
        }
        if (interactionTriggered) {
            SpellCastManager.castTechniqueFromInteraction(player, technique.get());
        } else {
            SpellCastManager.castTechnique(player, technique.get());
        }
        return true;
    }

    public static boolean tryMount(ServerPlayer player, SpellCastContext context) {
        if (PlayerSwordFlightData.isFlying(player)) {
            sendActionBar(player, "已在剑上，何须再登");
            return false;
        }
        if (context.cultivationStrength() < FOUNDATION_STRENGTH) {
            sendActionBar(player, "丹田未成气府，载不动这口飞剑——筑基方可御剑");
            return false;
        }
        ItemStack heldStack = player.getMainHandItem();
        if (!(heldStack.getItem() instanceof SwordItem)) {
            sendActionBar(player, "御剑先得有剑，须手持一柄剑");
            return false;
        }
        if (!player.onGround()
                || (!context.interactionTriggered() && !player.isShiftKeyDown())) {
            sendActionBar(player, "须立于实地、蹲身将剑踏于足下，方可起飞");
            return false;
        }
        int slot = player.getInventory().selected;
        PlayerSwordFlightData.start(player, slot, heldStack);
        player.getAbilities().mayfly = true;
        player.getAbilities().flying = true;
        player.getAbilities().setFlyingSpeed(getFlightSpeed(player));
        player.onUpdateAbilities();
        player.teleportTo(player.getX(), player.getY() + HOVER_GAP + 0.1D, player.getZ());
        player.level().playSound(
                null,
                player.blockPosition(),
                SoundEvents.TRIDENT_RETURN,
                SoundSource.PLAYERS,
                0.8F,
                0.7F
        );
        ModNetwork.sendSwordFlightState(player);
        return true;
    }

    public static void requestDismount(ServerPlayer player) {
        if (!PlayerSwordFlightData.isFlying(player)) {
            return;
        }
        if (getGroundDistance(player) > DISMOUNT_GROUND_DISTANCE) {
            sendActionBar(player, "离地尚远，剑不可收——贴近地面方能收剑");
            return;
        }
        dismount(player);
        sendActionBar(player, "剑光敛去，收剑而立");
    }

    public static void dismount(ServerPlayer player) {
        if (!PlayerSwordFlightData.isFlying(player)) {
            return;
        }
        PlayerSwordFlightData.stop(player);
        if (!player.isCreative() && !player.isSpectator()) {
            player.getAbilities().mayfly = false;
            player.getAbilities().flying = false;
        }
        player.getAbilities().setFlyingSpeed(BASE_FLYING_SPEED);
        player.onUpdateAbilities();
        player.level().playSound(
                null,
                player.blockPosition(),
                SoundEvents.TRIDENT_RETURN,
                SoundSource.PLAYERS,
                0.8F,
                1.3F
        );
        ModNetwork.sendSwordFlightState(player);
    }

    public static void serverTick(ServerPlayer player) {
        if (!PlayerSwordFlightData.isFlying(player)) {
            return;
        }
        if (player.isSpectator()) {
            dismount(player);
            return;
        }
        if (!enforceSwordLock(player)) {
            dismount(player);
            sendActionBar(player, "足下之剑已失，剑毁人落");
            return;
        }
        boolean abilitiesChanged = false;
        if (!player.getAbilities().mayfly || !player.getAbilities().flying) {
            player.getAbilities().mayfly = true;
            player.getAbilities().flying = true;
            abilitiesChanged = true;
        }
        float flightSpeed = getFlightSpeed(player);
        if (Math.abs(player.getAbilities().getFlyingSpeed() - flightSpeed) > 1.0E-4F) {
            player.getAbilities().setFlyingSpeed(flightSpeed);
            abilitiesChanged = true;
        }
        if (abilitiesChanged) {
            player.onUpdateAbilities();
        }
        if (player.tickCount % PROFICIENCY_INTERVAL_TICKS == 0) {
            PlayerTechniqueManager.addProficiency(player, TECHNIQUE_ID, PROFICIENCY_PER_INTERVAL);
        }
    }

    public static void refreshFlightSpeed(ServerPlayer player) {
        if (!PlayerSwordFlightData.isFlying(player)) {
            return;
        }
        player.getAbilities().mayfly = true;
        player.getAbilities().flying = true;
        player.getAbilities().setFlyingSpeed(getFlightSpeed(player));
        player.onUpdateAbilities();
    }

    private static boolean enforceSwordLock(ServerPlayer player) {
        ItemStack lockedSword = PlayerSwordFlightData.getSword(player);
        if (lockedSword.isEmpty()) {
            return false;
        }
        Inventory inventory = player.getInventory();
        int slot = PlayerSwordFlightData.getLockedSlot(player);
        if (slot < 0 || slot >= inventory.getContainerSize()) {
            return false;
        }
        if (inventory.getItem(slot).is(lockedSword.getItem())) {
            return true;
        }
        for (int searchSlot = 0; searchSlot < inventory.getContainerSize(); searchSlot++) {
            if (searchSlot == slot || !inventory.getItem(searchSlot).is(lockedSword.getItem())) {
                continue;
            }
            ItemStack sword = inventory.getItem(searchSlot);
            inventory.setItem(searchSlot, inventory.getItem(slot));
            inventory.setItem(slot, sword);
            sendActionBar(player, "御剑之时，剑不可离位");
            return true;
        }
        ItemStack carried = player.containerMenu.getCarried();
        if (carried.is(lockedSword.getItem())) {
            player.containerMenu.setCarried(inventory.getItem(slot));
            inventory.setItem(slot, carried);
            sendActionBar(player, "御剑之时，剑不可离位");
            return true;
        }
        return false;
    }

    public static float getFlightSpeed(ServerPlayer player) {
        CultivationStage stage = CultivationApi.getStage(player)
                .orElse(new CultivationStage(CultivationRealm.FOUNDATION_ESTABLISHMENT, 1));
        long proficiency = PlayerTechniqueProficiencyData.get(player, TECHNIQUE_ID);
        double masteryEfficiency = PlayerTechniqueData.getTechnique(player, TECHNIQUE_ID)
                .flatMap(progress -> TechniqueMasteryStage.fromMastery(progress.mastery()))
                .orElse(TechniqueMasteryStage.ENTRY)
                .getEfficiency();
        return applyFlightSpeedControl(
                player,
                getFlightSpeed(stage, proficiency, masteryEfficiency)
        );
    }

    public static float getDefaultFlightSpeed(ServerPlayer player) {
        CultivationStage stage = CultivationApi.getStage(player)
                .orElse(new CultivationStage(CultivationRealm.FOUNDATION_ESTABLISHMENT, 1));
        return getFlightSpeed(
                stage,
                TechniqueMasteryStage.PERFECTION.getRequiredProficiency(),
                TechniqueMasteryStage.PERFECTION.getEfficiency()
        );
    }

    @Deprecated
    public static float getPerfectionFlightSpeed(ServerPlayer player) {
        return getDefaultFlightSpeed(player);
    }

    private static float applyFlightSpeedControl(ServerPlayer player, float flightSpeed) {
        return (float) Mth.clamp(
                PlayerValueControlUtility.applyApplication(
                        flightSpeed,
                        PlayerValueControlUnlockUtility.getEffectiveApplication(
                                player,
                                PlayerValueControlType.SWORD_FLIGHT_SPEED
                        )
                ),
                BASE_FLYING_SPEED,
                MAXIMUM_FLYING_SPEED
        );
    }

    private static float getFlightSpeed(
            CultivationStage stage,
            long proficiency,
            double masteryEfficiency
    ) {
        int realmsAboveFoundation = Math.max(
                0,
                stage.realm().ordinal() - CultivationRealm.FOUNDATION_ESTABLISHMENT.ordinal()
        );
        double realmFactor = Math.pow(REALM_SPEED_RATIO, realmsAboveFoundation);
        double layerFactor = Math.pow(LAYER_SPEED_RATIO, stage.layer() - 1);
        double proficiencyFactor = Math.pow(
                2.0D,
                Math.min(proficiency, PROFICIENCY_SPEED_CAP)
                        / (double) TechniqueMasteryStage.PERFECTION.getRequiredProficiency()
        );
        double speed = FOUNDATION_BASE_SPEED * realmFactor * layerFactor
                * proficiencyFactor * (0.5D + 0.5D * masteryEfficiency);
        return (float) Mth.clamp(speed, BASE_FLYING_SPEED, MAXIMUM_FLYING_SPEED);
    }

    public static double getGroundDistance(ServerPlayer player) {
        Vec3 feet = player.position();
        HitResult hit = player.level().clip(new ClipContext(
                feet,
                feet.add(0.0D, -3.0D, 0.0D),
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                player
        ));
        if (hit.getType() == HitResult.Type.MISS) {
            return Double.MAX_VALUE;
        }
        return feet.y - hit.getLocation().y;
    }

    private static void sendActionBar(ServerPlayer player, String message) {
        player.displayClientMessage(Component.literal(message), true);
    }
}
