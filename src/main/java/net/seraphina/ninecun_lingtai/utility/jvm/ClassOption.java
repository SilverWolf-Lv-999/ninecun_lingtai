package net.seraphina.ninecun_lingtai.utility.jvm;


import java.util.Set;

public enum ClassOption {
    NESTMATE(1), STRONG(4);

    private final int flag;

    ClassOption(int flag) {
        this.flag = flag;
    }

    public static int optionsToFlag(Set<ClassOption> options) {
        int flags = 0;
        for (ClassOption cp : options) {
            flags |= cp.flag;
        }
        return flags;
    }
}
