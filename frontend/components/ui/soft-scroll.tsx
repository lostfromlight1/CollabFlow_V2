"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { cn } from "@/lib/utils";

type Axis = "horizontal" | "vertical";
type Orientation = Axis | "both";

type ThumbState = {
  size: number;
  offset: number;
  visible: boolean;
};

const EMPTY: ThumbState = { size: 0, offset: 0, visible: false };

/**
 * Hides native OS scrollbars and draws interactive theme thumbs.
 * Visible only when that axis overflows.
 * Click the track to jump · drag the thumb to scroll.
 * Supports horizontal, vertical, or both.
 */
export function SoftScroll({
  children,
  className,
  contentClassName,
  orientation = "horizontal",
}: {
  children: React.ReactNode;
  className?: string;
  contentClassName?: string;
  orientation?: Orientation;
}) {
  const viewportRef = useRef<HTMLDivElement>(null);
  const [h, setH] = useState<ThumbState>(EMPTY);
  const [v, setV] = useState<ThumbState>(EMPTY);

  const needH = orientation === "horizontal" || orientation === "both";
  const needV = orientation === "vertical" || orientation === "both";

  const sync = useCallback(() => {
    const el = viewportRef.current;
    if (!el) return;

    if (needH) {
      const { scrollWidth, clientWidth, scrollLeft } = el;
      if (scrollWidth <= clientWidth + 1) {
        setH(EMPTY);
      } else {
        const size = Math.max(28, (clientWidth / scrollWidth) * clientWidth);
        const max = Math.max(0, clientWidth - size);
        const offset =
          max === 0 ? 0 : (scrollLeft / (scrollWidth - clientWidth)) * max;
        setH({ size, offset, visible: true });
      }
    } else {
      setH(EMPTY);
    }

    if (needV) {
      const { scrollHeight, clientHeight, scrollTop } = el;
      if (scrollHeight <= clientHeight + 1) {
        setV(EMPTY);
      } else {
        const size = Math.max(28, (clientHeight / scrollHeight) * clientHeight);
        const max = Math.max(0, clientHeight - size);
        const offset =
          max === 0 ? 0 : (scrollTop / (scrollHeight - clientHeight)) * max;
        setV({ size, offset, visible: true });
      }
    } else {
      setV(EMPTY);
    }
  }, [needH, needV]);

  useEffect(() => {
    const el = viewportRef.current;
    if (!el) return;
    sync();
    el.addEventListener("scroll", sync, { passive: true });
    const ro = new ResizeObserver(() => sync());
    ro.observe(el);
    if (el.firstElementChild) ro.observe(el.firstElementChild);
    window.addEventListener("resize", sync);
    return () => {
      el.removeEventListener("scroll", sync);
      ro.disconnect();
      window.removeEventListener("resize", sync);
    };
  }, [sync]);

  function applyThumbOffset(axis: Axis, thumbOffset: number, size: number) {
    const el = viewportRef.current;
    if (!el) return;
    if (axis === "horizontal") {
      const maxThumb = Math.max(0, el.clientWidth - size);
      const ratio = maxThumb === 0 ? 0 : thumbOffset / maxThumb;
      el.scrollLeft = ratio * (el.scrollWidth - el.clientWidth);
    } else {
      const maxThumb = Math.max(0, el.clientHeight - size);
      const ratio = maxThumb === 0 ? 0 : thumbOffset / maxThumb;
      el.scrollTop = ratio * (el.scrollHeight - el.clientHeight);
    }
  }

  function onTrackPointerDown(
    axis: Axis,
    e: React.PointerEvent<HTMLDivElement>,
    thumb: ThumbState
  ) {
    if (!thumb.visible || !viewportRef.current) return;
    // Ignore if the event started on the thumb (thumb handler stops propagation)
    const track = e.currentTarget;
    const rect = track.getBoundingClientRect();
    const click =
      axis === "horizontal" ? e.clientX - rect.left : e.clientY - rect.top;
    const trackLen = axis === "horizontal" ? rect.width : rect.height;
    const next = Math.max(
      0,
      Math.min(trackLen - thumb.size, click - thumb.size / 2)
    );
    applyThumbOffset(axis, next, thumb.size);
  }

  function onThumbPointerDown(
    axis: Axis,
    e: React.PointerEvent<HTMLDivElement>,
    thumb: ThumbState
  ) {
    e.preventDefault();
    e.stopPropagation();
    if (!thumb.visible) return;

    const startPos = axis === "horizontal" ? e.clientX : e.clientY;
    const startOffset = thumb.offset;
    const size = thumb.size;
    const target = e.currentTarget;
    target.setPointerCapture(e.pointerId);

    function onMove(ev: PointerEvent) {
      const el = viewportRef.current;
      if (!el) return;
      const delta =
        (axis === "horizontal" ? ev.clientX : ev.clientY) - startPos;
      const max =
        axis === "horizontal"
          ? Math.max(0, el.clientWidth - size)
          : Math.max(0, el.clientHeight - size);
      applyThumbOffset(axis, Math.max(0, Math.min(max, startOffset + delta)), size);
    }

    function onUp(ev: PointerEvent) {
      try {
        target.releasePointerCapture(ev.pointerId);
      } catch {
        /* already released */
      }
      window.removeEventListener("pointermove", onMove);
      window.removeEventListener("pointerup", onUp);
      window.removeEventListener("pointercancel", onUp);
    }

    window.addEventListener("pointermove", onMove);
    window.addEventListener("pointerup", onUp);
    window.addEventListener("pointercancel", onUp);
  }

  const showCorner = h.visible && v.visible;

  return (
    <div
        className={cn(
        "relative min-w-0",
        h.visible && "pb-4",
        v.visible && "pr-4",
        className
      )}
    >
      <div
        ref={viewportRef}
        tabIndex={0}
        className={cn(
          "scroll-hide h-full max-h-full w-full outline-none",
          needH && needV && "overflow-auto",
          needH && !needV && "overflow-x-auto overflow-y-hidden",
          needV && !needH && "overflow-y-auto overflow-x-hidden",
          contentClassName
        )}
      >
        {children}
      </div>

      {h.visible ? (
        <div
          role="scrollbar"
          aria-orientation="horizontal"
          aria-valuenow={Math.round(h.offset)}
          className="absolute bottom-1 left-0 z-10 h-2 cursor-pointer"
          style={{ right: showCorner ? 10 : 0 }}
          onPointerDown={(e) => onTrackPointerDown("horizontal", e, h)}
        >
          <div className="absolute inset-x-0 top-1/2 h-0.5 -translate-y-1/2 rounded-full bg-border/50" />
          <div
            className="absolute top-1/2 h-1.5 -translate-y-1/2 cursor-grab rounded-full bg-primary/50 hover:bg-primary/70 active:cursor-grabbing"
            style={{ width: h.size, left: h.offset }}
            onPointerDown={(e) => onThumbPointerDown("horizontal", e, h)}
          />
        </div>
      ) : null}

      {v.visible ? (
        <div
          role="scrollbar"
          aria-orientation="vertical"
          aria-valuenow={Math.round(v.offset)}
          className="absolute top-0 right-1 z-10 w-2 cursor-pointer"
          style={{ bottom: showCorner ? 10 : 0 }}
          onPointerDown={(e) => onTrackPointerDown("vertical", e, v)}
        >
          <div className="absolute inset-y-0 left-1/2 w-0.5 -translate-x-1/2 rounded-full bg-border/50" />
          <div
            className="absolute left-1/2 w-1.5 -translate-x-1/2 cursor-grab rounded-full bg-primary/50 hover:bg-primary/70 active:cursor-grabbing"
            style={{ height: v.size, top: v.offset }}
            onPointerDown={(e) => onThumbPointerDown("vertical", e, v)}
          />
        </div>
      ) : null}
    </div>
  );
}
