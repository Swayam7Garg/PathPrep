// Domain Models and DTOs matching Spring Boot backend & frontend.md

export interface TrackSummary {
  id: number | string;
  name: string;
  description: string;
  topicCount: number;
  questionCount: number;
}

export interface TopicInTrack {
  topicId: number | string;
  name: string;
  domain: string;
  weight: number;
  questionCount: number;
  prerequisiteIds?: (number | string)[];
}

export type SessionMode = "QUIZ" | "INTERVIEW";
export type SessionStyle = "LITE" | "LIVE";
export type SessionStatus = "IN_PROGRESS" | "EVALUATING" | "COMPLETED" | "FAILED";
export type QuizDifficulty = "EASY" | "MEDIUM" | "HARD" | "MIXED";

export interface SessionCreateRequest {
  trackId: number | string;
  mode: SessionMode;
  style?: SessionStyle | null;
  topicIds?: (number | string)[];
  questionCount?: number;
  difficulty?: QuizDifficulty;
}

export interface OptionView {
  id: number | string;
  position: number;
  text: string;
}

export interface QuestionView {
  id: number | string;
  type: "MCQ" | "CONCEPT" | "SCENARIO" | "BEHAVIOURAL";
  topicId: number | string;
  topicName: string;
  difficulty: number;
  body: string;
  options: OptionView[];
}

export interface SessionResponse {
  id: number | string;
  mode: SessionMode;
  style: SessionStyle | null;
  status: SessionStatus;
  totalQuestions: number;
  currentIndex: number;
  currentQuestion: QuestionView;
  warnings?: string[];
}

export interface AnswerRequest {
  selectedOptionId: number | string;
  idempotencyKey: string;
  timeSpentSec?: number;
}

export interface AnswerResponse {
  correct: boolean;
  correctOptionId: number | string;
  explanation: string;
  sessionStatus?: SessionStatus;
  currentIndex?: number;
  totalQuestions?: number;
  nextQuestion: QuestionView | null;
  progress?: {
    answeredCount: number;
    totalCount: number;
    currentScore: number;
  };
}

export interface CriterionScore {
  criterionId: string;
  name: string;
  score: number; // 1 to 5
  evidenceQuote: string;
}

export interface SessionReport {
  sessionId: number | string;
  trackName: string;
  mode: SessionMode;
  overallScore: number;
  criteriaScores: CriterionScore[];
  missedKeyPoints: string[];
  modelAnswer: string;
  recommendedTopics: Array<{
    topicId: number | string;
    topicName: string;
    action: "REVIEW" | "PRACTICE";
  }>;
}

export interface TopicMastery {
  topicId: number | string;
  topicName: string;
  domain: string;
  masteryPercentage: number;
  confidenceLevel: "LOW" | "MEDIUM" | "HIGH";
  questionsAttempted: number;
  lastPracticedAt: string;
}

export interface SkillGapItem {
  topicId: number | string;
  topicName: string;
  gapWeight: number;
  missingPrerequisites: string[];
  recommendedDrillCount: number;
}

export interface DailyPlan {
  date: string;
  currentStreakDays: number;
  targetMinutes: number;
  completedMinutes: number;
  recommendedAction: {
    type: "DRILL" | "INTERVIEW" | "REVIEW";
    title: string;
    trackId: number | string;
    topicId?: number | string;
  };
}

export interface ReviewQueueItem {
  attemptId: string;
  questionId: number | string;
  questionBody: string;
  topicName: string;
  lastAttemptedAt: string;
  mistakeCount: number;
  intervalDays: number;
}

export interface AiUsageInfo {
  dailyQuota: number;
  usedToday: number;
  resetAt: string;
  hasCustomKey: boolean;
}

export interface ApiErrorResponse {
  code: string;
  message: string;
  details?: string[];
  retryAfterSeconds?: number | null;
  traceId?: string;
}
