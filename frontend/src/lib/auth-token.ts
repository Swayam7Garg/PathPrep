// In-memory access token storage and security helpers
// STRICT RULE: The access token lives ONLY in memory. Never localStorage, sessionStorage or a cookie.

let inMemoryAccessToken: string | null = null;
type TokenListener = (token: string | null) => void;
const listeners = new Set<TokenListener>();

export function getAccessToken(): string | null {
  return inMemoryAccessToken;
}

export function setAccessToken(token: string | null): void {
  inMemoryAccessToken = token;
  listeners.forEach((listener) => {
    try {
      listener(token);
    } catch {
      // Ignore listener error
    }
  });
}

export function subscribeAccessToken(listener: TokenListener): () => void {
  listeners.add(listener);
  return () => {
    listeners.delete(listener);
  };
}

/**
 * Sanitizes "next" redirect parameter to avoid open redirects.
 * Must match /^\/[^/]* / and NOT match //, http:, https:, javascript:
 */
export function sanitizeRedirectUrl(next: string | null | undefined, fallback: string = "/dashboard"): string {
  if (!next) return fallback;
  const trimmed = next.trim();
  
  // Must start with single / and not double // or /\
  if (!trimmed.startsWith("/") || trimmed.startsWith("//") || trimmed.startsWith("/\\")) {
    return fallback;
  }
  
  // Reject protocol strings or javascript execution
  const lower = trimmed.toLowerCase();
  if (
    lower.includes("://") ||
    lower.includes("javascript:") ||
    lower.includes("data:") ||
    lower.includes("vbscript:")
  ) {
    return fallback;
  }

  return trimmed;
}
