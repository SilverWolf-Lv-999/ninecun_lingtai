package net.seraphina.ninecun_lingtai.common.breakthrough;

import net.minecraft.server.level.ServerPlayer;

/**
 * 道心系数提供者。M1 阶段道心/杀孽系统尚未落地（M2/M5），一律返回常数 1.0；
 * 真实现就绪后由主控调用 {@link BreakthroughChanceCalculator#setDaoHeartProvider} 替换。
 */
@FunctionalInterface
public interface DaoHeartProvider {

    DaoHeartProvider CONSTANT = player -> 1.0D;

    /** 返回该玩家的道心系数（乘区，1.0 为道心平稳）。 */
    double getCoefficient(ServerPlayer player);
}
