"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import {
  Mic,
  ArrowRight,
  Clock,
  ShieldCheck,
  Check,
  Zap,
  Key,
  BookOpen,
  AlertCircle,
  HelpCircle,
  Radio,
  Layers,
} from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { useQuery } from "@tanstack/react-query";
import { getAiUsage, getTracks, createSession } from "@/lib/api";
import { queryKeys } from "@/lib/query-keys";
import { QuotaModal } from "@/components/interviews/quota-modal";
import { TrackSummary } from "@/lib/types";

export default function InterviewsLobbyPage() {
  const router = useRouter();
  const [quotaModalOpen, setQuotaModalOpen] = useState(false);
  const [selectedStyle, setSelectedStyle] = useState<"LITE" | "LIVE">("LIVE");
  const [startingTrackId, setStartingTrackId] = useState<number | null>(null);
  const [errorState, setErrorState] = useState<{
    code: string;
    message: string;
    suggestedStyle?: "LITE" | "LIVE";
    resetAt?: string;
  } | null>(null);

  const { data: usage } = useQuery({
    queryKey: queryKeys.me.aiUsage,
    queryFn: getAiUsage,
  });

  const { data: tracks = [], isLoading: isLoadingTracks } = useQuery({
    queryKey: queryKeys.tracks.all,
    queryFn: getTracks,
  });

  const isUsingOwnKey = usage?.usingOwnKey ?? usage?.hasCustomKey ?? false;
  const interviewLimit = usage?.interviews?.limit ?? (isUsingOwnKey ? null : (usage?.dailyQuota ?? 2));
  const interviewUsed = usage?.interviews?.used ?? usage?.usedToday ?? 0;
  const isQuotaDepleted = Boolean(
    !isUsingOwnKey && interviewLimit !== null && interviewUsed >= interviewLimit
  );

  const handleStartInterview = async (
    trackId: number,
    styleToUse: "LITE" | "LIVE" = selectedStyle
  ) => {
    if (isQuotaDepleted && !isUsingOwnKey) {
      setQuotaModalOpen(true);
      return;
    }

    setErrorState(null);
    setStartingTrackId(trackId);

    try {
      const session = await createSession({
        mode: "INTERVIEW",
        trackId,
        style: styleToUse,
      });
      router.push(`/interviews/${session.id}`);
    } catch (err: any) {
      const code = err?.code || "UNKNOWN";
      const message = err?.message || "Failed to start interview session";
      const suggestedStyle = err?.meta?.suggestedStyle as "LITE" | "LIVE" | undefined;
      const resetAt = err?.meta?.resetAt || usage?.resetAt;

      if (code === "AI_QUOTA_EXCEEDED") {
        setErrorState({
          code,
          message: "Your daily AI interview quota is used up.",
          suggestedStyle,
          resetAt,
        });
        setQuotaModalOpen(true);
      } else if (code === "AI_POOL_EXHAUSTED") {
        setErrorState({
          code,
          message:
            "The shared AI capacity is currently busy under heavy load. Please try again shortly or use your own Groq key for immediate dedicated inference.",
        });
      } else if (code === "AI_UNAVAILABLE") {
        setErrorState({
          code,
          message:
            "AI interview evaluation service is temporarily unavailable. Please try again later or practice with self-paced quizzes.",
        });
      } else if (code === "INVALID_API_KEY") {
        setErrorState({
          code,
          message:
            "Your configured Groq API key is invalid or expired. Please update or test your key in Settings.",
        });
      } else if (code === "RATE_LIMITED") {
        setErrorState({
          code,
          message:
            "Too many requests. Please wait a moment before starting another interview.",
        });
      } else {
        setErrorState({
          code,
          message,
        });
      }
    } finally {
      setStartingTrackId(null);
    }
  };

  return (
    <div className="w-full max-w-[1000px] mx-auto px-6 py-8 flex flex-col gap-8">
      <QuotaModal
        open={quotaModalOpen}
        onClose={() => setQuotaModalOpen(false)}
        resetAt={errorState?.resetAt || usage?.resetAt || "Midnight UTC (05:30 AM IST)"}
        suggestedStyle={errorState?.suggestedStyle}
        onSelectSuggestedStyle={(style) => {
          setSelectedStyle(style);
          if (tracks.length > 0) {
            handleStartInterview(tracks[0].id, style);
          }
        }}
      />

      {/* Header */}
      <div className="flex flex-col gap-1.5 border-b border-[#E5E5E5] pb-6">
        <div className="flex items-center gap-2">
          <span className="w-2 h-2 rounded-full bg-[#111111]" />
          <span className="font-mono text-[11px] uppercase tracking-wider text-[#8A8A8A]">
            Technical Interview Studio • STAR &amp; SDE Rubrics
          </span>
        </div>
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div>
            <h2 className="text-[30px] font-semibold text-[#111111] tracking-tight">
              Mock Interview Studio
            </h2>
            <p className="text-[13px] text-[#555555] mt-0.5">
              Experience conversational technical interviews with adaptive counter-questions, edge case probing, and structured rubrics.
            </p>
          </div>

          {/* AI Quota Pill */}
          <div className="flex items-center gap-2.5 px-3.5 py-1.5 rounded-full border border-[#E5E5E5] bg-white self-start sm:self-auto shrink-0 shadow-xs">
            <Zap
              className={`w-3.5 h-3.5 ${
                isUsingOwnKey
                  ? "text-[#16A34A]"
                  : isQuotaDepleted
                  ? "text-[#DC2626]"
                  : "text-[#D97706]"
              }`}
            />
            <span className="font-mono text-[11px] text-[#111111]">
              {isUsingOwnKey
                ? "Unlimited AI (Custom Key)"
                : interviewLimit === null
                ? "Unlimited AI"
                : `${Math.max(0, interviewLimit - interviewUsed)} of ${interviewLimit} interviews left today`}
            </span>
            {isQuotaDepleted && (
              <button
                type="button"
                onClick={() => setQuotaModalOpen(true)}
                className="text-[10px] text-[#DC2626] font-semibold underline ml-1 cursor-pointer"
              >
                Unlock
              </button>
            )}
          </div>
        </div>
      </div>

      {/* Error state banners */}
      {errorState && (
        <div className="p-4 rounded-xl bg-[#FEF2F2] border border-[#FEE2E2] flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
          <div className="flex items-start gap-3">
            <AlertCircle className="w-5 h-5 text-[#DC2626] shrink-0 mt-0.5" />
            <div className="flex flex-col">
              <span className="text-[14px] font-semibold text-[#991B1B]">
                {errorState.code === "AI_QUOTA_EXCEEDED"
                  ? "Your daily AI is used up"
                  : errorState.code === "AI_POOL_EXHAUSTED"
                  ? "Shared AI is busy"
                  : errorState.code === "INVALID_API_KEY"
                  ? "Invalid Groq API Key"
                  : "Interview Start Error"}
              </span>
              <span className="text-[12px] text-[#7F1D1D] mt-0.5">
                {errorState.message}
                {errorState.resetAt && ` Resets at: ${new Date(errorState.resetAt).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })}.`}
              </span>
            </div>
          </div>

          <div className="flex items-center gap-2.5 shrink-0 flex-wrap">
            {errorState.suggestedStyle && (
              <Button
                size="sm"
                onClick={() => {
                  const s = errorState.suggestedStyle!;
                  setSelectedStyle(s);
                  setErrorState(null);
                  if (tracks.length > 0) {
                    handleStartInterview(tracks[0].id, s);
                  }
                }}
                className="gap-1.5 text-xs bg-[#16A34A] text-white hover:bg-[#15803D]"
              >
                <span>Switch to {errorState.suggestedStyle} Style</span>
              </Button>
            )}
            <Link href="/practice">
              <Button size="sm" variant="outline" className="gap-1.5 text-xs bg-white">
                <BookOpen className="w-3.5 h-3.5 text-[#555555]" />
                <span>Zero-AI Quizzes</span>
              </Button>
            </Link>
            <Link href="/settings">
              <Button size="sm" variant="outline" className="gap-1.5 text-xs bg-white">
                <Key className="w-3.5 h-3.5 text-[#555555]" />
                <span>Use Own Key</span>
              </Button>
            </Link>
          </div>
        </div>
      )}

      {/* Style Selector */}
      <div className="p-4 rounded-xl bg-[#FAFAFA] border border-[#E5E5E5] flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div className="flex flex-col">
          <span className="text-[14px] font-semibold text-[#111111] flex items-center gap-2">
            <Radio className="w-4 h-4 text-[#111111]" />
            Interview Simulation Style
          </span>
          <span className="text-[12px] text-[#555555] mt-0.5">
            Choose between fast lightweight questioning or full conversational live simulation.
          </span>
        </div>

        <div className="flex items-center gap-2 bg-white p-1 rounded-lg border border-[#E5E5E5]">
          <button
            type="button"
            onClick={() => setSelectedStyle("LIVE")}
            className={`px-3 py-1.5 rounded-md text-xs font-medium transition-colors ${
              selectedStyle === "LIVE"
                ? "bg-[#111111] text-white shadow-xs"
                : "text-[#555555] hover:text-[#111111]"
            }`}
          >
            LIVE (Adaptive Conversational)
          </button>
          <button
            type="button"
            onClick={() => setSelectedStyle("LITE")}
            className={`px-3 py-1.5 rounded-md text-xs font-medium transition-colors ${
              selectedStyle === "LITE"
                ? "bg-[#111111] text-white shadow-xs"
                : "text-[#555555] hover:text-[#111111]"
            }`}
          >
            LITE (Standard Structured)
          </button>
        </div>
      </div>

      {/* Dynamic Tracks Grid from GET /tracks */}
      {isLoadingTracks ? (
        <div className="p-12 text-center text-xs text-[#8A8A8A] font-mono">
          Loading interview tracks from server...
        </div>
      ) : tracks.length === 0 ? (
        <div className="p-12 text-center text-xs text-[#8A8A8A] border border-dashed border-[#E5E5E5] rounded-xl">
          No tracks currently configured on backend.
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          {tracks.map((track) => {
            const questionCount = track.interviewQuestionCount ?? 3;
            const isStarting = startingTrackId === track.id;

            return (
              <Card
                key={track.id}
                className="border-[#E5E5E5] hover:border-[#111111] transition-all flex flex-col justify-between"
              >
                <CardHeader className="pb-3 flex flex-row items-center justify-between">
                  <Badge variant="primary">READY TO START</Badge>
                  <span className="font-mono text-[11px] text-[#8A8A8A]">
                    {isUsingOwnKey ? "Unlimited" : "Uses 1 Session"}
                  </span>
                </CardHeader>

                <CardContent className="flex flex-col gap-4">
                  <div>
                    <h3 className="text-[18px] font-semibold text-[#111111]">
                      {track.name}
                    </h3>
                    <p className="text-[13px] text-[#555555] mt-1 leading-relaxed">
                      {track.description ||
                        "Comprehensive interview round evaluating core data structures, algorithms, system concepts, and trade-off articulation."}
                    </p>
                  </div>

                  <div className="flex flex-col gap-2 pt-2 border-t border-[#E5E5E5]/60 text-[12px] text-[#555555]">
                    <div className="flex items-center gap-2">
                      <Check className="w-4 h-4 text-[#16A34A]" />
                      <span>{questionCount} core questions with adaptive counter-probing</span>
                    </div>
                    <div className="flex items-center gap-2">
                      <Check className="w-4 h-4 text-[#16A34A]" />
                      <span>Speech transcription &amp; code editor input support</span>
                    </div>
                    <div className="flex items-center gap-2">
                      <Check className="w-4 h-4 text-[#16A34A]" />
                      <span>Comprehensive multi-criteria scorecard upon completion</span>
                    </div>
                  </div>

                  <div className="pt-2 flex items-center justify-between border-t border-[#E5E5E5]/60">
                    <span className="text-[12px] text-[#8A8A8A] flex items-center gap-1 font-mono">
                      <Clock className="w-3.5 h-3.5" /> ~{questionCount * 5} mins • {questionCount} Questions
                    </span>
                    <Button
                      onClick={() => handleStartInterview(track.id)}
                      disabled={isStarting}
                      className="gap-2"
                    >
                      <span>{isStarting ? "Initializing..." : "Enter Interview Room"}</span>
                      <ArrowRight className="w-4 h-4" />
                    </Button>
                  </div>
                </CardContent>
              </Card>
            );
          })}
        </div>
      )}

      {/* Privacy Notice in Lobby (Requirement 8) */}
      <div className="p-4 rounded-xl bg-[#FAFAFA] border border-[#E5E5E5] text-[12px] text-[#555555] flex flex-col gap-2">
        <div className="flex items-center gap-2 text-[#111111] font-semibold">
          <ShieldCheck className="w-4 h-4" />
          <span>Privacy &amp; Audio Input Notice</span>
        </div>
        <p className="leading-relaxed">
          Interview responses and transcripts are transmitted to an AI provider (Groq / Llama) solely for grading, follow-up probe generation, and rubric scorecards. No personally identifying information is shared in inference prompts.
        </p>
        <p className="leading-relaxed text-[#8A8A8A]">
          If you choose voice input, browser speech recognition (Web Speech API) may process spoken audio via your browser vendor&apos;s speech servers (such as Google in Chrome). Keyboard text input is always available as a fully private fallback.
        </p>
      </div>
    </div>
  );
}
