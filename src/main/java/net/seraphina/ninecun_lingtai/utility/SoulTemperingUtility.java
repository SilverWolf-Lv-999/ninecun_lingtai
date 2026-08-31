package net.seraphina.ninecun_lingtai.utility;

import net.seraphina.ninecun_lingtai.ModMain;
import net.seraphina.ninecun_lingtai.api.aptitude.SoulStrength;
import net.seraphina.ninecun_lingtai.api.technique.TechniqueDefinition;
import net.seraphina.ninecun_lingtai.api.technique.TechniqueEffect;
import net.seraphina.ninecun_lingtai.common.config.CommonConfig;
import net.seraphina.ninecun_lingtai.common.network.ModNetwork;
import net.seraphina.ninecun_lingtai.common.player.PlayerAptitudeData;
import net.seraphina.ninecun_lingtai.common.player.PlayerSoulTemperingData;
import net.seraphina.ninecun_lingtai.common.registry.ModTombContent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;
import java.util.Optional;

public final class SoulTemperingUtility {
    public static final ResourceLocation SOUL_TEMPERING_EFFECT = ResourceLocation.fromNamespaceAndPath(
            ModMain.MOD_ID,
            "soul_tempering"
    );
    public static double getTemperingPerMinute(TechniqueDefinition technique) {
        for (TechniqueEffect effect : technique.effects()) {
            if (SOUL_TEMPERING_EFFECT.equals(effect.type())) {
                return Math.max(
                        0.0D,
                        GsonHelper.getAsDouble(effect.parameters(), "progress_per_minute", 0.0D)
                );
            }
        }
        return 0.0D;
    }

    public static double getRequiredProgress(int illuminatedStars) {
        return CommonConfig.SOUL_TEMPERING_BASE_PROGRESS.get()
                * Math.pow(
                        CommonConfig.SOUL_TEMPERING_STAR_GROWTH.get(),
                        Math.max(0, illuminatedStars - 1)
                );
    }

    public static void addProgress(ServerPlayer player, double amount) {
        if (!Double.isFinite(amount) || amount <= 0.0D) {
            return;
        }
        Optional<SoulStrength> soulStrength = PlayerAptitudeData.getSoulStrength(player);
        if (soulStrength.isEmpty()) {
            return;
        }
        // 温养疗伤：进度先 1:1 抵扣魂伤；SHAKEN 档起温养停滞——余量也全额转疗伤而非涨星（设计文档 R3）
        boolean stalled = SoulInjuryUtility.isTemperingStalled(player);
        double healed = SoulInjuryUtility.heal(player, amount, SoulInjuryUtility.HealSource.TEMPERING);
        double remaining = stalled ? 0.0D : amount - healed;
        if (healed > 0.0D && remaining <= 0.0D) {
            player.displayClientMessage(
                    Component.translatable("message.ninecun_lingtai.soul_injury_tempering_stall"),
                    true
            );
        }
        if (remaining <= 0.0D) {
            return;
        }
        int stars = soulStrength.get().illuminatedStars();
        if (stars >= SoulStrength.MAXIMUM_ILLUMINATED_STARS) {
            player.displayClientMessage(
                    Component.translatable("message.ninecun_lingtai.soul_tempering_full"),
                    true
            );
            return;
        }
        double progress = PlayerSoulTemperingData.getProgress(player) + remaining;
        double requirement = getRequiredProgress(stars);
        if (progress < requirement) {
            PlayerSoulTemperingData.setProgress(player, progress);
            return;
        }
        // 第 7 星封顶（读配置 soulInjury.sevenStarRequiresOpportunity）：
        // 6 星满进度后不升星，进度停在瓶颈处等机缘之物（M4 接）
        if (stars >= SoulStrength.MAXIMUM_ILLUMINATED_STARS - 1
                && CommonConfig.SOUL_INJURY_SEVEN_STAR_REQUIRES_OPPORTUNITY.get()
                && !hasSoulOpportunityToken(player)) {
            PlayerSoulTemperingData.setProgress(player, requirement);
            player.displayClientMessage(
                    Component.translatable("message.ninecun_lingtai.soul_tempering_seventh_star_locked"),
                    true
            );
            return;
        }
        SoulStrength.fromIlluminatedStars(stars + 1).ifPresent(updatedStrength -> {
            PlayerAptitudeData.setSoulStrength(player, updatedStrength);
            PlayerSoulTemperingData.setProgress(player, progress - requirement);
            player.displayClientMessage(
                    Component.translatable(
                            "message.ninecun_lingtai.soul_tempering_star",
                            updatedStrength.illuminatedStars()
                    ),
                    false
            );
            player.level().playSound(
                    null,
                    player.blockPosition(),
                    SoundEvents.PLAYER_LEVELUP,
                    SoundSource.PLAYERS,
                    0.9F,
                    1.5F
            );
            ModNetwork.sendCultivationInfo(player);
        });
    }

    /**
     * 魂道机缘之物判定（M4.4 墓穴切片落地）：检查背包中的墓主残魂 / 魂泉水，
     * 找到即当场消耗一件并放行。仅在"6 星满进度冲第 7 星"的封顶检查点被调用，
     * 返回 true 时第七星必然点亮，故消耗与解锁一一对应，不会白扣。
     * 机缘之物未注册（ModTombContent 尚未接线）时视同无机缘，维持封顶。
     */
    private static boolean hasSoulOpportunityToken(ServerPlayer player) {
        List<RegistryObject<Item>> tokens = List.of(
                ModTombContent.TOMB_MASTER_REMNANT_SOUL,
                ModTombContent.SOUL_SPRING_WATER
        );
        for (RegistryObject<Item> token : tokens) {
            if (!token.isPresent()) {
                continue;
            }
            Inventory inventory = player.getInventory();
            for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
                ItemStack stack = inventory.getItem(slot);
                if (!stack.isEmpty() && stack.is(token.get())) {
                    stack.shrink(1);
                    player.displayClientMessage(
                            Component.translatableWithFallback(
                                    "message.ninecun_lingtai.soul_opportunity_consumed",
                                    "机缘之物化作一线幽光没入眉心，魂桥飞架——通往第七星的道路豁然洞开"
                            ),
                            false
                    );
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 负向扣减温养进度的唯一入口（阵禁刻印用：刻禁是拿魂魄成长换安全）。
     * 只扣当前星级的未结算进度，不降星；进度不足时不扣并返回 false（拒绝刻印）。
     */
    public static boolean tryReduceProgress(ServerPlayer player, double amount) {
        if (!Double.isFinite(amount) || amount < 0.0D) {
            return false;
        }
        double progress = PlayerSoulTemperingData.getProgress(player);
        if (progress < amount) {
            return false;
        }
        PlayerSoulTemperingData.setProgress(player, progress - amount);
        return true;
    }
}
