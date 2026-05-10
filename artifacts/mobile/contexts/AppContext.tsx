import React, { createContext, useContext, useState, useEffect, ReactNode } from "react";
import AsyncStorage from "@react-native-async-storage/async-storage";
import { Platform } from "react-native";

export type Partner = "A" | "B";

export interface UserProfile {
  name: string;
  partner: Partner;
  roomCode: string;
  partnerName: string;
  /**
   * Base URL of the user's API server (e.g. "https://together.example.com").
   * No trailing slash. Set during onboarding; editable in Settings.
   */
  serverUrl: string;
}

interface AppContextValue {
  profile: UserProfile | null;
  setProfile: (profile: UserProfile | null) => void;
  updateProfile: (partial: Partial<UserProfile>) => Promise<void>;
  clearProfile: () => Promise<void>;
  selectedDate: Date;
  setSelectedDate: (date: Date) => void;
  isLoading: boolean;
}

const AppContext = createContext<AppContextValue | null>(null);

const PROFILE_KEY = "@couple_planner_profile";

/**
 * Default server URL handling:
 * - If `EXPO_PUBLIC_DOMAIN` is provided (CI/dev), use it
 * - Otherwise prefer emulator-local host for Android (`10.0.2.2`)
 * - Fallback to `localhost` for iOS/simulator and desktop
 */
export const DEFAULT_SERVER_URL: string = process.env.EXPO_PUBLIC_DOMAIN
  ? `https://${process.env.EXPO_PUBLIC_DOMAIN}`
  : Platform.OS === "android"
  ? "http://10.0.2.2:8080"
  : "http://localhost:8080";

export function normalizeServerUrl(raw: string): string {
  const trimmed = raw.trim().replace(/\/+$/, "");
  if (!trimmed) return "";
  if (/^https?:\/\//i.test(trimmed)) return trimmed;
  // For local development, allow http URLs
  if (trimmed.includes("localhost") || trimmed.includes("127.0.0.1") || trimmed.includes("10.0.2.2")) {
    return `http://${trimmed}`;
  }
  return `https://${trimmed}`;
}

export function AppProvider({ children }: { children: ReactNode }) {
  const [profile, setProfileState] = useState<UserProfile | null>(null);
  const [selectedDate, setSelectedDate] = useState(new Date());
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    let mounted = true;
    (async () => {
      try {
        const raw = await AsyncStorage.getItem(PROFILE_KEY);
        if (raw && mounted) {
          try {
            const parsed = JSON.parse(raw) as Partial<UserProfile>;
            // Backfill serverUrl for profiles created before the self-host pivot
            if (!parsed.serverUrl) {
              parsed.serverUrl = DEFAULT_SERVER_URL;
            }
            // If we still have no server URL (standalone build with no env default
            // and a legacy profile), the app cannot talk to anything. Drop the
            // profile so the user is routed back to onboarding to enter one,
            // instead of landing in tabs with broken API calls.
            if (!parsed.serverUrl) {
              await AsyncStorage.removeItem(PROFILE_KEY);
            } else {
              setProfileState(parsed as UserProfile);
            }
          } catch (e) {
            console.warn("Failed to parse profile JSON", e);
            await AsyncStorage.removeItem(PROFILE_KEY);
          }
        }
      } catch (e) {
        console.warn("AppProvider init failed", e);
      } finally {
        if (mounted) setIsLoading(false);
      }
    })();
    return () => {
      mounted = false;
    };
  }, []);

  const setProfile = (p: UserProfile | null) => {
    setProfileState(p);
    if (p) {
      AsyncStorage.setItem(PROFILE_KEY, JSON.stringify(p));
    } else {
      AsyncStorage.removeItem(PROFILE_KEY);
    }
  };

  const updateProfile = async (partial: Partial<UserProfile>) => {
    if (!profile) return;
    const next: UserProfile = { ...profile, ...partial };

    // If a name field changed, push it to the backend room
    const myNameChanged = partial.name !== undefined && partial.name !== profile.name;
    const partnerNameChanged =
      partial.partnerName !== undefined && partial.partnerName !== profile.partnerName;
    const roleChanged = partial.partner !== undefined && partial.partner !== profile.partner;

    if (myNameChanged || partnerNameChanged || roleChanged) {
      // Determine which name maps to which slot, based on the *new* role
      const myRole = next.partner;
      const partnerAName = myRole === "A" ? next.name : next.partnerName;
      const partnerBName = myRole === "B" ? next.name : next.partnerName;

      try {
        const res = await fetch(`${next.serverUrl}/api/rooms/${next.roomCode}`, {
          method: "PATCH",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ partnerAName, partnerBName }),
        });
        if (!res.ok) {
          const err = await res.json().catch(() => ({ error: "Update failed" }));
          throw new Error(err.error || "Update failed");
        }
      } catch (e) {
        // Re-throw so caller can show an error; don't persist locally on failure
        throw e;
      }
    }

    setProfileState(next);
    await AsyncStorage.setItem(PROFILE_KEY, JSON.stringify(next));
  };

  const clearProfile = async () => {
    setProfileState(null);
    await AsyncStorage.removeItem(PROFILE_KEY);
  };

  return (
    <AppContext.Provider
      value={{
        profile,
        setProfile,
        updateProfile,
        clearProfile,
        selectedDate,
        setSelectedDate,
        isLoading,
      }}
    >
      {children}
    </AppContext.Provider>
  );
}

export function useApp() {
  const ctx = useContext(AppContext);
  if (!ctx) throw new Error("useApp must be used within AppProvider");
  return ctx;
}
