package com.swayam.pathprep.service.admin;

import com.swayam.pathprep.dto.admin.request.*;
import com.swayam.pathprep.dto.admin.response.*;
import com.swayam.pathprep.entity.Question;
import com.swayam.pathprep.entity.QuestionOption;
import com.swayam.pathprep.entity.Topic;
import com.swayam.pathprep.enums.QuestionSource;
import com.swayam.pathprep.enums.QuestionStatus;
import com.swayam.pathprep.enums.QuestionType;
import com.swayam.pathprep.exception.BadRequestException;
import com.swayam.pathprep.exception.ConflictException;
import com.swayam.pathprep.exception.NotFoundException;
import com.swayam.pathprep.exception.UnprocessableException;
import com.swayam.pathprep.repository.AttemptRepository;
import com.swayam.pathprep.repository.QuestionOptionRepository;
import com.swayam.pathprep.repository.QuestionRepository;
import com.swayam.pathprep.repository.ReviewQueueRepository;
import com.swayam.pathprep.repository.TopicRepository;
import com.swayam.pathprep.util.ContentHashUtil;
import com.swayam.pathprep.util.QuizEligibility;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminQuestionService {

    private static final int MAX_IMPORT_ITEMS = 500;
    private static final int MAX_PUBLISH_ITEMS = 200;

    private final QuestionRepository questionRepository;
    private final QuestionOptionRepository questionOptionRepository;
    private final TopicRepository topicRepository;
    private final AttemptRepository attemptRepository;
    private final ReviewQueueRepository reviewQueueRepository;

    @Transactional
    public AdminQuestionResponse createQuestion(CreateQuestionRequest request) {
        Topic topic = topicRepository.findById(request.topicId())
                .orElseThrow(() -> new NotFoundException("TOPIC_NOT_FOUND", "Topic not found: " + request.topicId()));

        Question question = new Question();
        question.setTopic(topic);
        question.setType(request.typeOrDefault());
        question.setDifficulty(request.difficulty());
        question.setBody(request.body().trim());
        question.setKeyPoints(formatKeyPointsJson(request.keyPoints()));
        question.setModelAnswer(request.modelAnswer());
        // POST /admin/questions must ALWAYS create DRAFT
        question.setStatus(QuestionStatus.DRAFT);
        question.setSource(QuestionSource.MANUAL);
        question.setSubtopic(request.trimmedSubtopic());

        if (request.options() != null) {
            for (AdminQuestionOptionDto optDto : request.options()) {
                QuestionOption opt = new QuestionOption();
                opt.setQuestion(question);
                opt.setPosition((byte) optDto.position());
                opt.setText(optDto.text().trim());
                opt.setCorrect(optDto.isCorrect());
                opt.setExplanation(optDto.explanation());
                question.getOptions().add(opt);
            }
        }

        List<String> optionTexts = question.getOptions().stream().map(QuestionOption::getText).toList();
        String hash = ContentHashUtil.computeHash(question.getBody(), optionTexts);
        if (questionRepository.existsByContentHash(hash)) {
            throw new ConflictException("DUPLICATE_QUESTION", "A question with identical content already exists");
        }
        question.setContentHash(hash);

        Question saved = questionRepository.save(question);
        return toAdminResponse(saved);
    }

    @Transactional
    public AdminQuestionResponse updateQuestion(Long id, UpdateQuestionRequest request) {
        Question question = questionRepository.findWithDetailsById(id)
                .orElseThrow(() -> new NotFoundException("QUESTION_NOT_FOUND", "Question not found: " + id));

        // PUT is allowed only for DRAFT or FLAGGED
        if (question.getStatus() != QuestionStatus.DRAFT && question.getStatus() != QuestionStatus.FLAGGED) {
            throw new ConflictException("QUESTION_NOT_EDITABLE", "Only questions in DRAFT or FLAGGED status can be edited");
        }

        // Check if question has existing student attempts or review items
        boolean hasAttempts = attemptRepository.existsByQuestionId(id) || reviewQueueRepository.existsByQuestionId(id);

        if (hasAttempts && request.options() != null) {
            List<QuestionOption> existingOptions = question.getOptions();
            if (request.options().size() != existingOptions.size()) {
                throw new ConflictException("QUESTION_HAS_ATTEMPTS",
                        "Question has existing attempts; adding or removing options is not allowed");
            }
            for (int i = 0; i < request.options().size(); i++) {
                AdminQuestionOptionDto newOpt = request.options().get(i);
                QuestionOption oldOpt = existingOptions.get(i);
                if (newOpt.isCorrect() != oldOpt.isCorrect()) {
                    throw new ConflictException("QUESTION_HAS_ATTEMPTS",
                            "Question has existing attempts; changing which option is correct is not allowed");
                }
                // Text edits are permitted
                oldOpt.setText(newOpt.text().trim());
                oldOpt.setExplanation(newOpt.explanation());
            }
        } else if (request.options() != null) {
            question.getOptions().clear();
            for (AdminQuestionOptionDto optDto : request.options()) {
                QuestionOption opt = new QuestionOption();
                opt.setQuestion(question);
                opt.setPosition((byte) optDto.position());
                opt.setText(optDto.text().trim());
                opt.setCorrect(optDto.isCorrect());
                opt.setExplanation(optDto.explanation());
                question.getOptions().add(opt);
            }
        }

        if (request.topicId() != null) {
            Topic topic = topicRepository.findById(request.topicId())
                    .orElseThrow(() -> new NotFoundException("TOPIC_NOT_FOUND", "Topic not found: " + request.topicId()));
            question.setTopic(topic);
        }

        if (request.type() != null) {
            question.setType(request.type());
        }
        if (request.difficulty() != null) {
            question.setDifficulty(request.difficulty());
        }
        if (request.body() != null && !request.body().isBlank()) {
            question.setBody(request.body().trim());
        }
        if (request.keyPoints() != null) {
            question.setKeyPoints(formatKeyPointsJson(request.keyPoints()));
        }
        if (request.modelAnswer() != null) {
            question.setModelAnswer(request.modelAnswer());
        }
        if (request.subtopic() != null) {
            question.setSubtopic(request.trimmedSubtopic());
        }

        List<String> optionTexts = question.getOptions().stream().map(QuestionOption::getText).toList();
        String hash = ContentHashUtil.computeHash(question.getBody(), optionTexts);
        Optional<Question> existingWithHash = questionRepository.findByContentHash(hash);
        if (existingWithHash.isPresent() && !existingWithHash.get().getId().equals(question.getId())) {
            throw new ConflictException("DUPLICATE_QUESTION", "A question with identical content already exists (ID: " + existingWithHash.get().getId() + ")");
        }
        question.setContentHash(hash);

        question.setUpdatedAt(Instant.now());
        Question saved = questionRepository.save(question);
        return toAdminResponse(saved);
    }

    @Transactional
    public AdminQuestionResponse publishQuestion(Long id) {
        Question question = questionRepository.findWithDetailsById(id)
                .orElseThrow(() -> new NotFoundException("QUESTION_NOT_FOUND", "Question not found: " + id));

        if (question.getStatus() == QuestionStatus.ARCHIVED) {
            throw new ConflictException("QUESTION_ARCHIVED", "Cannot publish an archived question: " + id);
        }

        List<String> violations = QuizEligibility.violationsForPublishing(question);
        if (!violations.isEmpty()) {
            throw new UnprocessableException("QUESTION_INVALID", "Question does not meet quiz eligibility criteria", violations);
        }

        question.setStatus(QuestionStatus.PUBLISHED);
        question.setReviewedAt(Instant.now());
        question.setFlagReason(null);
        question.setUpdatedAt(Instant.now());

        Question saved = questionRepository.save(question);
        return toAdminResponse(saved);
    }

    @Transactional
    public BulkPublishResponse bulkPublish(BulkPublishRequest request) {
        if (request.questionIds().size() > MAX_PUBLISH_ITEMS) {
            throw new BadRequestException("MAX_PUBLISH_LIMIT_EXCEEDED",
                    "Bulk publish exceeds maximum allowed limit of " + MAX_PUBLISH_ITEMS + " questions");
        }

        List<BulkPublishItemResult> results = new ArrayList<>();
        int publishedCount = 0;
        int invalidCount = 0;

        for (Long id : request.questionIds()) {
            Optional<Question> optQuestion = questionRepository.findWithDetailsById(id);
            if (optQuestion.isEmpty()) {
                results.add(BulkPublishItemResult.invalid(id, List.of("Question not found: " + id)));
                invalidCount++;
                continue;
            }

            Question q = optQuestion.get();
            if (q.getStatus() == QuestionStatus.ARCHIVED) {
                results.add(BulkPublishItemResult.invalid(id, List.of("Cannot publish archived question")));
                invalidCount++;
                continue;
            }

            if (q.getStatus() == QuestionStatus.PUBLISHED) {
                // Already published (idempotent)
                results.add(BulkPublishItemResult.published(id));
                publishedCount++;
                continue;
            }

            List<String> violations = QuizEligibility.violationsForPublishing(q);
            if (!violations.isEmpty()) {
                results.add(BulkPublishItemResult.invalid(id, violations));
                invalidCount++;
                continue;
            }

            q.setStatus(QuestionStatus.PUBLISHED);
            q.setReviewedAt(Instant.now());
            q.setFlagReason(null);
            q.setUpdatedAt(Instant.now());
            questionRepository.save(q);

            results.add(BulkPublishItemResult.published(id));
            publishedCount++;
        }

        return new BulkPublishResponse(request.questionIds().size(), publishedCount, invalidCount, results);
    }

    @Transactional
    public AdminQuestionResponse flagQuestion(Long id, FlagQuestionRequest request) {
        Question question = questionRepository.findWithDetailsById(id)
                .orElseThrow(() -> new NotFoundException("QUESTION_NOT_FOUND", "Question not found: " + id));

        question.setStatus(QuestionStatus.FLAGGED);
        question.setFlagReason(request.reason().trim());
        question.setUpdatedAt(Instant.now());

        Question saved = questionRepository.save(question);
        return toAdminResponse(saved);
    }

    @Transactional
    public AdminQuestionResponse archiveQuestion(Long id) {
        Question question = questionRepository.findWithDetailsById(id)
                .orElseThrow(() -> new NotFoundException("QUESTION_NOT_FOUND", "Question not found: " + id));

        question.setStatus(QuestionStatus.ARCHIVED);
        question.setUpdatedAt(Instant.now());

        Question saved = questionRepository.save(question);
        return toAdminResponse(saved);
    }

    @Transactional(readOnly = true)
    public AdminQuestionResponse getQuestion(Long id) {
        Question question = questionRepository.findWithDetailsById(id)
                .orElseThrow(() -> new NotFoundException("QUESTION_NOT_FOUND", "Question not found: " + id));
        return toAdminResponse(question);
    }

    @Transactional(readOnly = true)
    public Page<AdminQuestionSummary> listQuestions(
            Long topicId,
            QuestionStatus status,
            Integer difficulty,
            String subtopic,
            String search,
            Pageable pageable
    ) {
        Specification<Question> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (topicId != null) {
                predicates.add(cb.equal(root.get("topic").get("id"), topicId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (difficulty != null) {
                predicates.add(cb.equal(root.get("difficulty"), difficulty));
            }
            if (subtopic != null && !subtopic.isBlank()) {
                predicates.add(cb.equal(root.get("subtopic"), subtopic.trim()));
            }
            if (search != null && !search.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("body")), "%" + search.toLowerCase().trim() + "%"));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Question> page = questionRepository.findAll(spec, pageable);
        return page.map(this::toSummary);
    }

    @Transactional
    public BulkImportResponse bulkImport(BulkImportRequest request) {
        if (request.questions().size() > MAX_IMPORT_ITEMS) {
            throw new BadRequestException("MAX_IMPORT_LIMIT_EXCEEDED",
                    "Bulk import exceeds maximum allowed limit of " + MAX_IMPORT_ITEMS + " questions");
        }

        String batch = (request.batch() != null && !request.batch().isBlank())
                ? request.batch().trim()
                : "batch-" + UUID.randomUUID().toString().substring(0, 8);

        boolean dryRun = request.isDryRun();

        Map<String, Topic> topicCache = new HashMap<>();
        Set<String> seenHashesInBatch = new HashSet<>();
        List<ImportItemResult> results = new ArrayList<>();

        int importedCount = 0;
        int duplicateCount = 0;
        int invalidCount = 0;

        for (int i = 0; i < request.questions().size(); i++) {
            ImportQuestionItem item = request.questions().get(i);
            List<String> errors = new ArrayList<>();

            if (item.body() == null || item.body().isBlank()) {
                errors.add("body is required");
            }

            Topic topic = null;
            if (item.topicId() != null) {
                topic = topicRepository.findById(item.topicId()).orElse(null);
                if (topic == null) {
                    errors.add("Topic not found by id: " + item.topicId());
                }
            } else if (item.topicName() != null && !item.topicName().isBlank()) {
                String name = item.topicName().trim();
                topic = topicCache.computeIfAbsent(name, n -> topicRepository.findAll().stream()
                        .filter(t -> t.getName().equalsIgnoreCase(n))
                        .findFirst()
                        .orElse(null));
                if (topic == null) {
                    errors.add("Topic not found by name: " + item.topicName());
                }
            } else {
                errors.add("Either topicId or topicName must be provided");
            }

            if (!errors.isEmpty()) {
                results.add(ImportItemResult.invalid(i, errors));
                invalidCount++;
                continue;
            }

            List<String> optTexts = item.options() != null
                    ? item.options().stream().map(AdminQuestionOptionDto::text).toList()
                    : List.of();
            String hash = ContentHashUtil.computeHash(item.body(), optTexts);

            // In-file duplicate check
            if (seenHashesInBatch.contains(hash)) {
                results.add(ImportItemResult.duplicate(i, null, "Duplicate question within the same import file"));
                duplicateCount++;
                continue;
            }

            // DB duplicate check
            Optional<Question> dbExisting = questionRepository.findByContentHash(hash);
            if (dbExisting.isPresent()) {
                results.add(ImportItemResult.duplicate(i, dbExisting.get().getId(), "Question with identical content already exists"));
                duplicateCount++;
                continue;
            }

            Question q = new Question();
            q.setTopic(topic);
            q.setType(item.typeOrDefault());
            q.setDifficulty(item.difficulty());
            q.setBody(item.body().trim());
            q.setKeyPoints(formatKeyPointsJson(item.keyPoints()));
            q.setModelAnswer(item.modelAnswer());
            q.setStatus(item.statusOrDefault());
            q.setSource(QuestionSource.IMPORTED);
            q.setImportBatch(batch);
            q.setSubtopic(item.trimmedSubtopic());
            q.setContentHash(hash);

            if (item.options() != null) {
                for (AdminQuestionOptionDto optDto : item.options()) {
                    QuestionOption opt = new QuestionOption();
                    opt.setQuestion(q);
                    opt.setPosition((byte) optDto.position());
                    opt.setText(optDto.text().trim());
                    opt.setCorrect(optDto.isCorrect());
                    opt.setExplanation(optDto.explanation());
                    q.getOptions().add(opt);
                }
            }

            if (q.getStatus() == QuestionStatus.PUBLISHED) {
                List<String> violations = QuizEligibility.violationsForPublishing(q);
                if (!violations.isEmpty()) {
                    results.add(ImportItemResult.invalid(i, violations));
                    invalidCount++;
                    continue;
                }
                q.setReviewedAt(Instant.now());
            }

            seenHashesInBatch.add(hash);

            if (dryRun) {
                results.add(ImportItemResult.wouldCreate(i));
                importedCount++;
            } else {
                Question saved = questionRepository.save(q);
                results.add(ImportItemResult.created(i, saved.getId()));
                importedCount++;
            }
        }

        return new BulkImportResponse(
                batch,
                request.questions().size(),
                importedCount,
                duplicateCount,
                invalidCount,
                results
        );
    }

    private AdminQuestionResponse toAdminResponse(Question q) {
        List<AdminQuestionOptionDto> optionDtos = q.getOptions().stream()
                .map(o -> new AdminQuestionOptionDto(o.getId(), o.getPosition(), o.getText(), o.isCorrect(), o.getExplanation()))
                .toList();

        return new AdminQuestionResponse(
                q.getId(),
                q.getTopic() != null ? q.getTopic().getId() : null,
                q.getTopic() != null ? q.getTopic().getName() : null,
                q.getType(),
                q.getDifficulty(),
                q.getBody(),
                q.getKeyPoints(),
                q.getModelAnswer(),
                q.getStatus(),
                q.getSource(),
                q.getSubtopic(),
                q.getContentHash(),
                q.getImportBatch(),
                q.getFlagReason(),
                q.getReviewedAt(),
                optionDtos,
                QuizEligibility.isEligible(q),
                q.getCreatedAt(),
                q.getUpdatedAt()
        );
    }

    private AdminQuestionSummary toSummary(Question q) {
        return new AdminQuestionSummary(
                q.getId(),
                q.getTopic() != null ? q.getTopic().getId() : null,
                q.getTopic() != null ? q.getTopic().getName() : null,
                q.getType(),
                q.getDifficulty(),
                q.getBody(),
                q.getStatus(),
                q.getSource(),
                q.getSubtopic(),
                q.getOptions() != null ? q.getOptions().size() : 0,
                QuizEligibility.isEligible(q),
                q.getCreatedAt(),
                q.getUpdatedAt()
        );
    }

    private String formatKeyPointsJson(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        String trimmed = input.trim();
        if ((trimmed.startsWith("[") && trimmed.endsWith("]")) ||
            (trimmed.startsWith("{") && trimmed.endsWith("}"))) {
            return trimmed;
        }
        StringBuilder sb = new StringBuilder("[\"");
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < ' ') {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        sb.append("\"]");
        return sb.toString();
    }
}
