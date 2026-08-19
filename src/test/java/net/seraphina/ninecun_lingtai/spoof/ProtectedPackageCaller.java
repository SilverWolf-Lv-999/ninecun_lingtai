package net.seraphina.ninecun_lingtai.spoof;

public final class ProtectedPackageCaller {
    public static boolean canLoadProtectedClass() {
        try {
            Class.forName("net.seraphina.ninecun_lingtai.security.ProtectedFixture");
            return true;
        } catch (ClassNotFoundException | SecurityException exception) {
            return false;
        }
    }
}
