package com.seraphina.ninecun_lingtai.utility;

import com.seraphina.ninecun_lingtai.api.ai.AiProvider;
import com.seraphina.ninecun_lingtai.common.config.CommonConfig;

public final class AiConfigUtility {
    private AiConfigUtility() {
    }

    public static AiProvider getProvider() {
        return CommonConfig.AI_PROVIDER.get();
    }

    public static String requireApiKey() {
        String apiKey = CommonConfig.AI_API_KEY.get().trim();
        if (apiKey.isEmpty()) {
            throw new IllegalStateException("AI apiKey is not configured");
        }
        return apiKey;
    }

    public static String requireModel() {
        String model = CommonConfig.AI_MODEL.get().trim();
        if (model.isEmpty()) {
            throw new IllegalStateException("AI model is not configured");
        }
        return model;
    }
}
