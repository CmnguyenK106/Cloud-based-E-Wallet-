import axiosClient from './axiosClient'

export type RegisterPayload = {
  phone: string
  password: string
  fullName: string
}

export type LoginPayload = {
  phone: string
  password: string
}

export type AuthUser = {
  id: number
  phone: string
  role: string
  status: string
  fullName?: string
}

export type Wallet = {
  id: number
  userId?: number
  balance: number
}

export type AuthResponse = {
  message: string
  token?: string
  user?: AuthUser
  wallet?: Wallet
}

export const authApi = {
  register: async (data: RegisterPayload) => {
    const response = await axiosClient.post<AuthResponse>('/auth/register', data)
    return response.data
  },

  login: async (data: LoginPayload) => {
    const response = await axiosClient.post<AuthResponse>('/auth/login', data)
    return response.data
  },
}
