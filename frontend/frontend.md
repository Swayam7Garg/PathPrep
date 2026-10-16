# PrepPath — Frontend Engineering & Backend Integration Specification

> **Target Audience**: Frontend Engineers, Full-Stack Developers, and AI Agents building the PrepPath Next.js client application.  
> **Source of Truth**: `PROJECT_CONTEXT.md` and Backend Spring Boot 4 / MySQL 8 schema.  
> **Location**: `frontend/frontend.md`

---

## 1. Architectural Overview & Frontend Principles

PrepPath is a focused interview-preparation platform tailored for college students in India preparing for internships and campus placements. The frontend is built as a separate single-page application (SPA) using **Next.js (App Router)**, **TypeScript**, **Tailwind CSS**, and **shadcn/ui**.

### Key System Rules Frontend Must Uphold
1. **Zero-AI Cost Rule**: AI is an expensive last-step utility, never the default. The application is completely functional without AI. Practice Quizzes, MCQ scoring, Skill Maps, and Progress tracking operate with 0% LLM dependency.
2. **Never Leak Answers Before Submission**: Question data returned during an active session (`QuestionView`) never includes `isCorrect`, `correctOptionId`, or `explanation`. These are only delivered in the response payload *after* the candidate submits an answer.
3. **Strict Idempotency on Answer Submissions**: Every answer submission must include a client-generated UUID in the `Idempotency-Key` header to prevent double-grading and corrupted streaks caused by network retries or double-clicks.
4. **Resilient Offline/Degraded Modes**: If the backend AI service returns `502` or `429` (`AI_QUOTA_EXCEEDED`), the frontend must gracefully display explanatory banners and offer non-AI quiz alternatives without crashing or breaking active sessions.

---

## 2. Technology Stack & Client Infrastructure

| Layer | Technology Choice | Responsibility |
|---|---|---|
| **Framework** | Next.js 15+ (App Router) | Client & server rendering, nested layout routing, search params handling |
| **Language** | TypeScript (Strict mode) | Type safety across all domain models, DTOs, and API contracts |
| **Styling** | Tailwind CSS v3/v4 | Design token implementation (`design.md` tokens), zero hardcoded colors |
| **Component Kit** | shadcn/ui + Radix UI primitives | Accessible base components (Dialog, Dropdown, Tabs, Tooltip, Progress) |
| **Server State / Cache** | TanStack Query v5 (`@tanstack/react-query`) | Query deduplication, polling, optimistic updates, cache invalidation |
| **Client Form / Schema** | React Hook Form + Zod | Schema validation, typing for form submissions and query parameters |
| **API Client** | Typed OpenAPI Client (`openapi-fetch` / generated) | Automatic synchronization with backend Springdoc OpenAPI endpoints |
| **Data Telemetry & Charts** | Recharts | Readiness trajectory curves, mastery radar, category breakdown bars |
| **Icons** | Lucide React | Thin line icons strictly set to 1.5px stroke width (`stroke-width="1.5"`) |

---

## 3. Communication Protocols & Base Contracts

### 3.1 Network & Base URLs
* **Local Development Base URL**: `http://localhost:8080/api/v1`
* **Production Base URL**: Set via `NEXT_PUBLIC_API_BASE_URL` environment variable.
* **Headers Required**:
  * `Content-Type: application/json`
  * `Accept: application/json`
  * `Authorization: Bearer <JWT>` (on all protected endpoints)
  * `X-Trace-Id: <UUID>` (generated client-side per session/request for debugging telemetry)
  * `Idempotency-Key: <UUID>` (strictly required for `POST /sessions/{id}/answers`)

### 3.2 Standard Error Response Shape
The Spring Boot backend enforces a single consistent JSON error envelope for all failures:

```typescript
export interface ApiErrorResponse {
  code: string;               // e.g. "TRACK_NOT_FOUND", "AI_QUOTA_EXCEEDED", "VALIDATION_FAILED"
  message: string;            // Human-readable summary
  details?: string[];         // Detailed validation field errors or constraint messages
  retryAfterSeconds?: number | null; // Set for rate limits or cooling-down windows
  traceId?: string;           // Distributed trace ID logged on server
}
```

#### Common Error Code Mapping Table
| HTTP Status | Error `code` | Frontend Behavior / UI State |
|---|---|---|
| `400` | `VALIDATION_FAILED` | Highlight invalid form fields using Zod/React Hook Form error labels. |
| `400` | `TOPIC_NOT_IN_TRACK` | Reset topic selector in practice lobby and notify candidate. |
| `400` | `MODE_NOT_AVAILABLE` | Display badge indicating feature (e.g., Live Interview) is locked. |
| `401` | `UNAUTHORIZED` | Clear local session token and redirect user to `/login`. |
| `404` | `TRACK_NOT_FOUND` / `SESSION_NOT_FOUND` | Render not-found state card with button to return to `/dashboard`. |
| `409` | `SESSION_ALREADY_COMPLETED` | Disable option selection, route user directly to `/sessions/{id}/report`. |
| `422` | `NOT_ENOUGH_QUESTIONS` | Show warning pill: "Not enough questions in selected topic(s); select more topics." |
| `429` | `AI_QUOTA_EXCEEDED` | Display quota modal: explain daily limit reached, option to enter Groq key. |
| `502` | `LLM_PROVIDER_ERROR` | Show retry button; fallback to Lite evaluation or code-only quiz practice. |

---

## 4. Endpoints & Data Contracts (Phased Breakdown)

### Phase 1: Track Catalog (`GET /api/v1/tracks`)
Used in: **Practice Lobby**, **Onboarding**, **Dashboard Track Pill**.

* **Endpoint**: `GET /api/v1/tracks`
* **Auth**: Public or Bearer JWT
* **Response**: `200 OK`
```typescript
export interface TrackSummary {
  id: string;             // RoleTrack ID (numeric string or UUID)
  name: string;           // e.g., "Java Backend Engineer", "Web Frontend Engineer"
  description: string;    // Short curriculum description
  topicCount: number;     // Number of topics mapped to this track
  questionCount: number;  // Total published questions in this track
}
```

---

### Phase 2: Topics in Track (`GET /api/v1/tracks/{trackId}/topics`)
Used in: **Topic Filter Grid**, **Skill Map Prerequisites**, **Practice Customization**.

* **Endpoint**: `GET /api/v1/tracks/{trackId}/topics`
* **URL Param**: `trackId`
* **Response**: `200 OK` (Sorted by weight descending, then name)
```typescript
export interface TopicInTrack {
  topicId: string;
  name: string;
  domain: string;           // e.g. "CONCURRENCY", "DATABASE", "FRAMEWORK"
  weight: number;           // Track topic importance weight (e.g. 1.50)
  questionCount: number;    // Published questions count for topic
  prerequisiteIds: string[]; // Topic IDs that must be learned before this topic
}
```

---

### Phase 3: Create Practice Session (`POST /api/v1/sessions`)
Used in: **Practice Lobby ("Start Session" Button)**.

* **Endpoint**: `POST /api/v1/sessions`
* **Request Body**:
```typescript
export interface SessionCreateRequest {
  trackId: string;
  mode: "QUIZ" | "INTERVIEW";     // INTERVIEW requires Phase 8 backend completion
  style?: "LITE" | "LIVE";        // Nullable for QUIZ; LITE = batch grading, LIVE = adaptive
  topicIds?: string[];            // Empty/omitted means all topics in track
  questionCount?: number;         // 1 to 20 (defaults to 8)
  difficulty?: "EASY" | "MEDIUM" | "HARD" | "MIXED"; // Default "MIXED"
}
```
* **Response**: `201 Created` with `Location: /api/v1/sessions/{sessionId}`
```typescript
export interface OptionView {
  id: string;
  position: number;   // 1, 2, 3, 4
  text: string;
}

export interface QuestionView {
  id: string;
  type: "MCQ" | "CONCEPT" | "SCENARIO" | "BEHAVIOURAL";
  topicId: string;
  topicName: string;
  difficulty: number; // 1, 2, 3
  body: string;       // Markdown supported question text
  options: OptionView[]; // Note: isCorrect & explanation are strictly stripped here!
}

export interface SessionResponse {
  id: string;
  mode: "QUIZ" | "INTERVIEW";
  style: "LITE" | "LIVE" | null;
  status: "IN_PROGRESS" | "EVALUATING" | "COMPLETED" | "FAILED";
  totalQuestions: number;
  currentIndex: number;          // 0-indexed position
  currentQuestion: QuestionView;
  warnings?: string[];           // e.g. ["Selected 10 questions, but only 8 published found"]
}
```

---

### Phase 4: Submit Answer (`POST /api/v1/sessions/{id}/answers`)
Used in: **Active Quiz Interface**, **MCQ Option Selection**.

* **Endpoint**: `POST /api/v1/sessions/{id}/answers`
* **Headers**: `Idempotency-Key: <client-generated-UUID>`
* **Request Body**:
```typescript
export interface AnswerRequest {
  questionId: string;
  selectedOptionId?: string; // Required for MCQ
  answerText?: string;       // Required for Concept / Interview questions
  timeSpentSec?: number;     // Time spent on this specific question
}
```
* **Response**: `200 OK`
```typescript
export interface AnswerResponse {
  correct: boolean;                 // Graded immediately in code for MCQ (Zero AI)
  correctOptionId: string;          // Revealed to student only after answering
  explanation: string;              // Model explanation and rationale
  nextQuestion: QuestionView | null;// Null if this was the last question in session
  progress: {
    answeredCount: number;
    totalCount: number;
    currentScore: number;
  };
}
```

---

### Phase 5: Complete Session & Report (`POST /sessions/{id}/complete` & `GET /sessions/{id}/report`)
Used in: **Final Report Screen**, **Exit Modal**.

* **Endpoint**: `POST /api/v1/sessions/{id}/complete`
* **Response**: `200 OK`
```typescript
export interface SessionSummaryResponse {
  sessionId: string;
  totalQuestions: number;
  correctCount: number;
  accuracyPercentage: number;
  timeSpentSeconds: number;
  status: "COMPLETED";
}
```

* **Endpoint**: `GET /api/v1/sessions/{id}/report`
* **Response**: `200 OK`
```typescript
export interface CriterionScore {
  criterionId: string;
  name: string;             // e.g. "Technical Depth", "Structure (STAR)", "Edge Case Analysis"
  score: number;            // 1 to 5 scale
  evidenceQuote: string;    // Quoted text from student's answer
}

export interface SessionReport {
  sessionId: string;
  trackName: string;
  mode: "QUIZ" | "INTERVIEW";
  overallScore: number;     // 0 to 100
  criteriaScores: CriterionScore[];
  missedKeyPoints: string[];
  modelAnswer: string;
  recommendedTopics: Array<{
    topicId: string;
    topicName: string;
    action: "REVIEW" | "PRACTICE";
  }>;
}
```

---

### Phase 6: Skill Mastery, Gap Analysis & Today's Plan
Used in: **Dashboard**, **Skills Page**, **Review Due Queue**.

#### 1. Skill Mastery (`GET /api/v1/me/skills`)
```typescript
export interface TopicMastery {
  topicId: string;
  topicName: string;
  domain: string;
  masteryPercentage: number; // 0 to 100
  confidenceLevel: "LOW" | "MEDIUM" | "HIGH";
  questionsAttempted: number;
  lastPracticedAt: string;
}
```

#### 2. Skill Gaps (`GET /api/v1/me/skills/gaps`)
Returns unmastered topics weighted by target track importance.
```typescript
export interface SkillGapItem {
  topicId: string;
  topicName: string;
  gapWeight: number;         // Weighted priority score
  missingPrerequisites: string[];
  recommendedDrillCount: number;
}
```

#### 3. Today's Plan (`GET /api/v1/me/plan/today`)
```typescript
export interface DailyPlan {
  date: string;              // ISO date "2026-10-13"
  currentStreakDays: number;
  targetMinutes: number;
  completedMinutes: number;
  recommendedAction: {
    type: "DRILL" | "INTERVIEW" | "REVIEW";
    title: string;
    trackId: string;
    topicId?: string;
  };
}
```

#### 4. Spaced Repetition Due (`GET /api/v1/me/review/due`)
```typescript
export interface ReviewQueueItem {
  attemptId: string;
  questionId: string;
  questionBody: string;
  lastAttemptedAt: string;
  mistakeCount: number;
  intervalDays: number;
}
```

---

### Phase 7: Authentication & Profile (`/api/v1/auth/*` & `/me`)

* **Google Login**: `POST /api/v1/auth/google`
  * Body: `{ idToken: string }`
  * Backend checks domain: requires registered institution college domain.
  * Response: `{ token: string, user: UserProfile }`
* **Current Profile**: `GET /api/v1/me`, `PATCH /api/v1/me` (update `targetTrackId`, `gradYear`).

---

### Phase 8: AI Stream & Usage Meter (`/api/v1/sessions/{id}/events` & `/me/usage`)

* **Live Interview Stream (Server-Sent Events)**: `GET /api/v1/sessions/{id}/events`
  * Transport: `EventSource` / `@microsoft/fetch-event-source` with `Authorization` header.
  * Event Types:
    * `interviewer-typing`: Indicator that LLM follow-up is generating.
    * `interviewer-chunk`: Partial text tokens streamed to UI.
    * `interviewer-complete`: Full follow-up prompt payload finalized.
    * `evaluation-done`: Full rubric score payload.
* **AI Usage Meter**: `GET /api/v1/me/usage`
  * Response: `{ dailyQuota: 2, usedToday: 1, resetAt: "2026-10-13T00:00:00Z", hasCustomKey: false }`
* **Custom Groq Key**: `POST /api/v1/me/llm-key` (stores encrypted key, sets `hasCustomKey: true`).

---

## 5. Frontend State Management & Routing Map

```
/
├── (auth)
│   ├── login/                  -> Google SSO, college email verification
├── (dashboard)
│   ├── dashboard/              -> Today's plan, streak, readiness score, telemetry curve
│   ├── practice/               -> Track catalog, topic selection, mode customization
│   │   └── quiz/[sessionId]/   -> Active 8-question drill, keyboard navigation, instant grading
│   ├── interviews/             -> Interview lobby, live conversational follow-up canvas
│   │   └── live/[sessionId]/   -> Audio/text prompt, response recorder, SseEmitter stream
│   ├── skills/                 -> Mastery tree, prerequisite edge links, gap priorities
│   ├── review/                 -> Mistake queue, spaced-repetition cards
│   ├── progress/               -> Longitudinal readiness graphs, cohort benchmark
│   └── settings/               -> User profile, target track, Groq API key modal
```

### TanStack Query Key Factory Pattern
```typescript
export const queryKeys = {
  tracks: {
    all: ['tracks'] as const,
    topics: (trackId: string) => ['tracks', trackId, 'topics'] as const,
  },
  sessions: {
    detail: (id: string) => ['sessions', id] as const,
    report: (id: string) => ['sessions', id, 'report'] as const,
  },
  me: {
    profile: ['me', 'profile'] as const,
    skills: ['me', 'skills'] as const,
    gaps: ['me', 'skills', 'gaps'] as const,
    plan: ['me', 'plan', 'today'] as const,
    reviewDue: ['me', 'review', 'due'] as const,
    aiUsage: ['me', 'usage'] as const,
  },
};
```

---

## 6. Implementation Guardrails for Frontend Coders
1. **Never mock data inside production routes**: Place all mock fixtures in `frontend/src/mocks/` and use MSW or mock fetch in tests.
2. **Keyboard Accessibility**: On the Quiz drill screen, keys `A`, `B`, `C`, `D` must trigger option selection; `Enter` submits; `Space` advances to next.
3. **Optimistic UI with Safety**: When an option is clicked, highlight the selection immediately. Once the server answers, transition the card border to `#16A34A` (Correct) or `#DC2626` (Incorrect) without layout shift.
4. **Clean Trace Telemetry**: When logging frontend errors, include the `X-Trace-Id` header returned by the server so developers can trace the issue in Spring Boot logs.
