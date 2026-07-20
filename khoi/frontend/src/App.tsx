import axios from 'axios'
import { useEffect, useRef, useState } from 'react'
import { Link, Navigate, Route, Routes, useNavigate } from 'react-router-dom'
import './App.css'
import { accountApi, accountToAuthUser } from './apis/accountApi'
import { walletApi } from './apis/walletApi'
import AdminRoute from './components/routes/AdminRoute'
import ProtectedRoute from './components/routes/ProtectedRoute'
import UserRoute from './components/routes/UserRoute'
import { ToastProvider } from './components/toast/ToastProvider'
import DashboardPage, { type WalletTab } from './pages/DashboardPage'
import HomePage from './pages/HomePage'
import LoginPage from './pages/LoginPage'
import ProfilePage from './pages/ProfilePage'
import RegisterPage from './pages/RegisterPage'
import VerifyEmailPage from './pages/VerifyEmailPage'
import ForgotPasswordPage from './pages/ForgotPasswordPage'
import ResetPasswordPage from './pages/ResetPasswordPage'
import AdminDashboardPage from './pages/admin/AdminDashboardPage'
import AdminUsersPage from './pages/admin/AdminUsersPage'
import AdminTransactionsPage from './pages/admin/AdminTransactionsPage'
import AdminServicesPage from './pages/admin/AdminServicesPage'
import { useAuthStore } from './store/authStore'

type AppHeaderProps = {
  activeTab: WalletTab
  setActiveTab: (tab: WalletTab) => void
}

type SessionErrorResponse = {
  code?: string
  message?: string
}

function formatBalance(balance?: number) {
  if (balance === undefined) {
    return 'Loading...'
  }

  return Number(balance).toFixed(2)
}

function SessionMonitor() {
  const navigate = useNavigate()
  const token = useAuthStore((state) => state.token)
  const logout = useAuthStore((state) => state.logout)
  const setAccount = useAuthStore((state) => state.setAccount)
  const intervalRef = useRef<number | null>(null)
  const isCheckingRef = useRef(false)

  useEffect(() => {
    if (intervalRef.current !== null) {
      window.clearInterval(intervalRef.current)
      intervalRef.current = null
    }

    if (!token) {
      return undefined
    }

    let isMounted = true

    const clearSession = (message: string) => {
      if (!isMounted) {
        return
      }

      if (intervalRef.current !== null) {
        window.clearInterval(intervalRef.current)
        intervalRef.current = null
      }

      sessionStorage.setItem('authMessage', message)
      logout()
      navigate('/login', { replace: true })
    }

    const checkAccount = async () => {
      if (isCheckingRef.current) {
        return
      }

      isCheckingRef.current = true

      try {
        const account = await accountApi.getCurrentAccount()
        if (isMounted) {
          setAccount(accountToAuthUser(account))
        }
      } catch (err) {
        if (axios.isAxiosError<SessionErrorResponse>(err)) {
          const code = err.response?.data?.code
          if (code === 'ACCOUNT_BLOCKED') {
            clearSession(
              err.response?.data?.message ||
                'Your account has been blocked by an administrator.',
            )
          } else if (code === 'UNAUTHORIZED') {
            clearSession('Your session has expired. Please log in again.')
          }
        }
      } finally {
        isCheckingRef.current = false
      }
    }

    void checkAccount()
    intervalRef.current = window.setInterval(checkAccount, 10000)

    return () => {
      isMounted = false
      if (intervalRef.current !== null) {
        window.clearInterval(intervalRef.current)
        intervalRef.current = null
      }
    }
  }, [logout, navigate, setAccount, token])

  return null
}

function AppHeader({ activeTab, setActiveTab }: AppHeaderProps) {
  const navigate = useNavigate()
  const token = useAuthStore((state) => state.token)
  const user = useAuthStore((state) => state.user)
  const wallet = useAuthStore((state) => state.wallet)
  const setWalletData = useAuthStore((state) => state.setWalletData)
  const logout = useAuthStore((state) => state.logout)
  const [isDropdownOpen, setIsDropdownOpen] = useState(false)

  const balanceText = formatBalance(wallet?.balance)
  const isUser = user?.role === 'user'
  const isAdmin = user?.role === 'admin'

  useEffect(() => {
    if (!token || user?.role !== 'user') {
      return
    }

    walletApi
      .getMyWallet()
      .then((data) => setWalletData(data.user, data.wallet))
      .catch((err) => console.error(err))
  }, [token, user?.role, setWalletData])

  const openDashboardTab = (tab: WalletTab) => {
    setActiveTab(tab)
    navigate('/dashboard')
  }

  const handleLogout = () => {
    logout()
    setIsDropdownOpen(false)
    navigate('/login')
  }

  const openProfile = () => {
    setIsDropdownOpen(false)
    navigate('/profile')
  }

  return (
    <>
      <header className="app-header">
        <Link className="brand" to="/">
          <span className="brand-mark">E</span>
          <span>E-Wallet</span>
        </Link>

        <div className="search-bar">
          <span aria-hidden="true">Search</span>
          <input placeholder="Search transactions, phone number, services..." />
        </div>

        <nav className="nav-menu" aria-label="Main navigation">
          <Link to="/">Home</Link>
          {isUser && (
            <>
              <button onClick={() => openDashboardTab('wallet')}>Wallet</button>
              <button onClick={() => openDashboardTab('history')}>
                Transactions
              </button>
              <button onClick={() => openDashboardTab('deposit')}>
                Deposit
              </button>
              <button onClick={() => openDashboardTab('services')}>
                Services
              </button>
            </>
          )}
          {isAdmin && (
            <>
              <Link to="/admin">Admin Dashboard</Link>
              <Link to="/admin/users">Users</Link>
              <Link to="/admin/transactions">Transactions</Link>
              <Link to="/admin/services">Services</Link>
              <button onClick={handleLogout}>Logout</button>
            </>
          )}
        </nav>

        <div className="account-menu">
          {token ? (
            <>
              <button
                className="user-icon-button"
                aria-label="Open account menu"
                onClick={() => setIsDropdownOpen((value) => !value)}
              >
                <span aria-hidden="true" />
              </button>

              {isDropdownOpen && (
                <div className="account-dropdown">
                  <div className="dropdown-heading">
                    <strong>{user?.fullName || user?.phone || 'Account'}</strong>
                    <span>Account details</span>
                  </div>
                  <div>
                    <span>Full name</span>
                    <strong>{user?.fullName || 'Not provided'}</strong>
                  </div>
                  <div>
                    <span>Phone</span>
                    <strong>{user?.phone || 'Unknown'}</strong>
                  </div>
                  <div>
                    <span>Role</span>
                    <strong>{user?.role || 'Unknown'}</strong>
                  </div>
                  <div>
                    <span>Status</span>
                    <strong>{user?.status || 'Unknown'}</strong>
                  </div>
                  {isAdmin && (
                    <div>
                      <span>Position</span>
                      <strong>{user?.position || 'N/A'}</strong>
                    </div>
                  )}
                  {isUser && (
                    <div>
                      <span>Current balance</span>
                      <strong>{balanceText}</strong>
                    </div>
                  )}
                  <button className="secondary-button" onClick={openProfile}>
                    Edit Profile
                  </button>
                  <button className="logout-button" onClick={handleLogout}>
                    Logout
                  </button>
                </div>
              )}
            </>
          ) : (
            <Link className="account-button" to="/login">
              Account
            </Link>
          )}
        </div>
      </header>

      {isUser && (
        <div className="wallet-nav-bar">
          <div className="wallet-nav-inner">
            <button
              className={activeTab === 'wallet' ? 'active' : ''}
              onClick={() => openDashboardTab('wallet')}
            >
              Wallet Info
            </button>
            <button
              className={activeTab === 'transfer' ? 'active' : ''}
              onClick={() => openDashboardTab('transfer')}
            >
              Transfer Money
            </button>
            <button
              className={activeTab === 'deposit' ? 'active' : ''}
              onClick={() => openDashboardTab('deposit')}
            >
              Deposit
            </button>
            <button
              className={activeTab === 'services' ? 'active' : ''}
              onClick={() => openDashboardTab('services')}
            >
              Services
            </button>
            <button
              className={activeTab === 'history' ? 'active' : ''}
              onClick={() => openDashboardTab('history')}
            >
              Transaction History
            </button>
          </div>
        </div>
      )}
    </>
  )
}

function App() {
  const [activeTab, setActiveTab] = useState<WalletTab>('wallet')

  return (
    <ToastProvider>
      <div className="app-shell">
        <SessionMonitor />
        <AppHeader activeTab={activeTab} setActiveTab={setActiveTab} />
        <Routes>
          <Route path="/" element={<HomePage />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route path="/verify-email" element={<VerifyEmailPage />} />
          <Route path="/forgot-password" element={<ForgotPasswordPage />} />
          <Route path="/reset-password" element={<ResetPasswordPage />} />
          <Route
            path="/profile"
            element={
              <ProtectedRoute>
                <ProfilePage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/dashboard"
            element={
              <UserRoute>
                <DashboardPage activeTab={activeTab} />
              </UserRoute>
            }
          />
          <Route
            path="/admin"
            element={
              <AdminRoute>
                <AdminDashboardPage />
              </AdminRoute>
            }
          />
          <Route
            path="/admin/users"
            element={
              <AdminRoute>
                <AdminUsersPage />
              </AdminRoute>
            }
          />
          <Route
            path="/admin/transactions"
            element={
              <AdminRoute>
                <AdminTransactionsPage />
              </AdminRoute>
            }
          />
          <Route path="/admin/services" element={<AdminRoute><AdminServicesPage /></AdminRoute>} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </div>
    </ToastProvider>
  )
}

export default App
