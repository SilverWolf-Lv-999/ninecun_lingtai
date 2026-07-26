package com.seraphina.ninecun_lingtai.utility;

import com.seraphina.ninecun_lingtai.api.aptitude.SpiritualRoot;
import com.seraphina.ninecun_lingtai.api.element.ElementType;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SpiritualRootUtilityTest {
    @Test
    void namesAllFiveElementsAsFiveElementRoot() {
        assertEquals(
                "五行灵根",
                SpiritualRootUtility.getSpiritualRootName(Set.copyOf(ElementType.FIVE_ELEMENTS))
        );
    }

    @Test
    void keepsChaosAndPartialElementNamesUnchanged() {
        assertEquals(
                "混沌灵根",
                SpiritualRootUtility.getSpiritualRootName(SpiritualRoot.SELECTABLE_ELEMENTS)
        );
        assertEquals(
                "水木火土灵根",
                SpiritualRootUtility.getSpiritualRootName(Set.of(
                        ElementType.WATER,
                        ElementType.WOOD,
                        ElementType.FIRE,
                        ElementType.EARTH
                ))
        );
    }
}
