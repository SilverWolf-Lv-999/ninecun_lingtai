package net.seraphina.ninecun_lingtai.client.player;

import net.seraphina.ninecun_lingtai.common.network.packet.SyncSoulInjuryPacket;
import net.seraphina.ninecun_lingtai.utility.SoulInjuryUtility;

/**
 * 客户端魂伤缓存：只持有分档与七魄星图明暗（0-255），永远拿不到精确 injury 值。
 * 需集中接线：ClientForgeEventHandler.onLoggingOut 补一行 reset()。
 */
public final class ClientSoulInjuryData {
    private static SoulInjuryUtility.InjuryTier tier = SoulInjuryUtility.InjuryTier.NONE;
    private static int[] starBrightness = createFullBrightness();

    public static void applySync(byte tierOrdinal, byte[] starDimming) {
        SoulInjuryUtility.InjuryTier[] tiers = SoulInjuryUtility.InjuryTier.values();
        tier = tierOrdinal >= 0 && tierOrdinal < tiers.length
                ? tiers[tierOrdinal]
                : SoulInjuryUtility.InjuryTier.NONE;
        int[] brightness = createFullBrightness();
        for (int index = 0; index < brightness.length && index < starDimming.length; index++) {
            brightness[index] = starDimming[index] & 0xFF;
        }
        starBrightness = brightness;
    }

    public static SoulInjuryUtility.InjuryTier getTier() {
        return tier;
    }

    /** 第 index 魄的明暗（0 暗 - 255 亮），index 越界按全亮处理。 */
    public static int getStarBrightness(int index) {
        if (index < 0 || index >= starBrightness.length) {
            return 255;
        }
        return starBrightness[index];
    }

    public static void reset() {
        tier = SoulInjuryUtility.InjuryTier.NONE;
        starBrightness = createFullBrightness();
    }

    private static int[] createFullBrightness() {
        int[] brightness = new int[SyncSoulInjuryPacket.STAR_COUNT];
        java.util.Arrays.fill(brightness, 255);
        return brightness;
    }
}
