"use client";

import Link from "next/link";
import { Mic, Sparkles, ArrowRight, Clock, ShieldCheck, Check } from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";

export default function InterviewsLobbyPage() {
  return (
    <div className="w-full max-w-[1000px] mx-auto px-6 py-8 flex flex-col gap-8">
      <div className="flex flex-col gap-1.5 border-b border-[#E5E5E5] pb-6">
        <div className="flex items-center gap-2">
          <span className="w-2 h-2 rounded-full bg-[#111111]" />
          <span className="font-mono text-[11px] uppercase tracking-wider text-[#8A8A8A]">
            Technical Interview Simulation • STAR & SDE Rubrics
          </span>
        </div>
        <h2 className="text-[30px] font-semibold text-[#111111] tracking-tight">
          Mock Interview Studio
        </h2>
        <p className="text-[13px] text-[#555555]">
          Experience conversational SDE interviews with adaptive counter-questions, edge case testing, and Tier-1 placement scoring.
        </p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {/* Active Scenario Card */}
        <Card className="border-[#111111]">
          <CardHeader className="pb-3 flex flex-row items-center justify-between">
            <Badge variant="primary">READY TO START</Badge>
            <span className="font-mono text-[11px] text-[#8A8A8A]">Uses 1 AI Credit</span>
          </CardHeader>
          <CardContent className="flex flex-col gap-4">
            <div>
              <h3 className="text-[18px] font-semibold text-[#111111]">
                Java Backend & Distributed State (SDE-1)
              </h3>
              <p className="text-[13px] text-[#555555] mt-1">
                4-stage conversational round: Thread safety primitives, ConcurrentHashMap internals, lock contention, and cache eviction patterns.
              </p>
            </div>

            <div className="flex flex-col gap-2 pt-2 border-t border-[#E5E5E5]/60 text-[12px] text-[#555555]">
              <div className="flex items-center gap-2">
                <Check className="w-4 h-4 text-[#16A34A]" />
                <span>Real-time adaptive follow-ups</span>
              </div>
              <div className="flex items-center gap-2">
                <Check className="w-4 h-4 text-[#16A34A]" />
                <span>Text or speech transcription options</span>
              </div>
              <div className="flex items-center gap-2">
                <Check className="w-4 h-4 text-[#16A34A]" />
                <span>Full diagnostic rubric report upon completion</span>
              </div>
            </div>

            <div className="pt-2 flex items-center justify-between">
              <span className="text-[12px] text-[#8A8A8A] flex items-center gap-1 font-mono">
                <Clock className="w-3.5 h-3.5" /> ~20 mins
              </span>
              <Link href="/interviews/402">
                <Button className="gap-2">
                  <span>Enter Interview Room</span>
                  <ArrowRight className="w-4 h-4" />
                </Button>
              </Link>
            </div>
          </CardContent>
        </Card>

        {/* System Design Round Card */}
        <Card>
          <CardHeader className="pb-3 flex flex-row items-center justify-between">
            <Badge variant="default">COMING NEXT</Badge>
            <span className="font-mono text-[11px] text-[#8A8A8A]">Phase 8</span>
          </CardHeader>
          <CardContent className="flex flex-col gap-4">
            <div>
              <h3 className="text-[18px] font-semibold text-[#111111]">
                High-Scale System Architecture (Campus Core)
              </h3>
              <p className="text-[13px] text-[#555555] mt-1">
                Design a high-throughput URL shortener or Distributed Rate Limiter. Tests microservices boundaries, database partitioning, and Redis caching.
              </p>
            </div>

            <div className="flex flex-col gap-2 pt-2 border-t border-[#E5E5E5]/60 text-[12px] text-[#8A8A8A]">
              <div className="flex items-center gap-2">
                <ShieldCheck className="w-4 h-4" />
                <span>Architecture topology whiteboard integration</span>
              </div>
              <div className="flex items-center gap-2">
                <ShieldCheck className="w-4 h-4" />
                <span>Latency vs Consistency trade-off evaluation</span>
              </div>
            </div>

            <div className="pt-2 flex items-center justify-between">
              <span className="text-[12px] text-[#8A8A8A] font-mono">Prerequisite: 80% on Core Drills</span>
              <Button variant="outline" disabled size="sm">
                Locked
              </Button>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
