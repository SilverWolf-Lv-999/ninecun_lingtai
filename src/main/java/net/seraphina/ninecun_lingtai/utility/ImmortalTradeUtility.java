package net.seraphina.ninecun_lingtai.utility;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.seraphina.ninecun_lingtai.ModMain;
import net.seraphina.ninecun_lingtai.api.alchemy.AlchemyRecipeDefinition;
import net.seraphina.ninecun_lingtai.api.technique.TechniqueDefinition;
import net.seraphina.ninecun_lingtai.common.data.AlchemyRecipeManager;
import net.seraphina.ninecun_lingtai.common.data.TechniqueManager;
import net.seraphina.ninecun_lingtai.common.item.SpiritualStoneGrade;

import java.util.List;

public final class ImmortalTradeUtility {
    private static final String IMMORTAL_TRADE_KEY = ModMain.MOD_ID + ":immortal_trade";
    private static final int MINIMUM_SPIRITUAL_STONE_COUNT = 3;
    private static final int MAXIMUM_SPIRITUAL_STONE_COUNT = 8;
    private static final int IMMORTAL_TRADE_MAX_USES = 1;
    private static final int IMMORTAL_TRADE_XP = 10;
    private static final float IMMORTAL_TRADE_PRICE_MULTIPLIER = 0.0F;

    public static void appendTrade(AbstractVillager villager, MerchantOffers offers) {
        if (villager.level().isClientSide()
                || !hasReachedImmortalPath(villager, offers)
                || hasImmortalTrade(offers)) {
            return;
        }

        MerchantOffer immortalTrade = createTrade(villager.getRandom());
        if (immortalTrade != null) {
            offers.add(immortalTrade);
        }
    }

    public static void refreshTrade(AbstractVillager villager, MerchantOffer completedTrade) {
        if (!isImmortalTrade(completedTrade)) {
            return;
        }

        MerchantOffers offers = villager.getOffers();
        MerchantOffer replacementTrade = createTrade(villager.getRandom());
        if (replacementTrade == null) {
            return;
        }

        int completedTradeIndex = offers.indexOf(completedTrade);
        if (completedTradeIndex < 0) {
            completedTradeIndex = findOutOfStockImmortalTrade(offers);
        }
        if (completedTradeIndex < 0) {
            return;
        }

        offers.set(completedTradeIndex, replacementTrade);
        sendOffersToTradingPlayer(villager, offers);
    }

    public static boolean hasImmortalTrade(MerchantOffers offers) {
        return offers.stream().anyMatch(ImmortalTradeUtility::isImmortalTrade);
    }

    public static boolean isImmortalTrade(MerchantOffer offer) {
        ItemStack result = offer.getResult();
        return result.hasTag() && result.getTag().getBoolean(IMMORTAL_TRADE_KEY);
    }

    private static boolean hasReachedImmortalPath(AbstractVillager villager, MerchantOffers offers) {
        if (villager instanceof Villager vanillaVillager) {
            return vanillaVillager.getVillagerData().getLevel() >= VillagerData.MAX_VILLAGER_LEVEL;
        }

        return !offers.isEmpty() && offers.stream().allMatch(MerchantOffer::isOutOfStock);
    }

    private static MerchantOffer createTrade(RandomSource random) {
        ItemStack result = createRandomJadeSlip(random);
        if (result.isEmpty()) {
            return null;
        }

        result.getOrCreateTag().putBoolean(IMMORTAL_TRADE_KEY, true);

        SpiritualStoneGrade[] grades = SpiritualStoneGrade.values();
        SpiritualStoneGrade grade = grades[random.nextInt(grades.length)];
        ItemStack spiritualStone = SpiritualStoneUtility.createStack(grade);
        spiritualStone.setCount(
                MINIMUM_SPIRITUAL_STONE_COUNT
                        + random.nextInt(MAXIMUM_SPIRITUAL_STONE_COUNT - MINIMUM_SPIRITUAL_STONE_COUNT + 1)
        );
        return new MerchantOffer(
                spiritualStone,
                ItemStack.EMPTY,
                result,
                IMMORTAL_TRADE_MAX_USES,
                IMMORTAL_TRADE_XP,
                IMMORTAL_TRADE_PRICE_MULTIPLIER
        );
    }

    private static ItemStack createRandomJadeSlip(RandomSource random) {
        List<TechniqueDefinition> techniques = TechniqueManager.INSTANCE.getTechniques();
        List<AlchemyRecipeDefinition> recipes = AlchemyRecipeManager.INSTANCE.getRecipes();
        int totalCount = techniques.size() + recipes.size();
        if (totalCount == 0) {
            return ItemStack.EMPTY;
        }

        int selectedItemIndex = random.nextInt(totalCount);
        if (selectedItemIndex < techniques.size()) {
            return TechniqueItemUtility.createStack(techniques.get(selectedItemIndex));
        }
        return AlchemyRecipeItemUtility.createStack(recipes.get(selectedItemIndex - techniques.size()));
    }

    private static int findOutOfStockImmortalTrade(MerchantOffers offers) {
        for (int offerIndex = 0; offerIndex < offers.size(); offerIndex++) {
            MerchantOffer offer = offers.get(offerIndex);
            if (offer.isOutOfStock() && isImmortalTrade(offer)) {
                return offerIndex;
            }
        }
        return -1;
    }

    private static void sendOffersToTradingPlayer(AbstractVillager villager, MerchantOffers offers) {
        Player tradingPlayer = villager.getTradingPlayer();
        if (!(tradingPlayer instanceof ServerPlayer serverPlayer)
                || !(serverPlayer.containerMenu instanceof MerchantMenu)) {
            return;
        }

        serverPlayer.sendMerchantOffers(
                serverPlayer.containerMenu.containerId,
                offers,
                villager instanceof Villager vanillaVillager
                        ? vanillaVillager.getVillagerData().getLevel()
                        : 1,
                villager.getVillagerXp(),
                villager.showProgressBar(),
                villager.canRestock()
        );
    }

    private ImmortalTradeUtility() {
    }
}
