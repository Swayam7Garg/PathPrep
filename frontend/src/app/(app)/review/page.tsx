"use client";

import { useState } from "react";
import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import {
  RotateCcw,
  CheckCircle2,
  Clock,
  History,
  BookOpen,
  ArrowRight,
  Flame,
  Check,
  ChevronDown,
  ChevronUp,
} from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { getReviewQueue } from "@/lib/api";
import { queryKeys } from "@/lib/query-keys";

export default function ReviewQueuePage() {
  const { data: reviewItems = [] } = useQuery({
    queryKey: queryKeys.me.reviewDue,
    queryFn: getReviewQueue,
  });

  const [expandedId, setExpandedId] = useState<string | null>("att-1");
  const [retryText, setRetryText] = useState(
    "computeIfAbsent computes lazily under bin lock, avoiding upfront memory allocations..."
  );
  const [showSolution, setShowSolution] = useState(false);
  const [reviewedIds, setReviewedIds] = useState<string[]>([]);

  const handleMarkResolved = (attemptId: string) => {
    setReviewedIds((prev) => [...prev, attemptId]);
  };

  return (
    <div className="w-full max-w-[1000px] mx-auto px-6 py-8 flex flex-col gap-8">
      {/* Page Header */}
      <div className="flex flex-col gap-1.5 border-b border-[#E5E5E5] pb-6">
        <div className="flex items-center gap-2">
          <span className="w-2 h-2 rounded-full bg-[#111111]" />
          <span className="font-mono text-[11px] uppercase tracking-wider text-[#8A8A8A]">
            Memory Retention • Spaced Repetition Schedule
          </span>
        </div>
        <div className="flex flex-wrap items-baseline justify-between gap-4 mt-1">
          <h2 className="text-[30px] font-semibold text-[#111111] tracking-tight">
            Mistake Revisit Queue
          </h2>
          <div className="flex items-center gap-2">
            <Badge variant="warning">{reviewItems.length - reviewedIds.length} Due Today</Badge>
            <Badge variant="success">{reviewedIds.length} Retained</Badge>
          </div>
        </div>
        <p className="text-[13px] text-[#555555]">
          Questions you missed during previous quiz sessions, scheduled algorithmically by forgetting-curve intervals.
        </p>
      </div>

      {/* Spaced repetition review list */}
      <div className="flex flex-col gap-5">
        {reviewItems.map((item) => {
          const isExpanded = expandedId === item.attemptId;
          const isResolved = reviewedIds.includes(item.attemptId);

          return (
            <Card
              key={item.attemptId}
              className={`transition-colors ${
                isResolved
                  ? "bg-[#FAFAFA] border-[#E5E5E5] opacity-60"
                  : isExpanded
                  ? "border-[#111111]"
                  : "hover:border-[#111111]"
              }`}
            >
              <CardHeader
                className="cursor-pointer pb-4"
                onClick={() => setExpandedId(isExpanded ? null : item.attemptId)}
              >
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                  <div className="flex items-center gap-2">
                    <Badge variant="default" className="text-[11px] font-mono">
                      {item.topicName}
                    </Badge>
                    <span className="text-[12px] font-mono text-[#8A8A8A]">
                      • {item.lastAttemptedAt}
                    </span>
                  </div>

                  <div className="flex items-center gap-3">
                    <Badge variant={item.mistakeCount > 1 ? "critical" : "warning"}>
                      {item.mistakeCount} Past Mistake{item.mistakeCount > 1 ? "s" : ""}
                    </Badge>
                    <span className="font-mono text-[11px] text-[#555555]">
                      Interval: {item.intervalDays}d
                    </span>
                    {isExpanded ? (
                      <ChevronUp className="w-4 h-4 text-[#8A8A8A]" />
                    ) : (
                      <ChevronDown className="w-4 h-4 text-[#8A8A8A]" />
                    )}
                  </div>
                </div>

                <h3 className="text-[16px] font-semibold text-[#111111] mt-2 leading-snug">
                  {item.questionBody}
                </h3>
              </CardHeader>

              {/* Expanded active recall retry interactive console */}
              {isExpanded && !isResolved && (
                <CardContent className="flex flex-col gap-4 pt-2 border-t border-[#E5E5E5]/60">
                  <div className="p-3 rounded-lg bg-[#FAFAFA] border border-[#E5E5E5] flex items-center gap-2 text-[12px] text-[#555555]">
                    <History className="w-4 h-4 text-[#DC2626]" />
                    <span>
                      Previous response failed on: <em>Lazy evaluation vs upfront value object instantiation</em>
                    </span>
                  </div>

                  <div className="flex flex-col gap-2">
                    <label className="text-[13px] font-medium text-[#111111]">
                      Active Recall (Formulate your mental model from memory):
                    </label>
                    <textarea
                      rows={3}
                      value={retryText}
                      onChange={(e) => setRetryText(e.target.value)}
                      className="w-full p-3 text-[13px] rounded-lg border border-[#E5E5E5] focus:border-[#111111] focus:outline-none bg-white font-sans text-[#111111]"
                    />
                  </div>

                  <div className="flex items-center justify-between pt-1">
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => setShowSolution(!showSolution)}
                      className="gap-1.5"
                    >
                      <BookOpen className="w-3.5 h-3.5" />
                      <span>{showSolution ? "Hide Model Answer" : "Reveal Benchmark Solution"}</span>
                    </Button>

                    <Button
                      size="sm"
                      onClick={() => handleMarkResolved(item.attemptId)}
                      className="gap-1.5"
                    >
                      <Check className="w-3.5 h-3.5" />
                      <span>Mark Retained (+5d Interval)</span>
                    </Button>
                  </div>

                  {showSolution && (
                    <div className="p-4 rounded-xl bg-[#F4F3F3] border border-[#E5E5E5] text-[13px] text-[#111111] leading-relaxed">
                      <span className="font-semibold block mb-1">
                        SDE-2 Engineering Solution Benchmark:
                      </span>
                      <p>
                        <code>computeIfAbsent(K, Function)</code> establishes a per-bin node lock using synchronized on the bucket head only when the key is missing, and lazily invokes the mappingFunction once. In contrast, <code>putIfAbsent</code> requires the value object to be constructed upfront before method invocation.
                      </p>
                    </div>
                  )}
                </CardContent>
              )}
            </Card>
          );
        })}
      </div>
    </div>
  );
}
