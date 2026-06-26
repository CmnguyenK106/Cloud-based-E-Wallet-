import axios from 'axios'
import { type FormEvent, useEffect, useState } from 'react'
import { walletApi } from '../apis/walletApi'
import { useAuthStore } from '../store/authStore'

export type WalletTab = 'wallet' | 'transfer' | 'services'

type DashboardPageProps = {
  activeTab: WalletTab
}

type TransferForm = {
  receiverPhone: string
  amount: string
  description: string
}

const services = [
  {
    name: 'Phone Top-up',
    price: '10.00',
    description: 'Add credit to a mobile phone account.',
  },
  {
    name: 'Electricity Bill',
    price: '50.00',
    description: 'Pay a sample electricity service bill.',
  },
  {
    name: 'Water Bill',
    price: '30.00',
    description: 'Pay a sample water service bill.',
  },
  {
    name: 'Internet Package',
    price: '100.00',
    description: 'Buy a virtual internet package.',
  },
  {
    name: 'Game Top-up',
    price: '20.00',
    description: 'Add demo credit to a game account.',
  },
]

function formatBalance(balance?: number) {
  if (balance === undefined) {
    return 'Loading...'
  }

  return Number(balance).toFixed(2)
}

function DashboardPage({ activeTab }: DashboardPageProps) {
  const user = useAuthStore((state) => state.user)
  const wallet = useAuthStore((state) => state.wallet)
  const setWalletData = useAuthStore((state) => state.setWalletData)
  const [transferForm, setTransferForm] = useState<TransferForm>({
    receiverPhone: '',
    amount: '',
    description: '',
  })
  const [walletMessage, setWalletMessage] = useState('')
  const [transferMessage, setTransferMessage] = useState('')
  const [isTransferSuccess, setIsTransferSuccess] = useState(false)
  const [isWalletLoading, setIsWalletLoading] = useState(false)
  const [isTransferLoading, setIsTransferLoading] = useState(false)
  const [paymentMessage, setPaymentMessage] = useState('')

  const balanceText = formatBalance(wallet?.balance)

  const loadWallet = async () => {
    setIsWalletLoading(true)
    setWalletMessage('')

    try {
      const data = await walletApi.getMyWallet()
      setWalletData(data.user, data.wallet)
    } catch (err) {
      console.error(err)
      if (axios.isAxiosError<{ message?: string }>(err)) {
        setWalletMessage(
          err.response?.data?.message || err.message || 'Cannot load wallet',
        )
      } else {
        setWalletMessage('Cannot load wallet')
      }
    } finally {
      setIsWalletLoading(false)
    }
  }

  useEffect(() => {
    loadWallet()
  }, [])

  const handleTransferSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setTransferMessage('')
    setIsTransferSuccess(false)
    setIsTransferLoading(true)

    try {
      const data = await walletApi.transferMoney({
        receiverPhone: transferForm.receiverPhone.trim(),
        amount: Number(transferForm.amount),
        description: transferForm.description.trim(),
      })
      setTransferMessage(data.message)
      setIsTransferSuccess(true)
      setTransferForm({ receiverPhone: '', amount: '', description: '' })
      await loadWallet()
    } catch (err) {
      console.error(err)
      if (axios.isAxiosError<{ message?: string }>(err)) {
        setTransferMessage(
          err.response?.data?.message || err.message || 'Transfer failed',
        )
      } else {
        setTransferMessage('Transfer failed')
      }
    } finally {
      setIsTransferLoading(false)
    }
  }

  const handlePayClick = (serviceName: string) => {
    setPaymentMessage(
      `${serviceName} payment feature will be connected later.`,
    )
  }

  return (
    <main className="dashboard-page">
      <section className="dashboard-hero">
        <div>
          <span className="eyebrow">My E-Wallet</span>
          <h1>Hello, {user?.fullName || user?.phone}</h1>
          <p>You are logged in and can manage your wallet.</p>
          {walletMessage && <div className="form-message error">{walletMessage}</div>}
        </div>
        <div className="balance-summary">
          <span>Current balance</span>
          <strong>{isWalletLoading ? 'Loading...' : balanceText}</strong>
        </div>
      </section>

      {activeTab === 'wallet' && (
        <section className="dashboard-card wallet-info-card">
          <div>
            <span className="eyebrow">Wallet Info</span>
            <h2>This is your current e-wallet information.</h2>
          </div>

          <div className="account-grid">
            <div>
              <span>Full name</span>
              <strong>{user?.fullName || 'Not provided'}</strong>
            </div>
            <div>
              <span>Phone</span>
              <strong>{user?.phone}</strong>
            </div>
            <div>
              <span>Role</span>
              <strong>{user?.role}</strong>
            </div>
            <div>
              <span>Status</span>
              <strong>{user?.status}</strong>
            </div>
            <div>
              <span>Current balance</span>
              <strong>{isWalletLoading ? 'Loading...' : balanceText}</strong>
            </div>
          </div>
        </section>
      )}

      {activeTab === 'transfer' && (
        <section className="dashboard-card">
          <div>
            <span className="eyebrow">Transfer Money</span>
            <h2>Send money to another wallet account.</h2>
          </div>

          <form className="transfer-form" onSubmit={handleTransferSubmit}>
            <label>
              Receiver phone number
              <input
                placeholder="0912345678"
                value={transferForm.receiverPhone}
                onChange={(event) =>
                  setTransferForm({
                    ...transferForm,
                    receiverPhone: event.target.value,
                  })
                }
              />
            </label>
            <label>
              Amount
              <input
                placeholder="10.00"
                type="number"
                min="0"
                step="0.01"
                value={transferForm.amount}
                onChange={(event) =>
                  setTransferForm({
                    ...transferForm,
                    amount: event.target.value,
                  })
                }
              />
            </label>
            <label>
              Note or description
              <textarea
                placeholder="Optional transfer note"
                rows={4}
                value={transferForm.description}
                onChange={(event) =>
                  setTransferForm({
                    ...transferForm,
                    description: event.target.value,
                  })
                }
              />
            </label>
            {transferMessage && (
              <div
                className={`form-message ${
                  isTransferSuccess ? 'success' : 'error'
                }`}
              >
                {transferMessage}
              </div>
            )}
            <button className="primary-button" disabled={isTransferLoading}>
              {isTransferLoading ? 'Transferring...' : 'Transfer'}
            </button>
          </form>
        </section>
      )}

      {activeTab === 'services' && (
        <section className="dashboard-card">
          <div>
            <span className="eyebrow">Services</span>
            <h2>Pay virtual services from your wallet.</h2>
          </div>

          {paymentMessage && (
            <div className="form-message success">{paymentMessage}</div>
          )}

          <div className="service-grid">
            {services.map((service) => (
              <article className="service-card" key={service.name}>
                <div className="service-image">No Image</div>
                <div>
                  <h3>{service.name}</h3>
                  <p>{service.description}</p>
                </div>
                <div className="service-footer">
                  <strong>{service.price}</strong>
                  <button
                    className="secondary-button"
                    onClick={() => handlePayClick(service.name)}
                  >
                    Pay
                  </button>
                </div>
              </article>
            ))}
          </div>
        </section>
      )}
    </main>
  )
}

export default DashboardPage
