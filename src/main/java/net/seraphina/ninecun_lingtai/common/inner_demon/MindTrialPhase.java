package net.seraphina.ninecun_lingtai.common.inner_demon;

/**
 * 心魔试炼五阶段状态机的相位（心魔即己 §6.2）。
 * RESOLVE 拆为三选一窗口（RESOLVE_CHOICE）与两条执行分支（SEVER/HOLD）；
 * ACCEPT 无执行分支（立即结算）。枚举名用于会话序列化，勿改名。
 */
public enum MindTrialPhase {
    /** 照见：无战斗，台词 + 因果剪影。 */
    REVEAL,
    /** 试锋：心魔按锁定快照 topSpells 复现玩家常用招。 */
    SPAR,
    /** 反照：解锁反制招，AI 变招。 */
    REFLECT,
    /** 决念：时停演出 + 三选一窗口（超时默认守念，R8）。 */
    RESOLVE_CHOICE,
    /** 斩念执行：限时击杀心魔。 */
    SEVER,
    /** 守念执行：心魔狂化，限时存活。 */
    HOLD;

    public static MindTrialPhase byName(String name) {
        for (MindTrialPhase phase : values()) {
            if (phase.name().equals(name)) {
                return phase;
            }
        }
        return null;
    }
}
