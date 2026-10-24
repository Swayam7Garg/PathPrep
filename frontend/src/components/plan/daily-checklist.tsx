"use client";

import React, { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {
  Check,
  CheckCircle2,
  Plus,
  Trash2,
  Clock,
  RotateCcw,
  Sparkles,
  BookOpen,
  ArrowRight,
  ListTodo,
  AlertCircle,
  X,
  Target,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Progress } from "@/components/ui/progress";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import {
  getUserDailyPlan,
  addDailyPlanItem,
  toggleDailyPlanItem,
  deleteDailyPlanItem,
  rolloverYesterdayDailyPlan,
  getTracks,
  getTopicsForTrack,
} from "@/lib/api";
import { queryKeys } from "@/lib/query-keys";
import {
  DailyPlanItemResponse,
  UserDailyPlanResponse,
  CreateDailyPlanItemRequest,
} from "@/lib/types";
import { useAuth } from "@/components/providers/auth-provider";

export function DailyChecklist() {
  const router = useRouter();
  const queryClient = useQueryClient();
  const { user } = useAuth();

  const [modalOpen, setModalOpen] = useState(false);
  const [taskType, setTaskType] = useState<"CUSTOM" | "QUIZ">("CUSTOM");
  const [customTitle, setCustomTitle] = useState("");
  const [selectedTopicId, setSelectedTopicId] = useState<string>("");
  const [questionCount, setQuestionCount] = useState<number>(5);
  const [formError, setFormError] = useState<string | null>(null);

  // Queries
  const { data: planData, isLoading } = useQuery<UserDailyPlanResponse>({
    queryKey: queryKeys.me.userPlan,
    queryFn: getUserDailyPlan,
  });

  const targetTrackId = user?.targetTrackId || 1;

  const { data: topics = [] } = useQuery({
    queryKey: queryKeys.tracks.topics(targetTrackId),
    queryFn: () => getTopicsForTrack(targetTrackId),
    enabled: modalOpen && taskType === "QUIZ",
  });

  // Mutations
  const addItemMutation = useMutation({
    mutationFn: addDailyPlanItem,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.me.userPlan });
      setModalOpen(false);
      setCustomTitle("");
      setSelectedTopicId("");
      setQuestionCount(5);
      setFormError(null);
    },
    onError: (err: unknown) => {
      const msg = err instanceof Error ? err.message : "Failed to add task";
      setFormError(msg);
    },
  });

  const toggleItemMutation = useMutation({
    mutationFn: toggleDailyPlanItem,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.me.userPlan });
    },
  });

  const deleteItemMutation = useMutation({
    mutationFn: deleteDailyPlanItem,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.me.userPlan });
    },
  });

  const rolloverMutation = useMutation({
    mutationFn: rolloverYesterdayDailyPlan,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.me.userPlan });
    },
  });

  const handleCreateTask = (e: React.FormEvent) => {
    e.preventDefault();
    setFormError(null);

    if (taskType === "CUSTOM") {
      const trimmed = customTitle.trim();
      if (!trimmed) {
        setFormError("Task description is required.");
        return;
      }
      addItemMutation.mutate({
        itemType: "CUSTOM",
        title: trimmed,
      });
    } else {
      if (!selectedTopicId) {
        setFormError("Please select a topic for the practice quiz.");
        return;
      }
      addItemMutation.mutate({
        itemType: "QUIZ",
        trackId: Number(targetTrackId),
        topicId: Number(selectedTopicId),
        questionCount,
      });
    }
  };

  const items = planData?.items || [];
  const totalItems = planData?.totalItems || 0;
  const completedItems = planData?.completedItems || 0;
  const progressPercent = planData?.progressPercent || 0;
  const hasRollover = planData?.hasUnfinishedYesterdayTasks || false;

  return (
    <Card className="border-[#E5E5E5] bg-white shadow-xs">
      {/* Card Header & Dynamic Goal Bar */}
      <CardHeader className="pb-3 border-b border-[#E5E5E5]/60 flex flex-col gap-3">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
          <div className="flex items-center gap-2">
            <ListTodo className="w-5 h-5 text-[#111111]" />
            <CardTitle className="text-[18px] font-semibold text-[#111111]">
              Today&apos;s Plan &amp; Checklist
            </CardTitle>
          </div>

          <div className="flex items-center gap-2.5">
            <span className="font-mono text-[12px] font-medium text-[#111111] bg-[#FAFAFA] px-2.5 py-1 rounded-md border border-[#E5E5E5]">
              {totalItems > 0
                ? `${completedItems} of ${totalItems} completed • ${progressPercent}%`
                : "0 items scheduled"}
            </span>

            <Button
              size="sm"
              onClick={() => setModalOpen(true)}
              className="gap-1.5 text-xs py-1.5"
            >
              <Plus className="w-3.5 h-3.5" />
              <span>Add Task</span>
            </Button>
          </div>
        </div>

        {/* Progress Bar */}
        {totalItems > 0 && (
          <div className="w-full">
            <Progress value={progressPercent} className="h-1.5" />
          </div>
        )}
      </CardHeader>

      <CardContent className="pt-4 flex flex-col gap-4">
        {/* Smart Rollover Banner */}
        {hasRollover && (
          <div className="p-3.5 rounded-xl bg-[#FEF3C7]/40 border border-[#FDE68A] flex flex-col sm:flex-row sm:items-center justify-between gap-3 text-xs text-[#92400E]">
            <div className="flex items-center gap-2">
              <RotateCcw className="w-4 h-4 text-[#D97706] shrink-0" />
              <span>You have unfinished tasks from yesterday. Carry them over to today?</span>
            </div>
            <Button
              size="sm"
              variant="outline"
              onClick={() => rolloverMutation.mutate()}
              disabled={rolloverMutation.isPending}
              className="text-xs bg-white text-[#92400E] border-[#FDE68A] hover:bg-[#FEF3C7] shrink-0"
            >
              {rolloverMutation.isPending ? "Rolling over..." : "Carry Over to Today"}
            </Button>
          </div>
        )}

        {/* Items List */}
        {isLoading ? (
          <div className="flex flex-col gap-2 py-4">
            <div className="h-10 bg-[#FAFAFA] rounded-lg animate-pulse" />
            <div className="h-10 bg-[#FAFAFA] rounded-lg animate-pulse" />
          </div>
        ) : items.length === 0 ? (
          <div className="py-8 text-center flex flex-col items-center justify-center gap-2 text-[#8A8A8A]">
            <Target className="w-8 h-8 text-[#CCCCCC]" />
            <p className="text-[13px] font-medium text-[#555555]">
              No goals scheduled for today yet.
            </p>
            <p className="text-[12px] max-w-sm">
              Define custom study milestones or schedule in-app quizzes to structure your placement preparation.
            </p>
            <Button
              variant="outline"
              size="sm"
              onClick={() => setModalOpen(true)}
              className="mt-2 text-xs gap-1.5"
            >
              <Plus className="w-3.5 h-3.5" />
              <span>Schedule First Task</span>
            </Button>
          </div>
        ) : (
          <div className="flex flex-col divide-y divide-[#E5E5E5]/60">
            {items.map((item) => (
              <div
                key={item.id}
                className="py-3 flex items-start justify-between gap-3 group transition-colors hover:bg-[#FAFAFA] -mx-4 px-4 rounded-lg"
              >
                {/* Left: Checkbox + Title */}
                <div className="flex items-start gap-3 flex-1">
                  <button
                    type="button"
                    onClick={() => toggleItemMutation.mutate(item.id)}
                    className={`w-5 h-5 rounded-md border mt-0.5 flex items-center justify-center transition-colors cursor-pointer shrink-0 ${
                      item.completed
                        ? "bg-[#111111] border-[#111111] text-white"
                        : "border-[#CCCCCC] bg-white hover:border-[#111111]"
                    }`}
                  >
                    {item.completed && <Check className="w-3.5 h-3.5 stroke-[2.5]" />}
                  </button>

                  <div className="flex flex-col gap-0.5">
                    <span
                      className={`text-[14px] leading-snug transition-all ${
                        item.completed
                          ? "line-through text-[#8A8A8A]"
                          : "text-[#111111] font-medium"
                      }`}
                    >
                      {item.title}
                    </span>

                    <div className="flex items-center gap-2 text-[11px] font-mono text-[#8A8A8A]">
                      <Badge
                        variant={item.itemType === "QUIZ" ? "default" : "outline"}
                        className="text-[9px] py-0 px-1.5"
                      >
                        {item.itemType === "QUIZ" ? "Quiz Assignment" : "Custom Goal"}
                      </Badge>
                      {item.topicName && <span>• {item.topicName}</span>}
                      {item.questionCount && <span>• {item.questionCount} Questions</span>}
                    </div>
                  </div>
                </div>

                {/* Right: Actions */}
                <div className="flex items-center gap-2 shrink-0">
                  {item.itemType === "QUIZ" && !item.completed && item.topicId && (
                    <Link
                      href={`/practice?trackId=${item.trackId || targetTrackId}&topicId=${item.topicId}&count=${item.questionCount || 5}`}
                    >
                      <Button size="sm" variant="outline" className="gap-1.5 text-xs py-1 h-7">
                        <span>Take Quiz</span>
                        <ArrowRight className="w-3 h-3" />
                      </Button>
                    </Link>
                  )}

                  {item.completed && (
                    <span className="text-[11px] text-[#16A34A] flex items-center gap-1 font-mono">
                      <CheckCircle2 className="w-3.5 h-3.5" /> Done
                    </span>
                  )}

                  <button
                    type="button"
                    onClick={() => deleteItemMutation.mutate(item.id)}
                    className="p-1 rounded text-[#8A8A8A] hover:text-[#DC2626] opacity-0 group-hover:opacity-100 transition-opacity"
                    title="Delete item"
                  >
                    <Trash2 className="w-3.5 h-3.5" />
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </CardContent>

      {/* Add Task Modal */}
      {modalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/40 backdrop-blur-xs animate-in fade-in duration-150">
          <div className="w-full max-w-md bg-white rounded-2xl border border-[#111111] shadow-2xl p-6 flex flex-col gap-4 relative">
            <button
              type="button"
              onClick={() => setModalOpen(false)}
              className="absolute right-4 top-4 p-1 text-[#8A8A8A] hover:text-[#111111] transition-colors"
            >
              <X className="w-4 h-4" />
            </button>

            <div className="flex flex-col gap-1">
              <h3 className="text-[17px] font-semibold text-[#111111]">
                Schedule Task for Today
              </h3>
              <p className="text-[12px] text-[#555555]">
                Plan your placement preparation timetable.
              </p>
            </div>

            {/* Type selector tabs */}
            <div className="grid grid-cols-2 p-1 rounded-lg bg-[#F4F3F3] text-[13px] font-medium text-center">
              <button
                type="button"
                onClick={() => setTaskType("CUSTOM")}
                className={`py-1.5 rounded-md transition-all ${
                  taskType === "CUSTOM"
                    ? "bg-white text-[#111111] shadow-xs"
                    : "text-[#555555] hover:text-[#111111]"
                }`}
              >
                Custom Goal
              </button>
              <button
                type="button"
                onClick={() => setTaskType("QUIZ")}
                className={`py-1.5 rounded-md transition-all ${
                  taskType === "QUIZ"
                    ? "bg-white text-[#111111] shadow-xs"
                    : "text-[#555555] hover:text-[#111111]"
                }`}
              >
                In-App Quiz Drill
              </button>
            </div>

            <form onSubmit={handleCreateTask} className="flex flex-col gap-3.5">
              {formError && (
                <div className="p-2.5 rounded-lg bg-[#FEE2E2] text-xs text-[#991B1B]">
                  {formError}
                </div>
              )}

              {taskType === "CUSTOM" ? (
                <div className="flex flex-col gap-1.5">
                  <label className="text-[12px] font-medium text-[#111111]">
                    Task Title / Objective
                  </label>
                  <input
                    type="text"
                    required
                    value={customTitle}
                    onChange={(e) => setCustomTitle(e.target.value)}
                    placeholder="e.g. Solve 3 LeetCode dynamic programming questions"
                    className="px-3.5 py-2.5 rounded-lg border border-[#E5E5E5] text-[13px] focus:outline-none focus:border-[#111111]"
                  />
                  <span className="text-[11px] text-[#8A8A8A]">
                    Freeform self-directed study goal checked off manually.
                  </span>
                </div>
              ) : (
                <div className="flex flex-col gap-3">
                  <div className="flex flex-col gap-1.5">
                    <label className="text-[12px] font-medium text-[#111111]">
                      Select Topic from Current Track
                    </label>
                    <select
                      value={selectedTopicId}
                      onChange={(e) => setSelectedTopicId(e.target.value)}
                      className="px-3 py-2 rounded-lg border border-[#E5E5E5] text-[13px] bg-white focus:outline-none focus:border-[#111111]"
                    >
                      <option value="" disabled>Select a topic</option>
                      {topics.map((t) => (
                        <option key={t.topicId} value={t.topicId}>
                          {t.name} ({t.questionCount} Qs)
                        </option>
                      ))}
                    </select>
                  </div>

                  <div className="flex flex-col gap-1.5">
                    <label className="text-[12px] font-medium text-[#111111]">
                      Question Count (1 – 20)
                    </label>
                    <input
                      type="number"
                      min={1}
                      max={20}
                      value={questionCount}
                      onChange={(e) => setQuestionCount(Number(e.target.value))}
                      className="px-3 py-2 rounded-lg border border-[#E5E5E5] text-[13px] focus:outline-none focus:border-[#111111]"
                    />
                  </div>
                  <span className="text-[11px] text-[#8A8A8A]">
                    Auto-completes automatically when you finish the quiz session!
                  </span>
                </div>
              )}

              <div className="flex items-center justify-end gap-2 pt-2 border-t border-[#E5E5E5]">
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  onClick={() => setModalOpen(false)}
                >
                  Cancel
                </Button>
                <Button
                  type="submit"
                  size="sm"
                  disabled={addItemMutation.isPending}
                >
                  <span>{addItemMutation.isPending ? "Adding..." : "Add to Today's Plan"}</span>
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}
    </Card>
  );
}
