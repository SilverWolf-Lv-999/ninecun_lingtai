package net.seraphina.ninecun_lingtai.client.render.shader.screen.pre;

import net.minecraft.client.renderer.PostChain;
import net.seraphina.ninecun_lingtai.client.render.shader.screen.CustomScreenEffect;

public interface PreScreenEffect extends CustomScreenEffect {
    @Override
    default PostChain current() {
        return PreProcessingShaders.INSTANCE.getPreChain(this);
    }
}
