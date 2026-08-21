package net.seraphina.ninecun_lingtai.common.registry;

import net.seraphina.ninecun_lingtai.ModMain;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModSounds {
    private static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(
            Registries.SOUND_EVENT,
            ModMain.MOD_ID
    );

    public static final RegistryObject<SoundEvent> PRESSURE_RINGING = SOUND_EVENTS.register(
            "pressure_ringing",
            () -> SoundEvent.createVariableRangeEvent(
                    ResourceLocation.fromNamespaceAndPath(ModMain.MOD_ID, "pressure_ringing")
            )
    );

    public static final RegistryObject<SoundEvent> BREAKTHROUGH_WATER_DROP = SOUND_EVENTS.register(
            "breakthrough_water_drop",
            () -> SoundEvent.createVariableRangeEvent(
                    ResourceLocation.fromNamespaceAndPath(ModMain.MOD_ID, "breakthrough_water_drop")
            )
    );

    public static final RegistryObject<SoundEvent> BREAKTHROUGH_VORTEX = SOUND_EVENTS.register(
            "breakthrough_vortex",
            () -> SoundEvent.createVariableRangeEvent(
                    ResourceLocation.fromNamespaceAndPath(ModMain.MOD_ID, "breakthrough_vortex")
            )
    );

    public static void register(IEventBus modEventBus) {
        SOUND_EVENTS.register(modEventBus);
    }
}
