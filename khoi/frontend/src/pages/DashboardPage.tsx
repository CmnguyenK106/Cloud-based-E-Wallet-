import axios from 'axios'
import { type FormEvent, useCallback, useEffect, useState } from 'react'
import {
  serviceApi,
  type Service,
} from '../apis/serviceApi'
import { walletApi } from '../apis/walletApi'
import { authApi } from '../apis/authApi'
import ConfirmationModal from '../components/ConfirmationModal'
import TransactionHistory from '../components/TransactionHistory'
import { useToast } from '../hooks/useToast'
import { depositSchema, type DepositForm } from '../schema/walletSchema'
import { useAuthStore } from '../store/authStore'

export type WalletTab = 'wallet' | 'transfer' | 'deposit' | 'services' | 'history'

type DashboardPageProps = {
  activeTab: WalletTab
}

type TransferForm = {
  receiverPhone: string
  amount: string
  description: string
}

type DepositErrors = Partial<Record<keyof DepositForm, string>>

function formatBalance(balance?: number) {
  if (balance === undefined) {
    return 'Loading...'
  }

  return Number(balance).toFixed(2)
}

function formatCoins(value: number) {
  return `${Number(value).toFixed(2)} coins`
}

function DashboardPage({ activeTab }: DashboardPageProps) {
  const { showToast } = useToast()
  const user = useAuthStore((state) => state.user)
  const wallet = useAuthStore((state) => state.wallet)
  const setWalletData = useAuthStore((state) => state.setWalletData)
  const [transferForm, setTransferForm] = useState<TransferForm>({
    receiverPhone: '',
    amount: '',
    description: '',
  })
  const [depositForm, setDepositForm] = useState<DepositForm>({
    amount: '',
    description: '',
  })
  const [depositErrors, setDepositErrors] = useState<DepositErrors>({})
  const [walletMessage, setWalletMessage] = useState('')
  const [transferMessage, setTransferMessage] = useState('')
  const [isTransferSuccess, setIsTransferSuccess] = useState(false)
  const [isWalletLoading, setIsWalletLoading] = useState(false)
  const [isTransferLoading, setIsTransferLoading] = useState(false)
  const [isDepositLoading, setIsDepositLoading] = useState(false)
  const [services, setServices] = useState<Service[]>([])
  const [selectedService, setSelectedService] = useState<Service | null>(null)
  const [isServicesLoading, setIsServicesLoading] = useState(false)
  const [servicesError, setServicesError] = useState('')
  const [payingServiceId, setPayingServiceId] = useState<number | null>(null)
  const [transactionRefreshKey, setTransactionRefreshKey] = useState(0)
  const [verificationMessage, setVerificationMessage] = useState('')
  const isEmailVerified = user?.emailVerified !== false

  const balanceText = formatBalance(wallet?.balance)
  const selectedServicePrice = Number(selectedService?.price || 0)
  const selectedRemainingBalance =
    wallet && selectedService
      ? Number(wallet.balance) - selectedServicePrice
      : undefined
  const selectedHasInsufficientBalance =
    selectedRemainingBalance !== undefined && selectedRemainingBalance < 0

  const loadWallet = useCallback(async () => {
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
  }, [setWalletData])

  useEffect(() => {
    void Promise.resolve().then(() => loadWallet())
  }, [loadWallet])

  const loadServices = useCallback(async () => {
    setIsServicesLoading(true)
    setServicesError('')

    try {
      const data = await serviceApi.getActiveServices()
      setServices(data.services)
    } catch (err) {
      console.error(err)
      if (axios.isAxiosError<{ message?: string }>(err)) {
        setServicesError(
          err.response?.data?.message ||
            err.message ||
            'Cannot load services',
        )
      } else {
        setServicesError('Cannot load services')
      }
    } finally {
      setIsServicesLoading(false)
    }
  }, [])

  useEffect(() => {
    if (activeTab === 'services') {
      void Promise.resolve().then(() => loadServices())
    }
  }, [activeTab, loadServices])

  const handleTransferSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!isEmailVerified) { setTransferMessage('Verify your email before transferring money.'); return }
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
      setTransactionRefreshKey((value) => value + 1)
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

  const handleDepositSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!isEmailVerified) { showToast('Verify your email before depositing funds.', 'error'); return }

    const result = depositSchema.safeParse(depositForm)
    if (!result.success) {
      const fieldErrors = result.error.flatten().fieldErrors
      setDepositErrors({
        amount: fieldErrors.amount?.[0],
        description: fieldErrors.description?.[0],
      })
      return
    }

    if (!user || !wallet || isDepositLoading) {
      return
    }

    setDepositErrors({})
    setIsDepositLoading(true)

    try {
      const data = await walletApi.depositMoney({
        amount: Number(result.data.amount),
        description: result.data.description,
      })
      setWalletData(user, { ...wallet, balance: data.balance })
      setDepositForm({ amount: '', description: '' })
      showToast('Deposit completed successfully.', 'success')
      await loadWallet()
      setTransactionRefreshKey((value) => value + 1)
    } catch (err) {
      if (axios.isAxiosError<{ message?: string }>(err)) {
        showToast(
          err.response?.data?.message ||
            err.message ||
            'Unable to complete deposit.',
          'error',
        )
      } else {
        showToast('Unable to complete deposit.', 'error')
      }
    } finally {
      setIsDepositLoading(false)
    }
  }

  const handlePayClick = (service: Service) => {
    if (!isEmailVerified) { showToast('Verify your email before paying for services.', 'error'); return }
    if (payingServiceId !== null) {
      return
    }

    setSelectedService(service)
  }

  const handlePaymentConfirm = async () => {
    if (
      !user ||
      !wallet ||
      !selectedService ||
      payingServiceId !== null ||
      !Number.isInteger(selectedService.id)
    ) {
      return
    }

    setPayingServiceId(selectedService.id)

    try {
      const data = await serviceApi.payService(
        selectedService.id,
        `Payment for ${selectedService.name}`,
      )
      setWalletData(user, { ...wallet, balance: data.balance })
      showToast('Payment completed successfully.', 'success')
      setSelectedService(null)
      await loadWallet()
      setTransactionRefreshKey((value) => value + 1)
    } catch (err) {
      if (axios.isAxiosError<{ message?: string }>(err)) {
        showToast(
          err.response?.data?.message ||
            err.message ||
            'Unable to complete payment.',
          'error',
        )
      } else {
        showToast('Unable to complete payment.', 'error')
      }
    } finally {
      setPayingServiceId(null)
    }
  }

  const paymentModalMessage = selectedService
    ? selectedHasInsufficientBalance
      ? `Pay ${formatCoins(selectedServicePrice)} for ${selectedService.name}? Current balance: ${balanceText} coins. Insufficient wallet balance.`
      : `Pay ${formatCoins(selectedServicePrice)} for ${selectedService.name}? Current balance: ${balanceText} coins. Remaining balance: ${formatCoins(selectedRemainingBalance || 0)}.`
    : ''

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

      {!isEmailVerified && (
        <section className="dashboard-card">
          <div className="form-message error">Your email is not verified. Wallet deposits, transfers, and payments are disabled.</div>
          <button className="secondary-button" disabled={!user?.email} onClick={async () => {
            if (!user?.email) return
            try { const data = await authApi.resendVerification(user.email); setVerificationMessage(data.message) }
            catch { setVerificationMessage('Unable to request another verification link.') }
          }}>Resend verification email</button>
          {verificationMessage && <div className="form-message success">{verificationMessage}</div>}
        </section>
      )}

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
            <button className="primary-button" disabled={isTransferLoading || !isEmailVerified}>
              {isTransferLoading ? 'Transferring...' : 'Transfer'}
            </button>
          </form>
        </section>
      )}

      {activeTab === 'deposit' && (
        <section className="dashboard-card">
          <div>
            <span className="eyebrow">Deposit</span>
            <h2>Add simulated funds to your wallet.</h2>
          </div>

          <form className="transfer-form deposit-form" onSubmit={handleDepositSubmit}>
            <label>
              Amount
              <input
                placeholder="100.00"
                type="number"
                min="1"
                max="10000000"
                step="0.01"
                value={depositForm.amount}
                onChange={(event) =>
                  setDepositForm({
                    ...depositForm,
                    amount: event.target.value,
                  })
                }
              />
              {depositErrors.amount && (
                <span className="field-error">{depositErrors.amount}</span>
              )}
            </label>
            <label>
              Description
              <textarea
                placeholder="Optional deposit note"
                rows={4}
                maxLength={255}
                value={depositForm.description}
                onChange={(event) =>
                  setDepositForm({
                    ...depositForm,
                    description: event.target.value,
                  })
                }
              />
              {depositErrors.description && (
                <span className="field-error">
                  {depositErrors.description}
                </span>
              )}
            </label>
            <div className="deposit-actions">
              <button className="primary-button" disabled={isDepositLoading || !isEmailVerified}>
                {isDepositLoading ? 'Processing...' : 'Deposit'}
              </button>
              <button
                type="button"
                className="secondary-button"
                disabled={isDepositLoading}
                onClick={() => {
                  setDepositForm({ amount: '', description: '' })
                  setDepositErrors({})
                }}
              >
                Clear
              </button>
            </div>
          </form>
        </section>
      )}

      {activeTab === 'services' && (
        <section className="dashboard-card">
          <div>
            <span className="eyebrow">Services</span>
            <h2>Pay virtual services from your wallet.</h2>
          </div>

          {servicesError && (
            <div className="service-state">
              <strong>{servicesError}</strong>
              <button
                className="secondary-button"
                onClick={loadServices}
                disabled={isServicesLoading}
              >
                {isServicesLoading ? 'Loading...' : 'Retry'}
              </button>
            </div>
          )}

          {isServicesLoading && !services.length && (
            <div className="service-state" role="status">
              Loading services...
            </div>
          )}

          {!isServicesLoading && !servicesError && !services.length && (
            <div className="service-state">No active services are available.</div>
          )}

          {services.length > 0 && (
            <div className="service-grid">
              {services.map((service) => {
                const servicePrice = Number(service.price)
                const hasInsufficientBalance =
                  wallet !== null && Number(wallet.balance) < servicePrice
                const isPaying = payingServiceId === service.id

                return (
                  <article className="service-card" key={service.id}>
                    <div className="service-image">Service</div>
                    <div>
                      <h3>{service.name}</h3>
                      <p>
                        {service.description ||
                          'Simulated e-wallet service payment.'}
                      </p>
                    </div>
                    <div className="service-footer">
                      <strong>{formatCoins(servicePrice)}</strong>
                      <button
                        className="secondary-button"
                        onClick={() => handlePayClick(service)}
                        disabled={isPaying || !isEmailVerified}
                        aria-label={`Pay ${formatCoins(servicePrice)} for ${service.name}`}
                      >
                        {isPaying ? 'Processing...' : 'Pay'}
                      </button>
                    </div>
                    {hasInsufficientBalance && (
                      <span className="service-warning">
                        Insufficient wallet balance.
                      </span>
                    )}
                  </article>
                )
              })}
            </div>
          )}

          {selectedService && (
            <ConfirmationModal
              title="Confirm payment"
              message={paymentModalMessage}
              confirmLabel="Confirm payment"
              isConfirming={payingServiceId === selectedService.id}
              isConfirmDisabled={selectedHasInsufficientBalance}
              onConfirm={handlePaymentConfirm}
              onCancel={() => setSelectedService(null)}
            />
          )}
        </section>
      )}

      {activeTab === 'history' && (
        <TransactionHistory
          currentWalletId={wallet?.id}
          refreshKey={transactionRefreshKey}
        />
      )}
    </main>
  )
}

export default DashboardPage
