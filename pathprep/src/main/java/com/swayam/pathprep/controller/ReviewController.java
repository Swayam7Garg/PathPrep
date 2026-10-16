package com.swayam.pathprep.controller;

import com.swayam.pathprep.dto.learning.DueReviewsResponse;
import com.swayam.pathprep.dto.learning.ReviewAnswerRequest;
import com.swayam.pathprep.dto.learning.ReviewAnswerResponse;
import com.swayam.pathprep.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/me/review")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping("/due")
    public ResponseEntity<DueReviewsResponse> getDueReviews(@RequestParam(required = false) Integer limit) {
        log.info("Received request for due review questions: limit={}", limit);
        DueReviewsResponse response = reviewService.getDueReviews(limit);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{reviewId}/answer")
    public ResponseEntity<ReviewAnswerResponse> answerReview(
            @PathVariable Long reviewId,
            @Valid @RequestBody ReviewAnswerRequest request
    ) {
        log.info("Received answer for review item: reviewId={}, selectedOptionId={}", reviewId, request.selectedOptionId());
        ReviewAnswerResponse response = reviewService.answerReview(reviewId, request);
        return ResponseEntity.ok(response);
    }
}
