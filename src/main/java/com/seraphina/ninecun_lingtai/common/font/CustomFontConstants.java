package com.seraphina.ninecun_lingtai.common.font;

import com.seraphina.ninecun_lingtai.ModMain;
import net.minecraft.resources.ResourceLocation;

public final class CustomFontConstants {
    public static final ResourceLocation SHAN_BEI_FONT_ID = ResourceLocation.fromNamespaceAndPath(
            ModMain.MOD_ID,
            "shan_bei"
    );
    public static final ResourceLocation SHAN_BEI_FONT_RESOURCE = ResourceLocation.fromNamespaceAndPath(
            ModMain.MOD_ID,
            "font/yi_shan_bei_zhuan_ti.ttf"
    );
    public static final String SHAN_BEI_CACHE_FILE_NAME = "YiShanBeiZhuanTi.ttf";
    public static final float SHAN_BEI_FONT_SIZE = 11.0F;
    public static final float SHAN_BEI_FONT_OVERSAMPLE = 2.0F;

    private CustomFontConstants() {
    }
}
