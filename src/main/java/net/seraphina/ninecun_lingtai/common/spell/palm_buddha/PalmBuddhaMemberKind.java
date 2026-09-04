package net.seraphina.ninecun_lingtai.common.spell.palm_buddha;

/** 佛国成员在水墨成员簿中的轻量分类。 */
public enum PalmBuddhaMemberKind {
    PLAYER(0),
    CREATURE(1);

    private final int thisWireId;

    PalmBuddhaMemberKind(int wireId) {
        this.thisWireId = wireId;
    }

    public int wireId() {
        return this.thisWireId;
    }

    public static PalmBuddhaMemberKind fromWireId(int wireId) {
        for (PalmBuddhaMemberKind kind : values()) {
            if (kind.thisWireId == wireId) {
                return kind;
            }
        }
        throw new IllegalArgumentException("Unknown palm-buddha member kind: " + wireId);
    }
}
