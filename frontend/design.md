# PrepPath — Design System & UI/UX Architectural Specification

> **Design System Name**: Monochrome Academic Focus  
> **Source of Truth**: Google Stitch HTML Prototypes in `/design` (`dashboard1`, `dashbaord2`, `practise`, `quiz`, `mock interview`, `report`, `report1`, `gap analysis`, `settings`).  
> **Location**: `frontend/design.md`

---

## 1. Design Philosophy & Aesthetic Identity

PrepPath embodies a **serene, high-clarity academic environment** engineered to eliminate cognitive clutter and alleviate interview anxiety for college students. The personality is disciplined, articulate, and quietly confident—drawing inspiration from minimalist Swiss typography, editorial architectural monographs, and precision developer utilities.

### Core Principles
1. **Monochrome-First Architecture**: The entire interface is constructed on a pure grayscale foundation. Surfaces are crisp white `#FFFFFF` and off-white `#FAFAFA`, framed by razor-sharp hairline borders (`#E5E5E5`).
2. **Color Exclusively for Telemetry**: Color is stripped of any ornamental role. It is strictly reserved for diagnostic metrics, performance feedback, and status states (Muted Green `#16A34A` for high mastery/correct, Amber `#D97706` for progressing/medium, and Soft Red `#DC2626` for needs review/incorrect).
3. **Planimetric Depth (Zero Diffuse Shadows)**: The system rejects ambient drop shadows, blurs, and skeuomorphic gradients. Depth is established through 1px hairline boundaries and subtle surface tonal shifts.
4. **Typographic Discipline**: Strict hierarchy powered by **Inter** for user interface elements and **JetBrains Mono** for code blocks, keyboard shortcuts, timestamps, and metric telemetry.

---

## 2. Complete Design Tokens

### 2.1 Color Palette

#### Canvas & Surfaces
| Token | Hex Value | Usage |
|---|---|---|
| `surface-canvas` | `#FFFFFF` | Primary card surfaces, modal background, content containers |
| `surface-wash` | `#FAFAFA` | Page background, subtle section wash, alternating containers |
| `surface-container-low` | `#F4F3F3` | Input background, message bubbles, inactive chip fills |
| `surface-container` | `#EEEEEE` | Progress tracks, badge backgrounds, pill backdrops |
| `surface-container-high` | `#E8E8E8` | Hover fills, subtle button states |
| `surface-dim` | `#DADADA` | Inactive borders, secondary delimiters |
| `border-hairline` | `#E5E5E5` | Standard 1px card borders, section dividers |
| `border-active` | `#111111` | Card hover state, input focus border, active selection outline |

#### Text Hierarchy
| Token | Hex Value | Usage |
|---|---|---|
| `text-primary` | `#111111` | Headings, primary button labels, active state text, key values |
| `text-body` | `#555555` | Long-form body text, question explanations, prompt prose |
| `text-secondary` | `#8A8A8A` | Captions, metadata, hints, passive placeholders |
| `text-inverse` | `#FFFFFF` | Text on solid primary buttons and dark badges |

#### Functional Semantics (Metrics, Status & Scoring)
| Semantic State | Text / Icon Hex | Background Fill Hex | Border Hex | Usage |
|---|---|---|---|---|
| **Success / Passed** | `#16A34A` | `#DCFCE7` | `#BBF7D0` | Correct MCQ answer, Tier-1 readiness, mastered topic |
| **Warning / Medium** | `#D97706` | `#FEF3C7` | `#FDE68A` | In-progress topic, moderate confidence, warning notes |
| **Critical / Review** | `#DC2626` | `#FEE2E2` | `#FECACA` | Incorrect selection, weak skill gap, quota depleted |

#### Telemetry & Data Chart Scale
| Data Metric | Hex Code | Visual Application |
|---|---|---|
| **High Performance / Student Trajectory** | `#111111` | Readiness line graph curve, active progress fill |
| **Cohort Benchmark Cutoff (80%)** | `#71717A` (dashed) | Benchmark reference line, secondary chart axes |
| **Inactive / Baseline Track** | `#E4E4E7` | Background progress tracks, inactive radar grid |

---

### 2.2 Typography Scale

The typographic scale uses **Inter** with negative tracking on headlines for editorial crispness, and **JetBrains Mono** for technical data:

| Token | Font Family | Size | Line Height | Letter Spacing | Weight | Tailwind Equivalent |
|---|---|---|---|---|---|---|
| `headline-xl` | Inter | 32px | 40px | -0.02em | 600 | `text-[32px] leading-[40px] tracking-tight font-semibold` |
| `headline-lg` | Inter | 28px | 36px | -0.015em | 600 | `text-[28px] leading-[36px] tracking-tight font-semibold` |
| `headline-md` | Inter | 20px | 28px | -0.01em | 600 | `text-[20px] leading-[28px] font-semibold` |
| `headline-sm` | Inter | 18px | 26px | -0.005em | 500 | `text-[18px] leading-[26px] font-medium` |
| `body-lg` | Inter | 16px | 25.6px | 0em | 400 | `text-[16px] leading-[25.6px] font-normal` |
| `body-md` | Inter | 15px | 24px | 0em | 400 | `text-[15px] leading-[24px] font-normal` |
| `body-sm` | Inter | 13px | 20px | 0em | 400 | `text-[13px] leading-[20px] font-normal` |
| `label-md` | Inter | 14px | 20px | 0.01em | 500 | `text-[14px] leading-[20px] font-medium` |
| `label-sm` | Inter | 12px | 16px | 0.02em | 500 | `text-[12px] leading-[16px] font-medium` |
| `code-sm` | JetBrains Mono | 13px | 20px | 0em | 400 | `font-mono text-[13px] leading-[20px] font-normal` |

---

### 2.3 Layout, Spacing & Container Grid

* **Maximum Content Container Width**: `max-width: 1200px` (centered via `mx-auto`)
* **Responsive Column Layout**:
  * **Desktop (1024px+)**: 12 columns, `gutter: 1.5rem` (24px), page margin `3rem` (48px)
  * **Tablet (768px – 1023px)**: 8 columns, `gutter: 1.5rem` (24px), page margin `2rem` (32px)
  * **Mobile (<768px)**: 4 columns, `gutter: 1rem` (16px), page margin `1.25rem` (20px)
* **Spacing Scale**:
  * `space-xs`: `0.25rem` (4px)
  * `space-sm`: `0.5rem` (8px)
  * `space-md`: `1rem` (16px)
  * `space-lg`: `1.5rem` (24px)
  * `space-xl`: `2.5rem` (40px)

---

### 2.4 Elevation, Depth & Shapes

* **Flat Surfaces**: All cards, panels, and sidebars sit on the base plane without blur filters or drop shadows.
* **Hairline Framing**: Contained by crisp `1px solid #E5E5E5`.
* **Hover Interaction**: Hover transitions update border color from `#E5E5E5` directly to `#111111` with a `150ms ease` transition (zero vertical elevation transform).
* **Modal Scrim**: Pure `#000000` with 12% opacity (`rgba(0, 0, 0, 0.12)`) and a `1px solid #111111` modal frame.
* **Border Radii**:
  * Cards, Dialogs, Sections: `12px` (`rounded-xl`)
  * Interactive Controls (Buttons, Inputs, Selects): `8px` (`rounded-lg`)
  * Status Pills, Telemetry Chips, Avatars: `9999px` (`rounded-full`)
  * Checkboxes: `4px` (`rounded-sm`)
  * Dividers: 1px hairline stroke (`h-px bg-outline-variant/40`)

---

## 3. Component System Specifications

### 3.1 Buttons
* **Primary Button**:
  * Background: Solid `#111111`
  * Text: `#FFFFFF`, weight 500 (`label-md`)
  * Height: 40px (desktop), 36px (compact)
  * Radius: 8px (`rounded-lg`)
  * Hover: `#262626` (smooth 150ms transition)
* **Secondary Button**:
  * Background: `#FFFFFF`
  * Border: `1px solid #E5E5E5` (swaps to `#111111` on hover)
  * Text: `#111111`, weight 500
  * Radius: 8px
* **Ghost / Tertiary Button**:
  * Background: Transparent
  * Text: `#555555` (transitions to `#111111` on hover with a `#FAFAFA` wash)

### 3.2 Quiz Option Cards (Interactive MCQ)
* **Base State**:
  * Background `#FFFFFF`, border `1px solid #E5E5E5`, corner radius `12px`, padding `16px 20px`.
  * Left badge: 28px square with `8px` radius, background `#EEEEEE`, text `#111111` (`A`, `B`, `C`, `D`).
* **Selected / Active State**:
  * Border swaps to `1px solid #111111`.
  * Left badge turns solid `#111111` with `#FFFFFF` text.
* **Post-Submission Correct Answer**:
  * Background `#F4F3F3` or `#DCFCE7/20`, border `1px solid #16A34A`.
  * Badge: Solid `#16A34A` background with checkmark icon and "Correct Answer" pill.
* **Post-Submission Incorrect Selection**:
  * Background `#FEE2E2/20`, border `1px solid #DC2626`.
  * Badge: Solid `#DC2626` background with cross icon and "Your Selection" pill.

### 3.3 Form Fields & Inputs
* **Text Input & Textarea**:
  * Background `#FFFFFF`, border `1px solid #E5E5E5`, corner radius `8px`.
  * Focus: Border switches instantly to `1px solid #111111` (zero outer glow halo).
  * Text: `#111111`, placeholder text `#8A8A8A`.
* **Keyboard Shortcut Chips**:
  * Minimalist chip: `<kbd>` tag styled with background `#EEEEEE`, text `#111111`, font `JetBrains Mono` 11px, border `1px solid #E5E5E5`.

### 3.4 Progress Rails & Telemetry Indicators
* **Segmented Micro-Rail**:
  * Height: 6px, radius 9999px (`rounded-full`), track background `#EEEEEE`.
  * Fill: Solid `#111111` proportional to `currentIndex / totalQuestions`.
* **Telemetry Radar / Longitudinal Line Graph**:
  * Background `#FFFFFF`, grid stroke `#EFEFEF` with dashed lines.
  * Student readiness curve: 2px solid `#111111` stroke with 8% opacity black area wash.
  * Target benchmark line: 1.2px dashed line at 80% with label "Tier-1 Placement Cutoff (80%)".

---

## 4. Screen-by-Screen Implementation Guide

Based on the 9 Google Stitch reference mockups in `frontend/design/`:

### 4.1 Dashboard (`/dashboard` — from `dashboard1` & `dashbaord2`)
* **Fixed Shell**: Left sidebar navigation (w-60, 240px) with logo, active link pill (`#F4F3F3`), and top bar showing "Spring Placement Season · Day 18" with "AI credits today: 1 of 2" indicator.
* **Longitudinal Readiness Matrix**:
  * Top KPI: Current Readiness (e.g. `78%`), +14% over 30d badge.
  * Large Recharts SVG line chart comparing trajectory against Tier-1 SDE Cutoff (80%).
  * Time range switcher: `7d`, `30d`, `90d`.
* **Actionable Next Steps**:
  * Daily recommendation card: "Today's Drill: Distributed Systems & Concurrency (8 Questions • ~15 mins)".
  * Primary action button: "Start Today's Drill".
* **Topic Readiness Grid**: Table of core topics with status pills (`Mastered`, `Progressing`, `Action Required`).

### 4.2 Practice Lobby (`/practice` — from `practise`)
* **Role Track Selector**: Cards for "Java Backend Engineer", "Web Frontend Engineer", and "General SDE" with question count badges.
* **Mode Selection Tabs**:
  * *Rapid Drill (MCQ)*: Zero-AI, instant feedback, code-graded, no quota consumed.
  * *AI Mock Interview*: Voice/text, adaptive follow-ups, rubric evaluation.
* **Configuration Controls**:
  * Difficulty pills (`Easy`, `Medium`, `Hard`, `Mixed`).
  * Topic checkboxes with weighted priority scores.
  * Question count slider (4 to 20 questions, default 8).

### 4.3 Active Quiz Session (`/practice/quiz/:id` — from `quiz`)
* **Top Status Ribbon**: Question position (e.g., "Question 3 of 8"), countdown timer (`01:42 remaining`), Save & Exit link.
* **Question Prompt**: Category metadata pill (`JAVA BACKEND • COLLECTIONS & CONCURRENCY`), prompt headline with inline code styling (`ConcurrentHashMap`), keyboard hint (`A`, `B`, `C`, `D`).
* **4 Option Cards**: Clean grid with key badges. On click or keypress, card updates immediately.
* **Answer Explanation Card**: Expands post-answer with detailed explanation and "Next Question" button.

### 4.4 Mock Interview Session (`/interviews/:id` — from `mock interview`)
* **Header Ribbon**: Topic title, active timer countdown, progress indicator.
* **Conversational Thread Canvas**:
  * AI Interviewer Bubble: `#F4F3F3` background with avatar, timestamp, prompt question, and evaluated criteria tags.
  * Candidate Response Bubble: `#FFFFFF` card with timestamp, delivery duration chip, and student transcript.
  * AI Follow-up Probe: Tagged with "Follow-up Probe" badge, dynamically appended via SSE stream.
* **Answer Console**:
  * Clean text input area with word count and time elapsed.
  * "Submit Response" button and "Skip / Pass" link.

### 4.5 Performance Report (`/sessions/:id/report` — from `report` & `report1`)
* **Header Score Card**: Overall Readiness Index (e.g. `78%`), session duration, and track title.
* **Criterion Breakdown Cards**:
  * 1 to 5 rating scale per criterion (Technical Depth, Problem Solving, Communication).
  * Quoted student evidence block showing verbatim excerpt analyzed by AI.
* **Diagnostic Checklists**:
  * Green checkmark list: "Demonstrated Strengths & Key Points Covered".
  * Amber warning list: "Missed Key Concepts & Edge Cases".
  * Blue code block: "Exemplary Model Answer Benchmark".

### 4.6 Skill Gap Analysis (`/skills` — from `gap analysis`)
* **Mastery Hierarchy Tree**: Topics grouped by domain (Language Internals, Concurrency, Database Design).
* **Prerequisite Directed Links**: Visual indicator showing prerequisite relationships (e.g. "Multithreading requires JVM Memory Model").
* **Target Track Importance Weights**: Visual weight pills (1.5x, 1.2x) indicating placement impact.

### 4.7 Mistake Revisit Queue (`/review`)
* **Spaced Repetition Cards**: List of past failed questions due for review today.
* **Retry Interface**: One-click drill to re-attempt questions and increment interval days.

### 4.8 Settings & AI Quota (`/settings` — from `settings`)
* **Student Profile**: College domain email, branch, graduation year, target track.
* **AI Quota Meter**: Visual circular gauge or bar showing daily free-tier Groq calls (e.g. "1 / 2 used today; resets at midnight UTC").
* **Custom Groq Key Modal**: Form to input personal Groq API key (AES encrypted on backend) to bypass daily platform limits.

---

## 5. Frontend Developer Implementation Rules

1. **Google Stitch Prototypes are Source of Truth**: The HTML code in `/design/*/code.html` defines the definitive structure, classes, and layout. Replicate spacing, padding, and alignments with exact fidelity.
2. **Zero Hardcoded Colors**: Always reference Tailwind tokens defined in this document (e.g., `text-primary`, `bg-surface-canvas`, `border-hairline`). Never insert ad-hoc hex values like `text-[#123456]` into component JSX.
3. **Build Shared Components First**:
   - `components/ui/button.tsx`
   - `components/ui/card.tsx`
   - `components/ui/badge.tsx`
   - `components/ui/progress.tsx`
   - `components/ui/input.tsx`
   - `components/navigation/sidebar.tsx`
   - `components/navigation/header.tsx`
4. **Visual Regression Gate**: Once a screen is built with mock data, capture Playwright screenshots at `1440px` (desktop) and `390px` (mobile). Verify that every padding, hairline border, and font weight matches the corresponding `screen.png` in `/design/` before wiring live backend queries.
