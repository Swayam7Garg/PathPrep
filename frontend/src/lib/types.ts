// Domain Models and DTOs strictly aligned with Spring Boot Phase 1 - 6 backend

// Enums matching backend
export type SessionMode = "QUIZ" | "INTERVIEW";
export type SessionStyle = "LITE" | "LIVE";
export type SessionStatus = "IN_PROGRESS" | "COMPLETED" | "ABANDONED" | "FAILED";
export type QuizDifficulty = "EASY" | "MEDIUM" | "HARD" | "MIXED";
export type CompletionReason = "ALL_QUESTIONS_ANSWERED" | "MANUAL_SUBMISSION" | "ABANDONED";
export type QuestionType = "MCQ" | "CONCEPT" | "SCENARIO" | "BEHAVIOURAL";
export type SkillStatus = "NOT_STARTED" | "IN_PROGRESS" | "MASTERED";
export type ConfidenceLevel = "LOW" | "MEDIUM" | "HIGH";
export type GapKind = "NOT_STARTED" | "WEAK" | "PREREQUISITE_BLOCKED";
export type PlanItemType = "RESUME" | "REVIEW" | "DRILL";
export type UserRole = "STUDENT" | "OFFICER" | "ADMIN";

// Phase 7: Authentication & User Profile
export interface InstitutionSummary {
  id: number;
  name: string;
}

export interface UserProfile {
  id: number;
  email: string;
  name: string | null;
  pictureUrl: string | null;
  role: UserRole;
  branch: string | null;
  gradYear: number | null;
  targetTrackId: number | null;
  targetTrackName: string | null;
  institution: InstitutionSummary | null;
  onboardingCompleted: boolean;
  createdAt: string;
}

export interface AuthResponse {
  accessToken: string;
  tokenType: string;
  expiresInSec: number;
  user: UserProfile;
}

export interface GoogleLoginRequest {
  credential: string;
}

export interface UpdateUserRequest {
  name?: string;
  branch?: string;
  gradYear?: number;
  targetTrackId?: number;
}

export interface DeleteUserRequest {
  confirm: "DELETE";
}

// Phase 1: Track Catalog
export interface TrackSummary {
  id: number;
  name: string;
  description: string;
  topicCount: number;
  questionCount: number;
}

// Phase 2: Topics in Track
export interface TopicInTrack {
  topicId: number;
  name: string;
  domain: string;
  weight: number;
  questionCount: number;
  prerequisiteIds?: number[];
}

// Phase 3 & 4: Sessions & Questions
export interface SessionCreateRequest {
  trackId: number;
  mode: SessionMode;
  style?: SessionStyle | null;
  topicIds?: number[];
  questionCount?: number;
  difficulty?: QuizDifficulty;
}

export interface OptionView {
  id: number;
  position: number;
  text: string;
}

export interface QuestionView {
  id: number;
  type: QuestionType;
  topicId: number;
  topicName: string;
  difficulty: number;
  body: string;
  options: OptionView[];
}

export interface SessionResponse {
  id: number;
  mode: SessionMode;
  style: SessionStyle | null;
  status: SessionStatus;
  totalQuestions: number;
  currentIndex: number;
  currentQuestion: QuestionView;
  warnings?: string[];
}

export interface AnswerRequest {
  selectedOptionId: number;
  idempotencyKey: string;
  timeSpentSec?: number;
}

export interface AnswerResponse {
  correct: boolean;
  correctOptionId: number;
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

// Phase 5: Session Detail, Completion, Report & History
export interface SessionDetailProgress {
  answeredCount: number;
  correctCount: number;
  totalQuestions: number;
  completed: boolean;
}

export interface SessionDetailResponse {
  id: number;
  mode: SessionMode;
  style: SessionStyle | null;
  trackId: number;
  trackName: string;
  difficulty: QuizDifficulty;
  status: SessionStatus;
  completionReason: CompletionReason | null;
  totalQuestions: number;
  currentIndex: number;
  startedAt: string;
  completedAt: string | null;
  currentQuestionStartedAt: string | null;
  currentQuestion: QuestionView | null;
  progress: SessionDetailProgress;
  resumable: boolean;
  reportAvailable: boolean;
}

export interface SessionCompleteResponse {
  sessionId: number;
  status: SessionStatus;
  completionReason: CompletionReason;
  totalQuestions: number;
  answeredCount: number;
  correctCount: number;
  unansweredCount: number;
  scorePercent: number;
  elapsedSec: number;
  activeTimeSec: number;
  reportAvailable: boolean;
}

export interface ReportSummary {
  totalQuestions: number;
  answeredCount: number;
  correctCount: number;
  unansweredCount: number;
  scorePercent: number;
  accuracyOfAnsweredPercent: number;
}

export interface ReportTopicBreakdown {
  topicId: number;
  topicName: string;
  questionsInTopic: number;
  answered: number;
  correct: number;
  accuracyPercent: number;
  label: string;
}

export interface ReportQuestionDetail {
  position: number;
  questionId: number;
  topicId: number;
  topicName: string;
  body: string;
  answered: boolean;
  correct: boolean | null;
  selectedOptionId: number | null;
  correctOptionId: number | null;
  explanation: string | null;
  selectedOptionExplanation: string | null;
  timeSpentSec: number | null;
  options: OptionView[];
}

export interface ReportRecommendation {
  type: string;
  topicId: number;
  topicName: string;
  reason: string;
  suggestedQuestionCount: number;
}

export interface SessionReportResponse {
  sessionId: number;
  trackId: number;
  trackName: string;
  difficulty: QuizDifficulty;
  status: SessionStatus;
  completionReason: CompletionReason;
  startedAt: string;
  completedAt: string;
  elapsedSec: number;
  activeTimeSec: number;
  summary: ReportSummary;
  previousScorePercent: number | null;
  topics: ReportTopicBreakdown[];
  questions: ReportQuestionDetail[];
  recommendations: ReportRecommendation[];
}

// Backward compatibility alias
export type SessionReport = SessionReportResponse;

export interface SessionListItem {
  id: number;
  mode: SessionMode;
  status: SessionStatus;
  trackId: number;
  trackName: string;
  difficulty: QuizDifficulty;
  totalQuestions: number;
  answeredCount: number;
  correctCount: number;
  scorePercent: number | null;
  startedAt: string;
  completedAt: string | null;
  resumable: boolean;
}

export interface PagedResponse<T> {
  items: T[];
  page: number;
  size: number;
  total: number;
  totalPages: number;
}

// Phase 6: Learning Loop (Plan, Skills, Gaps, Review, Progress)
export interface StreakView {
  current: number;
  longest: number;
  activeToday: boolean;
  atRisk: boolean;
}

export interface ReadinessView {
  percent: number | null;
  reason: string;
  basedOnQuestions: number;
  minAnswersRequired: number;
}

export interface PlanItem {
  type: PlanItemType;
  title: string;
  detail: string;
  estimatedMinutes: number;
  topicId: number | null;
  sessionId: number | null;
  questionCount: number | null;
}

export interface RecommendedFocus {
  topicId: number;
  topicName: string;
  kind: GapKind;
  reason: string;
}

export interface TodayPlanResponse {
  date: string;
  timeZone: string;
  streak: StreakView;
  dailyGoal: number;
  todayQuestionsAnswered: number;
  readiness: ReadinessView;
  reviewDueCount: number;
  activeSessionId: number | null;
  recommendedFocus: RecommendedFocus | null;
  items: PlanItem[];
}

// Backward compatibility alias
export type DailyPlan = TodayPlanResponse;

export interface UnmetPrerequisiteView {
  topicId: number;
  name: string;
  masteryPercent: number | null;
}

export interface TopicSkillView {
  topicId: number;
  name: string;
  domain: string;
  weight: number;
  questionCount: number;
  masteryPercent: number | null;
  attemptsCount: number;
  correctCount: number;
  confidence: ConfidenceLevel;
  status: SkillStatus;
  prerequisitesMet: boolean;
  unmetPrerequisites: UnmetPrerequisiteView[];
  lastPracticedAt: string | null;
}

export interface SkillProfileResponse {
  trackId: number | null;
  trackName: string | null;
  topics: TopicSkillView[];
}

// Backward compatibility alias
export type TopicMastery = TopicSkillView;

export interface SuggestedAction {
  type: string;
  topicId: number;
  questionCount: number;
}

export interface SkillGapView {
  topicId: number;
  topicName: string;
  kind: GapKind;
  masteryPercent: number | null;
  weight: number;
  impactScore: number;
  reason: string;
  suggestedAction: SuggestedAction;
}

export interface SkillGapsResponse {
  trackId: number | null;
  gaps: SkillGapView[];
}

// Backward compatibility alias
export type SkillGapItem = SkillGapView;

export interface DueReviewItem {
  reviewId: number;
  step: number;
  intervalDays: number;
  lastMissedAt: string;
  question: QuestionView;
}

export interface DueReviewsResponse {
  dueCount: number;
  items: DueReviewItem[];
}

export interface ReviewAnswerRequest {
  selectedOptionId: number;
  timeSpentSec: number;
}

export interface ReviewScheduleView {
  step: number;
  intervalDays: number | null;
  nextDueOn: string | null;
  graduated: boolean;
}

export interface ReviewAnswerResponse {
  reviewId: number;
  correct: boolean;
  selectedOptionId: number;
  correctOptionId: number;
  explanation: string;
  selectedOptionExplanation: string;
  schedule: ReviewScheduleView;
  dueRemaining: number;
}

// Progress
export interface DailyProgressView {
  date: string;
  questionsAnswered: number;
  correctAnswers: number;
  reviewsAnswered: number;
  sessionsCompleted: number;
  activeMinutes: number;
}

export interface ProgressTotals {
  sessionsCompleted: number;
  questionsAnswered: number;
  correctAnswers: number;
  accuracyPercent: number;
  activeMinutes: number;
}

export interface RecentSessionView {
  sessionId: number;
  trackName: string;
  scorePercent: number;
  completedAt: string;
}

export interface ProgressResponse {
  range: number;
  totals: ProgressTotals;
  streak: StreakView;
  days: DailyProgressView[];
  recentSessions: RecentSessionView[];
}

// Placeholders for Phase 8 / Settings
export interface AiUsageInfo {
  dailyQuota: number;
  usedToday: number;
  resetAt: string;
  hasCustomKey: boolean;
}

// Standard API Error
export interface ApiErrorResponse {
  code: string;
  message: string;
  details?: string[];
  retryAfterSeconds?: number | null;
  traceId?: string;
  meta?: Record<string, unknown>;
}
