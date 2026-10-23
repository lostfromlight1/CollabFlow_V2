"use client";

import { motion, useReducedMotion } from "framer-motion";
import { fadeInVariants, reducedVariant } from "@/lib/motion/variants";

export function FadeIn({
  children,
  className,
}: {
  children: React.ReactNode;
  className?: string;
}) {
  const reduced = useReducedMotion();
  const variants = reduced ? reducedVariant : fadeInVariants;

  return (
    <motion.div
      className={className}
      initial="initial"
      animate="animate"
      variants={variants}
    >
      {children}
    </motion.div>
  );
}
