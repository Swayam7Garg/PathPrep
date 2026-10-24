export const queryKeys = {
  tracks: {
    all: ["tracks"] as const,
    topics: (trackId: number | string) => ["tracks", trackId, "topics"] as const,
  },
  sessions: {
    detail: (id: number | string) => ["sessions", id] as const,
    report: (id: number | string) => ["sessions", id, "report"] as const,
    history: (params?: { page?: number; size?: number; status?: string; mode?: string; trackId?: number }) =>
      ["sessions", "history", params] as const,
  },
  me: {
    profile: ["me", "profile"] as const,
    skills: (trackId?: number | null) => ["me", "skills", trackId ?? "default"] as const,
    gaps: (trackId?: number | null, limit?: number) => ["me", "skills", "gaps", trackId ?? "default", limit ?? 5] as const,
    planToday: ["me", "plan", "today"] as const,
    userPlan: ["me", "plan", "user"] as const,
    reviewDue: (limit?: number) => ["me", "review", "due", limit ?? 20] as const,
    progress: (range?: number) => ["me", "progress", range ?? 30] as const,
    usage: ["me", "usage"] as const,
    aiUsage: ["me", "usage"] as const,
  },
};
