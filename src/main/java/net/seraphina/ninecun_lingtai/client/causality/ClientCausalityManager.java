package net.seraphina.ninecun_lingtai.client.causality;

import net.minecraft.client.Minecraft;
import net.seraphina.ninecun_lingtai.client.screen.GreatCausalityScreen;
import net.seraphina.ninecun_lingtai.common.causality.CausalitySnapshot;

public final class ClientCausalityManager {
    private ClientCausalityManager() {
    }

    public static void open(CausalitySnapshot snapshot) {
        Minecraft.getInstance().setScreen(new GreatCausalityScreen(snapshot));
    }

    public static void reset() {
        if (Minecraft.getInstance().screen instanceof GreatCausalityScreen) {
            Minecraft.getInstance().setScreen(null);
        }
    }
}
