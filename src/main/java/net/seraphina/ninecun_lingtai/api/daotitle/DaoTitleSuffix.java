package net.seraphina.ninecun_lingtai.api.daotitle;

public enum DaoTitleSuffix {
    IMMORTAL("仙"),
    IMMORTAL_PERSON("仙人"),
    IMMORTAL_LORD("仙君"),
    IMMORTAL_SUPREME("仙尊");

    private final String chineseName;

    DaoTitleSuffix(String chineseName) {
        this.chineseName = chineseName;
    }

    public String getChineseName() {
        return this.chineseName;
    }
}

