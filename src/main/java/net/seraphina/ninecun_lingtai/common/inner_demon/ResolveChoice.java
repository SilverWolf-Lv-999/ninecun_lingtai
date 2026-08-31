package net.seraphina.ninecun_lingtai.common.inner_demon;

/**
 * 决念三选一（心魔即己 §6.2 RESOLVE）。枚举名用于会话序列化与命令参数，勿改名。
 */
public enum ResolveChoice {
    /** 斩念：限时击杀心魔 → SUCCESS；超时 → FAIL。 */
    SEVER("斩念"),
    /** 守念：心魔狂化下限时存活 → SUCCESS；死亡 → FAIL。超时默认值（R8）。 */
    HOLD("守念"),
    /** 认念：立即 SUCCESS_WITH_SCAR，获得一条命名心障。 */
    ACCEPT("认念");

    private final String chineseName;

    ResolveChoice(String chineseName) {
        this.chineseName = chineseName;
    }

    public String chineseName() {
        return this.chineseName;
    }

    public static ResolveChoice byName(String name) {
        for (ResolveChoice choice : values()) {
            if (choice.name().equalsIgnoreCase(name)) {
                return choice;
            }
        }
        return null;
    }
}
