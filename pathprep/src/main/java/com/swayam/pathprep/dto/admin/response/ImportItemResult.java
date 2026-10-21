package com.swayam.pathprep.dto.admin.response;

import java.util.List;

public record ImportItemResult(
        int index,
        String status, // "IMPORTED", "DUPLICATE", "INVALID"
        Long questionId,
        Long duplicateQuestionId,
        List<String> errors
) {
    public static ImportItemResult created(int index, Long questionId) {
        return new ImportItemResult(index, "CREATED", questionId, null, List.of());
    }

    public static ImportItemResult wouldCreate(int index) {
        return new ImportItemResult(index, "WOULD_CREATE", null, null, List.of());
    }

    public static ImportItemResult duplicate(int index, Long duplicateQuestionId, String reason) {
        return new ImportItemResult(index, "DUPLICATE", null, duplicateQuestionId, List.of(reason));
    }

    public static ImportItemResult invalid(int index, List<String> errors) {
        return new ImportItemResult(index, "INVALID", null, null, errors);
    }
}
