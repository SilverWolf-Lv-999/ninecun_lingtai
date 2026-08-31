package net.seraphina.ninecun_lingtai.common.aura;

import net.seraphina.ninecun_lingtai.common.breakthrough.QiQualityProvider;
import net.minecraft.server.level.ServerPlayer;

/**
 * 区域灵气质量 → 破境系数的真实现（替换 M1 的 {@code QiQualityProvider.CONSTANT}）。
 *
 * <p>经 {@code BreakthroughChanceCalculator.setQiQualityProvider} 公开 setter 注册
 * （注册点见 {@code AuraEcologyEventHandler}），不改 breakthrough 包任何文件；
 * "此地灵气浑浊"拖后腿提示键（breakthrough_dragging_quality）随系数 < 1.0 自动生效。
 */
public final class SpiritualQiQualityProvider implements QiQualityProvider {

    @Override
    public double getCoefficient(ServerPlayer player) {
        return SpiritualAuraManager.getQualityMultiplier(player.serverLevel(), player.blockPosition());
    }
}
