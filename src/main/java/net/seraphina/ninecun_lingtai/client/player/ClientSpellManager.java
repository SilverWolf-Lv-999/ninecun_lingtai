package net.seraphina.ninecun_lingtai.client.player;

import net.seraphina.ninecun_lingtai.client.input.ModKeyMappings;
import net.seraphina.ninecun_lingtai.client.screen.SpellSelectionScreen;
import net.seraphina.ninecun_lingtai.client.tribulation.ClientTribulationManager;
import net.seraphina.ninecun_lingtai.common.network.ModNetwork;
import net.seraphina.ninecun_lingtai.common.network.packet.CastSpellPacket;
import net.seraphina.ninecun_lingtai.common.network.packet.SelectSpellPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.Optional;

public final class ClientSpellManager {
    @Nullable
    private static ResourceLocation selectedSpellId;

    private ClientSpellManager() {
    }

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        while (ModKeyMappings.SPELL_SELECTION.consumeClick()) {
            if (player != null
                    && minecraft.screen == null
                    && (ClientTribulationManager.isLocalPlayerInTribulation()
                    || !ClientMeditationManager.isLocalPlayerMeditating())
                    && !ClientSoulRoamingManager.isLocalPlayerSoulRoaming()) {
                ClientTechniqueManager.requestRefresh();
                minecraft.setScreen(new SpellSelectionScreen());
            }
        }
        while (ModKeyMappings.CAST_SPELL.consumeClick()) {
            if (player != null
                    && minecraft.screen == null
                    && (ClientTribulationManager.isLocalPlayerInTribulation()
                    || !ClientMeditationManager.isLocalPlayerMeditating())
                    && !ClientSoulRoamingManager.isLocalPlayerSoulRoaming()) {
                LivingEntity target = minecraft.crosshairPickEntity instanceof LivingEntity livingEntity
                        && !(livingEntity instanceof Player)
                        ? livingEntity
                        : null;
                ModNetwork.sendToServer(new CastSpellPacket(
                        target == null ? null : target.getUUID()
                ));
            }
        }
    }

    public static void selectSpell(ResourceLocation spellId) {
        if (ClientSoulRoamingManager.isLocalPlayerSoulRoaming()) {
            return;
        }
        selectedSpellId = spellId;
        ModNetwork.sendToServer(new SelectSpellPacket(spellId));
    }

    public static Optional<ResourceLocation> getSelectedSpell() {
        return Optional.ofNullable(selectedSpellId);
    }

    public static void reset() {
        selectedSpellId = null;
    }
}
