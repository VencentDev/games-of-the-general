'use client';

import { useCallback, useSyncExternalStore } from 'react';

import type { ToastActionElement, ToastProps } from '@/components/ui/toast';
import type { ReactNode } from 'react';

export type ToastInput = ToastProps & {
  action?: ToastActionElement;
  title?: ReactNode;
  description?: ReactNode;
};

type ToastRecord = ToastInput & {
  id: string;
};

let toasts: ToastRecord[] = [];
const listeners = new Set<() => void>();

function emit() {
  listeners.forEach((listener) => listener());
}

function subscribe(listener: () => void) {
  listeners.add(listener);

  return () => {
    listeners.delete(listener);
  };
}

function getSnapshot() {
  return toasts;
}

export function toast(input: ToastInput) {
  const id = crypto.randomUUID();

  toasts = [{ ...input, id, open: true }, ...toasts].slice(0, 5);
  emit();

  return {
    id,
    dismiss: () => dismissToast(id),
  };
}

export function dismissToast(id: string) {
  toasts = toasts.map((item) => (item.id === id ? { ...item, open: false } : item));
  emit();
}

export function removeToast(id: string) {
  toasts = toasts.filter((item) => item.id !== id);
  emit();
}

export function useToast() {
  const state = useSyncExternalStore(subscribe, getSnapshot, getSnapshot);
  const stableToast = useCallback((input: ToastInput) => toast(input), []);

  return {
    toast: stableToast,
    toasts: state,
  };
}
