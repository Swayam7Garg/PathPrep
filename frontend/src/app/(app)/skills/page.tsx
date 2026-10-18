"use client";

import { useState, Suspense } from "react";
import Link from "next/link";
import { useSearchParams, useRouter } from "next/navigation";
import { useQuery } from "@tanstack/react-query";
import {
  Brain,
  AlertTriangle,
  ArrowRight,
  Sparkles,
  RefreshCw,
  AlertCircle,
  HelpCircle,
  Play,
  Layers,
  BookOpen,
  Info,
} from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Progress } from "@/components/ui/progress";
import { getSkills, getSkillGaps, getTracks } from "@/lib/api";
import { queryKeys } from "@/lib/query-keys";
import { GapKind, SkillStatus, ConfidenceLevel } from "@/lib/types";

function SkillsPageContent() {
  const searchParams = useSearchParams();
  const router = useRouter();

  const trackParam = searchParams.get("trackId");
  const selectedTrackId = trackParam !== null && trackParam !== "" ? Number(trackParam) : 1;
  const isTrackNull = trackParam === "null" || (searchParams.has("trackId") && !trackParam);

  // Tracks Catalog Query
  const { data: tracks = [] } = useQuery({
    queryKey: queryKeys.tracks.all,
    queryFn: getTracks,
  });

  // Skills Query
  const {
    data: skillsData,
    isLoading: skillsLoading,
    isError: skillsError,
    error: skillsErrorObj,
    refetch: refetchSkills,
  } = useQuery({
    queryKey: queryKeys.me.skills(isTrackNull ? null : selectedTrackId),
    queryFn: () => getSkills(isTrackNull ? null : selectedTrackId),
    enabled: !isTrackNull,
  });

  // Gaps Query
  const {
    data: gapsData,
    isLoading: gapsLoading,
    isError: gapsError,
    refetch: refetchGaps,
  } = useQuery({
    queryKey: queryKeys.me.gaps(isTrackNull ? null : selectedTrackId),
    queryFn: () => getSkillGaps(isTrackNull ? null : selectedTrackId),
    enabled: !isTrackNull,
  });

  const handleTrackChange = (trackId: number | null) => {
    if (trackId === null) {
      router.push("/skills?trackId=null");
    } else {
      router.push(`/skills?trackId=${trackId}`);
    }
  };

  // If track is explicitly null
  if (isTrackNull) {
    return (
      <div className="w-full max-w-[1200px] mx-auto px-6 py-12 flex flex-col items-center justify-center min-h-[50vh]">
        <div className="p-8 rounded-xl border border-[#E5E5E5] bg-white max-w-md w-full flex flex-col items-center text-center gap-4">
          <div className="w-12 h-12 rounded-full bg-[#EEEEEE] flex items-center justify-center text-[#111111]">
            <Layers className="w-6 h-6" />
          </div>
          <div className="flex flex-col gap-1">
            <h3 className="text-[18px] font-semibold text-[#111111]">No Career Track Selected</h3>
            <p className="text-[13px] text-[#555555]">
              Please choose a track from the catalog to inspect skill competencies and gap analysis.
            </p>
          </div>
          <div className="flex flex-col gap-2 w-full mt-2">
            {tracks.map((t) => (
              <Button
                key={t.id}
                variant="outline"
                onClick={() => handleTrackChange(t.id)}
                className="w-full justify-between"
              >
                <span>{t.name}</span>
                <ArrowRight className="w-4 h-4" />
              </Button>
            ))}
          </div>
        </div>
      </div>
    );
  }

  // Error State
  if (skillsError || gapsError) {
    return (
      <div className="w-full max-w-[1200px] mx-auto px-6 py-12 flex flex-col items-center justify-center min-h-[50vh]">
        <div className="p-8 rounded-xl border border-[#E5E5E5] bg-white max-w-md w-full flex flex-col items-center text-center gap-4">
          <div className="w-12 h-12 rounded-full bg-[#FEE2E2] flex items-center justify-center text-[#DC2626]">
            <AlertCircle className="w-6 h-6" />
          </div>
          <div className="flex flex-col gap-1">
            <h3 className="text-[18px] font-semibold text-[#111111]">Failed to load skills profile</h3>
            <p className="text-[13px] text-[#555555]">
              {skillsErrorObj instanceof Error ? skillsErrorObj.message : "Unable to retrieve curriculum skills."}
            </p>
          </div>
          <Button
            onClick={() => {
              refetchSkills();
              refetchGaps();
            }}
            className="gap-2 mt-2 w-full"
          >
            <RefreshCw className="w-4 h-4" />
            <span>Retry</span>
          </Button>
        </div>
      </div>
    );
  }

  const topics = skillsData?.topics ?? [];
  const gaps = gapsData?.gaps ?? [];

  return (
    <div className="w-full max-w-[1200px] mx-auto px-6 py-8 flex flex-col gap-8">
      {/* 1. Header & Track Switcher */}
      <div className="flex flex-col gap-4 border-b border-[#E5E5E5] pb-6">
        <div className="flex flex-col md:flex-row md:items-end justify-between gap-4">
          <div className="flex flex-col gap-1.5">
            <div className="flex items-center gap-2">
              <span className="w-2 h-2 rounded-full bg-[#111111]" />
              <span className="font-mono text-[11px] uppercase tracking-wider text-[#8A8A8A]">
                Skill Matrix • Directed Learning Loop
              </span>
            </div>
            <h1 className="text-[30px] font-semibold text-[#111111] tracking-tight">
              Skill Profile & Gap Analysis
            </h1>
            <p className="text-[13px] text-[#555555]">
              Per-topic diagnostic scores and ranked weaknesses. Prerequisites are advisory: you can practice any topic anytime.
            </p>
          </div>

          <Link href="/practice">
            <Button size="sm" className="gap-2 self-start md:self-auto">
              <Play className="w-3.5 h-3.5 fill-current" />
              <span>Practice Lobby</span>
            </Button>
          </Link>
        </div>

        {/* Track Switcher Tabs */}
        <div className="flex items-center gap-2 overflow-x-auto pt-2">
          {tracks.map((track) => {
            const isSelected = track.id === selectedTrackId;
            return (
              <button
                key={track.id}
                type="button"
                onClick={() => handleTrackChange(track.id)}
                className={`px-3.5 py-1.5 rounded-lg text-[13px] font-medium transition-colors whitespace-nowrap flex items-center gap-2 ${
                  isSelected
                    ? "bg-[#111111] text-white shadow-sm"
                    : "bg-[#FAFAFA] border border-[#E5E5E5] text-[#555555] hover:text-[#111111] hover:border-[#111111]"
                }`}
              >
                <span>{track.name}</span>
                <span
                  className={`text-[10px] font-mono px-1.5 py-0.2 rounded-full ${
                    isSelected ? "bg-white/20 text-white" : "bg-[#EEEEEE] text-[#8A8A8A]"
                  }`}
                >
                  {track.topicCount}
                </span>
              </button>
            );
          })}
        </div>
      </div>

      {/* 2. Main Content Grid: Left (Gaps Panel) & Right (Topic List) */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
        {/* Left Column: Gaps Panel (Ranked Deficits) */}
        <div className="lg:col-span-5 flex flex-col gap-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <AlertTriangle className="w-4 h-4 text-[#111111]" />
              <h2 className="text-[18px] font-semibold text-[#111111]">
                Prioritized Skill Gaps
              </h2>
            </div>
            <span className="text-[11px] font-mono text-[#8A8A8A]">
              {gaps.length} gap{gaps.length === 1 ? "" : "s"}
            </span>
          </div>

          {gapsLoading ? (
            <div className="flex flex-col gap-3 animate-pulse">
              {[1, 2, 3].map((i) => (
                <div key={i} className="h-32 rounded-xl border border-[#E5E5E5] bg-[#FAFAFA]" />
              ))}
            </div>
          ) : gaps.length === 0 ? (
            <Card className="p-6 text-center text-[13px] text-[#8A8A8A]">
              No active skill deficits detected in this track! Keep practicing to maintain high mastery.
            </Card>
          ) : (
            <div className="flex flex-col gap-3">
              {gaps.map((gap) => (
                <Card
                  key={gap.topicId}
                  className="p-4 hover:border-[#111111] transition-colors flex flex-col gap-3"
                >
                  <div className="flex items-start justify-between gap-2">
                    <div className="flex flex-col">
                      <span className="text-[15px] font-semibold text-[#111111]">
                        {gap.topicName}
                      </span>
                      <span className="text-[11px] font-mono text-[#8A8A8A]">
                        Weight: {gap.weight}x • Impact Score: {gap.impactScore}
                      </span>
                    </div>

                    <Badge
                      variant={
                        gap.kind === "NOT_STARTED"
                          ? "default"
                          : gap.kind === "WEAK"
                          ? "warning"
                          : "critical"
                      }
                      className="text-[10px]"
                    >
                      {gap.kind === "NOT_STARTED"
                        ? "Start here"
                        : gap.kind === "WEAK"
                        ? "Needs work"
                        : "Prerequisite blocked"}
                    </Badge>
                  </div>

                  <p className="text-[12px] text-[#555555] leading-relaxed">
                    {gap.reason}
                  </p>

                  <div className="pt-2 border-t border-[#E5E5E5]/60 flex items-center justify-between">
                    <span className="text-[11px] font-mono text-[#8A8A8A]">
                      Mastery:{" "}
                      {gap.masteryPercent != null
                        ? `${gap.masteryPercent}%`
                        : "Not started"}
                    </span>

                    <Link
                      href={`/practice?trackId=${selectedTrackId}&topicId=${gap.topicId}&count=${gap.suggestedAction.questionCount}`}
                    >
                      <Button size="sm" variant="outline" className="h-7 text-[12px] px-2.5 gap-1.5">
                        <span>Practice ({gap.suggestedAction.questionCount} Qs)</span>
                        <ArrowRight className="w-3.5 h-3.5" />
                      </Button>
                    </Link>
                  </div>
                </Card>
              ))}
            </div>
          )}
        </div>

        {/* Right Column: Full Topic List */}
        <div className="lg:col-span-7 flex flex-col gap-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <Brain className="w-4 h-4 text-[#111111]" />
              <h2 className="text-[18px] font-semibold text-[#111111]">
                Curriculum Topic Mastery
              </h2>
            </div>
            <span className="text-[11px] font-mono text-[#8A8A8A]">
              {topics.length} topic{topics.length === 1 ? "" : "s"}
            </span>
          </div>

          {skillsLoading ? (
            <div className="flex flex-col gap-3 animate-pulse">
              {[1, 2, 3, 4].map((i) => (
                <div key={i} className="h-24 rounded-xl border border-[#E5E5E5] bg-[#FAFAFA]" />
              ))}
            </div>
          ) : topics.length === 0 ? (
            <Card className="p-8 text-center text-[13px] text-[#8A8A8A]">
              No topics found for this track.
            </Card>
          ) : (
            <div className="flex flex-col gap-2.5">
              {topics.map((topic) => (
                <Card
                  key={topic.topicId}
                  className="p-4 hover:border-[#111111] transition-colors flex flex-col gap-2.5"
                >
                  <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                    <div className="flex flex-col">
                      <div className="flex items-center gap-2">
                        <span className="text-[15px] font-semibold text-[#111111]">
                          {topic.name}
                        </span>
                        <span className="text-[11px] font-mono text-[#8A8A8A] uppercase">
                          • {topic.domain}
                        </span>
                      </div>
                      <span className="text-[11px] font-mono text-[#8A8A8A]">
                        {topic.correctCount} / {topic.attemptsCount} correct attempts
                      </span>
                    </div>

                    <div className="flex items-center gap-2 self-start sm:self-auto">
                      <Badge
                        variant={
                          topic.status === "MASTERED"
                            ? "success"
                            : topic.status === "IN_PROGRESS"
                            ? "warning"
                            : "default"
                        }
                        className="text-[10px]"
                      >
                        {topic.status === "MASTERED"
                          ? "Mastered"
                          : topic.status === "IN_PROGRESS"
                          ? "In Progress"
                          : "Not Started"}
                      </Badge>

                      <Badge
                        variant={
                          topic.confidence === "HIGH"
                            ? "primary"
                            : topic.confidence === "MEDIUM"
                            ? "warning"
                            : "default"
                        }
                        className="text-[10px]"
                      >
                        {topic.confidence} Confidence
                      </Badge>
                    </div>
                  </div>

                  {/* Mastery Progress Bar */}
                  <div className="flex items-center gap-3 pt-1">
                    <div className="flex-1">
                      <Progress
                        value={topic.masteryPercent ?? 0}
                        className={`h-1.5 ${topic.masteryPercent == null ? "bg-[#EEEEEE]" : ""}`}
                      />
                    </div>
                    <span className="font-mono text-[12px] font-medium text-[#111111] w-20 text-right">
                      {topic.masteryPercent != null
                        ? `${topic.masteryPercent}%`
                        : "Not started"}
                    </span>
                  </div>

                  {/* Advisory Prerequisites & Action Button */}
                  <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 pt-2 border-t border-[#E5E5E5]/60">
                    <div className="flex-1">
                      {!topic.prerequisitesMet &&
                      topic.unmetPrerequisites &&
                      topic.unmetPrerequisites.length > 0 ? (
                        <div className="flex items-center gap-1.5 text-[11px] text-[#D97706]">
                          <Info className="w-3.5 h-3.5 shrink-0" />
                          <span>
                            Learn first:{" "}
                            <strong className="font-medium">
                              {topic.unmetPrerequisites.map((p) => p.name).join(", ")}
                            </strong>{" "}
                            (advisory)
                          </span>
                        </div>
                      ) : (
                        <span className="text-[11px] text-[#8A8A8A] font-mono">
                          Weight: {topic.weight}x • {topic.questionCount} published questions
                        </span>
                      )}
                    </div>

                    <Link
                      href={`/practice?trackId=${selectedTrackId}&topicId=${topic.topicId}&count=8`}
                      className="self-end sm:self-auto shrink-0"
                    >
                      <Button variant="outline" size="sm" className="h-7 text-[12px] px-2.5">
                        Practice
                      </Button>
                    </Link>
                  </div>
                </Card>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

export default function SkillsPage() {
  return (
    <Suspense
      fallback={
        <div className="w-full max-w-[1200px] mx-auto px-6 py-8 flex flex-col gap-6 animate-pulse">
          <div className="h-6 w-48 bg-[#EEEEEE] rounded" />
          <div className="h-10 w-72 bg-[#EEEEEE] rounded" />
        </div>
      }
    >
      <SkillsPageContent />
    </Suspense>
  );
}

