package com.swayam.pathprep.controller.admin;

import com.swayam.pathprep.dto.admin.request.*;
import com.swayam.pathprep.dto.admin.response.*;
import com.swayam.pathprep.enums.QuestionStatus;
import com.swayam.pathprep.exception.BadRequestException;
import com.swayam.pathprep.service.admin.AdminQuestionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/questions")
@RequiredArgsConstructor
public class AdminQuestionController {

    private static final long MAX_IMPORT_BYTES = 1024 * 1024; // 1 MB cap

    private final AdminQuestionService adminQuestionService;

    @PostMapping
    public ResponseEntity<AdminQuestionResponse> createQuestion(@Valid @RequestBody CreateQuestionRequest request) {
        log.info("Admin creating question for topicId={}", request.topicId());
        AdminQuestionResponse response = adminQuestionService.createQuestion(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<Page<AdminQuestionSummary>> listQuestions(
            @RequestParam(required = false) Long topicId,
            @RequestParam(required = false) QuestionStatus status,
            @RequestParam(required = false) Integer difficulty,
            @RequestParam(required = false) String subtopic,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<AdminQuestionSummary> response = adminQuestionService.listQuestions(
                topicId, status, difficulty, subtopic, search, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminQuestionResponse> getQuestion(@PathVariable Long id) {
        AdminQuestionResponse response = adminQuestionService.getQuestion(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AdminQuestionResponse> updateQuestion(
            @PathVariable Long id,
            @Valid @RequestBody UpdateQuestionRequest request
    ) {
        log.info("Admin updating question id={}", id);
        AdminQuestionResponse response = adminQuestionService.updateQuestion(id, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<AdminQuestionResponse> publishQuestion(@PathVariable Long id) {
        log.info("Admin publishing question id={}", id);
        AdminQuestionResponse response = adminQuestionService.publishQuestion(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/publish")
    public ResponseEntity<BulkPublishResponse> bulkPublish(@Valid @RequestBody BulkPublishRequest request) {
        log.info("Admin bulk publishing {} questions", request.questionIds().size());
        BulkPublishResponse response = adminQuestionService.bulkPublish(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/flag")
    public ResponseEntity<AdminQuestionResponse> flagQuestion(
            @PathVariable Long id,
            @Valid @RequestBody FlagQuestionRequest request
    ) {
        log.info("Admin flagging question id={}, reason={}", id, request.reason());
        AdminQuestionResponse response = adminQuestionService.flagQuestion(id, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/archive")
    public ResponseEntity<AdminQuestionResponse> archiveQuestion(@PathVariable Long id) {
        log.info("Admin archiving question id={}", id);
        AdminQuestionResponse response = adminQuestionService.archiveQuestion(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/import")
    public ResponseEntity<BulkImportResponse> bulkImport(
            @Valid @RequestBody BulkImportRequest request,
            HttpServletRequest httpRequest
    ) {
        long contentLength = httpRequest.getContentLengthLong();
        if (contentLength > MAX_IMPORT_BYTES) {
            throw new BadRequestException("PAYLOAD_TOO_LARGE", "Import payload exceeds 1 MB limit (size: " + contentLength + " bytes)");
        }
        log.info("Admin bulk importing {} questions, batch={}", request.questions().size(), request.batch());
        BulkImportResponse response = adminQuestionService.bulkImport(request);
        return ResponseEntity.ok(response);
    }
}
