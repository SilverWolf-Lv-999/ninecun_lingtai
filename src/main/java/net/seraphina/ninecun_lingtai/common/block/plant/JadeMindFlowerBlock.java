package net.seraphina.ninecun_lingtai.common.block.plant;

import net.seraphina.ninecun_lingtai.api.element.ElementType;
import net.seraphina.ninecun_lingtai.api.plant.SpiritualPlantGrade;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** 凝神玉兰：三阶木属灵药，生于针叶林云杉木上（炼丹 M4-d 野生灵药）。 */
public final class JadeMindFlowerBlock extends SpiritualPlantBlock {
    public static final SpiritualPlantGrade BASE_GRADE = new SpiritualPlantGrade(3, 1);

    public JadeMindFlowerBlock() {
        super(
                ElementType.WOOD,
                BASE_GRADE,
                BlockBehaviour.Properties.copy(Blocks.CRIMSON_FUNGUS).noOcclusion().instabreak()
        );
    }
}
