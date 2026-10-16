package com.swayam.pathprep.dto.learning;

import java.util.List;

public record SkillGapsResponse(
        Long trackId,
        List<SkillGapView> gaps
) {}
