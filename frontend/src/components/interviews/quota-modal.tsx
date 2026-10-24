"use client";

import React, { useState } from "react";
import Link from "next/link";
import {
  Zap,
  Key,
  ShieldCheck,
  ArrowRight,
  BookOpen,
  CheckCircle2,
  X,
  ExternalLink,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { saveGroqApiKey } from "@/lib/api";
import { useQueryClient } from "@tanstack/react-query";
import { queryKeys } from "@/lib/query-keys";

interface QuotaModalProps {
  open: boolean;
  onClose: () => void;
  resetAt?: string;
  suggestedStyle?: "LITE" | "LIVE";
  onSelectSuggestedStyle?: (style: "LITE" | "LIVE") => void;
}

export function QuotaModal({
  open,
  onClose,
  resetAt = "Midnight UTC (05:30 AM IST)",
  suggestedStyle,
  onSelectSuggestedStyle,
}: QuotaModalProps) {
  const queryClient = useQueryClient();
  const [groqKey, setGroqKey] = useState("");
  const [isSaving, setIsSaving] = useState(false);
  const [saveSuccess, setSaveSuccess] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  if (!open) return null;

  const handleSaveKey = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage(null);
    const trimmed = groqKey.trim();

    if (!trimmed.startsWith("gsk_") || trimmed.length < 20) {
      setErrorMessage("Please enter a valid Groq Cloud API key starting with 'gsk_'.");
      return;
    }

    setIsSaving(true);
    try {
      await saveGroqApiKey(trimmed);
      setSaveSuccess(true);
      queryClient.invalidateQueries({ queryKey: queryKeys.me.aiUsage });
      setTimeout(() => {
        onClose();
        setSaveSuccess(false);
        setGroqKey("");
      }, 1500);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : "Failed to activate API key.";
      setErrorMessage(msg);
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/40 backdrop-blur-xs animate-in fade-in duration-150">
      <div className="w-full max-w-lg bg-white rounded-2xl border border-[#111111] shadow-2xl p-6 flex flex-col gap-5 relative">
        <button
          type="button"
          onClick={onClose}
          className="absolute right-4 top-4 p-1 rounded-lg text-[#8A8A8A] hover:text-[#111111] hover:bg-[#F4F3F3] transition-colors"
        >
          <X className="w-4 h-4" />
        </button>

        {/* Header */}
        <div className="flex items-start gap-3.5">
          <div className="w-10 h-10 rounded-xl bg-[#FEF3C7] border border-[#FDE68A] flex items-center justify-center text-[#D97706] shrink-0">
            <Zap className="w-5 h-5" />
          </div>
          <div className="flex flex-col gap-0.5">
            <div className="flex items-center gap-2">
              <h3 className="text-[17px] font-semibold text-[#111111]">
                Daily AI Interview Quota Reached
              </h3>
              <Badge variant="warning" className="text-[10px] py-0">
                Limit Reached
              </Badge>
            </div>
            <p className="text-[13px] text-[#555555]">
              Free campus tier daily quota is used up. Reset at: {resetAt}.
            </p>
          </div>
        </div>

        {/* Suggested Style Offer if available */}
        {suggestedStyle && onSelectSuggestedStyle && (
          <div className="p-3.5 rounded-xl bg-[#F0FDF4] border border-[#BBF7D0] flex items-center justify-between gap-3">
            <div className="flex flex-col">
              <span className="text-[13px] font-semibold text-[#166534]">
                Available Alternative: {suggestedStyle} Style
              </span>
              <span className="text-[11px] text-[#15803D]">
                You can still take this interview using the lighter {suggestedStyle} mode.
              </span>
            </div>
            <Button
              size="sm"
              onClick={() => {
                onClose();
                onSelectSuggestedStyle(suggestedStyle);
              }}
              className="bg-[#16A34A] hover:bg-[#15803D] text-white text-xs shrink-0"
            >
              Start in {suggestedStyle}
            </Button>
          </div>
        )}

        {/* Option 1: Provide Groq Key */}
        <div className="p-4 rounded-xl bg-[#FAFAFA] border border-[#E5E5E5] flex flex-col gap-3.5">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2 text-[14px] font-semibold text-[#111111]">
              <Key className="w-4 h-4 text-[#111111]" />
              <span>Unlock Unlimited AI with Your Free Groq Key</span>
            </div>
            <Badge variant="success" className="text-[10px] py-0">
              Free • Unlimited
            </Badge>
          </div>

          <p className="text-[12px] text-[#555555] leading-relaxed">
            Groq provides lightning-fast Llama-3 inference. Add your free key to bypass platform rate limits completely.
          </p>

          <form onSubmit={handleSaveKey} className="flex flex-col gap-2.5">
            {errorMessage && (
              <div className="text-[12px] text-[#DC2626] font-medium bg-[#FEE2E2]/50 p-2 rounded-lg">
                {errorMessage}
              </div>
            )}

            <div className="flex items-center gap-2">
              <input
                type="password"
                value={groqKey}
                onChange={(e) => setGroqKey(e.target.value)}
                placeholder="gsk_••••••••••••••••••••••••••••••••"
                className="flex-1 px-3 py-2 rounded-lg border border-[#E5E5E5] text-[13px] font-mono text-[#111111] focus:outline-none focus:border-[#111111] bg-white"
              />
              <Button type="submit" size="sm" disabled={isSaving || !groqKey.trim()} className="gap-1.5 shrink-0">
                {saveSuccess ? (
                  <>
                    <CheckCircle2 className="w-3.5 h-3.5 text-[#16A34A]" />
                    <span>Activated!</span>
                  </>
                ) : (
                  <>
                    <ShieldCheck className="w-3.5 h-3.5" />
                    <span>{isSaving ? "Saving..." : "Activate"}</span>
                  </>
                )}
              </Button>
            </div>

            <div className="flex items-center justify-between text-[11px] text-[#8A8A8A]">
              <span>Key is stored securely and encrypted.</span>
              <a
                href="https://console.groq.com/keys"
                target="_blank"
                rel="noopener noreferrer"
                className="hover:text-[#111111] flex items-center gap-1 underline"
              >
                <span>Get a free key in 60s</span>
                <ExternalLink className="w-3 h-3" />
              </a>
            </div>
          </form>
        </div>

        {/* Option 2: Zero-AI Practice */}
        <div className="pt-2 border-t border-[#E5E5E5]/60 flex items-center justify-between">
          <div className="flex items-center gap-2 text-[12px] text-[#555555]">
            <BookOpen className="w-4 h-4 text-[#8A8A8A]" />
            <span>Practice Quizzes require 0% AI and remain 100% free.</span>
          </div>

          <Link href="/practice">
            <Button variant="outline" size="sm" className="gap-1.5 text-xs">
              <span>Rapid MCQ Drill</span>
              <ArrowRight className="w-3.5 h-3.5" />
            </Button>
          </Link>
        </div>
      </div>
    </div>
  );
}
