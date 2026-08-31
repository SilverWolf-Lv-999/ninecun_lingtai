package net.seraphina.ninecun_lingtai.common.block.plant;

import net.seraphina.ninecun_lingtai.api.element.ElementType;
import net.seraphina.ninecun_lingtai.api.plant.SpiritualPlantGrade;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** 月露兰：三阶水属灵药，生于樱花木上（炼丹 M4-d 野生灵药）。 */
public final class MoonDewOrchidBlock extends SpiritualPlantBlock {
    public static final SpiritualPlantGrade BASE_GRADE = new SpiritualPlantGrade(3, 1);

    public MoonDewOrchidBlock() {
        super(
                ElementType.WATER,
                BASE_GRADE,
                BlockBehaviour.Properties.copy(Blocks.CRIMSON_FUNGUS).noOcclusion().instabreak()
        );
    }
}
