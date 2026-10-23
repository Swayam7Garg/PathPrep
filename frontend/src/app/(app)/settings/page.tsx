"use client";

import { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import { useQuery } from "@tanstack/react-query";
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
import { getAiUsage, getTracks } from "@/lib/api";
import { queryKeys } from "@/lib/query-keys";
import { useAuth } from "@/components/providers/auth-provider";

export default function SettingsPage() {
  const router = useRouter();
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

  // Danger zone state
  const [showDeleteModal, setShowDeleteModal] = useState(false);
  const [deleteConfirmation, setDeleteConfirmation] = useState("");
  const [deleteError, setDeleteError] = useState<string | null>(null);
  const [isDeleting, setIsDeleting] = useState(false);

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

      {/* 3. AI Credits & Daily Quota Card (Phase 8 - Not Available Yet) */}
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
