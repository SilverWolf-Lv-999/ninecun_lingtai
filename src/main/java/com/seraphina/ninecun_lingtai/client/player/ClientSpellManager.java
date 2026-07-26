package com.seraphina.ninecun_lingtai.client.player;

import com.seraphina.ninecun_lingtai.client.input.ModKeyMappings;
import com.seraphina.ninecun_lingtai.client.screen.SpellSelectionScreen;
import com.seraphina.ninecun_lingtai.common.network.ModNetwork;
import com.seraphina.ninecun_lingtai.common.network.packet.CastSpellPacket;
import com.seraphina.ninecun_lingtai.common.network.packet.SelectSpellPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;

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
                    && !ClientMeditationManager.isLocalPlayerMeditating()) {
                ClientTechniqueManager.requestRefresh();
                minecraft.setScreen(new SpellSelectionScreen());
            }
        }
        while (ModKeyMappings.CAST_SPELL.consumeClick()) {
            if (player != null
                    && minecraft.screen == null
                    && !ClientMeditationManager.isLocalPlayerMeditating()) {
                ModNetwork.sendToServer(CastSpellPacket.INSTANCE);
            }
        }
    }

    public static void selectSpell(ResourceLocation spellId) {
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
