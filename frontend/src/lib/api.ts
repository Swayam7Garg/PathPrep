import {
  TrackSummary,
  TopicInTrack,
  SessionCreateRequest,
  SessionResponse,
  AnswerRequest,
  AnswerResponse,
  SessionReport,
  TopicMastery,
  SkillGapItem,
  DailyPlan,
  ReviewQueueItem,
  AiUsageInfo,
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
} from "./mock-data";

const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:8080/api/v1";

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

async function request<T>(
  path: string,
  options: RequestInit = {},
  fallbackData?: T
): Promise<T> {
  const url = `${API_BASE_URL}${path}`;
  const headers = new Headers(options.headers || {});

  if (!headers.has("Content-Type") && options.body) {
    headers.set("Content-Type", "application/json");
  }
  if (!headers.has("Accept")) {
    headers.set("Accept", "application/json");
  }
  headers.set("X-Trace-Id", generateUUID());

  try {
    const response = await fetch(url, {
      ...options,
      headers,
    });

    if (!response.ok) {
      const errorText = await response.text();
      console.warn(`[API] HTTP ${response.status} from ${url}:`, errorText);
      if (fallbackData !== undefined) {
        return fallbackData;
      }
      throw new Error(`API Error ${response.status}: ${errorText}`);
    }

    return (await response.json()) as T;
  } catch (error) {
    console.warn(`[API] Request to ${url} failed; using fallback.`, error);
    if (fallbackData !== undefined) {
      return fallbackData;
    }
    throw error;
  }
}

// Phase 1: Track Catalog
export async function getTracks(): Promise<TrackSummary[]> {
  return request<TrackSummary[]>("/tracks", {}, MOCK_TRACKS);
}

// Phase 2: Topics in Track
export async function getTopicsForTrack(trackId: number | string): Promise<TopicInTrack[]> {
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

// Phase 4: Submit Answer (Idempotent)
export async function submitQuizAnswer(
  sessionId: number | string,
  selectedOptionId: number | string
): Promise<AnswerResponse> {
  const idempotencyKey = generateUUID();
  const payload: AnswerRequest = {
    selectedOptionId,
    idempotencyKey,
  };

  const headers: Record<string, string> = {
    "Idempotency-Key": idempotencyKey,
  };

  const mockAnswerResponse: AnswerResponse = {
    correct: Number(selectedOptionId) === 2,
    correctOptionId: 2,
    explanation:
      "In Java 8+, ConcurrentHashMap uses CAS on table array bins for lock-free initial insertion, and synchronizes exclusively on the head node when bucket hash collisions occur.",
    sessionStatus: "IN_PROGRESS",
    currentIndex: 3,
    totalQuestions: 8,
    nextQuestion: {
      id: 1025,
      type: "MCQ",
      topicId: 103,
      topicName: "Thread Pools & ExecutorService",
      difficulty: 2,
      body: "When tasks are submitted to a `ThreadPoolExecutor` with a `LinkedBlockingQueue` (unbounded capacity), which condition causes `maxPoolSize` threads to be created?",
      options: [
        { id: 1, position: 1, text: "Threads beyond corePoolSize are created as soon as all core threads are busy." },
        { id: 2, position: 2, text: "Threads beyond corePoolSize are never created because the queue never becomes full." },
        { id: 3, position: 3, text: "Threads beyond corePoolSize are spawned when CPU utilization crosses 80%." },
        { id: 4, position: 4, text: "The executor creates threads up to maxPoolSize upfront on initialization." },
      ],
    },
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

// Phase 5: Complete Session & Fetch Report
export async function completeSession(sessionId: number | string): Promise<{ status: string }> {
  return request<{ status: string }>(
    `/sessions/${sessionId}/complete`,
    { method: "POST" },
    { status: "COMPLETED" }
  );
}

export async function getSessionReport(sessionId: number | string): Promise<SessionReport> {
  return request<SessionReport>(
    `/sessions/${sessionId}/report`,
    {},
    MOCK_SESSION_REPORT
  );
}

// Phase 6: Skill Mastery, Gap Analysis, Daily Plan, Review Due
export async function getSkillMastery(): Promise<TopicMastery[]> {
  return request<TopicMastery[]>("/me/skills", {}, MOCK_SKILL_MASTERY);
}

export async function getSkillGaps(): Promise<SkillGapItem[]> {
  return request<SkillGapItem[]>("/me/skills/gaps", {}, MOCK_SKILL_GAPS);
}

export async function getDailyPlan(): Promise<DailyPlan> {
  return request<DailyPlan>("/me/plan/today", {}, MOCK_DAILY_PLAN);
}

export async function getReviewQueue(): Promise<ReviewQueueItem[]> {
  return request<ReviewQueueItem[]>("/me/review/due", {}, MOCK_REVIEW_QUEUE);
}

export async function getAiUsage(): Promise<AiUsageInfo> {
  return request<AiUsageInfo>("/me/usage", {}, MOCK_AI_USAGE);
}

export async function saveGroqApiKey(apiKey: string): Promise<{ success: boolean }> {
  return request<{ success: boolean }>(
    "/me/llm-key",
    {
      method: "POST",
      body: JSON.stringify({ apiKey }),
    },
    { success: true }
  );
}
