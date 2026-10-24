import {
  TrackSummary,
  TopicInTrack,
  SessionCreateRequest,
  SessionResponse,
  AnswerRequest,
  AnswerResponse,
  SessionDetailResponse,
  SessionCompleteResponse,
  SessionReportResponse,
  SessionListItem,
  PagedResponse,
  TodayPlanResponse,
  SkillProfileResponse,
  SkillGapsResponse,
  DueReviewsResponse,
  ReviewAnswerRequest,
  ReviewAnswerResponse,
  ProgressResponse,
  AiUsageInfo,
  AiUsageResponse,
  ApiErrorResponse,
  UserProfile,
  AuthResponse,
  UpdateUserRequest,
  DeleteUserRequest,
  InterviewSessionResponse,
  InterviewTurnAnswerRequest,
  LlmKeyTestResponse,
  EvaluationFeedbackRequest,
  UserDailyPlanResponse,
  DailyPlanItemResponse,
  CreateDailyPlanItemRequest,
} from "./types";
import {
  MOCK_TRACKS,
  MOCK_TOPICS,
  MOCK_DAILY_PLAN,
  MOCK_SKILL_MASTERY,
  MOCK_SKILL_GAPS,
  MOCK_REVIEW_QUEUE,
  MOCK_SESSION_RESPONSE,
  MOCK_SESSION_REPORT,
  MOCK_AI_USAGE,
  MOCK_PROGRESS,
  MOCK_USER_DAILY_PLAN,
} from "./mock-data";
import { getAccessToken, setAccessToken } from "./auth-token";

const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:8080/api/v1";

const USE_MOCKS = process.env.NEXT_PUBLIC_USE_MOCKS === "true";

export class ApiError extends Error {
  status: number;
  code: string;
  details?: string[];
  traceId?: string;
  meta?: Record<string, unknown>;

  constructor(
    status: number,
    message: string,
    code?: string,
    details?: string[],
    traceId?: string,
    meta?: Record<string, unknown>
  ) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.code = code || "UNKNOWN_ERROR";
    this.details = details;
    this.traceId = traceId;
    this.meta = meta;
  }
}

// Helper for generating client UUIDs
export function generateUUID(): string {
  if (typeof crypto !== "undefined" && crypto.randomUUID) {
    return crypto.randomUUID();
  }
  return "xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx".replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0;
    const v = c === "x" ? r : (r & 0x3) | 0x8;
    return v.toString(16);
  });
}

// In-flight refresh deduplication promise
let activeRefreshPromise: Promise<string | null> | null = null;

export async function getOrRefreshAccessToken(): Promise<string | null> {
  if (activeRefreshPromise) {
    return activeRefreshPromise;
  }
  activeRefreshPromise = (async () => {
    try {
      const res = await refreshToken();
      return res.accessToken;
    } catch {
      setAccessToken(null);
      return null;
    } finally {
      activeRefreshPromise = null;
    }
  })();
  return activeRefreshPromise;
}

interface RequestOptions extends RequestInit {
  _isRetry?: boolean;
}

async function request<T>(
  path: string,
  options: RequestOptions = {},
  fallbackData?: T
): Promise<T> {
  // If mocking is explicitly enabled, return mock data immediately
  if (USE_MOCKS && fallbackData !== undefined) {
    return fallbackData;
  }

  const url = `${API_BASE_URL}${path}`;
  const headers = new Headers(options.headers || {});

  if (!headers.has("Content-Type") && options.body) {
    headers.set("Content-Type", "application/json");
  }
  if (!headers.has("Accept")) {
    headers.set("Accept", "application/json");
  }
  if (!headers.has("X-Trace-Id")) {
    headers.set("X-Trace-Id", generateUUID());
  }

  // Attach in-memory access token if present
  const token = getAccessToken();
  if (token && !headers.has("Authorization")) {
    headers.set("Authorization", `Bearer ${token}`);
  }

  let response: Response;
  try {
    response = await fetch(url, {
      ...options,
      credentials: options.credentials || "include",
      headers,
    });
  } catch (err: unknown) {
    const errorMsg = err instanceof Error ? err.message : "Network error";
    // Mocks ONLY behind NEXT_PUBLIC_USE_MOCKS=true
    if (USE_MOCKS && fallbackData !== undefined) {
      return fallbackData;
    }
    throw new ApiError(0, errorMsg, "NETWORK_ERROR");
  }

  // Handle 401 with silent token refresh and single retry
  if (response.status === 401 && !options._isRetry && !path.startsWith("/auth/")) {
    const newToken = await getOrRefreshAccessToken();
    if (newToken) {
      const retryHeaders = new Headers(options.headers || {});
      retryHeaders.set("Authorization", `Bearer ${newToken}`);
      return request<T>(
        path,
        {
          ...options,
          headers: retryHeaders,
          _isRetry: true,
        },
        fallbackData
      );
    }
  }

  if (!response.ok) {
    let errorData: Partial<ApiErrorResponse> = {};
    let errorText = "";
    try {
      errorText = await response.text();
      errorData = JSON.parse(errorText);
    } catch {
      // response body was not JSON
    }

    if (USE_MOCKS && fallbackData !== undefined) {
      return fallbackData;
    }

    const message =
      errorData.message ||
      (errorText ? errorText : `Request failed with status ${response.status}`);

    throw new ApiError(
      response.status,
      message,
      errorData.code,
      errorData.details,
      errorData.traceId || headers.get("X-Trace-Id") || undefined,
      errorData.meta
    );
  }

  if (response.status === 204) {
    return {} as T;
  }

  return (await response.json()) as T;
}

// Phase 1: Track Catalog
export async function getTracks(): Promise<TrackSummary[]> {
  return request<TrackSummary[]>("/tracks", {}, MOCK_TRACKS);
}

// Phase 2: Topics in Track
export async function getTopicsForTrack(
  trackId: number | string
): Promise<TopicInTrack[]> {
  return request<TopicInTrack[]>(`/tracks/${trackId}/topics`, {}, MOCK_TOPICS);
}

// Phase 3: Create Practice Session
export async function createPracticeSession(
  payload: SessionCreateRequest
): Promise<SessionResponse> {
  return request<SessionResponse>(
    "/sessions",
    {
      method: "POST",
      body: JSON.stringify(payload),
    },
    {
      ...MOCK_SESSION_RESPONSE,
      id: Math.floor(Math.random() * 900) + 100,
      mode: payload.mode,
    }
  );
}

export const createSession = createPracticeSession;

// Phase 4: Submit Answer (Idempotent)
export async function submitQuizAnswer(
  sessionId: number | string,
  selectedOptionId: number,
  idempotencyKey?: string,
  timeSpentSec?: number
): Promise<AnswerResponse> {
  const key = idempotencyKey || generateUUID();
  const payload: AnswerRequest = {
    selectedOptionId,
    idempotencyKey: key,
    timeSpentSec,
  };

  const headers: Record<string, string> = {
    "Idempotency-Key": key,
  };

  const mockAnswerResponse: AnswerResponse = {
    correct: Number(selectedOptionId) === 2,
    correctOptionId: 2,
    explanation:
      "In Java 8+, ConcurrentHashMap uses CAS on table array bins for lock-free initial insertion, and synchronizes exclusively on the head node when bucket hash collisions occur.",
    sessionStatus: "IN_PROGRESS",
    currentIndex: 3,
    totalQuestions: 8,
    nextQuestion: null,
  };

  return request<AnswerResponse>(
    `/sessions/${sessionId}/answers`,
    {
      method: "POST",
      headers,
      body: JSON.stringify(payload),
    },
    mockAnswerResponse
  );
}

// Phase 5: Session Detail, Complete, Report & History
export async function getSession(
  sessionId: number | string
): Promise<SessionDetailResponse> {
  return request<SessionDetailResponse>(`/sessions/${sessionId}`);
}

export async function completeSession(
  sessionId: number | string
): Promise<SessionCompleteResponse> {
  return request<SessionCompleteResponse>(`/sessions/${sessionId}/complete`, {
    method: "POST",
  });
}

export async function getSessionReport(
  sessionId: number | string
): Promise<SessionReportResponse> {
  return request<SessionReportResponse>(
    `/sessions/${sessionId}/report`,
    {},
    MOCK_SESSION_REPORT
  );
}

export async function getSessionHistory(params?: {
  page?: number;
  size?: number;
  status?: string;
  mode?: string;
  trackId?: number;
}): Promise<PagedResponse<SessionListItem>> {
  const searchParams = new URLSearchParams();
  if (params?.page !== undefined) searchParams.set("page", params.page.toString());
  if (params?.size !== undefined) searchParams.set("size", params.size.toString());
  if (params?.status) searchParams.set("status", params.status);
  if (params?.mode) searchParams.set("mode", params.mode);
  if (params?.trackId !== undefined) searchParams.set("trackId", params.trackId.toString());

  const query = searchParams.toString() ? `?${searchParams.toString()}` : "";
  return request<PagedResponse<SessionListItem>>(`/sessions${query}`, {}, {
    items: [],
    page: 0,
    size: params?.size ?? 5,
    total: 0,
    totalPages: 0,
  });
}

// Phase 6: Learning Loop (Algorithmic Plan, Skills, Gaps, Review, Progress)
export async function getTodayPlan(): Promise<TodayPlanResponse> {
  return request<TodayPlanResponse>("/me/plan/today", {}, MOCK_DAILY_PLAN);
}

// User-Defined Daily Plan & Interactive Checklist
export async function getUserDailyPlan(): Promise<UserDailyPlanResponse> {
  return request<UserDailyPlanResponse>("/me/plan", {}, MOCK_USER_DAILY_PLAN);
}

export async function addDailyPlanItem(
  payload: CreateDailyPlanItemRequest
): Promise<DailyPlanItemResponse> {
  return request<DailyPlanItemResponse>("/me/plan/items", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export async function toggleDailyPlanItem(
  id: number
): Promise<DailyPlanItemResponse> {
  return request<DailyPlanItemResponse>(`/me/plan/items/${id}/toggle`, {
    method: "PATCH",
  });
}

export async function deleteDailyPlanItem(id: number): Promise<void> {
  return request<void>(`/me/plan/items/${id}`, {
    method: "DELETE",
  });
}

export async function rolloverYesterdayDailyPlan(): Promise<UserDailyPlanResponse> {
  return request<UserDailyPlanResponse>("/me/plan/rollover", {
    method: "POST",
  });
}

export async function getSkills(
  trackId?: number | null
): Promise<SkillProfileResponse> {
  const query = trackId ? `?trackId=${trackId}` : "";
  return request<SkillProfileResponse>(`/me/skills${query}`, {}, {
    trackId: trackId ?? 1,
    trackName: "Java Backend Engineer",
    topics: MOCK_SKILL_MASTERY,
  });
}

export async function getSkillGaps(
  trackId?: number | null,
  limit?: number
): Promise<SkillGapsResponse> {
  const params = new URLSearchParams();
  if (trackId) params.set("trackId", trackId.toString());
  if (limit) params.set("limit", limit.toString());
  const query = params.toString() ? `?${params.toString()}` : "";
  return request<SkillGapsResponse>(`/me/skills/gaps${query}`, {}, {
    trackId: trackId ?? 1,
    gaps: MOCK_SKILL_GAPS,
  });
}

export async function getDueReviews(
  limit?: number
): Promise<DueReviewsResponse> {
  const query = limit ? `?limit=${limit}` : "";
  return request<DueReviewsResponse>(`/me/review/due${query}`, {}, {
    dueCount: MOCK_REVIEW_QUEUE.length,
    items: MOCK_REVIEW_QUEUE,
  });
}

export async function submitReviewAnswer(
  reviewId: number,
  payload: ReviewAnswerRequest
): Promise<ReviewAnswerResponse> {
  return request<ReviewAnswerResponse>(
    `/me/review/${reviewId}/answer`,
    {
      method: "POST",
      body: JSON.stringify(payload),
    },
    {
      reviewId,
      correct: payload.selectedOptionId === 2,
      selectedOptionId: payload.selectedOptionId,
      correctOptionId: 2,
      explanation: "CAS bucket insertion provides lock-free put.",
      selectedOptionExplanation: "Option analysis benchmark explanation.",
      schedule: {
        step: 2,
        intervalDays: 3,
        nextDueOn: "2026-10-20",
        graduated: false,
      },
      dueRemaining: 0,
    }
  );
}

export async function getProgress(
  range?: number
): Promise<ProgressResponse> {
  const query = range ? `?range=${range}` : "";
  return request<ProgressResponse>(`/me/progress${query}`, {}, MOCK_PROGRESS);
}

// Phase 8: AI Stream & Usage Meter
export async function getAiUsage(): Promise<AiUsageResponse> {
  return request<AiUsageResponse>("/me/usage", {}, MOCK_AI_USAGE);
}

export async function saveGroqApiKey(apiKey: string): Promise<{ message?: string }> {
  return request<{ message?: string }>(
    "/me/llm-key",
    {
      method: "PUT",
      body: JSON.stringify({ apiKey, provider: "GROQ" }),
    },
    { message: "API key saved successfully" }
  );
}

export async function testGroqApiKey(apiKey?: string): Promise<LlmKeyTestResponse> {
  return request<LlmKeyTestResponse>(
    "/me/llm-key/test",
    {
      method: "POST",
      body: JSON.stringify({ apiKey, provider: "GROQ" }),
    },
    { valid: true, latencyMs: 115, message: "Connection to Groq API successfully verified" }
  );
}

export async function removeGroqApiKey(): Promise<{ message?: string }> {
  return request<{ message?: string }>(
    "/me/llm-key",
    {
      method: "DELETE",
    },
    { message: "API key removed successfully" }
  );
}

export async function getInterviewSession(
  sessionId: number | string
): Promise<InterviewSessionResponse> {
  return request<InterviewSessionResponse>(`/sessions/${sessionId}/interview`);
}

export async function submitInterviewTurn(
  sessionId: number | string,
  payload: InterviewTurnAnswerRequest,
  idempotencyKey?: string
): Promise<AnswerResponse> {
  const key = idempotencyKey || generateUUID();
  return request<AnswerResponse>(
    `/sessions/${sessionId}/answers`,
    {
      method: "POST",
      headers: {
        "Idempotency-Key": key,
      },
      body: JSON.stringify({
        turnId: payload.turnId,
        answerText: payload.answerText,
        timeSpentSec: payload.timeSpentSec,
        idempotencyKey: key,
      }),
    }
  );
}

export async function retryEvaluations(
  sessionId: number | string
): Promise<{ status: string; message?: string }> {
  return request<{ status: string; message?: string }>(
    `/sessions/${sessionId}/evaluations/retry`,
    {
      method: "POST",
    }
  );
}

export async function submitEvaluationFeedback(
  evaluationId: number | string,
  payload: EvaluationFeedbackRequest
): Promise<{ success: boolean; message?: string }> {
  return request<{ success: boolean; message?: string }>(
    `/evaluations/${evaluationId}/feedback`,
    {
      method: "POST",
      body: JSON.stringify(payload),
    }
  );
}

// Phase 7: Authentication & User Profile
export async function loginWithGoogle(credential: string): Promise<AuthResponse> {
  const res = await request<AuthResponse>("/auth/google", {
    method: "POST",
    headers: {
      "X-Requested-With": "pathprep",
    },
    body: JSON.stringify({ credential }),
  });
  setAccessToken(res.accessToken);
  return res;
}

export async function refreshToken(): Promise<AuthResponse> {
  const res = await request<AuthResponse>("/auth/refresh", {
    method: "POST",
    headers: {
      "X-Requested-With": "pathprep",
    },
  });
  setAccessToken(res.accessToken);
  return res;
}

export async function logout(): Promise<void> {
  try {
    await request<void>("/auth/logout", {
      method: "POST",
      headers: {
        "X-Requested-With": "pathprep",
      },
    });
  } finally {
    setAccessToken(null);
  }
}

export async function logoutAll(): Promise<void> {
  try {
    await request<void>("/auth/logout-all", {
      method: "POST",
      headers: {
        "X-Requested-With": "pathprep",
      },
    });
  } finally {
    setAccessToken(null);
  }
}

export async function devLogin(email?: string): Promise<AuthResponse> {
  const res = await request<AuthResponse>("/auth/dev-login", {
    method: "POST",
    headers: {
      "X-Requested-With": "pathprep",
    },
    body: email ? JSON.stringify({ email }) : undefined,
  });
  setAccessToken(res.accessToken);
  return res;
}

export async function getMe(): Promise<UserProfile> {
  return request<UserProfile>("/me");
}

export async function updateMe(payload: UpdateUserRequest): Promise<UserProfile> {
  return request<UserProfile>("/me", {
    method: "PATCH",
    body: JSON.stringify(payload),
  });
}

export async function deleteMe(): Promise<void> {
  try {
    const payload: DeleteUserRequest = { confirm: "DELETE" };
    await request<void>("/me", {
      method: "DELETE",
      body: JSON.stringify(payload),
    });
  } finally {
    setAccessToken(null);
  }
}

