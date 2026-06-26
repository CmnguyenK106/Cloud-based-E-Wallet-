import { type ReactNode, useEffect, useState } from 'react'
import { Link, Navigate, Route, Routes, useNavigate } from 'react-router-dom'
import './App.css'
import { walletApi } from './apis/walletApi'
import DashboardPage, { type WalletTab } from './pages/DashboardPage'
import HomePage from './pages/HomePage'
import LoginPage from './pages/LoginPage'
import RegisterPage from './pages/RegisterPage'
import { useAuthStore } from './store/authStore'

type AppHeaderProps = {
  activeTab: WalletTab
  setActiveTab: (tab: WalletTab) => void
}

function formatBalance(balance?: number) {
  if (balance === undefined) {
    return 'Loading...'
  }

  return Number(balance).toFixed(2)
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

  useEffect(() => {
    if (!token) {
      return
    }

    walletApi
      .getMyWallet()
      .then((data) => setWalletData(data.user, data.wallet))
      .catch((err) => console.error(err))
  }, [token, setWalletData])

  const openDashboardTab = (tab: WalletTab) => {
    setActiveTab(tab)
    navigate('/dashboard')
  }

  const handleLogout = () => {
    logout()
    setIsDropdownOpen(false)
    navigate('/login')
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
          <button onClick={() => openDashboardTab('wallet')}>Wallet</button>
          <a href="#transactions">Transactions</a>
          <button onClick={() => openDashboardTab('services')}>Services</button>
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
                  <div>
                    <span>Current balance</span>
                    <strong>{balanceText}</strong>
                  </div>
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
            className={activeTab === 'services' ? 'active' : ''}
            onClick={() => openDashboardTab('services')}
          >
            Services
          </button>
        </div>
      </div>
    </>
  )
}

function ProtectedRoute({ children }: { children: ReactNode }) {
  const token = useAuthStore((state) => state.token)

  if (!token) {
    return <Navigate to="/login" replace />
  }

  return children
}

function App() {
  const [activeTab, setActiveTab] = useState<WalletTab>('wallet')

  return (
    <div className="app-shell">
      <AppHeader activeTab={activeTab} setActiveTab={setActiveTab} />
      <Routes>
        <Route path="/" element={<HomePage />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route
          path="/dashboard"
          element={
            <ProtectedRoute>
              <DashboardPage activeTab={activeTab} />
            </ProtectedRoute>
          }
        />
      </Routes>
    </div>
  )
}

export default App
