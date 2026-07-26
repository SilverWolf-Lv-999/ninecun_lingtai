package com.seraphina.ninecun_lingtai.utility;

import com.google.gson.JsonParseException;
import com.seraphina.ninecun_lingtai.api.technique.TechniqueAssessment;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AiResponseUtilityTest {
    @Test
    void parsesPlainJsonAssessment() {
        TechniqueAssessment assessment = AiResponseUtility.parseTechniqueAssessment(
                "{\"score\":87,\"feedback\":\"理解了行气次序\"}"
        );

        assertEquals(87, assessment.score());
        assertEquals("理解了行气次序", assessment.feedback());
    }

    @Test
    void toleratesMarkdownFenceFromProvider() {
        TechniqueAssessment assessment = AiResponseUtility.parseTechniqueAssessment("""
                ```json
                {"score":100,"feedback":"已得小成"}
                ```
                """);

        assertEquals(100, assessment.score());
    }

    @Test
    void rejectsOutOfRangeScore() {
        assertThrows(JsonParseException.class, () -> AiResponseUtility.parseTechniqueAssessment(
                "{\"score\":101,\"feedback\":\"错误\"}"
        ));
    }
}
