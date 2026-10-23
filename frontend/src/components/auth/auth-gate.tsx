"use client";

import React, { useEffect } from "react";
import { useRouter, usePathname } from "next/navigation";
import { useAuth } from "@/components/providers/auth-provider";
import { Sparkles } from "lucide-react";

export function AuthGate({ children }: { children: React.ReactNode }) {
  const { user, status } = useAuth();
  const router = useRouter();
  const pathname = usePathname();

  useEffect(() => {
    if (status === "unauthenticated") {
      const nextParam = pathname ? `?next=${encodeURIComponent(pathname)}` : "";
      router.replace(`/login${nextParam}`);
    } else if (status === "authenticated") {
      if (!user?.onboardingCompleted && pathname !== "/onboarding") {
        router.replace("/onboarding");
      }
    }
  }, [status, user, pathname, router]);

  if (status === "loading") {
    return (
      <div className="min-h-screen bg-[#FAFAFA] flex flex-col items-center justify-center p-6">
        <div className="flex flex-col items-center gap-4 text-center">
          <div className="w-12 h-12 rounded-xl bg-white border border-[#E5E5E5] flex items-center justify-center shadow-sm animate-pulse">
            <span className="font-semibold text-lg text-[#111111]">P</span>
          </div>
          <div className="flex flex-col gap-1">
            <h3 className="text-[15px] font-semibold text-[#111111]">
              Securing session
            </h3>
            <p className="text-[12px] text-[#8A8A8A]">
              Verifying credentials with PathPrep server...
            </p>
          </div>
        </div>
      </div>
    );
  }

  if (status === "unauthenticated") {
    return null;
  }

  if (status === "authenticated" && !user?.onboardingCompleted && pathname !== "/onboarding") {
    return null;
  }

  return <>{children}</>;
}
