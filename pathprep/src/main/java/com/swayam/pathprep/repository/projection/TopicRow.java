package com.swayam.pathprep.repository.projection;

import java.math.BigDecimal;

/**
 * Spring Data projection capturing topic summary statistics and question counts within a track.
 */
public interface TopicRow {
    long getId();
    String getName();
    String getDomain();
    BigDecimal getWeight();
    long getQuestionCount();
}
