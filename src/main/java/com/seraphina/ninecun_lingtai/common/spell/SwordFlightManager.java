package com.seraphina.ninecun_lingtai.common.spell;

import com.seraphina.ninecun_lingtai.ModMain;
import com.seraphina.ninecun_lingtai.api.cultivation.CultivationApi;
import com.seraphina.ninecun_lingtai.api.cultivation.CultivationRealm;
import com.seraphina.ninecun_lingtai.api.cultivation.CultivationStage;
import com.seraphina.ninecun_lingtai.api.spell.SpellCastContext;
import com.seraphina.ninecun_lingtai.api.technique.TechniqueMasteryStage;
import com.seraphina.ninecun_lingtai.common.network.ModNetwork;
import com.seraphina.ninecun_lingtai.common.player.PlayerSwordFlightData;
import com.seraphina.ninecun_lingtai.common.player.PlayerTechniqueData;
import com.seraphina.ninecun_lingtai.common.player.PlayerTechniqueManager;
import com.seraphina.ninecun_lingtai.common.player.PlayerTechniqueProficiencyData;
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

/**
 * 御剑飞行的服务端管理：登剑、收剑、锁剑与飞行速度。
 * 御剑期间借用原版飞行输入（空格升、潜行降、WASD 平移），速度写入 Abilities.flyingSpeed 并同步客户端；
 * 贴地悬浮的细节由客户端逐刻校正（见 ClientSwordFlightManager）。
 */
public final class SwordFlightManager {
    public static final ResourceLocation TECHNIQUE_ID = ResourceLocation.fromNamespaceAndPath(
            ModMain.MOD_ID,
            "sword_flight_spell"
    );
    /** 御剑悬浮时与脚下方块保持的间隙。 */
    public static final double HOVER_GAP = 0.2D;
    /** 允许收剑的离地高度上限（客户端判定 0.3 以内，服务端放宽以容忍延迟）。 */
    public static final double DISMOUNT_GROUND_DISTANCE = 1.0D;

    /** 原版飞行速度，仅作御剑速度的下限与收剑后的恢复值。 */
    private static final float BASE_FLYING_SPEED = 0.05F;
    /** 筑基一层的御剑基准速度（约为原版飞行六倍，数十格每秒）。 */
    private static final float FOUNDATION_BASE_SPEED = 0.3F;
    /** 每进一个大境界，剑速翻六倍。 */
    private static final double REALM_SPEED_RATIO = 6.0D;
    /** 境界内每进一层，剑速再快一成二。 */
    private static final double LAYER_SPEED_RATIO = 1.12D;
    /** 熟练度加成封顶所需的熟练度（1800 点翻一倍，3600 点封顶四倍）。 */
    private static final long PROFICIENCY_SPEED_CAP = 3600L;
    /** 剑速硬上限（原版飞行两万倍），护住碰撞与区块加载不至崩坏。 */
    private static final float MAXIMUM_FLYING_SPEED = 1000.0F;
    private static final int FOUNDATION_STRENGTH =
            CultivationRealm.FOUNDATION_ESTABLISHMENT.ordinal() * CultivationStage.MAXIMUM_LAYER + 1;
    private static final int PROFICIENCY_INTERVAL_TICKS = 100;
    private static final long PROFICIENCY_PER_INTERVAL = 1L;

    private SwordFlightManager() {
    }

    /** 由 SwordFlightSpell.cast 调用；返回 false 时灵力由施法管线退还。 */
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
        if (!player.onGround() || !player.isShiftKeyDown()) {
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

    /** 双击潜行且贴近地面时收剑。 */
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

    /** 无条件解除御剑状态（死亡、剑失、切旁观等）。 */
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

    /** 每个服务端刻维持御剑状态：锁剑、护持飞行能力、更新速度、积累熟练度。 */
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
        // 御剑亦是修行，长御长熟。
        if (player.tickCount % PROFICIENCY_INTERVAL_TICKS == 0) {
            PlayerTechniqueManager.addProficiency(player, TECHNIQUE_ID, PROFICIENCY_PER_INTERVAL);
        }
    }

    /**
     * 剑被锁在登剑时的槽位：被移动则移回原位，彻底消失（损毁等）则返回 false。
     * 以剑的种类匹配，耐久变化不影响锁定。
     */
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
        // 剑不在原位：先在背包各处找（含副手），再看是否被拎在鼠标上，找到便换回去。
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

    /**
     * 飞行速度随境界与熟练度指数增长，一层一重天：
     * 筑基一层约为原版飞行六倍（数十格每秒），此后每进一个大境界剑速翻六倍——金丹便有数百格每秒，
     * 元婴破千，化神以上一骑绝尘；境界内每进一层再快一成二。
     * 熟练度每积一重（1800 点）速度翻倍（至多四倍），掌握不足者难展其速（入门七五折，圆满全速）。
     */
    public static float getFlightSpeed(ServerPlayer player) {
        CultivationStage stage = CultivationApi.getStage(player)
                .orElse(new CultivationStage(CultivationRealm.FOUNDATION_ESTABLISHMENT, 1));
        long proficiency = PlayerTechniqueProficiencyData.get(player, TECHNIQUE_ID);
        double masteryEfficiency = PlayerTechniqueData.getTechnique(player, TECHNIQUE_ID)
                .flatMap(progress -> TechniqueMasteryStage.fromMastery(progress.mastery()))
                .orElse(TechniqueMasteryStage.ENTRY)
                .getEfficiency();
        return getFlightSpeed(stage, proficiency, masteryEfficiency);
    }

    /** 遁光等高阶能力需要以圆满御剑术为基准，但不要求玩家实际学会御剑术。 */
    public static float getPerfectionFlightSpeed(ServerPlayer player) {
        CultivationStage stage = CultivationApi.getStage(player)
                .orElse(new CultivationStage(CultivationRealm.FOUNDATION_ESTABLISHMENT, 1));
        return getFlightSpeed(
                stage,
                TechniqueMasteryStage.PERFECTION.getRequiredProficiency(),
                TechniqueMasteryStage.PERFECTION.getEfficiency()
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

    /** 自脚底向下探测与地面（碰撞体）的距离，最远三格；悬于虚空时返回大数。 */
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
