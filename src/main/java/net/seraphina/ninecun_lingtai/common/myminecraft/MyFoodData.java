package net.seraphina.ninecun_lingtai.common.myminecraft;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.seraphina.ninecun_lingtai.utility.CultivationDefenseUtility;

public class MyFoodData extends FoodData {
    @Override
    public void tick(Player pPlayer) {
        super.tick(pPlayer);
        if (CultivationDefenseUtility.hasActiveDefense(pPlayer)) {
            this.foodLevel = 40;
            this.exhaustionLevel = 40;
            this.lastFoodLevel = 40;
        }
    }
}
