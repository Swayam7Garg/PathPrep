import {
  TrackSummary,
  TopicInTrack,
  SessionResponse,
  TopicMastery,
  SkillGapItem,
  DailyPlan,
  ReviewQueueItem,
  SessionReport,
  AiUsageInfo,
} from "./types";

export const MOCK_TRACKS: TrackSummary[] = [
  {
    id: 1,
    name: "Java Backend Engineer",
    description: "Core Java internals, multithreading, Spring Boot 3/4, SQL indexing, and distributed systems architecture.",
    topicCount: 14,
    questionCount: 168,
  },
  {
    id: 2,
    name: "Web Frontend Engineer",
    description: "Modern ECMAScript, React 19 reconciliation, Next.js App Router, web vitals, and browser rendering engines.",
    topicCount: 11,
    questionCount: 132,
  },
  {
    id: 3,
    name: "General SDE (Campus Core)",
    description: "Data structures, algorithms, operating system primitives, database internals, and computer networks.",
    topicCount: 18,
    questionCount: 220,
  },
];

export const MOCK_TOPICS: TopicInTrack[] = [
  { topicId: 101, name: "Collections & ConcurrentHashMap", domain: "JAVA INTERNALS", weight: 1.5, questionCount: 24, prerequisiteIds: [] },
  { topicId: 102, name: "JVM Memory Model & GC Tuning", domain: "JAVA INTERNALS", weight: 1.4, questionCount: 20, prerequisiteIds: [101] },
  { topicId: 103, name: "Thread Pools & ExecutorService", domain: "CONCURRENCY", weight: 1.6, questionCount: 18, prerequisiteIds: [101] },
  { topicId: 104, name: "Spring Transactional & Isolation", domain: "SPRING FRAMEWORK", weight: 1.5, questionCount: 22, prerequisiteIds: [101] },
  { topicId: 105, name: "Database Indexing (B-Tree vs Hash)", domain: "DATABASE DESIGN", weight: 1.3, questionCount: 16, prerequisiteIds: [] },
  { topicId: 106, name: "Distributed Locking & Redis", domain: "DISTRIBUTED SYSTEMS", weight: 1.2, questionCount: 14, prerequisiteIds: [103] },
];

export const MOCK_DAILY_PLAN: DailyPlan = {
  date: "2026-10-13",
  currentStreakDays: 18,
  targetMinutes: 25,
  completedMinutes: 15,
  recommendedAction: {
    type: "DRILL",
    title: "Distributed Systems & Concurrency (8 Questions • ~15 mins)",
    trackId: 1,
    topicId: 101,
  },
};

export const MOCK_SKILL_MASTERY: TopicMastery[] = [
  { topicId: 101, topicName: "Collections & ConcurrentHashMap", domain: "JAVA INTERNALS", masteryPercentage: 92, confidenceLevel: "HIGH", questionsAttempted: 42, lastPracticedAt: "2026-10-12T14:30:00Z" },
  { topicId: 102, topicName: "JVM Memory Model & GC Tuning", domain: "JAVA INTERNALS", masteryPercentage: 81, confidenceLevel: "HIGH", questionsAttempted: 30, lastPracticedAt: "2026-10-11T16:00:00Z" },
  { topicId: 103, topicName: "Thread Pools & ExecutorService", domain: "CONCURRENCY", masteryPercentage: 74, confidenceLevel: "MEDIUM", questionsAttempted: 28, lastPracticedAt: "2026-10-10T11:20:00Z" },
  { topicId: 104, topicName: "Spring Transactional & Isolation", domain: "SPRING FRAMEWORK", masteryPercentage: 68, confidenceLevel: "MEDIUM", questionsAttempted: 22, lastPracticedAt: "2026-10-09T09:15:00Z" },
  { topicId: 105, topicName: "Database Indexing (B-Tree vs Hash)", domain: "DATABASE DESIGN", masteryPercentage: 54, confidenceLevel: "LOW", questionsAttempted: 18, lastPracticedAt: "2026-10-07T18:45:00Z" },
  { topicId: 106, topicName: "Distributed Locking & Redis", domain: "DISTRIBUTED SYSTEMS", masteryPercentage: 48, confidenceLevel: "LOW", questionsAttempted: 12, lastPracticedAt: "2026-10-05T12:00:00Z" },
];

export const MOCK_SKILL_GAPS: SkillGapItem[] = [
  { topicId: 106, topicName: "Distributed Locking & Redis", gapWeight: 1.45, missingPrerequisites: ["Thread Pools & ExecutorService"], recommendedDrillCount: 8 },
  { topicId: 105, topicName: "Database Indexing (B-Tree vs Hash)", gapWeight: 1.30, missingPrerequisites: [], recommendedDrillCount: 6 },
  { topicId: 104, topicName: "Spring Transactional & Isolation", gapWeight: 1.15, missingPrerequisites: ["Collections & ConcurrentHashMap"], recommendedDrillCount: 6 },
];

export const MOCK_REVIEW_QUEUE: ReviewQueueItem[] = [
  { attemptId: "att-1", questionId: 501, questionBody: "Why does ConcurrentHashMap avoid locking the entire map during put operations in Java 8+?", topicName: "Collections & ConcurrentHashMap", lastAttemptedAt: "2 days ago", mistakeCount: 2, intervalDays: 3 },
  { attemptId: "att-2", questionId: 502, questionBody: "What happens when an exception is thrown inside a `@Transactional(propagation = Propagation.REQUIRES_NEW)` block?", topicName: "Spring Transactional & Isolation", lastAttemptedAt: "3 days ago", mistakeCount: 1, intervalDays: 5 },
  { attemptId: "att-3", questionId: 503, questionBody: "Compare clustered vs non-clustered index lookup B-tree traversal overhead.", topicName: "Database Indexing", lastAttemptedAt: "4 days ago", mistakeCount: 3, intervalDays: 1 },
];

export const MOCK_SESSION_RESPONSE: SessionResponse = {
  id: 402,
  mode: "QUIZ",
  style: null,
  status: "IN_PROGRESS",
  totalQuestions: 8,
  currentIndex: 2,
  currentQuestion: {
    id: 1024,
    type: "MCQ",
    topicId: 101,
    topicName: "Collections & ConcurrentHashMap",
    difficulty: 2,
    body: "In Java 8+, how does `ConcurrentHashMap` achieve high concurrency during `put()` operations without segment locks?",
    options: [
      { id: 1, position: 1, text: "It uses a global synchronized block around the table array during write." },
      { id: 2, position: 2, text: "It uses CAS (Compare-And-Swap) for empty bucket heads and synchronizes only on the head Node when collision occurs." },
      { id: 3, position: 3, text: "It divides the hash table into 16 distinct ReentrantLock Segments." },
      { id: 4, position: 4, text: "It uses optimistic read stamps with StampedLock across all buckets." },
    ],
  },
  warnings: [],
};

export const MOCK_SESSION_REPORT: SessionReport = {
  sessionId: 402,
  trackName: "Java Backend Engineer",
  mode: "QUIZ",
  overallScore: 78,
  criteriaScores: [
    { criterionId: "crit-1", name: "Technical Depth & Concurrency", score: 4, evidenceQuote: "Identified CAS bucket insertion and synchronized collision resolution." },
    { criterionId: "crit-2", name: "Memory Model & Volatile Semantics", score: 3, evidenceQuote: "Understood visibility guarantee, missed Happens-Before write-read edge." },
    { criterionId: "crit-3", name: "System Architecture Rationale", score: 4, evidenceQuote: "Clearly explained trade-offs between global locks and bucket striping." },
  ],
  missedKeyPoints: [
    "Difference between Java 7 Segment locks and Java 8 synchronized head node mechanism",
    "CounterCell striped adder optimization used in size() calculation",
  ],
  modelAnswer:
    "Java 8 ConcurrentHashMap eliminates the Segment[] hierarchy entirely. When inserting into an empty bin, CAS is used lock-free. When collisions occur, it synchronizes solely on the first Node (head) of that specific bin list or TreeBin, providing bin-level granularity.",
  recommendedTopics: [
    { topicId: 103, topicName: "Thread Pools & ExecutorService", action: "PRACTICE" },
    { topicId: 106, topicName: "Distributed Locking & Redis", action: "REVIEW" },
  ],
};

export const MOCK_AI_USAGE: AiUsageInfo = {
  dailyQuota: 2,
  usedToday: 1,
  resetAt: "Midnight UTC (05:30 AM IST)",
  hasCustomKey: false,
};
