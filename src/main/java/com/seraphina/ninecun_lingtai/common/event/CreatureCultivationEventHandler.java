package com.seraphina.ninecun_lingtai.common.event;

import com.seraphina.ninecun_lingtai.ModMain;
import com.seraphina.ninecun_lingtai.common.config.CommonConfig;
import com.seraphina.ninecun_lingtai.common.creature.CreatureCultivationData;
import com.seraphina.ninecun_lingtai.common.creature.CreatureCultivationGoalInstaller;
import com.seraphina.ninecun_lingtai.utility.CreatureCultivationUtility;
import com.seraphina.ninecun_lingtai.utility.CultivationDefenseUtility;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ModMain.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CreatureCultivationEventHandler {

    @SubscribeEvent
    public static void onFinalizeSpawn(MobSpawnEvent.FinalizeSpawn event) {
        Mob mob = event.getEntity();
        if (mob.level().isClientSide || CreatureCultivationData.isSpawnRollDone(mob)) {
            return;
        }
        CreatureCultivationData.markSpawnRollDone(mob);

        mob.setHealth(mob.getMaxHealth());

        RandomSource random = mob.getRandom();
        if (random.nextDouble() >= CommonConfig.CREATURE_CULTIVATOR_CHANCE.get()) {
            return;
        }
        CreatureCultivationData.setStage(mob, CreatureCultivationUtility.rollSpawnStage(random));
        CreatureCultivationData.setSpiritualRoot(mob, CreatureCultivationUtility.rollSpawnSpiritualRoot(random));
        CultivationDefenseUtility.syncMaxHealthModifier(mob);
        mob.setHealth(mob.getMaxHealth());
        CreatureCultivationGoalInstaller.install(mob);
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide
                || !(event.getEntity() instanceof Mob mob)
                || CreatureCultivationData.getStage(mob).isEmpty()) {
            return;
        }
        CreatureCultivationGoalInstaller.install(mob);
    }
}
