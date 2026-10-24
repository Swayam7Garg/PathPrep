"use client";

import { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import {
  Key,
  Zap,
  Check,
  ShieldCheck,
  User,
  GraduationCap,
  Save,
  Clock,
  Sparkles,
  LogOut,
  Trash2,
  AlertTriangle,
  Laptop,
} from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Progress } from "@/components/ui/progress";
import { getAiUsage, getTracks, saveGroqApiKey, removeGroqApiKey, testGroqApiKey } from "@/lib/api";
import { queryKeys } from "@/lib/query-keys";
import { useAuth } from "@/components/providers/auth-provider";

export default function SettingsPage() {
  const router = useRouter();
  const queryClient = useQueryClient();
  const { user, updateProfile, logout, logoutAll, deleteAccount } = useAuth();

  const { data: usage } = useQuery({
    queryKey: queryKeys.me.usage,
    queryFn: getAiUsage,
  });

  const { data: tracks } = useQuery({
    queryKey: queryKeys.tracks.all,
    queryFn: getTracks,
  });

  // Profile form state
  const [name, setName] = useState("");
  const [branch, setBranch] = useState("");
  const [gradYear, setGradYear] = useState("");
  const [targetTrackId, setTargetTrackId] = useState("");

  const [profileSaving, setProfileSaving] = useState(false);
  const [profileSaved, setProfileSaved] = useState(false);
  const [profileError, setProfileError] = useState<string | null>(null);

  // Groq API Key state
  const [groqKeyInput, setGroqKeyInput] = useState("");
  const [showGroqKey, setShowGroqKey] = useState(false);
  const [groqKeySaving, setGroqKeySaving] = useState(false);
  const [groqKeySaved, setGroqKeySaved] = useState(false);
  const [groqKeyError, setGroqKeyError] = useState<string | null>(null);
  const [groqTesting, setGroqTesting] = useState(false);
  const [groqTestResult, setGroqTestResult] = useState<string | null>(null);

  // Danger zone state
  const [showDeleteModal, setShowDeleteModal] = useState(false);
  const [deleteConfirmation, setDeleteConfirmation] = useState("");
  const [deleteError, setDeleteError] = useState<string | null>(null);
  const [isDeleting, setIsDeleting] = useState(false);

  const handleSaveGroqKey = async (e: React.FormEvent) => {
    e.preventDefault();
    setGroqKeyError(null);
    setGroqTestResult(null);

    const key = groqKeyInput.trim();
    if (!key.startsWith("gsk_") || key.length < 20) {
      setGroqKeyError("Please enter a valid Groq API key starting with 'gsk_'.");
      return;
    }

    setGroqKeySaving(true);
    try {
      await saveGroqApiKey(key);
      setGroqKeySaved(true);
      setGroqKeyInput(""); // Clear the key field after saving. Never store in browser.
      queryClient.invalidateQueries({ queryKey: queryKeys.me.usage });
      queryClient.invalidateQueries({ queryKey: queryKeys.me.aiUsage });
      setTimeout(() => setGroqKeySaved(false), 3000);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : "Failed to save Groq key";
      setGroqKeyError(msg);
    } finally {
      setGroqKeySaving(false);
    }
  };

  const handleTestGroqKey = async () => {
    setGroqKeyError(null);
    setGroqTestResult(null);
    const key = groqKeyInput.trim();
    if (!key.startsWith("gsk_") || key.length < 20) {
      setGroqKeyError("Invalid key format. Groq keys must begin with 'gsk_'.");
      return;
    }
    setGroqTesting(true);
    try {
      const res = await testGroqApiKey(key);
      if (res.valid) {
        setGroqTestResult(
          `Connection verified • Model: ${res.model || "Llama 3"} • Latency: ${res.latencyMs ?? 0}ms`
        );
      } else {
        setGroqKeyError(res.message || "Invalid API key");
      }
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : "Connection test failed";
      setGroqKeyError(msg);
    } finally {
      setGroqTesting(false);
    }
  };

  const handleRemoveGroqKey = async () => {
    try {
      await removeGroqApiKey();
      setGroqKeyInput("");
      queryClient.invalidateQueries({ queryKey: queryKeys.me.usage });
      queryClient.invalidateQueries({ queryKey: queryKeys.me.aiUsage });
    } catch {
      // Ignore
    }
  };

  // Sync state when user profile is loaded
  useEffect(() => {
    if (user) {
      if (user.name) setName(user.name);
      if (user.branch) setBranch(user.branch);
      if (user.gradYear) setGradYear(user.gradYear.toString());
      if (user.targetTrackId) setTargetTrackId(user.targetTrackId.toString());
    }
  }, [user]);

  const handleSaveProfile = async (e: React.FormEvent) => {
    e.preventDefault();
    setProfileError(null);
    setProfileSaved(false);

    const trimmedName = name.trim();
    if (!trimmedName || trimmedName.length > 100) {
      setProfileError("Full Name must be between 1 and 100 characters.");
      return;
    }

    const trimmedBranch = branch.trim();
    if (!trimmedBranch || trimmedBranch.length > 50) {
      setProfileError("Branch must be between 1 and 50 characters.");
      return;
    }

    const yearNum = parseInt(gradYear, 10);
    if (isNaN(yearNum) || yearNum < 2020 || yearNum > 2035) {
      setProfileError("Please select a valid graduation year.");
      return;
    }

    const trackNum = parseInt(targetTrackId, 10);
    if (isNaN(trackNum) || trackNum <= 0) {
      setProfileError("Please select a target track.");
      return;
    }

    setProfileSaving(true);
    try {
      await updateProfile({
        name: trimmedName,
        branch: trimmedBranch,
        gradYear: yearNum,
        targetTrackId: trackNum,
      });
      setProfileSaved(true);
      setTimeout(() => setProfileSaved(false), 4000);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : "Failed to update profile";
      setProfileError(msg);
    } finally {
      setProfileSaving(false);
    }
  };

  const handleLogout = async () => {
    await logout();
    router.replace("/login");
  };

  const handleLogoutAll = async () => {
    await logoutAll();
    router.replace("/login");
  };

  const handleDeleteAccount = async () => {
    if (deleteConfirmation !== "DELETE") {
      setDeleteError("You must type DELETE in all capital letters to confirm.");
      return;
    }

    setIsDeleting(true);
    setDeleteError(null);
    try {
      await deleteAccount();
      router.replace("/login");
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : "Failed to delete account";
      setDeleteError(msg);
      setIsDeleting(false);
    }
  };

  const isUsingOwnKey = usage?.usingOwnKey ?? usage?.hasCustomKey ?? false;
  const interviewLimit = usage?.interviews?.limit ?? (isUsingOwnKey ? null : (usage?.dailyQuota ?? 2));
  const interviewUsed = usage?.interviews?.used ?? usage?.usedToday ?? 0;
  const aiCallsLimit = usage?.aiCalls?.limit ?? (isUsingOwnKey ? null : (usage?.diagnosticQuota ?? 20));
  const aiCallsUsed = usage?.aiCalls?.used ?? usage?.diagnosticUsedToday ?? 0;

  return (
    <div className="w-full max-w-[1000px] mx-auto px-6 py-8 flex flex-col gap-8">
      {/* Header */}
      <div className="flex flex-col gap-1.5 border-b border-[#E5E5E5] pb-6">
        <div className="flex items-center gap-2">
          <span className="w-2 h-2 rounded-full bg-[#111111]" />
          <span className="font-mono text-[11px] uppercase tracking-wider text-[#8A8A8A]">
            Student Preferences • Account & Security
          </span>
        </div>
        <h2 className="text-[30px] font-semibold text-[#111111] tracking-tight">
          Settings & Credentials
        </h2>
        <p className="text-[13px] text-[#555555]">
          Manage your campus academic profile, active login sessions, and placement calibration.
        </p>
      </div>

      {/* 1. Academic Profile Card */}
      <Card>
        <CardHeader className="flex flex-col sm:flex-row sm:items-center justify-between pb-4 gap-2">
          <div>
            <CardTitle className="text-[18px]">Campus Academic Profile</CardTitle>
            <p className="text-[13px] text-[#8A8A8A] mt-1">
              Synced with the PathPrep backend to calibrate diagnostic drills and readiness rubrics.
            </p>
          </div>
          <Badge variant="outline" className="text-[11px] font-mono self-start sm:self-auto">
            Synced via PATCH /me
          </Badge>
        </CardHeader>

        <CardContent>
          <form onSubmit={handleSaveProfile} className="flex flex-col gap-5">
            {profileError && (
              <div className="p-3 rounded-lg bg-[#FEF2F2] border border-[#FEE2E2] text-xs text-[#991B1B]">
                {profileError}
              </div>
            )}

            <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
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

              {/* Email (Read-Only) */}
              <div className="flex flex-col gap-1.5">
                <label className="text-[13px] font-medium text-[#8A8A8A]">
                  Email Address (from Google)
                </label>
                <input
                  type="email"
                  disabled
                  value={user?.email || ""}
                  className="px-3.5 py-2.5 rounded-lg border border-[#E5E5E5] bg-[#FAFAFA] text-[14px] text-[#8A8A8A] cursor-not-allowed"
                />
              </div>

              {/* Branch / Major */}
              <div className="flex flex-col gap-1.5">
                <label className="text-[13px] font-medium text-[#111111]">
                  Branch / Major
                </label>
                <select
                  required
                  value={branch}
                  onChange={(e) => setBranch(e.target.value)}
                  className={`px-3.5 py-2.5 rounded-lg border border-[#E5E5E5] focus:border-[#111111] focus:outline-none text-[14px] bg-white ${
                    branch ? "text-[#111111]" : "text-[#8A8A8A]"
                  }`}
                >
                  <option value="" disabled>Select Branch / Major</option>
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
                  <option value="2026">2026 (Pre-final Year)</option>
                  <option value="2027">2027 (Sophomore)</option>
                  <option value="2028">2028 (Freshman)</option>
                </select>
              </div>

              {/* Target Track */}
              <div className="flex flex-col gap-1.5 md:col-span-2">
                <label className="text-[13px] font-medium text-[#111111]">
                  Target Career Track
                </label>
                <select
                  required
                  value={targetTrackId}
                  onChange={(e) => setTargetTrackId(e.target.value)}
                  className={`px-3.5 py-2.5 rounded-lg border border-[#E5E5E5] focus:border-[#111111] focus:outline-none text-[14px] bg-white ${
                    targetTrackId ? "text-[#111111]" : "text-[#8A8A8A]"
                  }`}
                >
                  <option value="" disabled>Select Target Track</option>
                  {(tracks || [
                    { id: 1, name: "Java Backend Engineer" },
                    { id: 2, name: "Web Frontend Engineer" },
                    { id: 3, name: "General SDE (Campus Core)" },
                  ]).map((t) => (
                    <option key={t.id} value={t.id}>
                      {t.name}
                    </option>
                  ))}
                </select>
              </div>
            </div>

            <div className="flex items-center justify-between pt-4 border-t border-[#E5E5E5]/60 mt-2">
              <span className="font-mono text-[11px] text-[#8A8A8A]">
                Role: {user?.role || "STUDENT"}
              </span>
              <div className="flex items-center gap-3">
                {profileSaved && (
                  <span className="text-[12px] text-[#16A34A] font-medium flex items-center gap-1">
                    <Check className="w-3.5 h-3.5" /> Saved to server!
                  </span>
                )}
                <Button type="submit" size="sm" disabled={profileSaving} className="gap-2">
                  <Save className="w-3.5 h-3.5" />
                  <span>{profileSaving ? "Saving..." : "Save Profile"}</span>
                </Button>
              </div>
            </div>
          </form>
        </CardContent>
      </Card>

      {/* 2. Session & Security Card */}
      <Card>
        <CardHeader className="flex flex-col sm:flex-row sm:items-center justify-between pb-4 gap-2">
          <div>
            <CardTitle className="text-[18px]">Session & Security</CardTitle>
            <p className="text-[13px] text-[#8A8A8A] mt-1">
              Active authentication sessions, refresh tokens and sign-out controls.
            </p>
          </div>
          <Badge variant="outline" className="text-[11px] font-mono self-start sm:self-auto">
            HttpOnly Cookies
          </Badge>
        </CardHeader>

        <CardContent className="flex flex-col gap-4">
          <div className="p-4 rounded-xl bg-[#FAFAFA] border border-[#E5E5E5] flex flex-col sm:flex-row sm:items-center justify-between gap-4">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-xl bg-white border border-[#E5E5E5] flex items-center justify-center text-[#111111]">
                <Laptop className="w-5 h-5" />
              </div>
              <div className="flex flex-col">
                <span className="text-[14px] font-semibold text-[#111111]">
                  Current Browser Session
                </span>
                <span className="text-[12px] text-[#8A8A8A]">
                  Signed in as {user?.email}
                </span>
              </div>
            </div>

            <div className="flex items-center gap-2">
              <Button
                variant="outline"
                size="sm"
                onClick={handleLogout}
                className="gap-2 text-xs"
              >
                <LogOut className="w-3.5 h-3.5 text-[#555555]" />
                <span>Sign Out</span>
              </Button>

              <Button
                variant="outline"
                size="sm"
                onClick={handleLogoutAll}
                className="gap-2 text-xs text-[#DC2626] border-[#FCA5A5] hover:bg-[#FEF2F2]"
              >
                <LogOut className="w-3.5 h-3.5 text-[#DC2626]" />
                <span>Sign Out All Devices</span>
              </Button>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* 3. AI Credits & Daily Quota Card */}
      <Card>
        <CardHeader className="flex flex-col sm:flex-row sm:items-center justify-between pb-4 gap-2">
          <div>
            <div className="flex items-center gap-2">
              <Zap className="w-4 h-4 text-[#111111]" />
              <CardTitle className="text-[18px]">AI Credits &amp; Daily Quota</CardTitle>
            </div>
            <p className="text-[13px] text-[#555555] mt-1">
              You receive daily AI-simulated mock interview rounds and AI feedback requests.
            </p>
          </div>
          <Badge
            variant={isUsingOwnKey ? "success" : "default"}
            className="text-[11px] self-start sm:self-auto"
          >
            {isUsingOwnKey ? "Active Tier: Unlimited (Custom Key)" : "Active Tier: Campus Free"}
          </Badge>
        </CardHeader>

        <CardContent className="flex flex-col gap-6">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            {/* Metric 1: Interviews */}
            <div className="p-4 rounded-xl bg-[#FAFAFA] border border-[#E5E5E5] flex flex-col justify-between gap-4">
              <div>
                <div className="flex items-center justify-between mb-1.5">
                  <span className="text-[14px] font-semibold text-[#111111] flex items-center gap-1.5">
                    Mock Interview Sessions
                  </span>
                  <span className="font-mono text-[12px] font-semibold text-[#111111]">
                    {interviewLimit === null
                      ? `${interviewUsed} used (Unlimited)`
                      : `${interviewUsed} of ${interviewLimit} used today`}
                  </span>
                </div>
                <p className="text-[12px] text-[#555555] mb-3">
                  Live conversational interviewer sessions (LITE / LIVE modes).
                </p>
                <Progress
                  value={interviewLimit === null ? 100 : Math.min(100, (interviewUsed / Math.max(1, interviewLimit)) * 100)}
                  className="h-2"
                />
              </div>

              <div className="pt-2 border-t border-[#E5E5E5]/60 flex items-center justify-between font-mono text-[11px] text-[#8A8A8A]">
                <span className="flex items-center gap-1">
                  <Clock className="w-3 h-3" />
                  {usage?.resetAt ? `Resets: ${new Date(usage.resetAt).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })}` : "Resets daily at 00:00 UTC"}
                </span>
                <span className="text-[#111111] font-medium">
                  {interviewLimit === null
                    ? "Unlimited"
                    : `${Math.max(0, interviewLimit - interviewUsed)} remaining`}
                </span>
              </div>
            </div>

            {/* Metric 2: AI feedback requests */}
            <div className="p-4 rounded-xl bg-[#FAFAFA] border border-[#E5E5E5] flex flex-col justify-between gap-4">
              <div>
                <div className="flex items-center justify-between mb-1.5">
                  <span className="text-[14px] font-semibold text-[#111111] flex items-center gap-1.5">
                    AI feedback requests
                  </span>
                  <span className="font-mono text-[12px] font-semibold text-[#111111]">
                    {aiCallsLimit === null
                      ? `${aiCallsUsed} used (Unlimited)`
                      : `${aiCallsUsed} of ${aiCallsLimit} used today`}
                  </span>
                </div>
                <p className="text-[12px] text-[#555555] mb-3">
                  Grading turns, rubric evaluation, and candidate answer critique.
                </p>
                <Progress
                  value={aiCallsLimit === null ? 100 : Math.min(100, (aiCallsUsed / Math.max(1, aiCallsLimit)) * 100)}
                  className="h-2"
                />
              </div>

              <div className="pt-2 border-t border-[#E5E5E5]/60 flex items-center justify-between font-mono text-[11px] text-[#8A8A8A]">
                <span className="flex items-center gap-1">
                  <Clock className="w-3 h-3" />
                  {usage?.resetAt ? `Resets: ${new Date(usage.resetAt).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })}` : "Resets daily at 00:00 UTC"}
                </span>
                <span className="text-[#111111] font-medium">
                  {aiCallsLimit === null
                    ? "Unlimited"
                    : `${Math.max(0, aiCallsLimit - aiCallsUsed)} remaining`}
                </span>
              </div>
            </div>
          </div>

          {usage?.pool && (
            <div className="p-3 rounded-lg bg-white border border-[#E5E5E5] flex items-center justify-between text-xs">
              <span className="text-[#555555]">
                Shared AI Engine: <strong className="text-[#111111] font-medium">{usage.pool.name || "Campus Shared Pool"}</strong>
              </span>
              <span className="font-mono text-[#8A8A8A]">
                Status: {usage.pool.available !== false ? "Online" : "Busy"}
                {usage.pool.remainingCapacityPct !== undefined ? ` • Capacity: ${usage.pool.remainingCapacityPct}%` : ""}
              </span>
            </div>
          )}

          <div className="p-3.5 rounded-lg bg-[#FAFAFA] border border-[#E5E5E5] flex items-start gap-2.5 text-[12px] text-[#555555]">
            <Sparkles className="w-4 h-4 text-[#111111] shrink-0 mt-0.5" />
            <div>
              <span className="font-semibold text-[#111111]">Need unlimited interview simulations for upcoming placement season drives? </span>
              <span>Add your personal Groq API key below to bypass daily quota limits completely. Zero usage throttling, ultra-low latency response times.</span>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* 4. Groq API Key (Bring Your Own Key) Card */}
      <Card>
        <CardHeader className="flex flex-col sm:flex-row sm:items-center justify-between pb-4 gap-2">
          <div>
            <div className="flex items-center gap-2">
              <Key className="w-4 h-4 text-[#111111]" />
              <CardTitle className="text-[18px]">Use your own Groq API key</CardTitle>
            </div>
            <p className="text-[13px] text-[#555555] mt-1">
              Keep your practice completely free and remove all daily AI credit limits.
            </p>
          </div>
          <Badge
            variant={isUsingOwnKey ? "success" : "default"}
            className="text-[11px] self-start sm:self-auto flex items-center gap-1"
          >
            {isUsingOwnKey ? (
              <>
                <span className="w-1.5 h-1.5 rounded-full bg-[#16A34A]" />
                <span>Configured &amp; Active {usage?.keyLast4 ? `(••••${usage.keyLast4})` : ""}</span>
              </>
            ) : (
              <span>Optional • Unlimited Simulations</span>
            )}
          </Badge>
        </CardHeader>

        <CardContent className="flex flex-col gap-4">
          <p className="text-[13px] text-[#555555] leading-relaxed">
            Keep your practice completely free and remove all daily AI credit limits by providing your personal Groq API key. Your key is stored securely in encrypted server storage and never in the browser. Inference calls are routed directly to Groq endpoints (<span className="font-mono text-xs bg-[#EEEEEE] px-1 py-0.5 rounded text-[#111111]">Llama 3 70B</span>).
          </p>

          <form onSubmit={handleSaveGroqKey} className="flex flex-col gap-3">
            {groqKeyError && (
              <div className="p-3 rounded-lg bg-[#FEF2F2] border border-[#FEE2E2] text-xs text-[#991B1B]">
                {groqKeyError}
              </div>
            )}
            {groqTestResult && (
              <div className="p-3 rounded-lg bg-[#DCFCE7] border border-[#BBF7D0] text-xs text-[#16A34A] flex items-center gap-1.5">
                <Check className="w-4 h-4" />
                <span>{groqTestResult}</span>
              </div>
            )}

            <div className="flex flex-col gap-1.5">
              <div className="flex items-center justify-between">
                <label className="text-[13px] font-medium text-[#111111]">
                  Groq API Key
                </label>
                {isUsingOwnKey && (
                  <span className="text-[11px] text-[#16A34A] font-mono flex items-center gap-1">
                    <Check className="w-3.5 h-3.5" /> Key currently active {usage?.keyLast4 ? `(ending in ${usage.keyLast4})` : ""}
                  </span>
                )}
              </div>

              <div className="relative flex items-center">
                <input
                  type={showGroqKey ? "text" : "password"}
                  value={groqKeyInput}
                  onChange={(e) => setGroqKeyInput(e.target.value)}
                  placeholder="gsk_••••••••••••••••••••••••••••••••"
                  className="w-full px-3.5 py-2.5 rounded-lg border border-[#E5E5E5] focus:border-[#111111] focus:outline-none text-[13px] font-mono text-[#111111] pr-16 bg-white placeholder:text-[#AAAAAA]"
                />
                <div className="absolute right-2 flex items-center gap-1">
                  <button
                    type="button"
                    onClick={() => setShowGroqKey(!showGroqKey)}
                    className="p-1 rounded text-[#8A8A8A] hover:text-[#111111] hover:bg-[#F4F3F3] text-xs"
                    title={showGroqKey ? "Hide key" : "Show key"}
                  >
                    {showGroqKey ? "Hide" : "Show"}
                  </button>
                </div>
              </div>
              <span className="text-[11px] text-[#8A8A8A]">
                The key field is cleared after saving. Your key is never stored in browser cookies, localStorage, or sessionStorage.
              </span>
            </div>

            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pt-2">
              <div className="flex items-center gap-2 flex-wrap">
                <Button
                  type="submit"
                  size="sm"
                  disabled={groqKeySaving || !groqKeyInput.trim()}
                  className="gap-2"
                >
                  <Save className="w-3.5 h-3.5" />
                  <span>{groqKeySaving ? "Saving..." : "Save Key"}</span>
                </Button>

                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  disabled={groqTesting || !groqKeyInput.trim()}
                  onClick={handleTestGroqKey}
                  className="gap-1.5"
                >
                  <span>{groqTesting ? "Testing..." : "Test Connection"}</span>
                </Button>

                {isUsingOwnKey && (
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    onClick={handleRemoveGroqKey}
                    className="text-xs text-[#DC2626] border-[#FCA5A5] hover:bg-[#FEF2F2]"
                  >
                    Remove Key
                  </Button>
                )}

                {groqKeySaved && (
                  <span className="text-[12px] text-[#16A34A] font-medium flex items-center gap-1">
                    <Check className="w-3.5 h-3.5" /> Key Saved! Field cleared.
                  </span>
                )}
              </div>

              <a
                href="https://console.groq.com/keys"
                target="_blank"
                rel="noopener noreferrer"
                className="text-[12px] text-[#555555] hover:text-[#111111] transition-colors underline flex items-center gap-1 self-start sm:self-auto"
              >
                <span>How to get a free Groq API key in 60 seconds</span>
                <span className="text-[10px]">↗</span>
              </a>
            </div>
          </form>

          {/* Privacy Notice in Settings */}
          <div className="p-3.5 rounded-lg bg-[#FAFAFA] border border-[#E5E5E5] text-[12px] text-[#555555] flex flex-col gap-1 mt-2">
            <span className="font-semibold text-[#111111]">Privacy Notice</span>
            <p>
              Your answers and code snippets during mock interviews are sent to an AI provider (Groq / Llama) for rubric scoring and evaluation. No personally identifiable account data is included with inference prompts. Your custom API key is encrypted on the server and is never stored in browser storage.
            </p>
          </div>
        </CardContent>
      </Card>

      {/* 4. Danger Zone: Delete Account */}
      <Card className="border-[#FCA5A5] bg-[#FEF2F2]/30">
        <CardHeader className="flex flex-col sm:flex-row sm:items-center justify-between pb-4 gap-2">
          <div>
            <div className="flex items-center gap-2">
              <AlertTriangle className="w-4 h-4 text-[#DC2626]" />
              <CardTitle className="text-[18px] text-[#991B1B]">Danger Zone</CardTitle>
            </div>
            <p className="text-[13px] text-[#7F1D1D] mt-1">
              Permanently delete your PathPrep account, practice history, and skill mastery metrics.
            </p>
          </div>
          <Badge variant="critical" className="text-[11px] font-mono self-start sm:self-auto">
            Irreversible
          </Badge>
        </CardHeader>

        <CardContent className="flex flex-col gap-4">
          <p className="text-[13px] text-[#7F1D1D] leading-relaxed">
            Deleting your account will erase all session records, spaced repetition intervals, and streak statistics. This action cannot be undone.
          </p>

          {!showDeleteModal ? (
            <div>
              <Button
                variant="destructive"
                size="sm"
                onClick={() => setShowDeleteModal(true)}
                className="gap-2 text-xs"
              >
                <Trash2 className="w-3.5 h-3.5" />
                <span>Delete Account</span>
              </Button>
            </div>
          ) : (
            <div className="p-4 rounded-xl bg-white border border-[#FCA5A5] flex flex-col gap-3 max-w-md">
              <span className="text-[13px] font-semibold text-[#991B1B]">
                Type &quot;DELETE&quot; below to permanently erase your account:
              </span>

              {deleteError && (
                <div className="text-xs text-[#DC2626] font-medium">{deleteError}</div>
              )}

              <input
                type="text"
                value={deleteConfirmation}
                onChange={(e) => setDeleteConfirmation(e.target.value)}
                placeholder="DELETE"
                className="px-3 py-2 rounded-lg border border-[#FCA5A5] text-[13px] font-mono text-[#991B1B] focus:outline-none focus:ring-1 focus:ring-[#DC2626]"
              />

              <div className="flex items-center gap-2 pt-1">
                <Button
                  variant="destructive"
                  size="sm"
                  disabled={isDeleting || deleteConfirmation !== "DELETE"}
                  onClick={handleDeleteAccount}
                  className="gap-1.5 text-xs"
                >
                  <Trash2 className="w-3.5 h-3.5" />
                  <span>{isDeleting ? "Deleting..." : "Permanently Delete"}</span>
                </Button>

                <Button
                  variant="outline"
                  size="sm"
                  disabled={isDeleting}
                  onClick={() => {
                    setShowDeleteModal(false);
                    setDeleteConfirmation("");
                    setDeleteError(null);
                  }}
                  className="text-xs"
                >
                  Cancel
                </Button>
              </div>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
