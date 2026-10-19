"use client";

import { useState, useEffect, useRef, use } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import {
  Clock,
  Send,
  Sparkles,
  Bot,
  User,
  Lightbulb,
  CheckCircle2,
  Mic,
  MicOff,
  LogOut,
  Info,
} from "lucide-react";
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

// Clean welcoming message to start interview preview
const INITIAL_MESSAGES: Message[] = [
  {
    id: "welcome-1",
    sender: "ai",
    time: "Just now",
    text: "Welcome to your technical mock interview room. When you are ready, describe a technical challenge you recently solved or explain your engineering approach to building high-concurrency systems.",
    tag: "Opening Round",
    evaluates: "System Architecture, Concurrency, Problem Solving",
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
  const [elapsedSeconds, setElapsedSeconds] = useState(0);

  const messagesEndRef = useRef<HTMLDivElement>(null);

  // Smooth scroll to bottom whenever messages change or typing indicator changes
  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages, isTyping]);

  // Timer starting from 00:00
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
      time: new Date().toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" }),
      text: inputText,
      duration: "Delivered live",
    };

    setMessages((prev) => [...prev, userMsg]);
    setInputText("");
    setIsTyping(true);

    // Simulated responsive counter-probe
    setTimeout(() => {
      setIsTyping(false);
      const aiResponse: Message = {
        id: (Date.now() + 1).toString(),
        sender: "ai",
        time: new Date().toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" }),
        text: "Thank you for sharing your approach. How would your design handle sudden traffic spikes or partial database partition failures? What trade-offs between consistency and availability would you accept?",
        tag: "Follow-up Probe",
        evaluates: "Edge Cases & Fault Tolerance",
      };
      setMessages((prev) => [...prev, aiResponse]);
    }, 1800);
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      handleSendResponse();
    }
  };

  return (
    <div className="flex flex-col h-[calc(100vh-65px)] w-full max-w-[1000px] mx-auto px-4 sm:px-6 overflow-hidden">
      {/* 1. Header Ribbon (Fixed at top of container) */}
      <div className="shrink-0 flex flex-col gap-3 py-4 border-b border-[#E5E5E5] bg-white z-10">
        <div className="flex items-center justify-between text-[13px]">
          <div className="flex items-center gap-2">
            <Badge variant="primary" className="text-[11px]">
              AI MOCK INTERVIEW
            </Badge>
            <span className="text-[#C4C7C7]">•</span>
            <span className="font-semibold text-[#111111]">
              Technical Assessment Simulator
            </span>
            <Badge variant="warning" className="text-[10px] py-0 hidden sm:inline-flex">
              Phase 8 Preview
            </Badge>
          </div>

          <div className="flex items-center gap-4">
            <div className="flex items-center gap-1.5 font-mono text-[12px] font-medium text-[#111111]">
              <Clock className="w-3.5 h-3.5 text-[#555555]" />
              <span>{formatTime(elapsedSeconds)}</span>
            </div>
            <Link
              href="/interviews"
              className="text-[#8A8A8A] hover:text-[#111111] flex items-center gap-1 text-[12px] transition-colors"
            >
              <LogOut className="w-3.5 h-3.5" />
              <span>Exit</span>
            </Link>
          </div>
        </div>

        {/* Phase notice banner */}
        <div className="flex items-center justify-between px-3 py-1.5 rounded-lg bg-[#FAFAFA] border border-[#E5E5E5] text-[11px] text-[#555555]">
          <div className="flex items-center gap-1.5">
            <Info className="w-3.5 h-3.5 text-[#8A8A8A]" />
            <span>Interactive Simulator Preview. Live Groq LLM streaming API connects in Phase 8.</span>
          </div>
          <span className="font-mono text-[#8A8A8A]">Session #{sessionId}</span>
        </div>
      </div>

      {/* 2. Conversational Messages Area (Smooth scroll container) */}
      <div className="flex-1 overflow-y-auto py-6 pr-1 sm:pr-2 flex flex-col gap-5 scroll-smooth overscroll-contain">
        {messages.map((msg) => {
          if (msg.sender === "ai") {
            return (
              <div
                key={msg.id}
                className="flex flex-col items-start max-w-[92%] sm:max-w-[82%] gap-1.5 self-start animate-in fade-in duration-200"
              >
                <div className="flex items-center gap-2 mb-0.5">
                  <div className="w-5 h-5 rounded-full bg-[#111111] flex items-center justify-center text-[10px] text-white font-bold">
                    AI
                  </div>
                  <span className="text-[12px] font-medium text-[#111111]">
                    Senior SDE Interviewer
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

                <div className="bg-[#F4F3F3] text-[#111111] p-4 rounded-2xl rounded-bl-sm border border-[#E5E5E5] shadow-xs">
                  <p className="text-[14px] leading-relaxed whitespace-pre-wrap">{msg.text}</p>
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
              className="flex flex-col items-end max-w-[92%] sm:max-w-[82%] gap-1.5 self-end animate-in fade-in duration-200"
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

              <div className="bg-white text-[#111111] p-4 rounded-2xl rounded-br-sm border border-[#E5E5E5] shadow-xs">
                <p className="text-[14px] leading-relaxed whitespace-pre-wrap">{msg.text}</p>
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

        {/* AI Typing stream indicator */}
        {isTyping && (
          <div className="flex items-center gap-2.5 self-start px-3.5 py-2 rounded-full bg-[#F4F3F3] border border-[#E5E5E5] text-[12px] text-[#555555] animate-in fade-in duration-150">
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
            <span>Interviewer formulating counter-probe...</span>
          </div>
        )}

        {/* Hint Drawer */}
        {showHint && (
          <div className="p-4 rounded-xl bg-white border border-[#111111] flex flex-col gap-2 animate-in fade-in duration-200">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2 text-[13px] font-semibold text-[#111111]">
                <Lightbulb className="w-4 h-4 text-[#D97706]" />
                <span>Interviewer Hint</span>
              </div>
              <button
                type="button"
                onClick={() => setShowHint(false)}
                className="text-[12px] text-[#8A8A8A] hover:text-[#111111] cursor-pointer"
              >
                Close
              </button>
            </div>
            <p className="text-[12px] text-[#555555] leading-relaxed">
              Structure your response using the STAR format (Situation, Task, Action, Result) or discuss performance trade-offs (latency vs throughput vs memory).
            </p>
          </div>
        )}

        {/* Scroll anchor */}
        <div ref={messagesEndRef} className="h-1 shrink-0" />
      </div>

      {/* 3. Docked Candidate Response Console (No overlap, naturally in flex layout) */}
      <div className="shrink-0 pt-3 pb-6 bg-white border-t border-[#E5E5E5] z-10">
        <div className="flex flex-col gap-2.5">
          <div className="flex items-center justify-between text-[12px] text-[#8A8A8A]">
            <div className="flex items-center gap-3">
              <button
                type="button"
                onClick={() => setShowHint(!showHint)}
                className="hover:text-[#111111] flex items-center gap-1 transition-colors cursor-pointer"
              >
                <Lightbulb className="w-3.5 h-3.5 text-[#D97706]" />
                <span>{showHint ? "Hide hint" : "Need a hint?"}</span>
              </button>
            </div>

            <div className="flex items-center gap-3 font-mono text-[11px]">
              <span>
                {inputText.trim() ? `${inputText.trim().split(/\s+/).length} words` : "0 words"}
              </span>
              <span>•</span>
              <span className="text-[#8A8A8A]">Press Enter to send (Shift+Enter for newline)</span>
            </div>
          </div>

          <div className="relative rounded-xl border border-[#E5E5E5] focus-within:border-[#111111] bg-white transition-colors">
            <textarea
              value={inputText}
              onChange={(e) => setInputText(e.target.value)}
              onKeyDown={handleKeyDown}
              placeholder="State your technical answer, trade-offs, and design reasoning..."
              rows={3}
              className="w-full p-3 text-[14px] text-[#111111] placeholder:text-[#8A8A8A] focus:outline-none resize-none bg-transparent"
            />

            <div className="flex items-center justify-between px-3 py-2 border-t border-[#E5E5E5]/60 bg-[#FAFAFA] rounded-b-xl">
              <div className="flex items-center gap-2">
                <button
                  type="button"
                  onClick={() => setIsRecording(!isRecording)}
                  className={`p-1.5 rounded-lg text-[12px] flex items-center gap-1.5 transition-colors cursor-pointer ${
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
                <Link href="/interviews">
                  <Button variant="outline" size="sm">
                    Exit Round
                  </Button>
                </Link>
                <Button
                  size="sm"
                  onClick={handleSendResponse}
                  disabled={!inputText.trim()}
                  className="gap-1.5"
                >
                  <span>Send</span>
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
