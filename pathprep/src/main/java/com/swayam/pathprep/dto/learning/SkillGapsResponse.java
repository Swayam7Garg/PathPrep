package com.swayam.pathprep.dto.learning;

import java.util.List;

/**
 * Response payload listing identified weak areas, unmet prerequisite topics, and remedial actions.
 */
public record SkillGapsResponse(
        Long trackId,
        List<SkillGapView> gaps
) {}
