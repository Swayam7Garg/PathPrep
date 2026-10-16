import type { Config } from "tailwindcss";

const config: Config = {
  darkMode: ["class"],
  content: [
    "./src/pages/**/*.{js,ts,jsx,tsx,mdx}",
    "./src/components/**/*.{js,ts,jsx,tsx,mdx}",
    "./src/app/**/*.{js,ts,jsx,tsx,mdx}",
  ],
  theme: {
    extend: {
      colors: {
        // Base surfaces from design.md & Google Stitch
        "surface-canvas": "#FFFFFF",
        "surface-wash": "#FAFAFA",
        "surface-container-lowest": "#ffffff",
        "surface-container-low": "#f4f3f3",
        "surface-container": "#eeeeee",
        "surface-container-high": "#e8e8e8",
        "surface-container-highest": "#e2e2e2",
        "surface-dim": "#dadada",
        "surface-bright": "#f9f9f9",
        surface: "#f9f9f9",

        // Text & ink hierarchy
        "text-primary": "#111111",
        "text-body": "#555555",
        "text-secondary": "#8a8a8a",
        "text-inverse": "#ffffff",
        "on-surface": "#1a1c1c",
        "on-surface-variant": "#444748",
        "on-background": "#1a1c1c",
        "on-primary": "#ffffff",
        "on-secondary": "#ffffff",

        // Primary / Secondary / Tertiary Monochrome
        primary: {
          DEFAULT: "#111111",
          hover: "#262626",
          container: "#1c1b1b",
          "on-container": "#858383",
        },
        secondary: {
          DEFAULT: "#5e5e5e",
          container: "#e4e2e2",
          "on-container": "#646464",
        },
        tertiary: {
          DEFAULT: "#111111",
          container: "#1b1c1c",
        },

        // Borders & Outlines
        "border-hairline": "#E5E5E5",
        "border-active": "#111111",
        outline: "#747878",
        "outline-variant": "#c4c7c7",

        // Functional Semantics (Metrics, Status & Scoring)
        status: {
          success: {
            DEFAULT: "#16A34A",
            bg: "#DCFCE7",
            border: "#BBF7D0",
          },
          warning: {
            DEFAULT: "#D97706",
            bg: "#FEF3C7",
            border: "#FDE68A",
          },
          critical: {
            DEFAULT: "#DC2626",
            bg: "#FEE2E2",
            border: "#FECACA",
          },
        },
        error: {
          DEFAULT: "#DC2626",
          container: "#ffdad6",
          "on-container": "#93000a",
        },
      },
      fontFamily: {
        sans: ["var(--font-inter)", "Inter", "-apple-system", "BlinkMacSystemFont", "sans-serif"],
        mono: ["var(--font-jetbrains)", "JetBrains Mono", "monospace"],
        "headline-xl": ["var(--font-inter)", "Inter", "sans-serif"],
        "headline-lg": ["var(--font-inter)", "Inter", "sans-serif"],
        "headline-md": ["var(--font-inter)", "Inter", "sans-serif"],
        "headline-sm": ["var(--font-inter)", "Inter", "sans-serif"],
        "body-lg": ["var(--font-inter)", "Inter", "sans-serif"],
        "body-md": ["var(--font-inter)", "Inter", "sans-serif"],
        "body-sm": ["var(--font-inter)", "Inter", "sans-serif"],
        "label-md": ["var(--font-inter)", "Inter", "sans-serif"],
        "label-sm": ["var(--font-inter)", "Inter", "sans-serif"],
        "code-sm": ["var(--font-jetbrains)", "JetBrains Mono", "monospace"],
      },
      fontSize: {
        "headline-xl": ["32px", { lineHeight: "40px", letterSpacing: "-0.02em", fontWeight: "600" }],
        "headline-lg": ["28px", { lineHeight: "36px", letterSpacing: "-0.015em", fontWeight: "600" }],
        "headline-md": ["20px", { lineHeight: "28px", letterSpacing: "-0.01em", fontWeight: "600" }],
        "headline-sm": ["18px", { lineHeight: "26px", letterSpacing: "-0.005em", fontWeight: "500" }],
        "body-lg": ["16px", { lineHeight: "25.6px", letterSpacing: "0em", fontWeight: "400" }],
        "body-md": ["15px", { lineHeight: "24px", letterSpacing: "0em", fontWeight: "400" }],
        "body-sm": ["13px", { lineHeight: "20px", letterSpacing: "0em", fontWeight: "400" }],
        "label-md": ["14px", { lineHeight: "20px", letterSpacing: "0.01em", fontWeight: "500" }],
        "label-sm": ["12px", { lineHeight: "16px", letterSpacing: "0.02em", fontWeight: "500" }],
        "code-sm": ["13px", { lineHeight: "20px", letterSpacing: "0em", fontWeight: "400" }],
      },
      spacing: {
        "space-xs": "0.25rem",  // 4px
        "space-sm": "0.5rem",   // 8px
        "space-md": "1rem",     // 16px
        "space-lg": "1.5rem",   // 24px
        "space-xl": "2.5rem",   // 40px
        gutter: "1.5rem",
        margin: "3rem",
        "margin-mobile": "1.25rem",
        "gutter-mobile": "1rem",
      },
      borderRadius: {
        DEFAULT: "0.25rem",
        sm: "4px",
        lg: "8px",
        xl: "12px",
        full: "9999px",
      },
      maxWidth: {
        container: "1200px",
      },
      boxShadow: {
        none: "none",
      },
    },
  },
  plugins: [],
};

export default config;
