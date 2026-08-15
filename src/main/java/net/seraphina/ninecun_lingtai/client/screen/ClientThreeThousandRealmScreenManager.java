package net.seraphina.ninecun_lingtai.client.screen;

import net.minecraft.client.Minecraft;
import net.seraphina.ninecun_lingtai.common.realm.ThreeThousandRealmCatalogSnapshot;

public final class ClientThreeThousandRealmScreenManager {
    private ClientThreeThousandRealmScreenManager() {
    }

    public static void open(ThreeThousandRealmCatalogSnapshot snapshot) {
        Minecraft.getInstance().setScreen(new ThreeThousandRealmScreen(snapshot));
    }

    public static void update(ThreeThousandRealmCatalogSnapshot snapshot) {
        if (Minecraft.getInstance().screen instanceof ThreeThousandRealmScreen realmScreen) {
            realmScreen.updateCatalog(snapshot);
        }
    }

    public static void handleTravelResult(boolean successful) {
        if (Minecraft.getInstance().screen instanceof ThreeThousandRealmScreen realmScreen) {
            realmScreen.handleTravelResult(successful);
        }
    }
}
