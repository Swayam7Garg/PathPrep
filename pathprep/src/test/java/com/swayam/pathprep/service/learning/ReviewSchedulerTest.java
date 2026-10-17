package com.swayam.pathprep.service.learning;

import com.swayam.pathprep.enums.ReviewStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReviewSchedulerTest {

    private ReviewScheduler scheduler;
    private final LocalDate today = LocalDate.of(2026, 10, 16);

    @BeforeEach
    void setUp() {
        scheduler = new ReviewScheduler(List.of(1, 3, 7, 14));
    }

    @Test
    void wrongAnswerAtStep0_staysAtStep0Interval1Tomorrow() {
        var res = scheduler.schedule(0, false, today);
        assertThat(res.repetitionCount()).isEqualTo(0);
        assertThat(res.intervalDays()).isEqualTo(1);
        assertThat(res.nextDueOn()).isEqualTo(today.plusDays(1));
        assertThat(res.graduated()).isFalse();
        assertThat(res.status()).isEqualTo(ReviewStatus.PENDING);
    }

    @Test
    void wrongAnswerAtHigherStep_resetsToStep0Interval1Tomorrow() {
        var res = scheduler.schedule(2, false, today);
        assertThat(res.repetitionCount()).isEqualTo(0);
        assertThat(res.intervalDays()).isEqualTo(1);
        assertThat(res.nextDueOn()).isEqualTo(today.plusDays(1));
        assertThat(res.graduated()).isFalse();
        assertThat(res.status()).isEqualTo(ReviewStatus.PENDING);
    }

    @Test
    void correctLadderSteps_allFourTransitions() {
        // Transition 1: Step 0 -> 1 (interval: 3 days)
        var step1 = scheduler.schedule(0, true, today);
        assertThat(step1.repetitionCount()).isEqualTo(1);
        assertThat(step1.intervalDays()).isEqualTo(3);
        assertThat(step1.nextDueOn()).isEqualTo(today.plusDays(3));
        assertThat(step1.graduated()).isFalse();
        assertThat(step1.status()).isEqualTo(ReviewStatus.PENDING);

        // Transition 2: Step 1 -> 2 (interval: 7 days)
        var step2 = scheduler.schedule(1, true, today);
        assertThat(step2.repetitionCount()).isEqualTo(2);
        assertThat(step2.intervalDays()).isEqualTo(7);
        assertThat(step2.nextDueOn()).isEqualTo(today.plusDays(7));
        assertThat(step2.graduated()).isFalse();

        // Transition 3: Step 2 -> 3 (interval: 14 days)
        var step3 = scheduler.schedule(2, true, today);
        assertThat(step3.repetitionCount()).isEqualTo(3);
        assertThat(step3.intervalDays()).isEqualTo(14);
        assertThat(step3.nextDueOn()).isEqualTo(today.plusDays(14));
        assertThat(step3.graduated()).isFalse();

        // Transition 4: Step 3 -> 4 (graduated)
        var step4 = scheduler.schedule(3, true, today);
        assertThat(step4.repetitionCount()).isEqualTo(4);
        assertThat(step4.intervalDays()).isNull();
        assertThat(step4.nextDueOn()).isNull();
        assertThat(step4.graduated()).isTrue();
        assertThat(step4.status()).isEqualTo(ReviewStatus.COMPLETED);
    }
}
