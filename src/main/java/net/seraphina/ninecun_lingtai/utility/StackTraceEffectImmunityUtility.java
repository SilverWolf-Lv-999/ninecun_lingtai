package net.seraphina.ninecun_lingtai.utility;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.seraphina.ninecun_lingtai.api.cultivation.CultivationApi;
import net.seraphina.ninecun_lingtai.api.cultivation.CultivationRealm;

public final class StackTraceEffectImmunityUtility {
    private static final char GENERATED_METHOD_NAME_SEPARATOR = '$';
    private static final int CURRENT_CALLBACK_FRAME_COUNT = 1;
    private static final StackWalker STACK_WALKER = StackWalker.getInstance();

    public static boolean shouldBlockSlowness(Player player, MobEffectInstance effectInstance) {
        return effectInstance.getEffect() == MobEffects.MOVEMENT_SLOWDOWN
                && hasReachedGoldenCore(player)
                && isCalledFromGeneratedMethod();
    }

    public static boolean shouldBlockBeneficialEffectRemoval(Player player, MobEffect effect) {
        return effect.isBeneficial()
                && hasReachedGoldenCore(player)
                && isCalledFromGeneratedMethod();
    }

    private static boolean hasReachedGoldenCore(Player player) {
        return CultivationApi.getStage(player)
                .map(stage -> stage.realm().ordinal() >= CultivationRealm.GOLDEN_CORE.ordinal())
                .orElse(false);
    }

    private static boolean isCalledFromGeneratedMethod() {
        return STACK_WALKER.walk(frames -> frames
                .filter(StackTraceEffectImmunityUtility::isGeneratedMethodFrame)
                .skip(CURRENT_CALLBACK_FRAME_COUNT)
                .findAny()
                .isPresent());
    }

    private static boolean isGeneratedMethodFrame(StackWalker.StackFrame frame) {
        String methodName = frame.getMethodName();
        int separatorIndex = methodName.indexOf(GENERATED_METHOD_NAME_SEPARATOR);
        return separatorIndex > 0 && separatorIndex < methodName.length() - 1;
    }
}
