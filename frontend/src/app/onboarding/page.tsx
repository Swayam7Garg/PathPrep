"use client";

import React, { useState, useEffect, Suspense } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { useAuth } from "@/components/providers/auth-provider";
import { getTracks } from "@/lib/api";
import { TrackSummary } from "@/lib/types";
import { sanitizeRedirectUrl } from "@/lib/auth-token";
import { useQuery } from "@tanstack/react-query";
import { queryKeys } from "@/lib/query-keys";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import {
  GraduationCap,
  Sparkles,
  ArrowRight,
  AlertCircle,
  CheckCircle2,
  BookOpen,
} from "lucide-react";

function OnboardingForm() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const rawNext = searchParams.get("next");
  const targetRedirect = sanitizeRedirectUrl(rawNext, "/dashboard");

  const { user, status, updateProfile } = useAuth();

  const [name, setName] = useState("");
  const [branch, setBranch] = useState("");
  const [gradYear, setGradYear] = useState<string>("");
  const [targetTrackId, setTargetTrackId] = useState<string>("");
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Load tracks catalog
  const { data: tracks, isLoading: tracksLoading } = useQuery({
    queryKey: queryKeys.tracks.all,
    queryFn: getTracks,
  });

  // Redirect if unauthenticated or already completed
  useEffect(() => {
    if (status === "unauthenticated") {
      router.replace("/login?next=/onboarding");
    } else if (status === "authenticated" && user) {
      if (user.onboardingCompleted) {
        router.replace(targetRedirect);
      } else {
        if (user.name && !name) setName(user.name);
        if (user.branch && !branch) setBranch(user.branch);
        if (user.gradYear && !gradYear) setGradYear(user.gradYear.toString());
        if (user.targetTrackId && !targetTrackId)
          setTargetTrackId(user.targetTrackId.toString());
      }
    }
  }, [status, user, router, targetRedirect, name, branch, gradYear, targetTrackId]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMsg(null);

    const trimmedName = name.trim();
    if (!trimmedName || trimmedName.length > 100) {
      setErrorMsg("Please enter your full name (1 to 100 characters).");
      return;
    }

    const trimmedBranch = branch.trim();
    if (!trimmedBranch || trimmedBranch.length > 50) {
      setErrorMsg("Please select or enter your branch / major (1 to 50 characters).");
      return;
    }

    const yearNum = parseInt(gradYear, 10);
    if (isNaN(yearNum) || yearNum < 2020 || yearNum > 2035) {
      setErrorMsg("Please enter a valid graduation year between 2020 and 2035.");
      return;
    }

    const trackIdNum = parseInt(targetTrackId, 10);
    if (isNaN(trackIdNum) || trackIdNum <= 0) {
      setErrorMsg("Please select your primary career target track.");
      return;
    }

    setIsSubmitting(true);
    try {
      await updateProfile({
        name: trimmedName,
        branch: trimmedBranch,
        gradYear: yearNum,
        targetTrackId: trackIdNum,
      });

      router.replace(targetRedirect);
    } catch (err: unknown) {
      const msg =
        err instanceof Error
          ? err.message
          : "Failed to save profile. Please check all fields.";
      setErrorMsg(msg);
      setIsSubmitting(false);
    }
  };

  if (status === "loading" || tracksLoading) {
    return (
      <div className="min-h-screen bg-[#FAFAFA] flex flex-col items-center justify-center p-6">
        <div className="w-10 h-10 rounded-xl bg-white border border-[#E5E5E5] flex items-center justify-center shadow-sm animate-pulse">
          <span className="font-semibold text-lg text-[#111111]">P</span>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-[#FAFAFA] flex flex-col items-center justify-center p-6">
      <div className="w-full max-w-[560px] flex flex-col gap-6">
        {/* Header Branding */}
        <div className="flex flex-col items-center text-center gap-2">
          <div className="flex items-center gap-2.5">
            <div className="w-9 h-9 rounded-xl bg-[#111111] text-white flex items-center justify-center font-bold text-sm tracking-tight shadow-sm">
              P
            </div>
            <span className="text-[20px] font-bold text-[#111111] tracking-tight">
              PathPrep
            </span>
            <Badge variant="outline" className="text-[10px] font-mono py-0 h-5">
              Welcome
            </Badge>
          </div>
          <h1 className="text-[24px] font-bold text-[#111111] tracking-tight mt-1">
            Let&apos;s personalize your prep journey
          </h1>
          <p className="text-[13px] text-[#555555] max-w-md">
            We use your graduation year and target role to tailor diagnostic drills, spaced reviews, and placement readiness cutoffs.
          </p>
        </div>

        {/* Card */}
        <Card className="border-[#E5E5E5] shadow-sm">
          <CardHeader className="pb-4">
            <CardTitle className="text-[18px] font-semibold text-[#111111] flex items-center gap-2">
              <GraduationCap className="w-4 h-4 text-[#111111]" />
              Academic & Placement Calibration
            </CardTitle>
            <p className="text-[12px] text-[#8A8A8A]">
              Step 1 of 1 · Takes less than 30 seconds
            </p>
          </CardHeader>

          <CardContent>
            <form onSubmit={handleSubmit} className="flex flex-col gap-5">
              {errorMsg && (
                <div className="p-3 rounded-lg bg-[#FEF2F2] border border-[#FEE2E2] flex items-start gap-2.5 text-xs text-[#991B1B]">
                  <AlertCircle className="w-4 h-4 shrink-0 mt-0.5 text-[#DC2626]" />
                  <div className="flex-1">{errorMsg}</div>
                </div>
              )}

              {/* Full Name */}
              <div className="flex flex-col gap-1.5">
                <label className="text-[13px] font-medium text-[#111111]">
                  Full Name
                </label>
                <input
                  type="text"
                  required
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  placeholder="e.g. Alex Sharma"
                  className="px-3.5 py-2.5 rounded-lg border border-[#E5E5E5] focus:border-[#111111] focus:outline-none text-[14px] bg-white text-[#111111] placeholder:text-[#AAAAAA]"
                />
              </div>

              {/* Branch / Major */}
              <div className="flex flex-col gap-1.5">
                <label className="text-[13px] font-medium text-[#111111]">
                  Branch / Engineering Major
                </label>
                <select
                  required
                  value={branch}
                  onChange={(e) => setBranch(e.target.value)}
                  className={`px-3.5 py-2.5 rounded-lg border border-[#E5E5E5] focus:border-[#111111] focus:outline-none text-[14px] bg-white ${
                    branch ? "text-[#111111]" : "text-[#8A8A8A]"
                  }`}
                >
                  <option value="" disabled>Select Branch</option>
                  <option value="Computer Science & Engineering">Computer Science & Engineering</option>
                  <option value="Information Technology">Information Technology</option>
                  <option value="Electronics & Communication">Electronics & Communication</option>
                  <option value="Data Science & Artificial Intelligence">Data Science & Artificial Intelligence</option>
                  <option value="Electrical & Electronics">Electrical & Electronics</option>
                  <option value="Mechanical / Core Engineering">Mechanical / Core Engineering</option>
                  <option value="Other">Other</option>
                </select>
              </div>

              {/* Graduation Year */}
              <div className="flex flex-col gap-1.5">
                <label className="text-[13px] font-medium text-[#111111]">
                  Graduation Year
                </label>
                <select
                  required
                  value={gradYear}
                  onChange={(e) => setGradYear(e.target.value)}
                  className={`px-3.5 py-2.5 rounded-lg border border-[#E5E5E5] focus:border-[#111111] focus:outline-none text-[14px] bg-white ${
                    gradYear ? "text-[#111111]" : "text-[#8A8A8A]"
                  }`}
                >
                  <option value="" disabled>Select Graduation Year</option>
                  <option value="2025">2025 (Final Year)</option>
                  <option value="2026">2026 (Pre-Final Year)</option>
                  <option value="2027">2027 (Sophomore)</option>
                  <option value="2028">2028 (Freshman)</option>
                </select>
              </div>

              {/* Target Track */}
              <div className="flex flex-col gap-2">
                <label className="text-[13px] font-medium text-[#111111] flex items-center justify-between">
                  <span>Target Career Track</span>
                  <span className="text-[11px] font-mono text-[#8A8A8A]">
                    You can change this anytime
                  </span>
                </label>

                <div className="grid grid-cols-1 gap-2.5">
                  {(tracks || [
                    { id: 1, name: "Java Backend Engineer", description: "Core Java, Concurrency, Spring Boot, Databases" },
                    { id: 2, name: "Web Frontend Engineer", description: "Modern JavaScript, TypeScript, React, Next.js" },
                    { id: 3, name: "General SDE (Campus Core)", description: "DSA, DBMS, OS, Computer Networks & System Design" },
                  ]).map((t) => {
                    const isSelected = targetTrackId === t.id.toString();
                    return (
                      <button
                        type="button"
                        key={t.id}
                        onClick={() => setTargetTrackId(t.id.toString())}
                        className={`p-3.5 rounded-xl border text-left transition-all flex items-start justify-between gap-3 ${
                          isSelected
                            ? "border-[#111111] bg-[#FAFAFA] ring-1 ring-[#111111]"
                            : "border-[#E5E5E5] bg-white hover:border-[#CCCCCC]"
                        }`}
                      >
                        <div className="flex flex-col gap-0.5">
                          <span className="text-[14px] font-semibold text-[#111111]">
                            {t.name}
                          </span>
                          <span className="text-[12px] text-[#555555]">
                            {t.description}
                          </span>
                        </div>
                        {isSelected && (
                          <CheckCircle2 className="w-4 h-4 text-[#111111] shrink-0 mt-0.5" />
                        )}
                      </button>
                    );
                  })}
                </div>
              </div>

              {/* Submit Button */}
              <div className="pt-2">
                <Button
                  type="submit"
                  disabled={isSubmitting}
                  className="w-full gap-2 py-5 text-[14px]"
                >
                  <span>{isSubmitting ? "Calibrating..." : "Complete Setup & Launch Dashboard"}</span>
                  <ArrowRight className="w-4 h-4" />
                </Button>
              </div>
            </form>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}

export default function OnboardingPage() {
  return (
    <Suspense
      fallback={
        <div className="min-h-screen bg-[#FAFAFA] flex items-center justify-center">
          <div className="w-8 h-8 rounded-full border-2 border-[#111111] border-t-transparent animate-spin" />
        </div>
      }
    >
      <OnboardingForm />
    </Suspense>
  );
}
