"use client";

import { motion, useReducedMotion } from "framer-motion";
import { STAGGER_CAP, motionDuration, motionEase } from "@/lib/motion/tokens";
import { motionDistance } from "@/lib/motion/tokens";

const ease = [...motionEase.standard] as [number, number, number, number];

/**
 * Stagger delay for index 0..7; index >= 8 uses the same delay as the 8th item
 * so long lists don't keep stacking delay.
 */
export function staggerDelayForIndex(index: number): number {
  const capped = Math.min(index, STAGGER_CAP - 1);
  return motionDuration.staggerDelayChildren + capped * motionDuration.stagger;
}

export function StaggerList({
  children,
  className,
}: {
  children: React.ReactNode;
  className?: string;
}) {
  return <div className={className}>{children}</div>;
}

export function StaggerItem({
  children,
  index,
  className,
}: {
  children: React.ReactNode;
  index: number;
  className?: string;
}) {
  const reduced = useReducedMotion();

  if (reduced) {
    return <div className={className}>{children}</div>;
  }

  return (
    <motion.div
      className={className}
      initial={{ opacity: 0, y: motionDistance.staggerItemY }}
      animate={{ opacity: 1, y: 0 }}
      transition={{
        duration: motionDuration.enter,
        ease,
        delay: staggerDelayForIndex(index),
      }}
    >
      {children}
    </motion.div>
  );
}
