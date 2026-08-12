package net.seraphina.ninecun_lingtai.client.player;

import net.seraphina.ninecun_lingtai.common.registry.ModSounds;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

public final class PressureSoundInstance extends AbstractTickableSoundInstance {
    public PressureSoundInstance() {
        super(ModSounds.PRESSURE_RINGING.get(), SoundSource.MASTER, RandomSource.create());
        this.looping = true;
        this.delay = 0;
        this.volume = 0.8F;
        this.pitch = 1.0F;
        this.relative = true;
    }

    @Override
    public void tick() {
        if (!ClientPressureManager.isLocalPlayerAffected()) {
            stop();
        }
    }

    public void finish() {
        stop();
    }
}
