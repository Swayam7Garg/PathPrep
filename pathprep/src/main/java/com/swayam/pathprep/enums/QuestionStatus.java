package com.swayam.pathprep.enums;

/**
 * Lifecycle states of a question.
 * <ul>
 *   <li>DRAFT – created but not yet approved for student use.</li>
 *   <li>PUBLISHED – visible to students and counted in track/topic stats.</li>
 *   <li>FLAGGED – hidden from new sessions; admin review required.</li>
 *   <li>ARCHIVED – permanently retired; excluded from all counts and selection.</li>
 * </ul>
 * There is no DELETE endpoint. Questions move through this lifecycle only.
 */
public enum QuestionStatus {
    DRAFT,
    PUBLISHED,
    FLAGGED,
    ARCHIVED
}
