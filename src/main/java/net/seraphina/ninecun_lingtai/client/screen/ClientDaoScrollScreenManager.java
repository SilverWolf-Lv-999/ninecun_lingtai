package net.seraphina.ninecun_lingtai.client.screen;

import net.seraphina.ninecun_lingtai.common.debug.DaoScrollSnapshot;
import net.minecraft.client.Minecraft;

public final class ClientDaoScrollScreenManager {
    public static void open(DaoScrollSnapshot snapshot, boolean editingSelf) {
        Minecraft.getInstance().setScreen(new DaoScrollScreen(snapshot, editingSelf));
    }
}
