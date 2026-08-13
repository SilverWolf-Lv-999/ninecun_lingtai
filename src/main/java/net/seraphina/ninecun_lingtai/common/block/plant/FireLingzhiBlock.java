package net.seraphina.ninecun_lingtai.common.block.plant;

import net.seraphina.ninecun_lingtai.api.element.ElementType;
import net.seraphina.ninecun_lingtai.api.plant.SpiritualPlantGrade;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class FireLingzhiBlock extends SpiritualPlantBlock {
    public static final SpiritualPlantGrade BASE_GRADE = new SpiritualPlantGrade(1, 1);

    public FireLingzhiBlock() {
        super(
                ElementType.FIRE,
                BASE_GRADE,
                BlockBehaviour.Properties.copy(Blocks.CRIMSON_FUNGUS).noOcclusion().instabreak()
        );
    }
}
