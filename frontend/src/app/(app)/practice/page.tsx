"use client";

import { useState, useEffect, Suspense } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import Link from "next/link";
import { useQuery, useMutation } from "@tanstack/react-query";
import {
  Code2,
  Mic,
  Zap,
  Check,
  Sliders,
  Sparkles,
  ArrowRight,
  ShieldCheck,
  HelpCircle,
  AlertCircle,
  RotateCcw,
} from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import {
  getTracks,
  getTopicsForTrack,
  createPracticeSession,
  ApiError,
} from "@/lib/api";
import { queryKeys } from "@/lib/query-keys";
import { SessionMode, QuizDifficulty, SessionStyle } from "@/lib/types";

function PracticeLobbyContent() {
  const router = useRouter();
  const searchParams = useSearchParams();

  // Search params prefill
  const paramTrackId = searchParams.get("trackId");
  const paramTopicId = searchParams.get("topicId");
  const paramCount = searchParams.get("count");

  // Selected State
  const [selectedTrackId, setSelectedTrackId] = useState<number>(
    paramTrackId ? Number(paramTrackId) : 1
  );
  const [selectedMode, setSelectedMode] = useState<SessionMode>("QUIZ");
  const [difficulty, setDifficulty] = useState<QuizDifficulty>("MIXED");
  const [questionCount, setQuestionCount] = useState<number>(
    paramCount ? Number(paramCount) : 8
  );
  const [interviewStyle, setInterviewStyle] = useState<SessionStyle>("LIVE");
  const [selectedTopicIds, setSelectedTopicIds] = useState<number[]>(
    paramTopicId ? [Number(paramTopicId)] : []
  );
  const [activeSessionConflict, setActiveSessionConflict] = useState<number | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  useEffect(() => {
    if (paramTrackId) setSelectedTrackId(Number(paramTrackId));
    if (paramTopicId) setSelectedTopicIds([Number(paramTopicId)]);
    if (paramCount) setQuestionCount(Number(paramCount));
  }, [paramTrackId, paramTopicId, paramCount]);

  // Queries
  const { data: tracks = [] } = useQuery({
    queryKey: queryKeys.tracks.all,
    queryFn: getTracks,
  });

  const { data: topics = [] } = useQuery({
    queryKey: queryKeys.tracks.topics(selectedTrackId),
    queryFn: () => getTopicsForTrack(selectedTrackId),
  });

  // Auto-select valid topics for the selected track when topics load
  useEffect(() => {
    if (!paramTopicId && topics.length > 0) {
      setSelectedTopicIds(topics.map((t) => Number(t.topicId)));
    }
  }, [topics, paramTopicId]);

  // Mutation
  const createSessionMutation = useMutation({
    mutationFn: createPracticeSession,
    onSuccess: (session) => {
      if (session.mode === "QUIZ") {
        router.push(`/practice/quiz/${session.id}`);
      } else {
        router.push(`/interviews/${session.id}`);
      }
    },
    onError: (err: unknown) => {
      if (err instanceof ApiError) {
        if (err.status === 409 && (err.code === "ACTIVE_SESSION_EXISTS" || err.meta?.activeSessionId)) {
          setActiveSessionConflict(Number(err.meta?.activeSessionId));
          setErrorMessage(err.message || "You already have an in-progress session.");
          return;
        }
        setErrorMessage(err.message || "Failed to start session.");
      } else {
        setErrorMessage("An unexpected error occurred while starting the session.");
      }
    },
  });

  const handleToggleTopic = (topicId: number) => {
    setSelectedTopicIds((prev) =>
      prev.includes(topicId)
        ? prev.filter((id) => id !== topicId)
        : [...prev, topicId]
    );
  };

  const handleSelectAllTopics = () => {
    if (selectedTopicIds.length === topics.length) {
      setSelectedTopicIds([]);
    } else {
      setSelectedTopicIds(topics.map((t) => Number(t.topicId)));
    }
  };

  const handleStartSession = () => {
    setErrorMessage(null);
    setActiveSessionConflict(null);
    createSessionMutation.mutate({
      trackId: selectedTrackId,
      mode: selectedMode,
      style: selectedMode === "INTERVIEW" ? interviewStyle : null,
      topicIds: selectedTopicIds.length > 0 ? selectedTopicIds : undefined,
      questionCount,
      difficulty,
    });
  };

  return (
    <div className="w-full max-w-[1200px] mx-auto px-6 py-8 flex flex-col gap-8">
      {/* Page Header */}
      <div className="flex flex-col gap-1.5 border-b border-[#E5E5E5] pb-6">
        <div className="flex items-center gap-2">
          <span className="w-2 h-2 rounded-full bg-[#111111]" />
          <span className="font-mono text-[11px] uppercase tracking-wider text-[#8A8A8A]">
            Practice Configuration • Adaptive Question Engine
          </span>
        </div>
        <h2 className="text-[30px] font-semibold text-[#111111] tracking-tight">
          Practice & Interview Lobby
        </h2>
        <p className="text-[13px] text-[#555555]">
          Customize your diagnostic drill or simulate live technical interviews aligned with college placements.
        </p>
      </div>

      {/* 1. Track Selection Strip */}
      <div className="flex flex-col gap-3">
        <label className="text-[14px] font-medium text-[#111111]">
          Select Career Track
        </label>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          {tracks.map((track) => {
            const isSelected = track.id === selectedTrackId;
            return (
              <div
                key={track.id}
                onClick={() => setSelectedTrackId(track.id)}
                className={`p-4 rounded-xl border transition-colors cursor-pointer flex flex-col justify-between ${
                  isSelected
                    ? "bg-white border-[#111111] ring-1 ring-[#111111]"
                    : "bg-white border-[#E5E5E5] hover:border-[#111111]"
                }`}
              >
                <div>
                  <div className="flex items-center justify-between">
                    <span className="text-[15px] font-semibold text-[#111111]">
                      {track.name}
                    </span>
                    {isSelected && (
                      <span className="w-2 h-2 rounded-full bg-[#111111]" />
                    )}
                  </div>
                  <p className="text-[12px] text-[#555555] mt-1.5 line-clamp-2">
                    {track.description}
                  </p>
                </div>
                <div className="flex items-center gap-3 text-[11px] font-mono text-[#8A8A8A] mt-4 pt-3 border-t border-[#E5E5E5]/60">
                  <span>{track.topicCount} Topics</span>
                  <span>•</span>
                  <span>{track.questionCount} Questions</span>
                </div>
              </div>
            );
          })}
        </div>
      </div>

      {/* 2. Practice Mode Choice (Rapid Drill vs Mock Interview) */}
      <div className="flex flex-col gap-3">
        <label className="text-[14px] font-medium text-[#111111]">
          Select Practice Mode
        </label>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          {/* Rapid Drill MCQ (Zero AI) */}
          <div
            onClick={() => setSelectedMode("QUIZ")}
            className={`p-6 rounded-xl border transition-colors cursor-pointer flex flex-col justify-between ${
              selectedMode === "QUIZ"
                ? "bg-white border-[#111111] ring-1 ring-[#111111]"
                : "bg-white border-[#E5E5E5] hover:border-[#111111]"
            }`}
          >
            <div className="flex flex-col gap-4">
              <div className="flex items-center justify-between">
                <div className="w-9 h-9 rounded-lg bg-[#EEEEEE] flex items-center justify-center text-[#111111]">
                  <Code2 className="w-5 h-5" strokeWidth={1.5} />
                </div>
                <Badge variant="success">Zero-AI · Instant Grading</Badge>
              </div>

              <div>
                <div className="flex items-center gap-2">
                  <h3 className="text-[18px] font-semibold text-[#111111]">Rapid Drill (MCQ)</h3>
                  {selectedMode === "QUIZ" && (
                    <Badge variant="primary" className="text-[10px] py-0">Active</Badge>
                  )}
                </div>
                <p className="text-[13px] text-[#555555] mt-1">
                  Fast-paced, code-graded active recall. Instant Java-evaluated feedback on every option with zero AI quota consumption.
                </p>
              </div>

              <div className="flex flex-col gap-2 pt-2 border-t border-[#E5E5E5]/60 text-[12px] text-[#555555]">
                <div className="flex items-center gap-2">
                  <Check className="w-4 h-4 text-[#16A34A]" />
                  <span>Immediate explanation & benchmark rationale</span>
                </div>
                <div className="flex items-center gap-2">
                  <Check className="w-4 h-4 text-[#16A34A]" />
                  <span>Keyboard shortcuts (A, B, C, D) for velocity</span>
                </div>
                <div className="flex items-center gap-2 font-mono text-[11px] text-[#8A8A8A]">
                  <Zap className="w-3.5 h-3.5 text-[#111111]" />
                  <span>Unlimited runs · 0 credits consumed</span>
                </div>
              </div>
            </div>

            <div className="pt-4 mt-4 border-t border-[#E5E5E5]/60 flex items-center justify-between text-[12px]">
              <span className="text-[#8A8A8A]">Self-paced recall</span>
              <div className={`w-4 h-4 rounded-full border flex items-center justify-center ${selectedMode === "QUIZ" ? "border-[#111111]" : "border-[#C4C7C7]"}`}>
                {selectedMode === "QUIZ" && <div className="w-2 h-2 rounded-full bg-[#111111]" />}
              </div>
            </div>
          </div>

          {/* AI Mock Interview */}
          <div
            onClick={() => setSelectedMode("INTERVIEW")}
            className={`p-6 rounded-xl border transition-colors cursor-pointer flex flex-col justify-between ${
              selectedMode === "INTERVIEW"
                ? "bg-white border-[#111111] ring-1 ring-[#111111]"
                : "bg-white border-[#E5E5E5] hover:border-[#111111]"
            }`}
          >
            <div className="flex flex-col gap-4">
              <div className="flex items-center justify-between">
                <div className="w-9 h-9 rounded-lg bg-[#111111] text-white flex items-center justify-center">
                  <Mic className="w-5 h-5" strokeWidth={1.5} />
                </div>
                <Badge variant="default">AI Evaluated · 1 Credit</Badge>
              </div>

              <div>
                <div className="flex items-center gap-2">
                  <h3 className="text-[18px] font-semibold text-[#111111]">AI Mock Interview</h3>
                  {selectedMode === "INTERVIEW" && (
                    <Badge variant="primary" className="text-[10px] py-0">Active</Badge>
                  )}
                </div>
                <p className="text-[13px] text-[#555555] mt-1">
                  Simulated campus technical round. Dynamic follow-up probes on your answers, edge case challenges, and STAR rubric scoring.
                </p>
              </div>

              <div className="flex flex-col gap-2 pt-2 border-t border-[#E5E5E5]/60 text-[12px] text-[#555555]">
                <div className="flex items-center gap-2">
                  <Check className="w-4 h-4 text-[#111111]" />
                  <span>Adaptive counter-questions & follow-up probes</span>
                </div>
                <div className="flex items-center gap-2">
                  <Check className="w-4 h-4 text-[#111111]" />
                  <span>Detailed rubric breakdown across 4 criteria</span>
                </div>
                <div className="flex items-center gap-2 font-mono text-[11px] text-[#8A8A8A]">
                  <Sparkles className="w-3.5 h-3.5 text-[#111111]" />
                  <span>Uses 1 credit (1 of 2 free today)</span>
                </div>
              </div>
            </div>

            <div className="pt-4 mt-4 border-t border-[#E5E5E5]/60 flex items-center justify-between text-[12px]">
              <span className="text-[#8A8A8A]">Conversational evaluation</span>
              <div className={`w-4 h-4 rounded-full border flex items-center justify-center ${selectedMode === "INTERVIEW" ? "border-[#111111]" : "border-[#C4C7C7]"}`}>
                {selectedMode === "INTERVIEW" && <div className="w-2 h-2 rounded-full bg-[#111111]" />}
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* 3. Session Configuration Panel */}
      <Card>
        <CardHeader className="flex flex-row items-center justify-between pb-4">
          <div>
            <CardTitle className="text-[18px]">Session Configuration</CardTitle>
            <p className="text-[13px] text-[#8A8A8A] mt-1">
              Filter specific topics, calibrate difficulty, and customize session size
            </p>
          </div>
          <div className="flex items-center gap-1.5 px-3 py-1 rounded-lg bg-[#F4F3F3] font-mono text-[11px] text-[#555555]">
            <Sliders className="w-3.5 h-3.5" />
            <span>Adaptive Engine v2.4</span>
          </div>
        </CardHeader>

        <CardContent className="flex flex-col gap-6 pt-2">
          {/* Topic Multi-Select */}
          <div className="flex flex-col gap-2">
            <div className="flex items-center justify-between">
              <div>
                <label className="text-[14px] font-medium text-[#111111]">
                  Filter Topics
                </label>
                <p className="text-[12px] text-[#8A8A8A]">
                  Select one or more topics to target specific gaps
                </p>
              </div>
              <button
                type="button"
                onClick={handleSelectAllTopics}
                className="text-[12px] text-[#555555] hover:text-[#111111] font-medium"
              >
                {selectedTopicIds.length === topics.length ? "Deselect All" : "Select All"}
              </button>
            </div>

            <div className="flex flex-wrap gap-2 pt-1">
              {topics.map((topic) => {
                const isSelected = selectedTopicIds.includes(topic.topicId);
                return (
                  <button
                    key={topic.topicId}
                    type="button"
                    onClick={() => handleToggleTopic(topic.topicId)}
                    className={`px-3 py-1.5 rounded-lg text-[13px] font-medium transition-colors flex items-center gap-1.5 ${
                      isSelected
                        ? "bg-[#111111] text-white"
                        : "bg-white border border-[#E5E5E5] text-[#555555] hover:border-[#111111] hover:text-[#111111]"
                    }`}
                  >
                    {isSelected && <Check className="w-3.5 h-3.5" />}
                    <span>{topic.name}</span>
                  </button>
                );
              })}
            </div>
          </div>

          {/* Difficulty & Question Count Grid */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6 pt-2 border-t border-[#E5E5E5]/60">
            {/* Difficulty */}
            <div className="flex flex-col gap-2">
              <label className="text-[14px] font-medium text-[#111111]">
                Difficulty Level
              </label>
              <div className="grid grid-cols-3 p-1 bg-[#F4F3F3] rounded-lg gap-1">
                {(["EASY", "MIXED", "HARD"] as QuizDifficulty[]).map((level) => (
                  <button
                    key={level}
                    type="button"
                    onClick={() => setDifficulty(level)}
                    className={`py-1.5 rounded text-[13px] font-medium capitalize transition-colors ${
                      difficulty === level
                        ? "bg-white text-[#111111] shadow-sm"
                        : "text-[#555555] hover:text-[#111111]"
                    }`}
                  >
                    {level.toLowerCase()}
                  </button>
                ))}
              </div>
            </div>

            {/* Question Count */}
            <div className="flex flex-col gap-2">
              <label className="text-[14px] font-medium text-[#111111]">
                Question Count
              </label>
              <div className="grid grid-cols-3 gap-2">
                {[5, 8, 10].map((count) => (
                  <button
                    key={count}
                    type="button"
                    onClick={() => setQuestionCount(count)}
                    className={`py-2 px-2 rounded-lg text-[13px] transition-colors text-center ${
                      questionCount === count
                        ? "bg-[#111111] text-white font-medium"
                        : "bg-white border border-[#E5E5E5] text-[#555555] hover:border-[#111111] hover:text-[#111111]"
                    }`}
                  >
                    {count} Questions
                    {count === 8 && (
                      <span className="block text-[10px] text-[#A1A1AA] uppercase tracking-wider font-normal">
                        Standard
                      </span>
                    )}
                  </button>
                ))}
              </div>
            </div>
          </div>

          {/* Interview Pacing (Mock only) */}
          {selectedMode === "INTERVIEW" && (
            <div className="flex flex-col gap-2 pt-2 border-t border-[#E5E5E5]/60">
              <label className="text-[14px] font-medium text-[#111111]">
                Interview Style
              </label>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div
                  onClick={() => setInterviewStyle("LITE")}
                  className={`p-4 rounded-lg border cursor-pointer transition-colors ${
                    interviewStyle === "LITE"
                      ? "bg-[#F4F3F3] border-[#111111]"
                      : "bg-white border-[#E5E5E5] hover:border-[#111111]"
                  }`}
                >
                  <div className="font-medium text-[14px] text-[#111111]">
                    Lite: Comprehensive Rubric at End
                  </div>
                  <p className="text-[12px] text-[#555555] mt-1">
                    Complete your answers uninterrupted. Receive diagnostic scorecards upon submission.
                  </p>
                </div>

                <div
                  onClick={() => setInterviewStyle("LIVE")}
                  className={`p-4 rounded-lg border cursor-pointer transition-colors ${
                    interviewStyle === "LIVE"
                      ? "bg-[#F4F3F3] border-[#111111]"
                      : "bg-white border-[#E5E5E5] hover:border-[#111111]"
                  }`}
                >
                  <div className="flex items-center gap-2">
                    <span className="font-medium text-[14px] text-[#111111]">
                      Live: Adaptive Follow-up Probes
                    </span>
                    <Badge variant="primary" className="text-[10px]">Recommended</Badge>
                  </div>
                  <p className="text-[12px] text-[#555555] mt-1">
                    AI interviewer dynamically probes edge cases and complexity trade-offs after each response.
                  </p>
                </div>
              </div>
            </div>
          )}

          {/* Error / Conflict Banner */}
          {errorMessage && (
            <div className="p-4 rounded-xl border border-[#DC2626] bg-[#FEE2E2]/30 flex flex-col sm:flex-row sm:items-center justify-between gap-3 text-[13px]">
              <div className="flex items-center gap-2 text-[#DC2626]">
                <AlertCircle className="w-5 h-5 shrink-0" />
                <span>{errorMessage}</span>
              </div>
              {activeSessionConflict && (
                <Link href={`/practice/quiz/${activeSessionConflict}`}>
                  <Button size="sm" className="gap-1.5 shrink-0">
                    <RotateCcw className="w-3.5 h-3.5" />
                    <span>Resume Session #{activeSessionConflict}</span>
                  </Button>
                </Link>
              )}
            </div>
          )}

          {/* Action Row */}
          <div className="p-4 rounded-lg bg-[#FAFAFA] border border-[#E5E5E5] flex flex-col sm:flex-row sm:items-center justify-between gap-4 mt-2">
            <div className="flex items-center gap-2 text-[12px] text-[#555555]">
              <span className="w-2 h-2 rounded-full bg-[#111111]" />
              <span>
                Estimated time: ~{questionCount * 2} mins ·{" "}
                {selectedMode === "QUIZ" ? "0 credits (Free)" : "1 AI credit"}
              </span>
            </div>

            <Button
              onClick={handleStartSession}
              disabled={createSessionMutation.isPending || selectedTopicIds.length === 0}
              className="gap-2"
            >
              <span>{createSessionMutation.isPending ? "Generating Session..." : "Start Session"}</span>
              <ArrowRight className="w-4 h-4" />
            </Button>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}

export default function PracticeLobbyPage() {
  return (
    <Suspense
      fallback={
        <div className="w-full max-w-[1200px] mx-auto px-6 py-8 flex flex-col gap-6 animate-pulse">
          <div className="h-6 w-48 bg-[#EEEEEE] rounded" />
          <div className="h-10 w-72 bg-[#EEEEEE] rounded" />
        </div>
      }
    >
      <PracticeLobbyContent />
    </Suspense>
  );
}

