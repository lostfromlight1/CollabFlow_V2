/** Single source for motion — import here; do not hardcode ms/easings elsewhere. */

export const motionDuration = {
  fast: 0.15,
  normal: 0.22,
  enter: 0.32,
  stagger: 0.06,
  staggerDelayChildren: 0.04,
  skeletonDelayMs: 180,
} as const;

/** CSS-facing ms strings for rare style props (prefer CSS vars). */
export const motionDurationMs = {
  fast: "150ms",
  normal: "220ms",
  enter: "320ms",
  skeletonDelay: "180ms",
} as const;

export const motionEase = {
  standard: [0.2, 0.8, 0.2, 1] as const,
};

export const motionDistance = {
  liftPx: 2,
  pageEnterY: 8,
  staggerItemY: 6,
} as const;

export const motionScale = {
  press: 0.98,
} as const;

export const STAGGER_CAP = 8;
