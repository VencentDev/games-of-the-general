'use client';

import { LoaderCircle, Search, X } from 'lucide-react';

import { Button } from '@/components/ui/button';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Label } from '@/components/ui/label';

const PREP_TIME_OPTIONS = [
  { label: 'Unlimited', value: '0' },
  { label: '1 minute', value: '60' },
  { label: '1 minute 30 seconds', value: '90' },
];

export function FindingMatchDialog({
  cancelPending,
  isSearching,
  onCancel,
  onOpenChange,
  onPreparationSecondsChange,
  onStart,
  open,
  preparationSeconds,
  searchPending,
}: {
  cancelPending: boolean;
  isSearching: boolean;
  onCancel: () => void;
  onOpenChange: (open: boolean) => void;
  onPreparationSecondsChange: (value: string) => void;
  onStart: () => void;
  open: boolean;
  preparationSeconds: string;
  searchPending: boolean;
}) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent
        className="max-h-[calc(100svh-2rem)] max-w-sm gap-6 overflow-y-auto border-[#8a7b62]/25 bg-[#fbf8ef] p-6 text-[#201b16] shadow-2xl shadow-black/20 [&>button.absolute]:hidden dark:border-white/10 dark:bg-[#171b15] dark:text-[#f6f0e4]"
        onEscapeKeyDown={(event) => {
          if (isSearching) {
            event.preventDefault();
          }
        }}
        onInteractOutside={(event) => {
          if (isSearching) {
            event.preventDefault();
          }
        }}
      >
        <DialogHeader className="items-center text-center">
          <DialogTitle className="text-2xl font-black tracking-normal">
            {isSearching ? 'Finding match' : 'Find match'}
          </DialogTitle>
          <DialogDescription className="text-sm leading-6 text-[#655c51] dark:text-[#c9c1b4]">
            {isSearching
              ? 'Scanning the battlefield for an available commander.'
              : 'Choose a preparation time. You will only match commanders using the same timer.'}
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-2">
          <Label>Preparation time</Label>
          <PreparationTimeOptions
            value={preparationSeconds}
            onValueChange={onPreparationSecondsChange}
            disabled={isSearching}
          />
        </div>

        {isSearching ? (
          <div className="mx-auto flex w-full max-w-64 flex-col items-center gap-4">
            <div className="got-radar" aria-hidden="true">
              <div className="got-radar-sweep" />
              <span className="got-radar-blip left-[31%] top-[28%]" />
              <span className="got-radar-blip got-radar-blip-delay left-[63%] top-[55%]" />
              <span className="got-radar-blip got-radar-blip-slow left-[47%] top-[71%]" />
              <Search className="relative z-10 size-7 text-[#d7bd73]" />
            </div>

            <div className="grid w-full grid-cols-5 gap-1.5" aria-hidden="true">
              {Array.from({ length: 5 }, (_, index) => (
                <span
                  key={index}
                  className="h-1.5 rounded-full bg-[#8f2f24]/25 got-scan-pulse dark:bg-[#d7bd73]/25"
                  style={{ animationDelay: `${index * 140}ms` }}
                />
              ))}
            </div>
          </div>
        ) : null}

        {isSearching ? (
          <Button
            type="button"
            variant="outline"
            className="h-11 w-full border-[#8a7b62]/35 bg-white/80 font-semibold text-[#201b16] hover:bg-[#2c3520] hover:text-[#fff8ea] dark:border-white/15 dark:bg-white/[0.06] dark:text-[#f6f0e4] dark:hover:bg-[#202817] dark:hover:text-[#fff8ea]"
            disabled={cancelPending}
            onClick={onCancel}
          >
            {cancelPending ? (
              <LoaderCircle className="mr-2 size-4 animate-spin" />
            ) : (
              <X className="mr-2 size-4" />
            )}
            Cancel search
          </Button>
        ) : (
          <Button
            type="button"
            className="h-11 w-full bg-[#2c3520] font-semibold text-[#fff8ea] hover:bg-[#202817] dark:bg-[#d7bd73] dark:text-[#15130d] dark:hover:bg-[#e7ce88]"
            disabled={searchPending}
            onClick={onStart}
          >
            {searchPending ? (
              <LoaderCircle className="mr-2 size-4 animate-spin" />
            ) : (
              <Search className="mr-2 size-4" />
            )}
            Start search
          </Button>
        )}
      </DialogContent>
    </Dialog>
  );
}

export function PreparationTimeOptions({
  disabled,
  onValueChange,
  value,
}: {
  disabled?: boolean;
  onValueChange: (value: string) => void;
  value: string;
}) {
  return (
    <div role="radiogroup" aria-label="Preparation time" className="grid grid-cols-3 gap-2">
      {PREP_TIME_OPTIONS.map((option) => {
        const active = option.value === value;

        return (
          <button
            key={option.value}
            type="button"
            role="radio"
            aria-checked={active}
            className={[
              'min-h-11 rounded-md border px-2.5 text-center text-xs font-black transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#8f2f24]',
              active
                ? 'border-[#2c3520] bg-[#2c3520] text-[#fff8ea] dark:border-[#d7bd73] dark:bg-[#2c3520] dark:text-[#fff8ea]'
                : 'border-[#8a7b62]/30 bg-white/70 text-[#201b16] hover:bg-white dark:border-white/15 dark:bg-white/5 dark:text-[#f6f0e4] dark:hover:bg-white/10',
            ].join(' ')}
            disabled={disabled}
            onClick={() => onValueChange(option.value)}
          >
            {option.label}
          </button>
        );
      })}
    </div>
  );
}
