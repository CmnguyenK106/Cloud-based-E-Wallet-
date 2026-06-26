import axiosClient from './axiosClient'
import type { AuthUser, Wallet } from './authApi'

export type WalletInfoResponse = {
  user: AuthUser
  wallet: Wallet
}

export type TransferPayload = {
  receiverPhone: string
  amount: number
  description?: string
}

export type TransferResponse = {
  message: string
  balance: number
  transaction: {
    transactionCode: string
    amount: number
    receiverPhone: string
  }
}

export type WalletTransaction = {
  id: number
  transactionCode: string
  type: string
  senderPhone?: string
  receiverPhone?: string
  serviceName?: string
  amount: number
  balanceBefore?: number
  balanceAfter?: number
  status: string
  description?: string
  createdAt: string
}

export type TransactionsResponse = {
  transactions: WalletTransaction[]
}

const getAuthHeaders = () => {
  const token = localStorage.getItem('token')

  return {
    Authorization: `Bearer ${token}`,
  }
}

export const walletApi = {
  getMyWallet: async () => {
    const response = await axiosClient.get<WalletInfoResponse>(
      '/user/wallet/me',
      {
        headers: getAuthHeaders(),
      },
    )
    return response.data
  },

  transferMoney: async (data: TransferPayload) => {
    const response = await axiosClient.post<TransferResponse>(
      '/user/wallet/transfer',
      data,
      {
        headers: getAuthHeaders(),
      },
    )
    return response.data
  },

  getMyTransactions: async () => {
    const response = await axiosClient.get<TransactionsResponse>(
      '/user/wallet/transactions',
      {
        headers: getAuthHeaders(),
      },
    )
    return response.data
  },
}
