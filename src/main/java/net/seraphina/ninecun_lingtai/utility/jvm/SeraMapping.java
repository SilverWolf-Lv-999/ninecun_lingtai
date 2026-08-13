package net.seraphina.ninecun_lingtai.utility.jvm;

import net.minecraftforge.fml.loading.FMLEnvironment;

public enum SeraMapping {
    LivingEntity$getHealth$F("getHealth", "m_21223_"),
    Entity$isAlive$Z("isAlive", "m_6084_"),
    LivingEntity$isDeadOrDying$Z("isDeadOrDying", "m_21224_"),
    Gui$renderHeart$V("renderHeart", "m_280593_"),
    ForgeGui$renderHealth$V("renderHealth", "renderHealth"),
    Item$useOn$InteractionResult("useOn", "m_6225_"),
    ;

    final String noObf, obf;

    SeraMapping(String noObf, String obf) {
        this.noObf = noObf;
        this.obf = obf;
    }

    public String get() {
        return isDev() ? noObf : obf;
    }

    static boolean isDev() {
        return !FMLEnvironment.production;
    }
}
