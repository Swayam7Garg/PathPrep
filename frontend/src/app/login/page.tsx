"use client";

import React, { useState, useEffect, Suspense } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { useAuth } from "@/components/providers/auth-provider";
import { GoogleSignInButton } from "@/components/auth/google-sign-in-button";
import { sanitizeRedirectUrl } from "@/lib/auth-token";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { ShieldCheck, AlertCircle, Sparkles, Terminal } from "lucide-react";

function LoginForm() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const rawNext = searchParams.get("next");
  const targetRedirect = sanitizeRedirectUrl(rawNext, "/dashboard");

  const { user, status, loginWithGoogle, devLogin } = useAuth();
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  // If already authenticated, redirect immediately
  useEffect(() => {
    if (status === "authenticated" && user) {
      if (!user.onboardingCompleted) {
        router.replace("/onboarding");
      } else {
        router.replace(targetRedirect);
      }
    }
  }, [status, user, router, targetRedirect]);

  const handleGoogleSuccess = async (credential: string) => {
    setIsSubmitting(true);
    setErrorMessage(null);
    try {
      await loginWithGoogle(credential);
      // AuthProvider will update user & status, triggering useEffect redirect
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : "Authentication failed. Please try again.";
      setErrorMessage(msg);
      setIsSubmitting(false);
    }
  };

  const handleDevLogin = async () => {
    setIsSubmitting(true);
    setErrorMessage(null);
    try {
      await devLogin();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : "Dev login failed. Check backend connection.";
      setErrorMessage(msg);
      setIsSubmitting(false);
    }
  };

  if (status === "loading") {
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
      <div className="w-full max-w-[440px] flex flex-col gap-6">
        {/* Brand identity */}
        <div className="flex flex-col items-center text-center gap-2">
          <div className="flex items-center gap-2.5">
            <div className="w-9 h-9 rounded-xl bg-[#111111] text-white flex items-center justify-center font-bold text-sm tracking-tight shadow-sm">
              P
            </div>
            <span className="text-[20px] font-bold text-[#111111] tracking-tight">
              PathPrep
            </span>
            <Badge variant="outline" className="text-[10px] font-mono py-0 h-5">
              Campus v2
            </Badge>
          </div>
          <p className="text-[13px] text-[#555555] max-w-sm mt-1">
            Zero-anxiety career track preparation, active spaced-repetition drills, and benchmark readiness.
          </p>
        </div>

        {/* Login Card */}
        <Card className="border-[#E5E5E5] shadow-sm">
          <CardHeader className="pb-4 text-center">
            <CardTitle className="text-[19px] font-semibold text-[#111111]">
              Sign in to your account
            </CardTitle>
            <p className="text-[13px] text-[#8A8A8A] mt-1">
              Continue with your Google college or personal email
            </p>
          </CardHeader>

          <CardContent className="flex flex-col gap-5">
            {errorMessage && (
              <div className="p-3 rounded-lg bg-[#FEF2F2] border border-[#FEE2E2] flex items-start gap-2.5 text-xs text-[#991B1B]">
                <AlertCircle className="w-4 h-4 shrink-0 mt-0.5 text-[#DC2626]" />
                <div className="flex-1">
                  <p className="font-medium">Authentication Error</p>
                  <p className="mt-0.5">{errorMessage}</p>
                </div>
              </div>
            )}

            {/* Google Identity Services button */}
            <div className="flex flex-col items-center justify-center py-2">
              <GoogleSignInButton
                disabled={isSubmitting}
                onSuccess={handleGoogleSuccess}
                onError={(msg) => setErrorMessage(msg)}
              />
            </div>

            {/* Developer Fast-Login (Only available when backend dev profile is active) */}
            <div className="relative flex py-1 items-center">
              <div className="flex-grow border-t border-[#E5E5E5]" />
              <span className="flex-shrink mx-3 text-[11px] font-mono uppercase text-[#8A8A8A]">
                Local Dev Quick-Access
              </span>
              <div className="flex-grow border-t border-[#E5E5E5]" />
            </div>

            <Button
              type="button"
              variant="outline"
              disabled={isSubmitting}
              onClick={handleDevLogin}
              className="w-full gap-2 text-xs font-mono border-dashed"
            >
              <Terminal className="w-3.5 h-3.5 text-[#555555]" />
              <span>Sign in with Dev Test Account</span>
            </Button>

            {/* Security Guarantee */}
            <div className="pt-3 border-t border-[#E5E5E5]/60 flex items-start gap-2 text-[11px] text-[#8A8A8A] leading-relaxed">
              <ShieldCheck className="w-4 h-4 shrink-0 text-[#16A34A] mt-0.5" />
              <span>
                Tokens stored in memory only. Refresh tokens are secured in HttpOnly SameSite browser cookies.
              </span>
            </div>
          </CardContent>
        </Card>

        {/* Footer info */}
        <p className="text-center text-[12px] text-[#8A8A8A]">
          Need help? Contact your campus placement coordinator.
        </p>
      </div>
    </div>
  );
}

export default function LoginPage() {
  return (
    <Suspense
      fallback={
        <div className="min-h-screen bg-[#FAFAFA] flex items-center justify-center">
          <div className="w-8 h-8 rounded-full border-2 border-[#111111] border-t-transparent animate-spin" />
        </div>
      }
    >
      <LoginForm />
    </Suspense>
  );
}
