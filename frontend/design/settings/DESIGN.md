---
name: Monochrome Academic Focus
colors:
  surface: '#f9f9f9'
  surface-dim: '#dadada'
  surface-bright: '#f9f9f9'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f4f3f3'
  surface-container: '#eeeeee'
  surface-container-high: '#e8e8e8'
  surface-container-highest: '#e2e2e2'
  on-surface: '#1a1c1c'
  on-surface-variant: '#444748'
  inverse-surface: '#2f3131'
  inverse-on-surface: '#f1f1f1'
  outline: '#747878'
  outline-variant: '#c4c7c7'
  surface-tint: '#5f5e5e'
  primary: '#000000'
  on-primary: '#ffffff'
  primary-container: '#1c1b1b'
  on-primary-container: '#858383'
  inverse-primary: '#c8c6c5'
  secondary: '#5e5e5e'
  on-secondary: '#ffffff'
  secondary-container: '#e4e2e2'
  on-secondary-container: '#646464'
  tertiary: '#000000'
  on-tertiary: '#ffffff'
  tertiary-container: '#1b1c1c'
  on-tertiary-container: '#848484'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#e5e2e1'
  primary-fixed-dim: '#c8c6c5'
  on-primary-fixed: '#1c1b1b'
  on-primary-fixed-variant: '#474646'
  secondary-fixed: '#e4e2e2'
  secondary-fixed-dim: '#c8c6c6'
  on-secondary-fixed: '#1b1c1c'
  on-secondary-fixed-variant: '#464747'
  tertiary-fixed: '#e3e2e2'
  tertiary-fixed-dim: '#c7c6c6'
  on-tertiary-fixed: '#1b1c1c'
  on-tertiary-fixed-variant: '#464747'
  background: '#f9f9f9'
  on-background: '#1a1c1c'
  surface-variant: '#e2e2e2'
typography:
  headline-xl:
    fontFamily: Inter
    fontSize: 32px
    fontWeight: '600'
    lineHeight: 40px
    letterSpacing: -0.02em
  headline-xl-mobile:
    fontFamily: Inter
    fontSize: 26px
    fontWeight: '600'
    lineHeight: 34px
    letterSpacing: -0.015em
  headline-lg:
    fontFamily: Inter
    fontSize: 28px
    fontWeight: '600'
    lineHeight: 36px
    letterSpacing: -0.015em
  headline-lg-mobile:
    fontFamily: Inter
    fontSize: 22px
    fontWeight: '600'
    lineHeight: 30px
    letterSpacing: -0.01em
  headline-md:
    fontFamily: Inter
    fontSize: 20px
    fontWeight: '600'
    lineHeight: 28px
    letterSpacing: -0.01em
  headline-sm:
    fontFamily: Inter
    fontSize: 18px
    fontWeight: '500'
    lineHeight: 26px
    letterSpacing: -0.005em
  body-lg:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 25.6px
    letterSpacing: 0em
  body-md:
    fontFamily: Inter
    fontSize: 15px
    fontWeight: '400'
    lineHeight: 24px
    letterSpacing: 0em
  body-sm:
    fontFamily: Inter
    fontSize: 13px
    fontWeight: '400'
    lineHeight: 20px
    letterSpacing: 0em
  label-md:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '500'
    lineHeight: 20px
    letterSpacing: 0.01em
  label-sm:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '500'
    lineHeight: 16px
    letterSpacing: 0.02em
  code-sm:
    fontFamily: JetBrains Mono
    fontSize: 13px
    fontWeight: '400'
    lineHeight: 20px
    letterSpacing: 0em
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  gutter: 1.5rem
  gutter-mobile: 1rem
  margin: 3rem
  margin-mobile: 1.25rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 1rem
  space-lg: 1.5rem
  space-xl: 2.5rem
---

## Brand & Style
The design system embodies a serene, high-clarity academic environment designed to reduce cognitive load and interview anxiety for college students. The personality is disciplined, articulate, and quietly confident—drawing inspiration from minimalist Swiss typography, editorial architectural monographs, and precision developer utilities. 

Aesthetics are strictly monochrome-first: pure white backdrops, razor-sharp hairline dividers, and stark typographic hierarchy. Color is excluded from ornamental roles and reserved strictly as functional data telemetry (skill diagnostics, session outcomes). By avoiding gradients, heavy drop shadows, and visual noise, the platform turns the user interface into an unobtrusive blank canvas where critical thinking, mock interviews, and code reviews take precedence.

## Colors
The color architecture relies on high-contrast tonal grayscale with calibrated functional semantics:

- **Canvas & Surfaces:**
  - Base canvas: `#FFFFFF`
  - Subtle section alternation and container wash: `#FAFAFA`
  - Structural structural borders and dividers: 1px `#E5E5E5`
- **Text & Content Hierarchy:**
  - Headings, primary commands, and prominent labels: `#111111`
  - Long-form body, prose explanations, and prompts: `#555555`
  - Metadata, hints, passive placeholders, and secondary captions: `#8A8A8A`
- **Functional Semantics (Reserved purely for metrics, status pills, and performance scoring):**
  - Success / Strong Competency: Text/Icon `#16A34A`, Surface Fill `#DCFCE7`
  - Warning / Progressing / Medium: Text/Icon `#D97706`, Surface Fill `#FEF3C7`
  - Critical / Attention Required / Weak: Text/Icon `#DC2626`, Surface Fill `#FEE2E2`
- **Data & Telemetry Scale:**
  - High performance: `#111111`
  - Benchmark median: `#71717A`
  - Inactive / Baseline axis: `#E4E4E7`

## Typography
Typographic rhythm centers on clarity, readability, and structural hierarchy using Inter. Text is calibrated for intensive reading sessions—such as reviewing technical interview rubrics, analyzing coding feedback, and reading behavioral case studies.

Headings leverage negative tracking (`-0.01em` to `-0.02em`) to maintain editorial density without sacrificing legibility. Body text prioritizes breathing room, utilizing a relaxed 1.6 relative line height (`24px` on `15px` base, `25.6px` on `16px` base) to minimize visual fatigue during prolonged practice sessions. JetBrains Mono is designated strictly for coding challenge solutions, keyboard shortcuts, and code-based answer blocks.

## Layout & Spacing
The layout model employs a responsive 12-column fixed-max-width container (`max-width: 1200px`) centered with generous outer margins. 

- **Desktop (1024px+):** 12-column grid, `gutter: 1.5rem` (24px), page margins `3rem` (48px). Multi-column splits are optimized for interactive interview flows (e.g., a 7-column problem statement panel paired with a 5-column response console).
- **Tablet (768px – 1023px):** 8-column grid, `gutter: 1.5rem` (24px), margins `2rem` (32px). Secondary telemetry stacks below primary prompt sections.
- **Mobile (Below 768px):** 4-column grid, `gutter-mobile: 1rem` (16px), margins `margin-mobile: 1.25rem` (20px). Panels collapse sequentially into stacked single-column viewports.

Internal section spacing prioritizes structural whitespace: sections receive 40px to 64px vertical clearance, while internal card padding relies consistently on `space-lg` (24px) for desktop and `space-md` (16px) for mobile.

## Elevation & Depth
This design system intentionally avoids diffuse ambient drop shadows, blurs, and skeuomorphic gradients in favor of **structural low-contrast containment**:

- **Surfaces:** All depth is planimetric. Primary card surfaces remain flat `#FFFFFF`, distinguished from `#FAFAFA` sub-containers through 1px border lines set to `#E5E5E5`.
- **Modals & Overlays:** Where layer separation is strictly needed (such as popover answer hints or prompt configuration sheets), use a flat `1px solid #111111` or `#E5E5E5` containment border combined with a pure `#000000` scrim rendered at `12%` opacity (`rgba(0, 0, 0, 0.12)`).
- **Hover Transitions:** Interactive cards and module options express hover state not via shadow lifts, but by swapping the border color from `#E5E5E5` to `#111111` with an instantaneous or 150ms ease transition.

## Shapes
Geometry is disciplined, utilitarian, and cohesive:

- **Cards & Primary Panels:** Fixed at `12px` border radius (`rounded-lg`), producing clean framing without aggressive circularity.
- **Interactive Controls (Buttons, Inputs, Selects):** Standardized at `8px` border radius (`rounded-md`), maintaining an architectural, precise hand-feel.
- **Pills & Status Badges:** Fully circular (`rounded-full` / `9999px`) to contrast against rectangular cards and highlight metric outcomes.
- **Dividers & Strokes:** Fixed at crisp 1px hairline thickness.

## Components

- **Buttons:**
  - *Primary Button:* Solid `#111111` background, `#FFFFFF` text, 8px border radius, medium font weight (`label-md`). Hover state transitions to `#262626`. Active state scales subtly or applies 90% opacity.
  - *Secondary Button:* Solid `#FFFFFF` background, 1px `#111111` border, `#111111` text, 8px border radius. Hover introduces `#FAFAFA` background fill.
  - *Tertiary / Ghost Button:* Transparent background, `#555555` text. Hover state yields `#111111` text and a subtle `#FAFAFA` bounding box.

- **Status & Diagnostic Badges:**
  - Always rendered as compact pills (`padding: 2px 10px`, font size `12px`, weight `500`).
  - *Passed / High Skill:* `#16A34A` text on `#DCFCE7` background.
  - *Progressing / Medium Skill:* `#D97706` text on `#FEF3C7` background.
  - *Needs Revision / Low Skill:* `#DC2626` text on `#FEE2E2` background.

- **Cards & Panels:**
  - Constructed using `#FFFFFF` background, wrapped in an exact `1px solid #E5E5E5` border, with a `12px` corner radius.
  - Section headers inside cards feature a bottom divider border (`1px solid #E5E5E5`).
  - Hoverable session cards transition their border directly to `#111111` without elevation shift.

- **Form Fields & Inputs:**
  - Background is pure `#FFFFFF` with a `1px solid #E5E5E5` border and 8px corner radius.
  - Focus state swaps border directly to `1px solid #111111` with no glowing halo or outer shadow ring.
  - Text entry is `#111111`, placeholder text is `#8A8A8A`.

- **Checkboxes & Radios:**
  - Unchecked: `1px solid #E5E5E5` on `#FFFFFF` ground.
  - Checked: Solid `#111111` ground with a crisp `#FFFFFF` checkmark (for checkboxes) or inner solid dot (for radios).
  - Radios maintain circular geometry; checkboxes use a tight `4px` border radius.

- **Lists & Rubric Items:**
  - Stacked list items are delimited by `1px solid #E5E5E5` hairline dividers.
  - Spacing is uniform (`padding: 16px 0`).
  - Bullet elements use minimal 4px square ticks (`#111111`) or thin-line icons.

- **Telemetry & Practice Visualizations:**
  - Progress bars utilize an `#E4E4E7` track with a solid `#111111` or status-tinted fill bar. Height fixed at `6px` with full pill radius.
  - Radar and line analytics are rendered strictly with hairline strokes (`1.5px`) in `#111111` and `#71717A`.

- **Iconography:**
  - Thin line icons (Lucide or equivalent) restricted strictly to `stroke-width: 1.5px` and size `16px` to `20px` in monochrome `#111111` or `#555555`.