"use client";

import React, { useEffect, useRef, useState } from "react";
import Script from "next/script";

interface GoogleSignInButtonProps {
  onSuccess: (credential: string) => void;
  onError: (errorMsg: string) => void;
  disabled?: boolean;
}

declare global {
  interface Window {
    google?: {
      accounts: {
        id: {
          initialize: (config: {
            client_id: string;
            callback: (res: { credential: string }) => void;
            auto_select?: boolean;
            cancel_on_tap_outside?: boolean;
          }) => void;
          renderButton: (
            element: HTMLElement,
            options: {
              theme?: "outline" | "filled_blue" | "filled_black";
              size?: "large" | "medium" | "small";
              text?: "signin_with" | "signup_with" | "continue_with" | "signin";
              shape?: "rectangular" | "pill" | "circle" | "square";
              logo_alignment?: "left" | "center";
              width?: number | string;
            }
          ) => void;
          prompt?: () => void;
        };
      };
    };
  }
}

export function GoogleSignInButton({
  onSuccess,
  onError,
  disabled = false,
}: GoogleSignInButtonProps) {
  const containerRef = useRef<HTMLDivElement>(null);
  const [scriptLoaded, setScriptLoaded] = useState(false);
  const clientId = process.env.NEXT_PUBLIC_GOOGLE_CLIENT_ID || "";

  useEffect(() => {
    if (!scriptLoaded || !containerRef.current) return;

    if (!clientId) {
      return;
    }

    try {
      if (window.google?.accounts?.id) {
        window.google.accounts.id.initialize({
          client_id: clientId,
          callback: (response) => {
            if (response.credential) {
              onSuccess(response.credential);
            } else {
              onError("No credential returned from Google Identity Services");
            }
          },
        });

        containerRef.current.innerHTML = "";
        window.google.accounts.id.renderButton(containerRef.current, {
          theme: "outline",
          size: "large",
          shape: "rectangular",
          text: "continue_with",
          logo_alignment: "left",
          width: 320,
        });
      }
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : "Failed to render Google Sign-In button";
      onError(msg);
    }
  }, [scriptLoaded, clientId, onSuccess, onError]);

  return (
    <div className="w-full flex flex-col items-center">
      <Script
        src="https://accounts.google.com/gsi/client"
        strategy="afterInteractive"
        onLoad={() => setScriptLoaded(true)}
      />

      <div
        ref={containerRef}
        className={`min-h-[44px] flex items-center justify-center ${
          disabled ? "pointer-events-none opacity-50" : ""
        }`}
      />

      {!clientId && (
        <div className="p-3 bg-[#FFFBEB] border border-[#FEF3C7] rounded-lg text-xs text-[#92400E] max-w-sm text-center">
          <code>NEXT_PUBLIC_GOOGLE_CLIENT_ID</code> is not configured. Google Sign-In requires a Google Cloud OAuth Client ID.
        </div>
      )}
    </div>
  );
}
