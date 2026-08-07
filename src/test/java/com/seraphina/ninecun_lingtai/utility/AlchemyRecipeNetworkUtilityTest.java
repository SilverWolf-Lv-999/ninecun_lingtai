package com.seraphina.ninecun_lingtai.utility;

import com.seraphina.ninecun_lingtai.api.alchemy.AlchemyGrade;
import com.seraphina.ninecun_lingtai.api.alchemy.AlchemyIngredient;
import com.seraphina.ninecun_lingtai.api.alchemy.AlchemyRecipeJeiData;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AlchemyRecipeNetworkUtilityTest {
    @Test
    void roundTripsJeiRecipeData() {
        AlchemyRecipeJeiData recipe = new AlchemyRecipeJeiData(
                ResourceLocation.fromNamespaceAndPath("test", "restoration_pill"),
                new AlchemyIngredient(ResourceLocation.withDefaultNamespace("grass"), 6),
                List.of(
                        new AlchemyIngredient(ResourceLocation.withDefaultNamespace("fern"), 4),
                        new AlchemyIngredient(ResourceLocation.withDefaultNamespace("moss_block"), 2)
                ),
                List.of(
                        new AlchemyIngredient(ResourceLocation.withDefaultNamespace("azalea_leaves"), 4),
                        new AlchemyIngredient(
                                ResourceLocation.withDefaultNamespace("flowering_azalea_leaves"),
                                4
                        )
                ),
                ResourceLocation.fromNamespaceAndPath("ninecun_lingtai", "grass_restoration_pill"),
                2,
                new AlchemyGrade(2, 3)
        );
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());

        AlchemyRecipeNetworkUtility.writeJeiRecipes(buffer, List.of(recipe));

        assertEquals(List.of(recipe), AlchemyRecipeNetworkUtility.readJeiRecipes(buffer));
    }
}
