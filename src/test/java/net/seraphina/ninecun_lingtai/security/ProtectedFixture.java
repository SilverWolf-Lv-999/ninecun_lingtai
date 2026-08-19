package net.seraphina.ninecun_lingtai.security;

public final class ProtectedFixture {
    private static final String SECRET = "ninecun-lingtai";

    public static String secret() {
        return SECRET;
    }
}
