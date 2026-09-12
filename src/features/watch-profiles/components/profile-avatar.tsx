import { cn } from "@/lib/utils";
import { avatarGradient } from "@/types/watch-profile";

interface ProfileAvatarProps {
  name: string;
  avatarKey: string;
  size?: "sm" | "md" | "lg" | "xl";
  className?: string;
}

const sizes = {
  sm: "h-9 w-9 text-xs",
  md: "h-12 w-12 text-sm",
  lg: "h-24 w-24 text-2xl",
  xl: "h-32 w-32 text-3xl",
};

export function ProfileAvatar({
  name,
  avatarKey,
  size = "md",
  className,
}: ProfileAvatarProps) {
  const { from, to } = avatarGradient(avatarKey);
  const initial = (name.trim()[0] ?? "?").toUpperCase();

  return (
    <span
      aria-hidden
      className={cn(
        "inline-flex items-center justify-center rounded-full font-semibold text-white shadow-inner ring-1 ring-white/15",
        sizes[size],
        className,
      )}
      style={{ background: `linear-gradient(145deg, ${from}, ${to})` }}
    >
      {initial}
    </span>
  );
}
