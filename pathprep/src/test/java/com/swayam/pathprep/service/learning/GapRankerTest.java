package com.swayam.pathprep.service.learning;

import com.swayam.pathprep.enums.GapKind;
import com.swayam.pathprep.enums.SkillStatus;
import com.swayam.pathprep.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GapRankerTest {

    private GapRanker ranker;

    @BeforeEach
    void setUp() {
        ranker = new GapRanker();
    }

    @Test
    void invalidLimit_throwsBadRequest() {
        assertThatThrownBy(() -> ranker.rank(List.of(), 0))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> ranker.rank(List.of(), 11))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void filtersOutMasteredAndZeroQuestionTopics() {
        var c1 = new GapRanker.Candidate(1L, "T1", BigDecimal.valueOf(2.0), BigDecimal.valueOf(90.0), 5, SkillStatus.MASTERED, 10, 9, true);
        var c2 = new GapRanker.Candidate(2L, "T2", BigDecimal.valueOf(2.0), null, 0, SkillStatus.NOT_STARTED, 0, 0, true); // 0 questions
        var c3 = new GapRanker.Candidate(3L, "T3", BigDecimal.valueOf(2.0), BigDecimal.valueOf(40.0), 5, SkillStatus.IN_PROGRESS, 5, 2, true);

        var ranked = ranker.rank(List.of(c1, c2, c3), 5);
        assertThat(ranked).hasSize(1);
        assertThat(ranked.get(0).topicId()).isEqualTo(3L);
        assertThat(ranked.get(0).kind()).isEqualTo(GapKind.WEAK);
        assertThat(ranked.get(0).impactScore()).isEqualByComparingTo("120.0"); // 2.0 * (100 - 40)
    }

    @Test
    void orderingAndTieBreaks() {
        var cA = new GapRanker.Candidate(10L, "Alpha", BigDecimal.valueOf(1.0), null, 2, SkillStatus.NOT_STARTED, 0, 0, true); // impact: 1 * 100 = 100
        var cB = new GapRanker.Candidate(20L, "Beta", BigDecimal.valueOf(2.0), BigDecimal.valueOf(50.0), 2, SkillStatus.IN_PROGRESS, 4, 2, true); // impact: 2 * 50 = 100 (higher weight)

        var ranked = ranker.rank(List.of(cA, cB), 5);
        assertThat(ranked).hasSize(2);
        assertThat(ranked.get(0).topicId()).isEqualTo(20L); // higher weight wins tie
        assertThat(ranked.get(1).topicId()).isEqualTo(10L);
    }

    @Test
    void prerequisiteBlocked_markedAsPrerequisiteBlockedKind() {
        var cBlocked = new GapRanker.Candidate(30L, "BlockedTopic", BigDecimal.valueOf(2.0), null, 3, SkillStatus.NOT_STARTED, 0, 0, false);
        var ranked = ranker.rank(List.of(cBlocked), 5);
        assertThat(ranked).hasSize(1);
        assertThat(ranked.get(0).kind()).isEqualTo(GapKind.PREREQUISITE_BLOCKED);
        assertThat(ranked.get(0).suggestedAction().type()).isEqualTo("DRILL");
    }
}
