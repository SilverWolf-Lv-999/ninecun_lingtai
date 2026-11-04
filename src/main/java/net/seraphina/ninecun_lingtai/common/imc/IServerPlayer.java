package net.seraphina.ninecun_lingtai.common.imc;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.seraphina.ninecun_lingtai.utility.CultivationDefenseUtility;

@SuppressWarnings("all")
public class IServerPlayer extends ServerPlayer {
    public IServerPlayer(MinecraftServer pServer, ServerLevel pLevel, GameProfile pGameProfile) {
        super(pServer, pLevel, pGameProfile);
    }

    @Override
    public void die(DamageSource pCause) {
        if (CultivationDefenseUtility.hasActiveDefense(this))
            return;
        super.die(pCause);
    }

    @Override
    public void defineSynchedData() {
        super.defineSynchedData();
    }
}
