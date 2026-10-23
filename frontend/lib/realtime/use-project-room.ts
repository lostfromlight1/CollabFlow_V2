"use client";

import { useEffect } from "react";
import { gatewayBaseUrl, publicApiKey } from "@/lib/api/client";
import { getSession } from "@/lib/auth/session";

/**
 * Subscribe to a project room. Any server message triggers `onEvent`
 * so the board/task page can reload from the REST API (source of truth).
 */
export function useProjectRoom(projectId: string | null, onEvent: () => void) {
  useEffect(() => {
    if (!projectId) return;
    const session = getSession();
    if (!session?.accessToken || !session.organizationId) return;

    const http = gatewayBaseUrl();
    const wsBase = http.replace(/^http/, "ws");
    const url =
      `${wsBase}/api/v1/realtime/ws?token=${encodeURIComponent(session.accessToken)}` +
      `&apiKey=${encodeURIComponent(publicApiKey())}` +
      `&organizationId=${encodeURIComponent(session.organizationId)}`;

    let socket: WebSocket | null = null;
    let closed = false;
    try {
      socket = new WebSocket(url);
    } catch {
      return;
    }

    socket.onopen = () => {
      socket?.send(JSON.stringify({ action: "subscribe", room: `project:${projectId}` }));
    };
    socket.onmessage = () => {
      if (!closed) onEvent();
    };

    return () => {
      closed = true;
      if (socket && socket.readyState === WebSocket.OPEN) {
        socket.send(JSON.stringify({ action: "unsubscribe", room: `project:${projectId}` }));
      }
      socket?.close();
    };
  }, [projectId, onEvent]);
}