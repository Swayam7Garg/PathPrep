# PrepPath: project context for the coding agent

Working name: PrepPath (placeholder). Read this whole file before making changes. Everything here is a decision already made. If you think one should change, say so and explain why before changing it. If something is unclear or conflicts, ask one short question first.

---

## 1. What we are building

An interview-preparation website for college students in India who are preparing for internships and campus placements. Students choose a role track, practise quizzes and AI-assisted mock interviews, get rubric-based feedback, and see a skill-gap plan that tells them what to do next.

One-line definition: a website where a student picks a role track, practises short AI-assisted interviews, gets criterion-based feedback, and sees a skill-gap plan that tells them what to do tomorrow.

Owner: a solo college student building this to learn real engineering. Not a generic question generator. Prefer solutions that teach good practice over shortcuts, and explain why briefly.

### Differentiators (keep these in mind when designing)
- Per-topic mastery (a skill map), not a single score. Topics have prerequisites.
- Role tracks weight topics by importance.
- Explainable grading: score per rubric criterion, quoted evidence from the student's answer, missed key points, stronger sample answer.
- Adaptive follow-up questions in live interviews.
- Mistake revisit with spaced repetition.
- Daily plan, streak, readiness score for habit.
- Later: cohort analytics for placement officers (aggregated, anonymised only).

### Success looks like
Students find it useful and come back regularly. Pilot targets (30 to 50 students): 60%+ finish a first session within 24h, 40%+ return within a week, 3+ sessions per active user per week, 70%+ find feedback helpful, AI grader within 1 point of a human grader at least 80% of the time on a 5-point scale. Cost per active user: zero.

---

## 2. Hard constraints

1. **AI cost must be zero.** AI provider is the Groq free tier. Verified Sept 2026 (check the Groq console Limits page for the truth): about 30 requests/min, 1,000 requests/day, 8,000 tokens/min, 200,000 tokens/day on text models, applied per organisation. A 6-question AI interview costs roughly 4,000 to 9,000 tokens, so the whole platform gets only about 20 to 25 AI interviews per day.
2. **Design rule: AI is the expensive last step, never the default path.** The app must be fully useful with AI switched off. Flag any suggestion that increases LLM calls or tokens.
3. Cost tactics already chosen: pre-written human-reviewed question bank with key points; MCQ and concept checks graded in code; one LLM call per answer that grades and generates the follow-up together; "Lite" interview mode with one batched grading call at the end; per-user daily AI quota; caching; optional bring-your-own Groq key (stored encrypted); provider fallback behind an interface. Never create fake accounts to bypass limits. Do not send names or emails to the LLM.
4. Free or low-cost hosting. Keep the JVM heap capped and dependencies lean.
5. No Python in this project.

---

## 3. Tech stack (decided)

### Backend
- Java 21, **Spring Boot 4.1.x** (Boot 3.x is out of free support; do not downgrade). Spring MVC with virtual threads (not WebFlux).
- **MySQL 8.0.16+**, Spring Data JPA (Hibernate 7), **Flyway** with SQL migrations. `ddl-auto: validate`. Never use `update` or `create`.
- Starters in Boot 4 are split into smaller modules (for example `spring-boot-starter-webmvc`, `spring-boot-starter-flyway`, separate `*-test` starters). Use `pom.xml` as the source of truth and check names against start.spring.io before adding dependencies.
- Jackson 3 is the default in Boot 4: imports are `tools.jackson...`, not `com.fasterxml.jackson...`. Many old tutorials are wrong about this.
- Jakarta Bean Validation, MapStruct (annotation processor), springdoc-openapi (3.x line for Boot 4), Spring Security (resource-server starter for JWT), Actuator.
- **Not used:** Lombok (use Java records for DTOs), Spring Modulith (we use layered packages), Resilience4j (use Spring Framework 7 built-in `@Retryable` with `@EnableResilientMethods`, plus a small hand-written token-bucket for the Groq limit), Redis (not in MVP).
- Background jobs: start with a DB-backed worker using the `evaluations` table (`status = PENDING`) and a `@Scheduled` poller with `SELECT ... FOR UPDATE SKIP LOCKED` (supported in MySQL 8). JobRunr is optional later, once a Boot 4 compatible starter is verified on Maven Central.
- Streaming of interviewer replies: Server-Sent Events (`SseEmitter`).
- AI: Groq (OpenAI-compatible API) called through our own `LlmClient` interface using `RestClient`. Two model tiers set in config, not code: a small fast model for follow-ups and generation, a larger one for rubric grading. Grading: temperature near 0, strict JSON output validated into a Java record, retry once, then mark the evaluation `FAILED`. Store `model_version` and `rubric_version` on every evaluation. Provide a mock and an Ollama implementation for dev and tests.
- Encryption of stored API keys: Spring Security crypto `Encryptors` (no extra dependency).
- Tests: JUnit 5, Mockito, Testcontainers (MySQL, module `testcontainers-mysql`), MockMvc.

### Frontend (separate repo or folder)
Next.js (App Router), TypeScript, Tailwind CSS, shadcn/ui, TanStack Query, Zod, typed API client generated from OpenAPI, Recharts. UI is generated in Google Stitch: mostly white, black solid primary buttons, 1px light-gray card borders, Inter font, thin Lucide line icons, color only for status (muted green, amber, soft red).
Rules when implementing designs: treat the Stitch HTML in `/design` as the source of truth, use only design tokens from `DESIGN.md` and the Tailwind theme (no hard-coded colours or sizes), build shared components first (Button, Card, Input, Sidebar, TopBar, Badge, Progress), one screen at a time with mock data, then screenshot with Playwright at 1440px and 390px and fix every visible difference before moving on.

### DevOps
Docker and Docker Compose, GitHub Actions CI. Host choices are open: frontend on Vercel, API on a small container host or VPS, MySQL on a managed free tier or in Compose. Verify current free-tier terms at deploy time.

---

> [!NOTE]
> ### Architectural Discrepancies & Stale Documentation Notes (Phase 5 Reconciliation)
> The actual implemented codebase diverges from initial design notes in several specific areas. Always trust the code over outdated design lines:
> 1. **Lombok**: Lines 47 stated Lombok is not used. In practice, Lombok is actively used across entities (`@Getter`, `@Setter`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`).
> 2. **Mapper Library**: Line 46 stated MapStruct. The project uses `ModelMapper` (`com.swayam.pathprep.config.ModelMapperConfig`) with strict mapping and custom converters.
> 3. **Package Structure**: Lines 65-75 referenced `com.preppath`. The codebase root package is `com.swayam.pathprep`, which also includes `enums`, `security`, `filter`, and `event` subpackages.
> 4. **Primary Key ID Strategy**: Lines 79 & 91 specified UUIDs stored as CHAR(36). In actual implementation, entities use database-generated `Long` auto-increment identity keys (`GenerationType.IDENTITY`).

---

## 4. Backend structure: layered packages

```
com.preppath
 ├─ controller   HTTP in/out only. No logic, no repositories.
 ├─ service      Use cases, transactions, business rules. Services may call other services. Never call another service's repository.
 ├─ repository   Spring Data interfaces and queries.
 ├─ entity       JPA entities and enums stored in the database.
 ├─ dto          Request and response records, query projections. Never return entities from controllers.
 ├─ mapper       MapStruct mappers.
 ├─ exception    ApiException family, GlobalExceptionHandler.
 └─ config       Security, CORS, filters, typed properties, beans.
```

Code conventions:
- Constructor injection only. Controllers are thin. Services own `@Transactional` (use `readOnly = true` for reads).
- IDs are UUIDs generated in Java (`@GeneratedValue(strategy = GenerationType.UUID)`), stored as `CHAR(36)`. Required setting: `spring.jpa.properties.hibernate.type.preferred_uuid_jdbc_type: CHAR`. In native queries, read ids as `String` and convert with `UUID.fromString`.
- MySQL native queries: no double-quoted aliases.
- `spring.jpa.open-in-view: false`.
- Flyway migrations live in `src/main/resources/db/migration`. **Never edit a migration that has already run**; add a new version. Seed data for dev lives in its own migration and must be removable.
- Avoid N+1 queries: use one query for ids and counts, then load related rows with `IN` queries.
- Do not leak correct answers: question views sent to students never include `correct` or `explanation` until after the answer is submitted.
- Never send stack traces to clients. Log with a trace id.

---

## 5. Error format and conventions (API)

Base path `/api/v1`. JSON camelCase, UTC ISO-8601 timestamps, UUID ids, Bearer JWT on everything except `/auth/*` and public catalog endpoints. Paging: `?page=0&size=20` returning `{ items, page, size, total }`. `Idempotency-Key` header on `POST /sessions/{id}/answers`.

Error body (always this shape):
```
{ "code": "TRACK_NOT_FOUND", "message": "...", "details": [], "retryAfterSeconds": null, "traceId": "..." }
```
Status codes: 400 validation or malformed, 401, 403, 404, 409 conflict, 422 valid but unsatisfiable, 429 rate or quota (`AI_QUOTA_EXCEEDED`), 500 internal, 502 AI provider failure.

Exception family: `ApiException` (abstract: code, httpStatus, message, details) with `NotFoundException` (404), `BadRequestException` (400), `UnprocessableException` (422), `FeatureNotAvailableException` (400, `MODE_NOT_AVAILABLE`). `GlobalExceptionHandler` maps `ApiException`, Bean Validation failures (`VALIDATION_FAILED` with field details), unreadable JSON or bad enum (`MALFORMED_REQUEST`), bad path or query types (`INVALID_PARAMETER`), and everything else to a generic 500 `INTERNAL_ERROR`.

Config classes planned: `DevSecurityConfig` (temporary, permits all, delete in the auth step), `CorsConfig` (Next.js dev origin from properties), `TraceIdFilter` (creates or reuses `X-Trace-Id`, returns it as a header), `OpenApiConfig`, `ClockConfig` (single `Clock` bean), `SessionProperties` (`app.sessions.maxQuestions` = 20, `defaultQuestionCount` = 8), and `CurrentUser` (interface with the logged-in user id) with `DevCurrentUser` (returns a seeded user's id; replaced by a JWT-based implementation in the auth step).

---

## 6. Data model

Identity: `institutions` (name, email_domain), `users` (institution_id, email, name, role STUDENT/OFFICER/ADMIN, branch, grad_year, target_track_id), `cohorts`, `cohort_members`.

Content (migration V1): `role_tracks` (name, description, company_type SERVICE/PRODUCT/MIXED), `topics` (name, domain), `topic_prereqs` (topic_id, prereq_id), `track_topics` (track_id, topic_id, weight), `rubrics` (name, version), `rubric_criteria` (rubric_id, name, description, weight), `questions` (topic_id, rubric_id nullable, type MCQ/CONCEPT/SCENARIO/BEHAVIOURAL, difficulty 1 to 3, body, key_points JSON, model_answer, status DRAFT/PUBLISHED/FLAGGED), `question_options` (question_id, position, text, is_correct, explanation).

Practice: `sessions` (user_id, track_id, mode QUIZ/INTERVIEW, style LITE/LIVE nullable, difficulty EASY/MEDIUM/HARD/MIXED, status IN_PROGRESS/EVALUATING/COMPLETED/FAILED, total_questions, current_index, started_at, completed_at), **`session_questions`** (session_id, question_id, position; unique on (session_id, position) and (session_id, question_id)), `attempts` (session_id, question_id, parent_attempt_id for follow-ups, answer_text or selected_option_id, time_spent_sec, created_at, idempotency key), `evaluations` (attempt_id, status PENDING/DONE/FAILED, overall_score, feedback, model_version, rubric_version, tokens_used), `criterion_scores` (evaluation_id, criterion_id, score, evidence_quote).

Learning: `skill_mastery` (user_id, topic_id, mastery, confidence, updated_at), `review_queue` (user_id, attempt_id, due_at, interval_days), `daily_activity` (user_id, date, counts, minutes).

Platform: `user_llm_keys` (user_id, provider, encrypted_key, last4), `llm_usage` (user_id, date, calls, tokens).

Migration plan: V1 catalog, V2 dev seed (3 tracks, 4 topics, one rubric, 2 questions), V3 users and institutions, V4 sessions and session_questions, V5 dev user (id matches `DevCurrentUser`). Later versions add attempts, evaluations, learning and platform tables as their endpoints are built.

---

## 7. MVP scope

In: college-email Google login, 3 tracks (Java Backend, Web Frontend, General SDE) with about 150 to 200 reviewed questions, MCQ quiz mode (no AI), interview mode with AI feedback (Lite and Live styles), rubric-based report, skill-gap profile, daily plan, mistake revisit, progress page, AI usage meter and optional own key, a simple admin question import.
Later: cohorts and officer dashboard, voice, behavioural mode, coding rounds, company patterns, skill graph visual.
Not planned: video proctoring, payments, native mobile app.

---

## 8. Endpoint plan (build in this order; auth comes late)

| Phase | Endpoint |
|---|---|
| 1 | `GET /tracks` |
| 2 | `GET /tracks/{trackId}/topics` |
| 3 | `POST /sessions` (quiz mode); admin question endpoints |
| 4 | `POST /sessions/{id}/answers` (MCQ graded in code, idempotent) |
| 5 | `POST /sessions/{id}/complete`, `GET /sessions/{id}/report`, `GET /sessions`, `GET /sessions/{id}` |
| 6 | `GET /me/skills`, `/me/skills/gaps`, `/me/plan/today`, `/me/progress`, `/me/review/due`, `POST /me/review/{id}/answer` |
| 7 | Auth: `POST /auth/google` (verify Google ID token, college domain only), `/auth/refresh`, `/auth/logout`, `GET/PATCH/DELETE /me` |
| 8 | AI: evaluations, SSE events, `/me/usage`, `/me/llm-key`, feedback endpoint, admin AI usage |
| later | Cohorts (`POST /cohorts`, `POST /cohorts/join`, `GET /cohorts/{id}/analytics`) |

Controllers: `TrackController`, `SessionController`, `AnswerController`, `SessionEventsController`, `EvaluationController`, `SkillController`, `PlanController`, `ProgressController`, `ReviewController`, `AuthController`, `MeController`, `UsageController`, `LlmKeyController`, `CohortController`, `AdminQuestionController`, `AdminUsageController`, `QuestionReportController`, `HealthController`.

### Phase 1: `GET /api/v1/tracks`
Returns every track with topic and published-question counts. `TrackController.list()` calls `TrackService.listTracks()` (read-only transaction), which calls `RoleTrackRepository.findAllWithCounts()` (one native query, sorted by name) returning `TrackRow` projections (`id` String, `name`, `description`, `topicCount`, `questionCount`) and maps them to `TrackSummary` DTOs. Entity: `RoleTrack`. `200` with `[]` if empty. No custom errors.

### Phase 2: `GET /api/v1/tracks/{trackId}/topics`
Returns a track's topics with weight, question count and prerequisite ids. `TrackController.getTopics(trackId)` calls `TrackService.getTopicsForTrack(trackId)`: check the track exists (else `NotFoundException` `TRACK_NOT_FOUND`), load topics via `TopicRepository.findTopicsForTrack` (native, returns `TopicRow`: id, name, domain, weight, questionCount), load prerequisites in one query via `TopicPrereqRepository.findByTopicIdIn`, assemble `TopicInTrack` DTOs (`topicId`, `name`, `domain`, `weight`, `questionCount`, `prerequisiteIds`), sort by weight descending then name. Entities: `Topic`, `TrackTopic` (+ `TrackTopicId`), `TopicPrereq` (+ `TopicPrereqId`). Bad UUID gives 400 `INVALID_PARAMETER`.

### Phase 3: `POST /api/v1/sessions` (quiz mode)
Request `SessionCreateRequest`: `mode` (required; only `QUIZ` works now, `INTERVIEW` throws `FeatureNotAvailableException`), `style` (ignored for quiz), `trackId` (required, must exist), `topicIds` (optional; empty means all of the track's topics; every id must belong to the track else 400 `TOPIC_NOT_IN_TRACK`), `questionCount` (1 to `maxQuestions`, default `defaultQuestionCount`), `difficulty` (default `MIXED`).
Response `SessionResponse` (201 plus `Location` header): `id`, `mode`, `style`, `status`, `totalQuestions`, `currentIndex` (0), `currentQuestion` (`QuestionView`: id, type, topicId, topicName, difficulty, body, `options` of `OptionView` id/position/text only), optional `warnings` (for example fewer questions available than requested).
`SessionService.createSession` in one write transaction: get the user from `CurrentUser`, reject interview mode, load the track (404), resolve and validate topics, apply defaults, call `QuestionSelector.select(topicIds, difficulty, count)` (a plain helper class: loads candidate published MCQ ids, spreads fairly across topics favouring higher weights, shuffles, never repeats; unit-testable without a database), throw `UnprocessableException` `NOT_ENOUGH_QUESTIONS` if none, save `PracticeSession` and one `SessionQuestion` per question with positions 0..n, load the first question and options, map without answer data.
Entities: `PracticeSession`, `SessionQuestion`, `Question`, `QuestionOption`, `User` (minimal). Enums: `SessionMode`, `SessionStyle`, `SessionStatus`, `QuizDifficulty` (`EASY`=1, `MEDIUM`=2, `HARD`=3, `MIXED`=1..3), `QuestionType`, `QuestionStatus`. Repositories: `UserRepository`, `TrackTopicRepository`, `QuestionRepository` (`findCandidateIds`, `findAllByIdIn`), `QuestionOptionRepository`, `PracticeSessionRepository`, `SessionQuestionRepository`. Mappers: `SessionMapper`, `QuestionMapper`.

### Phase 4: `POST /api/v1/sessions/{id}/answers`
Submits an answer for the current question in a practice session.
- **Request**: `questionId`, `selectedOptionId` (for MCQ) or `answerText` (for interview text answers), optional `timeSpentSec`.
- **Header**: `Idempotency-Key` prevents duplicate attempts and double-counting.
- **Behavior**: Evaluated in Java code for MCQs. Advances session `current_index` if more questions remain, or completes session via `SessionFinalizer` when the last question is answered.
- **Response**: `correct`, `correctOptionId`, `explanation`, `nextQuestion` (sanitised `QuestionView`), and `sessionStatus`.

### Phase 5: Session Completion, Resume, Report, and History (Zero AI)
Turns finished quiz sessions into persistent scorecards, enables resuming in-progress quizzes after page reloads, enforces a single active session per user, and provides paged session history.

#### Endpoints
1. **GET `/api/v1/sessions/{id}` (Session Resume & State)**
   - Returns full session state, current question, and questions answered so far.
   - User-isolated: only returns data for the authenticated `CurrentUser` (foreign or non-existent IDs return 404 `SESSION_NOT_FOUND`).
   - Response includes: `resumable` (true only if `status == IN_PROGRESS`), `reportAvailable` (true only if `status == COMPLETED`), `currentQuestion` (`QuestionView` without answers), and `questions` list indicating whether each question is `answered`.

2. **POST `/api/v1/sessions/{id}/complete` (Session Completion)**
   - Idempotently finalizes a session. Can be called on the final question or when ending early.
   - Calculates `scorePercent` (`correctCount / totalQuestions * 100`), sets `completionReason` (`FINISHED` or `ENDED_EARLY`), and calculates elapsed time.
   - Subsequent calls return 200 with unchanged statistics (idempotent).
   - Publishes `SessionCompletedEvent` via `SessionEventListener` (`@TransactionalEventListener(phase = AFTER_COMMIT)`).

3. **GET `/api/v1/sessions/{id}/report` (Session Report & Scorecard)**
   - Returns comprehensive post-quiz scorecard. Returns 400 `SESSION_NOT_COMPLETED` if the session is still in progress.
   - Summary: `scorePercent` (relative to total questions), `accuracyPercent` (relative to answered questions), `totalQuestions`, `answeredCount`, `correctCount`, `unansweredCount`, `timeSpentSeconds`, `previousScorePercent` (from user's previous completed session in same track).
   - Topic Breakdown: Evaluated via `TopicPerformanceEvaluator`:
     - `STRONG` (≥ 80% accuracy)
     - `COMPETENT` (60% – 79.9% accuracy)
     - `NEEDS_WORK` (< 60% accuracy)
   - Recommendations: Weakest topics flagged with `priority` (`HIGH`, `MEDIUM`), capped at top 3 topics.
   - Question Review: List of all questions. For answered questions, shows chosen option, correct option, explanation. For unanswered questions, correct option and explanation are strictly hidden (`null`).

4. **GET `/api/v1/sessions?page=0&size=20&status=&mode=&trackId=` (Session History)**
   - Returns paginated list of sessions for the logged-in user.
   - Deterministic sorting: `started_at DESC, id DESC`.
   - Supports filtering by `status` (`IN_PROGRESS`, `COMPLETED`, `ABANDONED`), `mode` (`QUIZ`, `INTERVIEW`), and `trackId`.

#### Business Rules & Database Enforcements
- **Single Active Session Rule**: A user can have at most one `IN_PROGRESS` session at any time. Enforced at the database level using MySQL generated virtual column `active_marker` with unique index `uk_sessions_user_active (user_id, active_marker)`. Concurrent attempts return 409 `ACTIVE_SESSION_EXISTS` with `meta: {"activeSessionId": <id>}`.
- **Zero AI**: All scoring, grading, time metrics, and topic evaluations are calculated deterministically in Java code with zero LLM/AI dependencies.
- **Strict JSON Boolean Naming**: Boolean fields serialize as `"correct"`, `"completed"`, `"answered"`, `"resumable"`, `"reportAvailable"` (no `"isX"` prefixes).
- **SessionFinalizer**: Centralized service ensuring consistent score calculations and event dispatching whether triggered by answering the final question or an explicit complete request.

---


### Phase 6 (Completed): The Learning Loop (Zero AI)
#### Endpoints Implemented
1. **GET `/api/v1/me/skills?trackId=`**
   - Returns per-topic skill mastery for the student in the target track.
   - Includes mastery percentage, confidence level (`LOW`, `MEDIUM`, `HIGH`), skill status (`MASTERED`, `PRACTICING`, `NEEDS_WORK`, `UNPRACTICED`), and prerequisites validation (`prerequisitesMet`, unmet prerequisite list).
   - Sorted deterministically by topic weight DESC, topic name ASC.

2. **GET `/api/v1/me/skills/gaps?trackId=&limit=1..10`**
   - Ranked topic skill gaps computed as `weight * (100 - mastery)`.
   - Identifies gap kind (`CRITICAL_GAP`, `PREREQUISITE_BLOCKED`, `NEEDS_REINFORCEMENT`, `UNTAPPED_HIGH_WEIGHT`).
   - Includes human-readable reasons and suggested actions.

3. **GET `/api/v1/me/plan/today`**
   - Daily personalized study plan.
   - Includes streak statistics (`current`, `longest`, `activeToday`, `atRisk`).
   - Readiness score calculated across track topics with minimum question thresholds and mistake clearance factor.
   - Due review item counts.
   - Up to 3 prioritized plan items (`RESUME` for active session, `REVIEW` for spaced repetition, `DRILL` for top skill gap).

4. **GET `/api/v1/me/progress?range=7|30|90`**
   - Time-windowed progress analytics.
   - Dense calendar day array of exact size `range` ending on today, with `questionsAnswered`, `questionsCorrect`, `reviewsCompleted`, and `activeMinutes`.
   - All-time totals and last 10 completed practice sessions.

5. **GET `/api/v1/me/review/due?limit=1..50`**
   - Spaced repetition review queue questions due on or before today.
   - Strictly sanitizes question views: hides `isCorrect`, `correctAnswerId`, and explanations.

6. **POST `/api/v1/me/review/{id}/answer`**
   - Grades review answer in Java and updates the spaced-repetition ladder:
     - Correct: advances ladder (1d -> 3d -> 7d -> 14d -> `GRADUATED`).
     - Incorrect: resets to step 0 and schedules for +1 day.
   - Atomically records attempt, updates `daily_activity` with capped active seconds, and returns updated schedule and explanation.

#### Architecture & Pure Logic
- **Pure Calculators**: Isolated, thoroughly unit-tested algorithmic classes:
  - `MasteryCalculator`: Beta-binomial smoothing / weighted recency window.
  - `StreakCalculator`: Continuous active day streak & at-risk detection.
  - `ReviewScheduler`: Exponential interval ladder.
  - `ReadinessCalculator`: Weighted topic coverage & mistake clearance.
  - `GapRanker`: Multi-factor gap ranking.
- **Event-Driven Integration**: `LearningEventListener` listens to `AttemptGradedEvent` and `SessionCompletedEvent` inside transactions for atomic updates to mastery, queue, and daily metrics.

---

### Phase 7 (Completed): Authentication & Authorization (Zero AI)
#### Endpoints Implemented
1. **POST `/api/v1/auth/google`**
   - Verifies Google ID tokens using Google's public keys via Nimbus JWT.
   - Strictly enforces Google Client ID (no client secret anywhere in code, configuration, or logs).
   - Domain whitelisting against `ADMIN_EMAILS`, `ALLOW_ANY_GOOGLE_ACCOUNT`, `ALLOWED_EMAIL_DOMAINS`, and `institutions.email_domain`.
   - Idempotently provisions or updates `User` records with unique conflict recovery on concurrent first logins.
   - Promotes emails in `ADMIN_EMAILS` to `ADMIN` without silently demoting existing `OFFICER` roles.
   - Issues 15-minute HS256 access token in JSON body and 30-day rotating opaque refresh token in HttpOnly `pp_refresh` cookie.

2. **POST `/api/v1/auth/refresh`**
   - Rotates refresh token using database pessimistic write locks (`SELECT ... FOR UPDATE`).
   - Token reuse detection: if a previously revoked refresh token is presented, the entire family is immediately revoked, an audit log is emitted, and 401 `REFRESH_REUSED` is returned.
   - Mints a new access token and replaces the refresh cookie with a newly rotated token in the same family.
   - Protected against CSRF via strict `X-Requested-With: pathprep` validation and CORS origin verification.

3. **POST `/api/v1/auth/logout`**
   - Revokes the entire refresh token family and clears the `pp_refresh` cookie. Idempotent.

4. **POST `/api/v1/auth/dev-login`**
   - Fast-track development login endpoint strictly isolated to `dev` and `test` profiles (fails production startup if enabled).

5. **GET `/api/v1/me`**
   - Returns the authenticated user's profile (`email`, `name`, `role`, `pictureUrl`, `branch`, `gradYear`, `targetTrackId`, `onboardingCompleted`).
   - Cache-Control: `no-store`.

6. **PATCH `/api/v1/me`**
   - Updates student onboarding fields: `name`, `branch`, `gradYear` (validated 2025..2032), `targetTrackId` (validated against tracks repository).
   - Rejects unknown fields with 400 `MALFORMED_REQUEST`.
   - Computes `onboardingCompleted` (requires `branch`, `gradYear`, and `targetTrackId`).

7. **DELETE `/api/v1/me`**
   - Hard-deletes all user data across all 11 user tables in reverse foreign-key dependency order (`review_attempts` -> `review_queue` -> `skill_mastery` -> `daily_activity` -> `evaluations` -> `attempts` -> `sessions` -> `question_reports` -> `institution_memberships` -> `refresh_tokens` -> `users`).
   - Requires body `{"confirm": "DELETE"}`. Clears refresh cookie upon deletion.
   - Coverage verified against MySQL `information_schema.KEY_COLUMN_USAGE`.

#### Security Architecture
- **Token Model**:
  - Access Token: Stateless HS256 signed JWT (15-minute TTL). Signed with minimum 32-byte secret. Claims: `sub` (userId), `role`, `iss` ("pathprep").
  - Refresh Token: Cryptographically secure 32-byte random opaque token, stored as SHA-256 hex hash (30-day TTL).
  - Rotation: Each refresh invalidates the used token and issues a new token in the same family.
  - Known Limitation: Access tokens are stateless and remain valid until their 15-minute expiry even after logout or token family revocation. Refresh tokens are immediately revoked.
- **Dual-Auth Filter (`AdminKeyFilter`)**:
  - Validates `X-Admin-Key` for `/api/v1/admin/**` endpoints when configured.
  - If header is absent or key mode disabled, passes request down the filter chain to JWT authentication (`ROLE_ADMIN` authority required).
- **Production Startup Validator (`AuthStartupValidator`)**:
  - Enforces at startup under `prod` profile: JWT secret >= 32 bytes, Google Client ID non-blank, refresh cookie secure=true, dev-login-enabled=false.
- **Current User Resolution (`CurrentUser` / `JwtCurrentUser`)**:
  - User ID is always extracted from the authenticated principal (`sub` claim).
  - In `dev` and `test` profiles, safely falls back to `DevCurrentUser` for test harness convenience when unauthenticated.
  - In `prod`, rejects unauthenticated access with 401 `UNAUTHENTICATED`.
- **Rate Limiting**: Sliding-window rate limiter (20 req/min per IP on `/auth/**`), returning 429 `RATE_LIMITED` with `Retry-After` header.

---

### Phase 8 (Planned): User-Defined Daily Plan & Interactive Checklist
#### Concept & Goals
Replace the static daily goal block with an interactive, student-driven daily study timetable widget ("Today's Plan") on the Dashboard and Practice screens:
- **Student Agency**: Daily goals are defined by the student, resetting every morning at 12:00 AM local time.
- **Two Item Types**:
  1. *Custom / External Task*: Freeform text (e.g. "Solve 3 DSA questions on LeetCode", "Revise OS virtual memory notes"). Manually checked off by the student.
  2. *In-App Practice Quiz Task*: Structured quiz assignment (e.g. "Quiz: Spring Boot Dependency Injection, 5 Qs"). Renders with a 1-click `[Take Quiz]` button and auto-checks off upon session completion.
- **Dynamic Goal Bar**: Header progress indicator showing completion count and percentage (`2 of 3 completed • 67%`).
- **Cross-Screen Practice Integration**: Scheduled quizzes appear pinned at the top of `/practice` under "Scheduled for Today" alongside the regular full catalog.
- **Smart Rollover**: Prompt to carry over yesterday's unfinished items with a single click.

#### Database Architecture (`V12__daily_plan.sql`)
- Table `daily_plan_items`:
  - `id` (BIGINT AUTO_INCREMENT PRIMARY KEY)
  - `user_id` (BIGINT NOT NULL, FK to `users.id` ON DELETE CASCADE)
  - `plan_date` (DATE NOT NULL)
  - `title` (VARCHAR(255) NOT NULL)
  - `item_type` (VARCHAR(32) NOT NULL: `CUSTOM`, `QUIZ`)
  - `track_id` (BIGINT NULL, FK to `role_tracks.id`)
  - `topic_id` (BIGINT NULL, FK to `topics.id`)
  - `question_count` (INT NULL)
  - `session_id` (BIGINT NULL, FK to `practice_sessions.id`)
  - `completed` (BOOLEAN NOT NULL DEFAULT FALSE)
  - `completed_at` (TIMESTAMP NULL)
  - `sort_order` (INT NOT NULL DEFAULT 0)
  - `created_at` (TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)
  - Index: `idx_daily_plan_user_date (user_id, plan_date)`

#### Backend Endpoints (`/api/v1/me/plan`)
- `GET /api/v1/me/plan`: Returns today's plan items, progress stats (`totalItems`, `completedItems`, `progressPercent`), and `hasUnfinishedYesterdayTasks`.
- `POST /api/v1/me/plan/items`: Adds a task (`CUSTOM` with title, or `QUIZ` with trackId, topicId, questionCount).
- `PATCH /api/v1/me/plan/items/{id}/toggle`: Toggles completion status for manual tasks.
- `DELETE /api/v1/me/plan/items/{id}`: Deletes an item from today's plan.
- `POST /api/v1/me/plan/rollover`: Carries forward yesterday's incomplete items to today.
- **Auto-Complete Hook**: `SessionCompletedEvent` in `LearningEventListener` marks matching open `QUIZ` items completed and binds `session_id`.

## 9. Testing expectations
- Every endpoint gets a MockMvc integration test against a Testcontainers MySQL, including each error row in its spec.
- Pure logic (like `QuestionSelector`, mastery updates, quota checks) gets plain unit tests.
- Add a test asserting question responses contain no `correct` or `explanation` field.
- Flyway must migrate an empty database cleanly in tests.

---

## 10. How you (the agent) should work
1. Work in small steps: one endpoint or one piece at a time. Do not build ahead of the current phase.
2. Before coding a step, state the plan in a few lines (files to add or change). After coding, run the build and tests and report the result honestly, including failures.
3. Do not add dependencies, change the stack, rename packages, or edit applied migrations without asking first.
4. Never hard-code secrets. Config goes in `application.yml` or environment variables.
5. Keep controllers thin, DTOs separate from entities, and errors in the standard format.
6. Respect the zero-AI-cost rule in every design choice.
7. For frontend work, follow the Stitch-design rules in section 3 and verify visually before saying it is done.
8. When something in this file is wrong or outdated, say so and propose an edit to this file.

---

## 11. Current status (update this as you go)
- [x] Project created, `pom.xml` in place (Boot 4.1, MySQL, Flyway)
- [x] `application.yml` with MySQL, Flyway, UUID CHAR setting
- [x] V1 and V2 migrations applied
- [x] Shared exceptions, `GlobalExceptionHandler`, `TraceIdFilter`, `DevSecurityConfig`, `CorsConfig`
- [x] Phase 1 `GET /tracks` done and tested
- [x] Phase 2 `GET /tracks/{trackId}/topics` done and tested
- [x] V3, V4, V5 migrations; `CurrentUser` and `DevCurrentUser`
- [x] Phase 3 `POST /sessions` done and tested
- [x] V6 migration (`attempts` table with idempotency key and answer recording)
- [x] Phase 4 `POST /sessions/{id}/answers` done and tested (MCQ graded in code, idempotent)
- [x] V7 migration (`sessions` summary fields, completion reason, time tracking, `uk_sessions_user_active` constraint)
- [x] Phase 5 `GET /sessions/{id}`, `POST /sessions/{id}/complete`, `GET /sessions/{id}/report`, `GET /sessions` done and tested (Zero AI, single active session guard, scorecard, history)
- [x] V8 migration (`skill_mastery`, `review_queue`, `review_attempts`, `daily_activity`)
- [x] Phase 6 The Learning Loop done and tested (`/me/skills`, `/me/skills/gaps`, `/me/plan/today`, `/me/progress`, `/me/review/due`, `/me/review/{id}/answer`, zero-AI pure calculators, atomic daily activity upsert, strict JSON boolean naming)
- [x] Phases 5 & 6 Gap Closure & Robustness:
  - Report on IN_PROGRESS returns 400 `SESSION_NOT_COMPLETED`; ABANDONED returns 409 `REPORT_NOT_AVAILABLE`.
  - `ReadinessView` contains `minAnswersRequired` matching configured threshold.
  - `GapKind` includes advisory `PREREQUISITE_BLOCKED` for topics with unmet dependencies.
  - `LearningEventListener` handles concurrent initial insert race on `skill_mastery` via pessimistic retry.
  - `QuestionMapper` record mapping without ModelMapper reflection.
  - 83/83 unit and integration tests passing covering full review ladder, atomicity, replay invariance, and single-active-session constraints.
- [x] Phase 7 Authentication & Authorization:
  - V11 migration: `google_sub`, `picture_url`, `last_login_at` on `users`; `refresh_tokens` table with required indexes, family tracking, and foreign key cascades.
  - Production `SecurityConfig` replacing `DevSecurityConfig`: stateless sessions, JWT resource server validation, custom JSON entry points (`401 UNAUTHENTICATED`, `403 FORBIDDEN`), and custom CORS filter returning `403 ORIGIN_NOT_ALLOWED` in standard error format.
  - Dual-mode `AdminKeyFilter` supporting both `X-Admin-Key` and JWT `ROLE_ADMIN` access.
  - Rotating refresh token engine with pessimistic locking concurrency control and reuse attack detection revoking entire token families.
  - Scheduled cleanup job purging expired refresh tokens.
  - Endpoints: `POST /auth/google`, `POST /auth/refresh`, `POST /auth/logout`, `POST /auth/dev-login` (dev/test only), `GET /me`, `PATCH /me`, `DELETE /me` (full 11-table cascading hard-deletion).
  - 132/132 unit and integration tests passing cleanly across the entire codebase.
- [x] Phase 8 User-Defined Daily Plan & Interactive Checklist:
  - V12 migration: `daily_plan_items` table with foreign keys, types (`CUSTOM`, `QUIZ`), and `(user_id, plan_date)` index.
  - Endpoints: `GET /api/v1/me/plan`, `POST /api/v1/me/plan/items`, `PATCH /api/v1/me/plan/items/{id}/toggle`, `DELETE /api/v1/me/plan/items/{id}`, `POST /api/v1/me/plan/rollover`.
  - Auto-complete hook via `SessionCompletedEvent` in `LearningEventListener`.
  - Frontend: Replace static goal block on dashboard with interactive "Today's Plan" timetable card & goal bar; pinned banner on `/practice`.
- [x] Phase 9 AI Interview Mode (LITE & LIVE) & LLM Infrastructure:
  - V13 migration: `user_llm_keys`, `llm_usage`, `evaluations`, `criterion_scores`, altered `attempts` for free-text answers and `parent_attempt_id`.
  - `LlmClient` interface with `GroqLlmClient` (OpenAI-compatible JSON structured completions) and `MockLlmClient` for zero-cost offline tests.
  - Zero-AI quiz path strictly preserved; question selection in interview mode selects vetted `CONCEPT` and `SCENARIO` questions.
  - Rubric grading engine generating explainable criterion scores, strengths, and improvements.
  - BYOK support with AES-256-GCM encryption (`/api/v1/me/llm-key`), usage meters (`/api/v1/me/usage`), and daily free quota enforcement (`QuotaExceededException` 429).
  - Admin AI usage audit (`/api/v1/admin/ai/usage`) for tracking token metrics and model costs.
  - 100% test green with integration tests for interview session creation, free-text grading, evaluation polling, and quota management.

