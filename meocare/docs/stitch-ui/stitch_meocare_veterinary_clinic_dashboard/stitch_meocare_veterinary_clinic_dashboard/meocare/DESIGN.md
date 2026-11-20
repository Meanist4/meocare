---
name: Meocare
colors:
  surface: '#faf9f5'
  surface-dim: '#dadad6'
  surface-bright: '#faf9f5'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f4f4f0'
  surface-container: '#eeeeea'
  surface-container-high: '#e8e8e4'
  surface-container-highest: '#e2e3df'
  on-surface: '#1a1c1a'
  on-surface-variant: '#3d4947'
  inverse-surface: '#2f312e'
  inverse-on-surface: '#f1f1ed'
  outline: '#6d7a77'
  outline-variant: '#bcc9c6'
  surface-tint: '#006a61'
  primary: '#00685f'
  on-primary: '#ffffff'
  primary-container: '#008378'
  on-primary-container: '#f4fffc'
  inverse-primary: '#6bd8cb'
  secondary: '#006c49'
  on-secondary: '#ffffff'
  secondary-container: '#6cf8bb'
  on-secondary-container: '#00714d'
  tertiary: '#3d6261'
  on-tertiary: '#ffffff'
  tertiary-container: '#567b7a'
  on-tertiary-container: '#f3fffd'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#89f5e7'
  primary-fixed-dim: '#6bd8cb'
  on-primary-fixed: '#00201d'
  on-primary-fixed-variant: '#005049'
  secondary-fixed: '#6ffbbe'
  secondary-fixed-dim: '#4edea3'
  on-secondary-fixed: '#002113'
  on-secondary-fixed-variant: '#005236'
  tertiary-fixed: '#c2eae8'
  tertiary-fixed-dim: '#a7cecc'
  on-tertiary-fixed: '#00201f'
  on-tertiary-fixed-variant: '#274d4c'
  background: '#faf9f5'
  on-background: '#1a1c1a'
  surface-variant: '#e2e3df'
typography:
  headline-xl:
    fontFamily: Plus Jakarta Sans
    fontSize: 32px
    fontWeight: '700'
    lineHeight: 40px
  headline-xl-mobile:
    fontFamily: Plus Jakarta Sans
    fontSize: 26px
    fontWeight: '700'
    lineHeight: 34px
  headline-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 32px
  headline-lg-mobile:
    fontFamily: Plus Jakarta Sans
    fontSize: 20px
    fontWeight: '600'
    lineHeight: 28px
  headline-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 18px
    fontWeight: '600'
    lineHeight: 26px
  body-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  body-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  body-sm:
    fontFamily: Plus Jakarta Sans
    fontSize: 12px
    fontWeight: '400'
    lineHeight: 18px
  label-md:
    fontFamily: Inter
    fontSize: 13px
    fontWeight: '600'
    lineHeight: 18px
  label-sm:
    fontFamily: Inter
    fontSize: 11px
    fontWeight: '600'
    lineHeight: 16px
  numeric-data:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '500'
    lineHeight: 20px
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  gutter: 1.5rem
  gutter-sm: 1rem
  margin: 2rem
  margin-sm: 1rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 1rem
  space-lg: 1.5rem
  space-xl: 2.25rem
---

## Brand & Style

This design system blends **Warm Clinical Modernism** with an empathetic, high-trust healthcare approach. It supports veterinary practitioners, clinic administrators, and pet caregivers by transforming dense clinical data into approachable, stress-reducing workflows.

### Aesthetic Principles
- **Compassionate Precision:** Combines clinical exactness with tactile warmth. Interfaces avoid sterile hospital blues in favor of grounded teals, botanical sage, and soothing cream stone.
- **Calm Velocity:** Veterinary clinics are high-stress environments. Clear visual hierarchies, muted pastel signifiers, and generous target areas reduce cognitive friction under pressure.
- **Subtle Layering:** Visual depth is created through soft, organic cards resting over tiered warm-neutral backdrops rather than harsh outlines or deep drop shadows.

## Colors

The palette balances clinical credibility with domestic warmth, avoiding stark optic whites that contribute to eye fatigue.

### Color Hierarchy & Roles
- **Primary Teal (`#0D9488`):** Anchors core actions, navigational highlights, interactive states, and active chart visualizations. Accessible over neutral creams and pure whites.
- **Secondary Sage (`#10B981`):** Represents biological health, successful metric validations, and stable telemetry.
- **Deep Cypress (`#042F2E`):** Used for primary typography, dense clinical chart text, structural borders, and high-emphasis framing.
- **Warm Neutral Foundation (`#FBFBF9` base, `#F4F4F0` surfaces, `#F8FAFC` data modules):** Mitigates clinical glare and conveys organic care.
- **Borders & Separators (`#E2E8F0`):** Soft, structural lines that define boundaries without introducing visual clutter.

### Clinical Status & Badge System
Status colors employ soft pastel fills paired with high-contrast text:
- **Pending:** Amber Fill (`#FEF3C7`) / Dark Amber Label (`#92400E`)
- **Confirmed:** Emerald Fill (`#D1FAE5`) / Forest Label (`#065F46`)
- **Reschedule Proposed:** Soft Violet Fill (`#EDE9FE`) / Slate Violet Label (`#5B21B6`)
- **Rejected / Cancelled:** Delicate Rose Fill (`#FFE4E6`) / Deep Berry Label (`#9F1239`)
- **Completed:** Muted Teal-Slate Fill (`#CCFBF1`) / Deep Cypress Label (`#115E59`)

## Typography

The type system pairs **Plus Jakarta Sans** for humanist readability with **Inter** for clinical data density and numeric alignment.

### Roles and Application
- **Plus Jakarta Sans (Headings & Primary Content):** Friendly geometric curves reduce anxiety across headers, patient summaries, care instructions, and timeline titles.
- **Inter (Labels & Clinical Tables):** High legibility at small sizes. Used with tabular lining numbers (`font-variant-numeric: tabular-nums`) across dosage tables, biometric telemetry feeds, weight trends, and schedule timelines to prevent layout shift.

## Layout & Spacing

A structured 12-column responsive fluid grid provides flexible layouts across administrative dashboards and mobile treatment views.

### Form Factors
- **Desktop (1024px+):** Fixed left navigation rail (260px) paired with an expansive 12-column workspace, `margin: 2rem`, and `gutter: 1.5rem`.
- **Tablet (768px - 1023px):** Condensed left icon-rail (72px), unified 8-column layout, and `margin: 1.5rem`.
- **Mobile (< 768px):** Single-column stack with top header, persistent bottom care-action drawer, `margin-sm: 1rem`, and `gutter-sm: 1rem`.

### Density & Cadence
- **Dense Clinical Sections:** Tables, medication charts, and appointment cards use compact spacing (`space-xs` and `space-sm`) to maximize visible context.
- **Workspace Canvas:** Canvas sections, overview cards, and patient history feeds leverage `space-lg` and `space-xl` to maintain a calm, organized interface.

## Elevation & Depth

Visual hierarchy uses soft tonal surfaces combined with delicate, ambient drop shadows rather than heavy contrast layers.

### Elevation Hierarchy
- **Canvas Base (Level 0):** Muted warm stone background (`#FBFBF9`).
- **Surface Cards (Level 1):** Solid white (`#FFFFFF`) with a subtle border (`1px solid #E2E8F0`) and ambient shadow: `box-shadow: 0 1px 3px 0 rgba(4, 47, 46, 0.04), 0 1px 2px -1px rgba(4, 47, 46, 0.02)`.
- **Interactive Modals & Floating Tooltips (Level 2):** Elevated cards, dropdowns, and appointment popovers: `box-shadow: 0 10px 15px -3px rgba(4, 47, 46, 0.07), 0 4px 6px -4px rgba(4, 47, 46, 0.04)`.
- **Sticky Clinical Banners / Role Alerts (Level 3):** Floating timeline actions use an ambient cyan-tinted blur: `box-shadow: 0 20px 25px -5px rgba(13, 148, 136, 0.1), 0 8px 10px -6px rgba(13, 148, 136, 0.05)`.

## Shapes

The design system embraces gentle, approachable contours that soften the clinical experience without appearing juvenile.

### Radius System
- **Core Elements (`rounded` / 0.5rem):** Form inputs, table action buttons, and small badge pills.
- **Card Containers (`rounded-lg` / 1rem):** Appointment cards, patient summary boards, and biometric graphs.
- **Hero Containers (`rounded-xl` / 1.5rem):** Clinical summaries, negotiation timeline modules, and persistent modal sheets.
- **Badge Indicators & Pills:** Fully circular radii (`rounded-full` / 9999px) for status indicators, avatars, and chip filters.

## Components

### Buttons
- **Primary:** Background `#0D9488`, text `#FFFFFF`, radius `rounded`, font `Inter` SemiBold. Hover: `#0F766E`. Active: `#115E59`.
- **Secondary:** Surface `#FFFFFF`, border `1px solid #E2E8F0`, text `#042F2E`. Hover: `#F4F4F0`.
- **Ghost:** Transparent background, text `#0D9488`. Hover: `#F0FDFA`.

### Status Badges & Chips
- Fully rounded pills (`rounded-full`), padded `0.25rem 0.75rem`, font `Inter` (11px, weight 600, uppercase letter-spacing +0.05em).
- Accompanied by a 6px circular indicator dot inside the pill matching the badge's primary text tone.

### Medical Data Tables
- Header: `#F8FAFC`, uppercase `Inter` 11px font, tracking +0.05em, text `#64748B`.
- Row styling: Bottom border `1px solid #E2E8F0`, height 52px, alternating hover state `#FBFBF9`. Tabular numeric alignment for patient weights, dosages, and timeline timestamps.

### Negotiation & Appointment Timeline Nodes
- Vertical connecting line (`2px solid #E2E8F0`).
- Timeline Nodes: 32px circular icon badges tinted according to status (e.g., Violet for Reschedule Proposed, Slate for Completed).
- Inner content box: `#FFFFFF`, `rounded-lg`, `border: 1px solid #E2E8F0`, padded with `space-md`.

### Input Fields & Controls
- **Inputs:** White background, `1px solid #E2E8F0`, radius `rounded`, text `#042F2E`, placeholder `#94A3B8`. Focus ring: `2px solid #14B8A6` with zero offset.
- **Checkboxes & Radios:** Primary teal `#0D9488` fill when selected, border `#CBD5E1` when unselected, rounded corners (checkbox: 4px, radio: full).

### Clinical Vitals Metric Cards
- White background (`#FFFFFF`), `rounded-lg`, border `1px solid #E2E8F0`, padding `space-md`.
- Top row: Metric label in `label-sm` muted uppercase with subtle icon indicator.
- Center: Metric value in `headline-lg` (`#042F2E`) accompanied by a trend badge (Sage for healthy, Rose for alert).