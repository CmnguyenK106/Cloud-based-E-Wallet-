import axios from 'axios'
import { useAuthStore } from '../store/authStore'

const axiosClient = axios.create({
  baseURL: 'http://localhost:8080/api',
  headers: {
    'Content-Type': 'application/json',
  },
})

const sessionErrorCodes = new Set(['UNAUTHORIZED', 'ACCOUNT_BLOCKED'])
let isClearingSession = false

type ErrorResponse = {
  code?: string
  message?: string
}

axiosClient.interceptors.request.use((config) => {
  const accessToken = localStorage.getItem('token')
  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`
  }
  return config
})

axiosClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (
      axios.isAxiosError<ErrorResponse>(error) &&
      error.response &&
      sessionErrorCodes.has(error.response.data?.code || '') &&
      localStorage.getItem('token') &&
      !isClearingSession
    ) {
      isClearingSession = true
      const message =
        error.response.data?.code === 'ACCOUNT_BLOCKED'
          ? error.response.data.message ||
            'Your account has been blocked by an administrator.'
          : 'Your session has expired. Please log in again.'

      sessionStorage.setItem('authMessage', message)
      useAuthStore.getState().logout()

      if (window.location.pathname !== '/login') {
        window.location.assign('/login')
      }

      window.setTimeout(() => {
        isClearingSession = false
      }, 1000)
    }

    return Promise.reject(error)
  },
)

export default axiosClient
