export const queryKeys = {
  tracks: {
    all: ["tracks"] as const,
    topics: (trackId: number | string) => ["tracks", trackId, "topics"] as const,
  },
  sessions: {
    detail: (id: number | string) => ["sessions", id] as const,
    report: (id: number | string) => ["sessions", id, "report"] as const,
  },
  me: {
    profile: ["me", "profile"] as const,
    skills: ["me", "skills"] as const,
    gaps: ["me", "skills", "gaps"] as const,
    planToday: ["me", "plan", "today"] as const,
    reviewDue: ["me", "review", "due"] as const,
    usage: ["me", "usage"] as const,
  },
};
