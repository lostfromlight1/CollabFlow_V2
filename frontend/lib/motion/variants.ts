import type { Variants } from "framer-motion";
import { motionDistance, motionDuration, motionEase } from "@/lib/motion/tokens";

const ease = [...motionEase.standard] as [number, number, number, number];

export const reducedVariant = {
  initial: { opacity: 1, y: 0 },
  animate: { opacity: 1, y: 0, transition: { duration: 0 } },
};

export const pageEnterVariants: Variants = {
  initial: { opacity: 0, y: motionDistance.pageEnterY },
  animate: {
    opacity: 1,
    y: 0,
    transition: { duration: motionDuration.enter, ease },
  },
};

export const fadeInVariants: Variants = {
  initial: { opacity: 0 },
  animate: {
    opacity: 1,
    transition: { duration: motionDuration.enter, ease },
  },
};

/** Menus / hover cards — opacity + slight y, no bounce. */
export const popoverEnterVariants: Variants = {
  initial: { opacity: 0, y: 4 },
  animate: {
    opacity: 1,
    y: 0,
    transition: { duration: motionDuration.fast, ease },
  },
  exit: {
    opacity: 0,
    y: 4,
    transition: { duration: motionDuration.fast, ease },
  },
};

export const staggerContainerVariants: Variants = {
  initial: {},
  animate: {
    transition: {
      staggerChildren: motionDuration.stagger,
      delayChildren: motionDuration.staggerDelayChildren,
    },
  },
};

export const staggerItemVariants: Variants = {
  initial: { opacity: 0, y: motionDistance.staggerItemY },
  animate: {
    opacity: 1,
    y: 0,
    transition: { duration: motionDuration.enter, ease },
  },
};

export const staggerItemImmediateVariants: Variants = {
  initial: { opacity: 0, y: motionDistance.staggerItemY },
  animate: {
    opacity: 1,
    y: 0,
    transition: { duration: motionDuration.enter, ease, delay: 0 },
  },
};
