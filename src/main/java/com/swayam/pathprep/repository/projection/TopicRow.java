package com.swayam.pathprep.repository.projection;

import java.math.BigDecimal;

public interface TopicRow {
    long getId();
    String getName();
    String getDomain();
    BigDecimal getWeight();
    long getQuestionCount();
}
