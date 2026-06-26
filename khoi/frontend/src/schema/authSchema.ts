import { z } from 'zod'

const phoneRule = z
  .string()
  .regex(/^0[0-9]{9}$/, 'Phone must match 0xxxxxxxxx')

export const loginSchema = z.object({
  phone: phoneRule,
  password: z.string().min(1, 'Password is required'),
})

export const registerSchema = z.object({
  phone: phoneRule,
  password: z.string().min(6, 'Password must be at least 6 characters'),
  fullName: z.string().trim().min(1, 'Full name is required'),
})

export type LoginForm = z.infer<typeof loginSchema>
export type RegisterForm = z.infer<typeof registerSchema>
