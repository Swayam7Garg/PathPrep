"use client";

import { useState, useEffect, use } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import {
  Clock,
  Send,
  Sparkles,
  Bot,
  User,
  ArrowRight,
  HelpCircle,
  Lightbulb,
  CheckCircle2,
  Mic,
  MicOff,
  LogOut,
  ChevronRight,
} from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Progress } from "@/components/ui/progress";

interface Message {
  id: string;
  sender: "ai" | "user";
  time: string;
  text: string;
  tag?: string;
  evaluates?: string;
  duration?: string;
}

const INITIAL_MESSAGES: Message[] = [
  {
    id: "1",
    sender: "ai",
    time: "09:12 AM",
    text: "Let's discuss handling concurrent state in Java. How would you design a thread-safe in-memory cache without locking the entire map on every write?",
    evaluates: "Thread safety primitives, lock granularity, cache eviction",
  },
  {
    id: "2",
    sender: "user",
    time: "09:14 AM",
    text: "I would utilize ConcurrentHashMap which relies on bucket-level striping and CAS (Compare-And-Swap) operations rather than synchronized blocks. For cache eviction like LRU, we can combine it with Caffeine's Window TinyLFU mechanism to avoid global lock contention.",
    duration: "Delivered in 2m 14s",
  },
  {
    id: "3",
    sender: "ai",
    time: "09:15 AM",
    text: "Good point on ConcurrentHashMap bucket locks. Now follow-up: What happens if two threads simultaneously call computeIfAbsent on the exact same key where the mapping function is computationally heavy? Can this lead to thread starvation?",
    tag: "Follow-up Probe",
    evaluates: "Bucket-level synchronization locks & starvation hazards",
  },
];

export default function MockInterviewSessionPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const router = useRouter();
  const resolvedParams = use(params);
  const sessionId = resolvedParams.id;

  const [messages, setMessages] = useState<Message[]>(INITIAL_MESSAGES);
  const [inputText, setInputText] = useState("");
  const [isTyping, setIsTyping] = useState(false);
  const [isRecording, setIsRecording] = useState(false);
  const [showHint, setShowHint] = useState(false);
  const [elapsedSeconds, setElapsedSeconds] = useState(380);

  // Timer
  useEffect(() => {
    const timer = setInterval(() => {
      setElapsedSeconds((prev) => prev + 1);
    }, 1000);
    return () => clearInterval(timer);
  }, []);

  const formatTime = (secs: number) => {
    const m = Math.floor(secs / 60);
    const s = secs % 60;
    return `${m.toString().padStart(2, "0")}:${s.toString().padStart(2, "0")}`;
  };

  const handleSendResponse = () => {
    if (!inputText.trim()) return;

    const userMsg: Message = {
      id: Date.now().toString(),
      sender: "user",
      time: "Just now",
      text: inputText,
      duration: "Delivered in 1m 05s",
    };

    setMessages((prev) => [...prev, userMsg]);
    setInputText("");
    setIsTyping(true);

    // Simulated adaptive follow-up / evaluation conclusion
    setTimeout(() => {
      setIsTyping(false);
      const aiResponse: Message = {
        id: (Date.now() + 1).toString(),
        sender: "ai",
        time: "Just now",
        text: "Excellent analysis. You clearly identified that computeIfAbsent synchronizes on the root bucket node, so heavy computations block all other threads accessing that same bucket. That concludes this scenario. Let's move to the evaluation report.",
        tag: "Scenario Evaluation",
      };
      setMessages((prev) => [...prev, aiResponse]);
    }, 2000);
  };

  return (
    <div className="w-full max-w-[1000px] mx-auto px-6 py-6 pb-44 flex flex-col gap-6">
      {/* 1. Header Ribbon */}
      <div className="flex flex-col gap-3 pb-4 border-b border-[#E5E5E5]">
        <div className="flex items-center justify-between text-[13px]">
          <div className="flex items-center gap-2">
            <Badge variant="primary" className="text-[11px]">
              AI MOCK INTERVIEW
            </Badge>
            <span className="text-[#C4C7C7]">•</span>
            <span className="font-semibold text-[#111111]">
              Java Backend Core: Concurrency & Systems
            </span>
          </div>

          <div className="flex items-center gap-4">
            <div className="flex items-center gap-1.5 font-mono text-[12px] font-medium text-[#111111]">
              <Clock className="w-3.5 h-3.5 text-[#555555]" />
              <span>{formatTime(elapsedSeconds)}</span>
            </div>
            <Link
              href="/dashboard"
              className="text-[#8A8A8A] hover:text-[#111111] flex items-center gap-1 text-[12px] transition-colors"
            >
              <LogOut className="w-3.5 h-3.5" />
              <span>Exit</span>
            </Link>
          </div>
        </div>

        <div className="flex items-center justify-between text-[12px] text-[#8A8A8A]">
          <span>Scenario 2 of 4 • Technical Architecture Depth</span>
          <span className="font-mono text-[11px] text-[#111111]">1 AI credit active</span>
        </div>
        <Progress value={50} className="h-1.5" />
      </div>

      {/* 2. Conversational Thread Canvas */}
      <div className="flex flex-col gap-5">
        {messages.map((msg) => {
          if (msg.sender === "ai") {
            return (
              <div
                key={msg.id}
                className="flex flex-col items-start max-w-[90%] sm:max-w-[82%] gap-1.5 self-start"
              >
                <div className="flex items-center gap-2 mb-0.5">
                  <div className="w-5 h-5 rounded-full bg-[#111111] flex items-center justify-center text-[10px] text-white font-bold">
                    AI
                  </div>
                  <span className="text-[12px] font-medium text-[#111111]">
                    Senior SDE Auditor
                  </span>
                  {msg.tag && (
                    <Badge variant="default" className="text-[10px] py-0">
                      {msg.tag}
                    </Badge>
                  )}
                  <span className="font-mono text-[11px] text-[#8A8A8A]">
                    {msg.time}
                  </span>
                </div>

                <div className="bg-[#F4F3F3] text-[#111111] p-4 rounded-2xl rounded-bl-sm border border-[#E5E5E5]">
                  <p className="text-[14px] leading-relaxed">{msg.text}</p>
                </div>

                {msg.evaluates && (
                  <span className="text-[11px] text-[#8A8A8A] ml-1">
                    Evaluates: {msg.evaluates}
                  </span>
                )}
              </div>
            );
          }

          return (
            <div
              key={msg.id}
              className="flex flex-col items-end max-w-[90%] sm:max-w-[82%] gap-1.5 self-end"
            >
              <div className="flex items-center gap-2 mb-0.5">
                <span className="font-mono text-[11px] text-[#8A8A8A]">
                  {msg.time}
                </span>
                <span className="text-[12px] font-medium text-[#555555]">
                  You
                </span>
                <div className="w-5 h-5 rounded-full bg-[#EEEEEE] flex items-center justify-center text-[11px] text-[#111111]">
                  <User className="w-3 h-3" />
                </div>
              </div>

              <div className="bg-white text-[#111111] p-4 rounded-2xl rounded-br-sm border border-[#E5E5E5]">
                <p className="text-[14px] leading-relaxed">{msg.text}</p>
              </div>

              {msg.duration && (
                <div className="flex items-center gap-1.5 mr-1 text-[11px] font-mono text-[#8A8A8A]">
                  <CheckCircle2 className="w-3.5 h-3.5 text-[#16A34A]" />
                  <span>{msg.duration}</span>
                </div>
              )}
            </div>
          );
        })}

        {/* AI Thinking stream indicator */}
        {isTyping && (
          <div className="flex items-center gap-2.5 self-start px-3 py-1.5 rounded-full bg-[#F4F3F3] border border-[#E5E5E5] text-[12px] text-[#555555]">
            <div className="flex items-center gap-1">
              <span className="w-1.5 h-1.5 rounded-full bg-[#111111] animate-bounce" />
              <span
                className="w-1.5 h-1.5 rounded-full bg-[#111111] animate-bounce"
                style={{ animationDelay: "150ms" }}
              />
              <span
                className="w-1.5 h-1.5 rounded-full bg-[#111111] animate-bounce"
                style={{ animationDelay: "300ms" }}
              />
            </div>
            <span>Interviewer evaluating answer & formulating probe...</span>
          </div>
        )}

        {/* Collapsible Academic Hint Drawer */}
        {showHint && (
          <div className="p-4 rounded-xl bg-white border border-[#111111] flex flex-col gap-2">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2 text-[13px] font-semibold text-[#111111]">
                <Lightbulb className="w-4 h-4 text-[#D97706]" />
                <span>Academic Hint (1 of 2 used)</span>
              </div>
              <button
                type="button"
                onClick={() => setShowHint(false)}
                className="text-[12px] text-[#8A8A8A] hover:text-[#111111]"
              >
                Close
              </button>
            </div>
            <p className="text-[12px] text-[#555555] leading-relaxed">
              Recall that Java 8+ ConcurrentHashMap synchronizes on the root Node (head) of that specific hash bucket bin. Consider how other threads requesting that exact entry behave while the lambda executes.
            </p>
          </div>
        )}
      </div>

      {/* 3. Anchored Candidate Response Console */}
      <div className="fixed bottom-0 left-60 right-0 bg-white border-t border-[#E5E5E5] p-4 z-40">
        <div className="max-w-[1000px] mx-auto flex flex-col gap-3">
          <div className="flex items-center justify-between text-[12px] text-[#8A8A8A]">
            <div className="flex items-center gap-3">
              <button
                type="button"
                onClick={() => setShowHint(!showHint)}
                className="hover:text-[#111111] flex items-center gap-1 transition-colors"
              >
                <Lightbulb className="w-3.5 h-3.5 text-[#D97706]" />
                <span>Need a hint? (1 left)</span>
              </button>
            </div>

            <div className="flex items-center gap-3 font-mono text-[11px]">
              <span>
                {inputText.trim() ? `${inputText.trim().split(/\s+/).length} words` : "0 words"}
              </span>
              <span>•</span>
              <span>Expected: 150-250 words</span>
            </div>
          </div>

          <div className="relative rounded-xl border border-[#E5E5E5] focus-within:border-[#111111] bg-white transition-colors">
            <textarea
              value={inputText}
              onChange={(e) => setInputText(e.target.value)}
              placeholder="State your engineering reasoning, algorithmic trade-offs, and edge case handling..."
              rows={3}
              className="w-full p-3 text-[14px] text-[#111111] placeholder:text-[#8A8A8A] focus:outline-none resize-none bg-transparent"
            />

            <div className="flex items-center justify-between px-3 py-2 border-t border-[#E5E5E5]/60 bg-[#FAFAFA] rounded-b-xl">
              <div className="flex items-center gap-2">
                <button
                  type="button"
                  onClick={() => setIsRecording(!isRecording)}
                  className={`p-1.5 rounded-lg text-[12px] flex items-center gap-1.5 transition-colors ${
                    isRecording
                      ? "bg-[#FEE2E2] text-[#DC2626]"
                      : "text-[#555555] hover:bg-[#EEEEEE] hover:text-[#111111]"
                  }`}
                >
                  {isRecording ? <MicOff className="w-4 h-4" /> : <Mic className="w-4 h-4" />}
                  <span>{isRecording ? "Recording..." : "Voice input"}</span>
                </button>
              </div>

              <div className="flex items-center gap-2">
                <Link href={`/practice/quiz/${sessionId}/report`}>
                  <Button variant="outline" size="sm">
                    Complete & View Report
                  </Button>
                </Link>
                <Button
                  size="sm"
                  onClick={handleSendResponse}
                  disabled={!inputText.trim()}
                  className="gap-1.5"
                >
                  <span>Submit Response</span>
                  <Send className="w-3.5 h-3.5" />
                </Button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
