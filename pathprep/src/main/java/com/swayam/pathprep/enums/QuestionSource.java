package com.swayam.pathprep.enums;

/**
 * Provenance of a question: how it was created.
 * Used for auditing and content-pipeline tracking. Admin-only; not exposed to students.
 */
public enum QuestionSource {
    /** Created by an admin via the POST /admin/questions endpoint. */
    MANUAL,
    /** Brought in via the bulk JSON import endpoint. */
    IMPORTED,
    /** Reserved for future AI-assisted generation. */
    AI_GENERATED
}
