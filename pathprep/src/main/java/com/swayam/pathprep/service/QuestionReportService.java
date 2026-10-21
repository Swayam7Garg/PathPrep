package com.swayam.pathprep.service;

import com.swayam.pathprep.dto.request.CreateQuestionReportRequest;
import com.swayam.pathprep.dto.response.QuestionReportResponse;
import com.swayam.pathprep.entity.Question;
import com.swayam.pathprep.entity.QuestionReport;
import com.swayam.pathprep.entity.User;
import com.swayam.pathprep.exception.ConflictException;
import com.swayam.pathprep.exception.NotFoundException;
import com.swayam.pathprep.repository.QuestionReportRepository;
import com.swayam.pathprep.repository.QuestionRepository;
import com.swayam.pathprep.repository.UserRepository;
import com.swayam.pathprep.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class QuestionReportService {

    private final QuestionReportRepository questionReportRepository;
    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;
    private final CurrentUser currentUser;

    @Transactional
    public QuestionReportResponse reportQuestion(Long questionId, CreateQuestionReportRequest request) {
        Long userId = currentUser.getUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found: " + userId));

        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new NotFoundException("QUESTION_NOT_FOUND", "Question not found: " + questionId));

        if (questionReportRepository.existsByUserIdAndQuestionId(userId, questionId)) {
            throw new ConflictException("ALREADY_REPORTED", "You have already reported this question");
        }

        QuestionReport report = new QuestionReport(user, question, request.reason().trim());
        QuestionReport saved = questionReportRepository.save(report);

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<QuestionReportResponse> listReports(Long questionId, Pageable pageable) {
        Page<QuestionReport> page = (questionId != null)
                ? questionReportRepository.findByQuestionId(questionId, pageable)
                : questionReportRepository.findAllWithDetails(pageable);

        return page.map(this::toResponse);
    }

    private QuestionReportResponse toResponse(QuestionReport report) {
        return new QuestionReportResponse(
                report.getId(),
                report.getUser().getId(),
                report.getUser().getEmail(),
                report.getQuestion().getId(),
                report.getQuestion().getBody(),
                report.getReason(),
                report.getCreatedAt()
        );
    }
}
