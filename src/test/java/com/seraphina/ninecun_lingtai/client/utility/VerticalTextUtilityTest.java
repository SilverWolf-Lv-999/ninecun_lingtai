package com.seraphina.ninecun_lingtai.client.utility;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VerticalTextUtilityTest {
    @Test
    void splitsChineseTextTopToBottomThenRightToLeftByColumns() {
        assertEquals(
                List.of(
                        List.of("天", "地", "玄"),
                        List.of("黄", "宇", "宙"),
                        List.of("洪", "荒")
                ),
                VerticalTextUtility.splitIntoColumns("天地玄黄宇宙洪荒", 3)
        );
    }

    @Test
    void preservesExplicitLineBreakAsANewColumn() {
        assertEquals(
                List.of(List.of("甲", "乙"), List.of("丙", "丁")),
                VerticalTextUtility.splitIntoColumns("甲乙\n丙丁", 4)
        );
    }

    @Test
    void doesNotSplitSupplementaryUnicodeCharacters() {
        assertEquals(
                List.of(List.of("𠀀", "道")),
                VerticalTextUtility.splitIntoColumns("𠀀道", 2)
        );
    }

    @Test
    void rejectsNonPositiveRowLimit() {
        assertThrows(
                IllegalArgumentException.class,
                () -> VerticalTextUtility.splitIntoColumns("内容", 0)
        );
    }
}
