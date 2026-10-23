"use client";

import { motion, useReducedMotion } from "framer-motion";
import { pageEnterVariants, reducedVariant } from "@/lib/motion/variants";

/** Enter-only route content animation — no exit / AnimatePresence. */
export function PageTransition({
  children,
  className,
}: {
  children: React.ReactNode;
  className?: string;
}) {
  const reduced = useReducedMotion();
  const variants = reduced ? reducedVariant : pageEnterVariants;

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
