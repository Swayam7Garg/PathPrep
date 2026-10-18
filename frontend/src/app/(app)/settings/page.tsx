"use client";

import { useState } from "react";
import { useQuery, useMutation } from "@tanstack/react-query";
import {
  Key,
  Zap,
  Check,
  ShieldCheck,
  Copy,
  Eye,
  EyeOff,
  User,
  GraduationCap,
  Building,
  Save,
  Hourglass,
  Clock,
  Sparkles,
} from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Progress } from "@/components/ui/progress";
import { getAiUsage, saveGroqApiKey } from "@/lib/api";
import { queryKeys } from "@/lib/query-keys";

export default function SettingsPage() {
  const { data: usage } = useQuery({
    queryKey: queryKeys.me.usage,
    queryFn: getAiUsage,
  });

  const [groqKey, setGroqKey] = useState("gsk_9K2p8L110vMc1QhuEe9K465aKvAn-HuPcRiYFxblt");
  const [showKey, setShowKey] = useState(false);
  const [keySaved, setKeySaved] = useState(false);
  const [profileSaved, setProfileSaved] = useState(false);

  // Profile form state
  const [fullName, setFullName] = useState("Ananya Sharma");
  const [college, setCollege] = useState("National Institute of Technology, Trichy");
  const [branch, setBranch] = useState("Computer Science & Engineering");
  const [gradYear, setGradYear] = useState("2026 (Pre-final Year)");
  const [targetTrack, setTargetTrack] = useState("Java Backend SDE");

  const saveKeyMutation = useMutation({
    mutationFn: saveGroqApiKey,
    onSuccess: () => {
      setKeySaved(true);
      setTimeout(() => setKeySaved(false), 3000);
    },
  });

  const handleSaveProfile = (e: React.FormEvent) => {
    e.preventDefault();
    setProfileSaved(true);
    setTimeout(() => setProfileSaved(false), 3000);
  };

  return (
    <div className="w-full max-w-[1000px] mx-auto px-6 py-8 flex flex-col gap-8">
      {/* Header */}
      <div className="flex flex-col gap-1.5 border-b border-[#E5E5E5] pb-6">
        <div className="flex items-center gap-2">
          <span className="w-2 h-2 rounded-full bg-[#111111]" />
          <span className="font-mono text-[11px] uppercase tracking-wider text-[#8A8A8A]">
            Student Preferences • AI Quota & Infrastructure
          </span>
        </div>
        <h2 className="text-[30px] font-semibold text-[#111111] tracking-tight">
          Settings & Credentials
        </h2>
        <p className="text-[13px] text-[#555555]">
          Manage your campus academic profile, daily AI simulation limits, and personal LLM keys.
        </p>
      </div>

      {/* 1. Academic Profile Card */}
      <Card>
        <CardHeader className="flex flex-col sm:flex-row sm:items-center justify-between pb-4 gap-2">
          <div>
            <CardTitle className="text-[18px]">Campus Academic Profile</CardTitle>
            <p className="text-[13px] text-[#8A8A8A] mt-1">
              Used to calibrate placement benchmark cutoffs and target company rubrics
            </p>
          </div>
          <Badge variant="default" className="text-[11px] self-start sm:self-auto">
            Verified NIT Trichy EduID
          </Badge>
        </CardHeader>

        <CardContent>
          <form onSubmit={handleSaveProfile} className="flex flex-col gap-5">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
              <div className="flex flex-col gap-1.5">
                <label className="text-[13px] font-medium text-[#111111]">
                  Full Name
                </label>
                <input
                  type="text"
                  value={fullName}
                  onChange={(e) => setFullName(e.target.value)}
                  className="px-3.5 py-2.5 rounded-lg border border-[#E5E5E5] focus:border-[#111111] focus:outline-none text-[14px] bg-white text-[#111111]"
                />
              </div>

              <div className="flex flex-col gap-1.5">
                <label className="text-[13px] font-medium text-[#111111]">
                  College / Institute
                </label>
                <input
                  type="text"
                  value={college}
                  onChange={(e) => setCollege(e.target.value)}
                  className="px-3.5 py-2.5 rounded-lg border border-[#E5E5E5] focus:border-[#111111] focus:outline-none text-[14px] bg-white text-[#111111]"
                />
              </div>

              <div className="flex flex-col gap-1.5">
                <label className="text-[13px] font-medium text-[#111111]">
                  Branch / Major
                </label>
                <select
                  value={branch}
                  onChange={(e) => setBranch(e.target.value)}
                  className="px-3.5 py-2.5 rounded-lg border border-[#E5E5E5] focus:border-[#111111] focus:outline-none text-[14px] bg-white text-[#111111]"
                >
                  <option>Computer Science & Engineering</option>
                  <option>Information Technology</option>
                  <option>Electronics & Communication Engineering</option>
                  <option>Data Science & Artificial Intelligence</option>
                </select>
              </div>

              <div className="flex flex-col gap-1.5">
                <label className="text-[13px] font-medium text-[#111111]">
                  Graduation Year
                </label>
                <select
                  value={gradYear}
                  onChange={(e) => setGradYear(e.target.value)}
                  className="px-3.5 py-2.5 rounded-lg border border-[#E5E5E5] focus:border-[#111111] focus:outline-none text-[14px] bg-white text-[#111111]"
                >
                  <option>2025 (Final Year)</option>
                  <option>2026 (Pre-final Year)</option>
                  <option>2027 (Sophomore)</option>
                </select>
              </div>

              <div className="flex flex-col gap-1.5">
                <label className="text-[13px] font-medium text-[#111111]">
                  Target Career Track
                </label>
                <select
                  value={targetTrack}
                  onChange={(e) => setTargetTrack(e.target.value)}
                  className="px-3.5 py-2.5 rounded-lg border border-[#E5E5E5] focus:border-[#111111] focus:outline-none text-[14px] bg-white text-[#111111]"
                >
                  <option>Java Backend SDE</option>
                  <option>Web Frontend SDE</option>
                  <option>General SDE (Campus Core)</option>
                </select>
              </div>

              <div className="flex flex-col gap-1.5">
                <label className="text-[13px] font-medium text-[#111111]">
                  Target Placement Tier
                </label>
                <select className="px-3.5 py-2.5 rounded-lg border border-[#E5E5E5] focus:border-[#111111] focus:outline-none text-[14px] bg-white text-[#111111]">
                  <option>Tier-1 Product (₹24+ LPA)</option>
                  <option>Tier-1 Core Product / Cloud (₹16 - ₹24 LPA)</option>
                  <option>Tier-2 High-Growth FinTech (₹10 - ₹16 LPA)</option>
                </select>
              </div>
            </div>

            <div className="flex items-center justify-between pt-4 border-t border-[#E5E5E5]/60 mt-2">
              <span className="font-mono text-[11px] text-[#8A8A8A]">
                Last synced 2 days ago (Cloud DB)
              </span>
              <div className="flex items-center gap-3">
                {profileSaved && (
                  <span className="text-[12px] text-[#16A34A] font-medium flex items-center gap-1">
                    <Check className="w-3.5 h-3.5" /> Saved!
                  </span>
                )}
                <Button type="submit" size="sm" className="gap-2">
                  <Save className="w-3.5 h-3.5" />
                  <span>Save Profile</span>
                </Button>
              </div>
            </div>
          </form>
        </CardContent>
      </Card>

      {/* 2. AI Credits & Daily Quota Card (Phase 8 - Not Available Yet) */}
      <Card className="opacity-80">
        <CardHeader className="flex flex-col sm:flex-row sm:items-center justify-between pb-4 gap-2">
          <div>
            <div className="flex items-center gap-2">
              <Zap className="w-4 h-4 text-[#8A8A8A]" />
              <CardTitle className="text-[18px]">AI Credits & Daily Quota</CardTitle>
            </div>
            <p className="text-[13px] text-[#8A8A8A] mt-1">
              Live conversational interviewer agent and diagnostic evaluations.
            </p>
          </div>
          <Badge variant="warning" className="text-[11px] self-start sm:self-auto">
            Not available yet (Phase 8)
          </Badge>
        </CardHeader>

        <CardContent className="flex flex-col gap-6">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            {/* Metric 1 */}
            <div className="p-4 rounded-xl bg-[#FAFAFA] border border-[#E5E5E5] flex flex-col justify-between gap-4">
              <div>
                <div className="flex items-center justify-between mb-1">
                  <span className="text-[14px] font-semibold text-[#8A8A8A]">
                    Mock Interview Rounds
                  </span>
                  <span className="font-mono text-[12px] font-semibold text-[#8A8A8A]">
                    0 of 2 used
                  </span>
                </div>
                <p className="text-[12px] text-[#8A8A8A] mb-3">
                  Live conversational interviewer agent (30m session).
                </p>
                <Progress value={0} className="h-2" />
              </div>

              <div className="pt-2 border-t border-[#E5E5E5]/60 flex items-center justify-between font-mono text-[11px] text-[#8A8A8A]">
                <span className="flex items-center gap-1">
                  <Clock className="w-3 h-3" /> Offline
                </span>
                <span className="text-[#8A8A8A]">Feature in development</span>
              </div>
            </div>

            {/* Metric 2 */}
            <div className="p-4 rounded-xl bg-[#FAFAFA] border border-[#E5E5E5] flex flex-col justify-between gap-4">
              <div>
                <div className="flex items-center justify-between mb-1">
                  <span className="text-[14px] font-semibold text-[#8A8A8A]">
                    Diagnostic Reviews
                  </span>
                  <span className="font-mono text-[12px] font-semibold text-[#8A8A8A]">
                    0 of 20 used
                  </span>
                </div>
                <p className="text-[12px] text-[#8A8A8A] mb-3">
                  Deep-dive code complexity and rubric feedback.
                </p>
                <Progress value={0} className="h-2" />
              </div>

              <div className="pt-2 border-t border-[#E5E5E5]/60 flex items-center justify-between font-mono text-[11px] text-[#8A8A8A]">
                <span className="flex items-center gap-1">
                  <Clock className="w-3 h-3" /> Offline
                </span>
                <span className="text-[#8A8A8A]">Feature in development</span>
              </div>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* 3. Bring Your Own Groq Key Card (Phase 8 - Not Available Yet) */}
      <Card className="opacity-80">
        <CardHeader className="flex flex-col sm:flex-row sm:items-center justify-between pb-4 gap-2">
          <div>
            <div className="flex items-center gap-2">
              <Key className="w-4 h-4 text-[#8A8A8A]" />
              <CardTitle className="text-[18px]">Use Your Own Groq API Key</CardTitle>
            </div>
            <p className="text-[13px] text-[#8A8A8A] mt-1">
              Direct connection to Groq Cloud for unlimited LLM interview simulations.
            </p>
          </div>
          <Badge variant="warning" className="text-[11px] self-start sm:self-auto">
            Not available yet (Phase 8)
          </Badge>
        </CardHeader>

        <CardContent className="flex flex-col gap-4">
          <p className="text-[13px] text-[#8A8A8A] leading-relaxed">
            Personal LLM key encryption and routing will be enabled in Phase 8 (Mock Interviews). For now, all practice quizzes and learning loop metrics run 100% locally with zero AI dependency.
          </p>

          <div className="flex flex-col gap-2">
            <label className="text-[13px] font-medium text-[#8A8A8A]">
              Groq Cloud API Key
            </label>
            <input
              type="password"
              disabled
              placeholder="gsk_•••••••••••••••••••••••••••••••• (Not available yet)"
              className="w-full px-3.5 py-2.5 rounded-lg border border-[#E5E5E5] bg-[#FAFAFA] text-[13px] font-mono text-[#8A8A8A] cursor-not-allowed"
            />
          </div>

          <div className="flex items-center justify-between pt-2">
            <span className="text-[12px] text-[#8A8A8A]">
              Custom LLM integration unlocks in Phase 8
            </span>

            <Button size="sm" disabled className="gap-2 cursor-not-allowed opacity-50">
              <ShieldCheck className="w-3.5 h-3.5" />
              <span>Save Encrypted Key</span>
            </Button>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
