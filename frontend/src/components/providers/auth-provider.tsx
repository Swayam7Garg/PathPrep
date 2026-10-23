"use client";

import React, {
  createContext,
  useContext,
  useEffect,
  useState,
  useCallback,
  useMemo,
} from "react";
import { UserProfile, UpdateUserRequest } from "@/lib/types";
import {
  loginWithGoogle as apiLoginWithGoogle,
  refreshToken as apiRefreshToken,
  logout as apiLogout,
  logoutAll as apiLogoutAll,
  devLogin as apiDevLogin,
  updateMe as apiUpdateMe,
  deleteMe as apiDeleteMe,
  getMe as apiGetMe,
} from "@/lib/api";
import { setAccessToken } from "@/lib/auth-token";
import { useQueryClient } from "@tanstack/react-query";

export type AuthStatus = "loading" | "authenticated" | "unauthenticated";

interface AuthContextValue {
  user: UserProfile | null;
  status: AuthStatus;
  loginWithGoogle: (credential: string) => Promise<void>;
  devLogin: (email?: string) => Promise<void>;
  logout: () => Promise<void>;
  logoutAll: () => Promise<void>;
  updateProfile: (patch: UpdateUserRequest) => Promise<UserProfile>;
  deleteAccount: () => Promise<void>;
  refreshSession: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<UserProfile | null>(null);
  const [status, setStatus] = useState<AuthStatus>("loading");
  const queryClient = useQueryClient();

  // Initial silent refresh on app load
  const refreshSession = useCallback(async () => {
    try {
      const res = await apiRefreshToken();
      setUser(res.user);
      setStatus("authenticated");
    } catch {
      setAccessToken(null);
      setUser(null);
      setStatus("unauthenticated");
    }
  }, []);

  useEffect(() => {
    refreshSession();
  }, [refreshSession]);

  const loginWithGoogle = useCallback(
    async (credential: string) => {
      try {
        const res = await apiLoginWithGoogle(credential);
        setUser(res.user);
        setStatus("authenticated");
      } catch (err) {
        setStatus("unauthenticated");
        throw err;
      }
    },
    []
  );

  const devLogin = useCallback(
    async (email?: string) => {
      try {
        const res = await apiDevLogin(email);
        setUser(res.user);
        setStatus("authenticated");
      } catch (err) {
        setStatus("unauthenticated");
        throw err;
      }
    },
    []
  );

  const logout = useCallback(async () => {
    try {
      await apiLogout();
    } catch {
      // Ignore errors on logout
    } finally {
      setUser(null);
      setStatus("unauthenticated");
      queryClient.clear();
    }
  }, [queryClient]);

  const logoutAll = useCallback(async () => {
    try {
      await apiLogoutAll();
    } catch {
      // Ignore errors
    } finally {
      setUser(null);
      setStatus("unauthenticated");
      queryClient.clear();
    }
  }, [queryClient]);

  const updateProfile = useCallback(
    async (patch: UpdateUserRequest): Promise<UserProfile> => {
      const updated = await apiUpdateMe(patch);
      setUser(updated);
      return updated;
    },
    []
  );

  const deleteAccount = useCallback(async () => {
    try {
      await apiDeleteMe();
    } finally {
      setUser(null);
      setStatus("unauthenticated");
      queryClient.clear();
    }
  }, [queryClient]);

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      status,
      loginWithGoogle,
      devLogin,
      logout,
      logoutAll,
      updateProfile,
      deleteAccount,
      refreshSession,
    }),
    [
      user,
      status,
      loginWithGoogle,
      devLogin,
      logout,
      logoutAll,
      updateProfile,
      deleteAccount,
      refreshSession,
    ]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return context;
}
