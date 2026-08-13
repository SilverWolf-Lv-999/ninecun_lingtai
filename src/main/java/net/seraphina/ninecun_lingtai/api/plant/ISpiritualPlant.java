package net.seraphina.ninecun_lingtai.api.plant;

import net.seraphina.ninecun_lingtai.api.element.ElementType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public interface ISpiritualPlant {
    ElementType getPrimaryElement();

    SpiritualPlantGrade getBaseGrade();

    SpiritualPlantGrade calculateGrade(double elementEnvironmentMultiplier);

    SpiritualPlantGrade getGrade(Level level, BlockPos blockPosition);
}
