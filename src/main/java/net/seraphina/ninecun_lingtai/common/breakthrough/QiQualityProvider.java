package net.seraphina.ninecun_lingtai.common.breakthrough;

import net.minecraft.server.level.ServerPlayer;

/**
 * 灵气质量系数提供者。M1 阶段灵气质量四档尚未落地（见灵气生态日志 M2），
 * 一律返回常数 1.0；M2 上线后由主控调用 {@link BreakthroughChanceCalculator#setQiQualityProvider}
 * 替换为真实现（浑浊 0.70 / 中平 1.00 / 清灵 1.15 / 至纯 1.30，对应配置键
 * breakthrough.qualityCoefTurbid/Neutral/Clear/Pristine）。
 */
@FunctionalInterface
public interface QiQualityProvider {

    QiQualityProvider CONSTANT = player -> 1.0D;

    /** 返回该玩家所处环境的灵气质量系数（乘区，1.0 为中平）。 */
    double getCoefficient(ServerPlayer player);
}
