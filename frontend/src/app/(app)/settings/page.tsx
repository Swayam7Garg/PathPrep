"use client";

import { useState, useEffect } from "react";
import { useQuery, useMutation } from "@tanstack/react-query";
import {
  Key,
  Zap,
  Check,
  ShieldCheck,
  User,
  GraduationCap,
  Building,
  Save,
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

  const [groqKey, setGroqKey] = useState("");
  const [keySaved, setKeySaved] = useState(false);
  const [profileSaved, setProfileSaved] = useState(false);

  // Profile form state (empty defaults, with local persistence)
  const [fullName, setFullName] = useState("");
  const [college, setCollege] = useState("");
  const [branch, setBranch] = useState("");
  const [gradYear, setGradYear] = useState("");
  const [targetTrack, setTargetTrack] = useState("");
  const [targetTier, setTargetTier] = useState("");

  // Load saved profile from localStorage if present
  useEffect(() => {
    try {
      const saved = localStorage.getItem("pathprep_user_profile");
      if (saved) {
        const data = JSON.parse(saved);
        if (data.fullName) setFullName(data.fullName);
        if (data.college) setCollege(data.college);
        if (data.branch) setBranch(data.branch);
        if (data.gradYear) setGradYear(data.gradYear);
        if (data.targetTrack) setTargetTrack(data.targetTrack);
        if (data.targetTier) setTargetTier(data.targetTier);
      }
    } catch {
      // Ignore storage errors
    }
  }, []);

  const handleSaveProfile = (e: React.FormEvent) => {
    e.preventDefault();
    try {
      localStorage.setItem(
        "pathprep_user_profile",
        JSON.stringify({ fullName, college, branch, gradYear, targetTrack, targetTier })
      );
    } catch {
      // Ignore
    }
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
          <Badge variant="outline" className="text-[11px] font-mono self-start sm:self-auto">
            Academic Profile
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
                  placeholder="e.g. John Doe"
                  className="px-3.5 py-2.5 rounded-lg border border-[#E5E5E5] focus:border-[#111111] focus:outline-none text-[14px] bg-white text-[#111111] placeholder:text-[#AAAAAA]"
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
                  placeholder="e.g. Indian Institute of Technology"
                  className="px-3.5 py-2.5 rounded-lg border border-[#E5E5E5] focus:border-[#111111] focus:outline-none text-[14px] bg-white text-[#111111] placeholder:text-[#AAAAAA]"
                />
              </div>

              <div className="flex flex-col gap-1.5">
                <label className="text-[13px] font-medium text-[#111111]">
                  Branch / Major
                </label>
                <select
                  value={branch}
                  onChange={(e) => setBranch(e.target.value)}
                  className={`px-3.5 py-2.5 rounded-lg border border-[#E5E5E5] focus:border-[#111111] focus:outline-none text-[14px] bg-white ${
                    branch ? "text-[#111111]" : "text-[#8A8A8A]"
                  }`}
                >
                  <option value="" disabled>Select Branch / Major</option>
                  <option value="Computer Science & Engineering">Computer Science & Engineering</option>
                  <option value="Information Technology">Information Technology</option>
                  <option value="Electronics & Communication Engineering">Electronics & Communication Engineering</option>
                  <option value="Data Science & Artificial Intelligence">Data Science & Artificial Intelligence</option>
                  <option value="Electrical Engineering">Electrical Engineering</option>
                  <option value="Other">Other</option>
                </select>
              </div>

              <div className="flex flex-col gap-1.5">
                <label className="text-[13px] font-medium text-[#111111]">
                  Graduation Year
                </label>
                <select
                  value={gradYear}
                  onChange={(e) => setGradYear(e.target.value)}
                  className={`px-3.5 py-2.5 rounded-lg border border-[#E5E5E5] focus:border-[#111111] focus:outline-none text-[14px] bg-white ${
                    gradYear ? "text-[#111111]" : "text-[#8A8A8A]"
                  }`}
                >
                  <option value="" disabled>Select Graduation Year</option>
                  <option value="2025 (Final Year)">2025 (Final Year)</option>
                  <option value="2026 (Pre-final Year)">2026 (Pre-final Year)</option>
                  <option value="2027 (Sophomore)">2027 (Sophomore)</option>
                  <option value="2028 (Freshman)">2028 (Freshman)</option>
                </select>
              </div>

              <div className="flex flex-col gap-1.5">
                <label className="text-[13px] font-medium text-[#111111]">
                  Target Career Track
                </label>
                <select
                  value={targetTrack}
                  onChange={(e) => setTargetTrack(e.target.value)}
                  className={`px-3.5 py-2.5 rounded-lg border border-[#E5E5E5] focus:border-[#111111] focus:outline-none text-[14px] bg-white ${
                    targetTrack ? "text-[#111111]" : "text-[#8A8A8A]"
                  }`}
                >
                  <option value="" disabled>Select Target Career Track</option>
                  <option value="Java Backend">Java Backend SDE</option>
                  <option value="Web Frontend">Web Frontend SDE</option>
                  <option value="General SDE">General SDE (Campus Core)</option>
                </select>
              </div>

              <div className="flex flex-col gap-1.5">
                <label className="text-[13px] font-medium text-[#111111]">
                  Target Placement Tier
                </label>
                <select
                  value={targetTier}
                  onChange={(e) => setTargetTier(e.target.value)}
                  className={`px-3.5 py-2.5 rounded-lg border border-[#E5E5E5] focus:border-[#111111] focus:outline-none text-[14px] bg-white ${
                    targetTier ? "text-[#111111]" : "text-[#8A8A8A]"
                  }`}
                >
                  <option value="" disabled>Select Target Placement Tier</option>
                  <option value="Tier-1 Product (₹24+ LPA)">Tier-1 Product (₹24+ LPA)</option>
                  <option value="Tier-1 Core Product / Cloud (₹16 - ₹24 LPA)">Tier-1 Core Product / Cloud (₹16 - ₹24 LPA)</option>
                  <option value="Tier-2 High-Growth FinTech (₹10 - ₹16 LPA)">Tier-2 High-Growth FinTech (₹10 - ₹16 LPA)</option>
                  <option value="Tier-3 Enterprise / Services (₹5 - ₹10 LPA)">Tier-3 Enterprise / Services (₹5 - ₹10 LPA)</option>
                </select>
              </div>
            </div>

            <div className="flex items-center justify-between pt-4 border-t border-[#E5E5E5]/60 mt-2">
              <span className="font-mono text-[11px] text-[#8A8A8A]">
                Profile preferences stored locally
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
            Coming in Phase 8
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
                <span className="text-[#8A8A8A]">Activates in Phase 8</span>
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
                <span className="text-[#8A8A8A]">Activates in Phase 8</span>
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
            Coming in Phase 8
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
              placeholder="gsk_•••••••••••••••••••••••••••••••• (Enabled in Phase 8)"
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
