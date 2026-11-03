package net.seraphina.ninecun_lingtai.client.imc;

import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.stats.StatsCounter;
import net.minecraft.world.damagesource.DamageSource;
import net.seraphina.ninecun_lingtai.utility.CultivationDefenseUtility;

@SuppressWarnings("all")
public final class IClientPlayer extends LocalPlayer {
    public IClientPlayer(Minecraft pMinecraft, ClientLevel pClientLevel, ClientPacketListener pConnection, StatsCounter pStats, ClientRecipeBook pRecipeBook, boolean pWasShiftKeyDown, boolean pWasSprinting) {
        super(pMinecraft, pClientLevel, pConnection, pStats, pRecipeBook, pWasShiftKeyDown, pWasSprinting);
    }

    @Override
    public void tick() {
        super.tick();
    }

    @Override
    public void die(DamageSource pCause) {
        if (CultivationDefenseUtility.hasActiveDefense(this))
            return;
        super.die(pCause);
    }
}
