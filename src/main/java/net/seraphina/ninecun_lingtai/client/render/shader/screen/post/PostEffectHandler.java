package net.seraphina.ninecun_lingtai.client.render.shader.screen.post;

import com.mojang.blaze3d.shaders.Uniform;
import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import net.minecraft.client.renderer.PostPass;
import net.seraphina.ninecun_lingtai.client.render.shader.screen.CustomScreenEffect;

import java.nio.FloatBuffer;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.Set;

public final class PostEffectHandler {
    private static final Int2ObjectLinkedOpenHashMap<CustomScreenEffect> DATA =
            new Int2ObjectLinkedOpenHashMap<>();
    private static final Set<CustomScreenEffect> REGISTERED_EFFECTS =
            Collections.newSetFromMap(new IdentityHashMap<>());
    private static final Set<CustomScreenEffect> ACTIVE_EFFECTS = new LinkedHashSet<>();
    private static int nextIndex;

    private PostEffectHandler() {
    }

    public static void registerEffect(CustomScreenEffect effect) {
        if (effect == null) {
            throw new NullPointerException("post effect");
        }
        if (!REGISTERED_EFFECTS.add(effect)) {
            return;
        }
        DATA.put(nextIndex++, effect);
    }

    public static Iterable<CustomScreenEffect> getEffects() {
        return DATA.values();
    }

    public static Iterable<CustomScreenEffect> getActiveEffects() {
        return ACTIVE_EFFECTS;
    }

    public static void setActive(CustomScreenEffect effect, boolean active) {
        if (effect == null || !REGISTERED_EFFECTS.contains(effect)) {
            return;
        }
        if (active) {
            ACTIVE_EFFECTS.add(effect);
        } else {
            ACTIVE_EFFECTS.remove(effect);
        }
    }

    public static void updateUniform(CustomScreenEffect effect, String name, float value) {
        if (effect == null || name == null || effect.current() == null) {
            return;
        }
        for (PostPass pass : effect.current().passes) {
            Uniform uniform = pass.getEffect().getUniform(name);
            if (uniform != null) {
                uniform.set(value);
            }
        }
    }

    public static void updateUniform(CustomScreenEffect effect, String name, float[] values) {
        if (effect == null || name == null || values == null || effect.current() == null) {
            return;
        }
        for (PostPass pass : effect.current().passes) {
            Uniform uniform = pass.getEffect().getUniform(name);
            if (uniform != null) {
                uniform.set(values);
            }
        }
    }

    public static float getUniform(CustomScreenEffect effect, String name) {
        if (effect == null || name == null || effect.current() == null) {
            return 0.0F;
        }
        for (PostPass pass : effect.current().passes) {
            Uniform uniform = pass.getEffect().getUniform(name);
            if (uniform != null) {
                FloatBuffer floatBuffer = uniform.getFloatBuffer();
                return floatBuffer.get(0);
            }
        }
        return 0.0F;
    }

    public static float[] getUniform(CustomScreenEffect effect, String name, int length) {
        if (effect == null || name == null || length <= 0 || effect.current() == null) {
            return new float[0];
        }
        for (PostPass pass : effect.current().passes) {
            Uniform uniform = pass.getEffect().getUniform(name);
            if (uniform != null) {
                FloatBuffer floatBuffer = uniform.getFloatBuffer();
                int readableLength = Math.min(length, floatBuffer.capacity());
                float[] values = new float[readableLength];
                for (int index = 0; index < readableLength; index++) {
                    values[index] = floatBuffer.get(index);
                }
                return values;
            }
        }
        return new float[0];
    }
}
