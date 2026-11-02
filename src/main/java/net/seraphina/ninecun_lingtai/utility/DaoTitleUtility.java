package net.seraphina.ninecun_lingtai.utility;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.seraphina.ninecun_lingtai.common.font.CustomFontConstants;
import net.minecraft.world.entity.player.Player;
import net.seraphina.ninecun_lingtai.api.cultivation.CultivationRealm;
import net.seraphina.ninecun_lingtai.api.cultivation.CultivationStage;
import net.seraphina.ninecun_lingtai.api.daotitle.DaoTitleSuffix;
import net.seraphina.ninecun_lingtai.common.player.DaoTitleSnapshot;
import net.seraphina.ninecun_lingtai.common.player.PlayerCultivationData;
import net.seraphina.ninecun_lingtai.common.player.PlayerDaoTitleData;

public final class DaoTitleUtility {
    public static DaoTitleSnapshot getSnapshot(Player player) {
        CultivationStage stage = PlayerCultivationData.getStage(player).orElse(null);
        return new DaoTitleSnapshot(
                PlayerDaoTitleData.getCustomName(player),
                PlayerDaoTitleData.getImmortalSuffix(player),
                stage == null ? null : stage.realm()
        );
    }

    public static String getDisplayTitle(Player player) {
        DaoTitleSnapshot snapshot = getSnapshot(player);
        return getDisplayTitle(snapshot);
    }

    public static String getDisplayTitle(DaoTitleSnapshot snapshot) {
        if (!snapshot.isUnlocked()) {
            return "";
        }
        String suffix = switch (snapshot.realm()) {
            case GOLDEN_CORE -> "真人";
            case NASCENT_SOUL -> "真君";
            case SPIRIT_TRANSFORMATION -> "神君";
            case DAO_INTEGRATION -> "道君";
            case VOID_REFINEMENT -> "虚尊";
            case MAHAYANA -> "天尊";
            case IMMORTAL -> snapshot.immortalSuffix().getChineseName();
            default -> "";
        };
        return snapshot.customName().isBlank() ? suffix : snapshot.customName() + suffix;
    }

    public static MutableComponent formatName(Component username, String title) {
        if (title == null || title.isBlank()) {
            return username.copy();
        }
        return Component.literal("[ " + title + " ] ")
                .withStyle(style -> style.withFont(CustomFontConstants.DAO_TITLE_FONT_ID))
                .append(username.copy());
    }

    public static boolean hasTitle(Component component) {
        return component != null && component.getString().startsWith("[ ");
    }

    public static DaoTitleSuffix getSuffix(int index) {
        DaoTitleSuffix[] suffixes = DaoTitleSuffix.values();
        return index >= 0 && index < suffixes.length ? suffixes[index] : DaoTitleSuffix.IMMORTAL;
    }
}
