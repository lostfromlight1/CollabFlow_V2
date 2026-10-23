"use client";

import Link from "next/link";
import { motion, useReducedMotion } from "framer-motion";
import { buttonVariants } from "@/components/ui/button";
import { pageEnterVariants, reducedVariant } from "@/lib/motion/variants";
import { cn } from "@/lib/utils";

export function LandingHero({
  primaryHref,
  primaryLabel,
}: {
  primaryHref: string;
  primaryLabel: string;
}) {
  const reduced = useReducedMotion();
  const variants = reduced ? reducedVariant : pageEnterVariants;

  return (
    <section className="relative flex min-h-[100dvh] flex-col justify-center overflow-hidden px-6 py-16">
      <div
        aria-hidden
        className="pointer-events-none absolute inset-0 bg-[radial-gradient(ellipse_80%_60%_at_20%_20%,_oklch(0.9_0.05_155)_0%,_transparent_55%),radial-gradient(ellipse_70%_50%_at_90%_80%,_oklch(0.93_0.04_145)_0%,_transparent_50%),linear-gradient(180deg,_oklch(0.97_0.015_145)_0%,_oklch(0.95_0.02_150)_100%)]"
      />
      <div className="relative z-10 mx-auto w-full max-w-3xl">
        <motion.div
          initial="initial"
          animate="animate"
          variants={variants}
          className="space-y-6"
        >
          <p className="font-heading text-4xl font-semibold tracking-tight text-primary sm:text-5xl md:text-6xl">
            CollabFlow
          </p>
          <h1 className="max-w-2xl text-balance text-xl font-medium text-foreground sm:text-2xl">
            Projects that grow with your team — calmly.
          </h1>
          <p className="max-w-lg text-pretty text-base text-muted-foreground sm:text-lg">
            Multi-tenant collaboration from organization to task, built for clear
            ownership and quiet focus.
          </p>
          <div className="flex flex-wrap items-center gap-3 pt-2">
            <Link
              href={primaryHref}
              className={cn(buttonVariants({ size: "lg" }), "px-6")}
            >
              {primaryLabel}
            </Link>
          </div>
        </motion.div>
      </div>
    </section>
  );
}
