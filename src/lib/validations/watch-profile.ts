import { z } from "zod";

import { WATCH_PROFILE_AVATARS } from "@/types/watch-profile";

const avatarKeys = WATCH_PROFILE_AVATARS.map((item) => item.key) as [
  string,
  ...string[],
];

export const watchProfileSchema = z.object({
  name: z
    .string()
    .trim()
    .min(1, "Name is required")
    .max(32, "Keep names under 32 characters"),
  avatarKey: z.enum(avatarKeys).default("slate"),
  age: z.coerce
    .number()
    .int("Age must be a whole number")
    .min(1, "Enter an age of at least 1")
    .max(120, "Enter a realistic age"),
});

export type WatchProfileInput = z.infer<typeof watchProfileSchema>;
