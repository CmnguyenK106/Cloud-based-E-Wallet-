import { Link } from 'react-router-dom'
import { useAuthStore } from '../store/authStore'

function HomePage() {
  const token = useAuthStore((state) => state.token)

  return (
    <main className="home-page">
      <section className="hero-section">
        <div className="hero-copy">
          <span className="eyebrow">Cloud Wallet</span>
          <h1>A simple cloud-based wallet for digital payments.</h1>
          <p>
            Register with your phone number, receive an initial balance,
            transfer money, pay virtual services, and track transactions.
          </p>
          {!token && (
            <div className="hero-actions">
              <Link className="primary-button" to="/register">
                Get Started
              </Link>
              <Link className="secondary-button" to="/login">
                Log in
              </Link>
            </div>
          )}
        </div>

        <div className="wallet-showcase" aria-label="Wallet highlights">
          <div className="wallet-card blue-wallet">
            <span>Balance</span>
            <strong>10.00 coins</strong>
          </div>
          <div className="wallet-card black-wallet">
            <span>Transfer</span>
            <strong>Phone to phone</strong>
          </div>
          <div className="wallet-card white-wallet">
            <span>Payment</span>
            <strong>Virtual services</strong>
          </div>
        </div>
      </section>

      <section className="feature-grid">
        <article>
          <h2>One Account One Wallet</h2>
          <p>Create one account and receive one personal wallet.</p>
        </article>
        <article>
          <h2>Mock Deposit</h2>
          <p>Add demo coins to test wallet balance changes.</p>
        </article>
        <article>
          <h2>Fast Transfer</h2>
          <p>Send coins to another account by phone number.</p>
        </article>
        <article>
          <h2>Virtual Payment</h2>
          <p>Pay sample services with your wallet balance.</p>
        </article>
        <article>
          <h2>Transaction History</h2>
          <p>Track deposits, transfers, payments, and account activity.</p>
        </article>
      </section>
    </main>
  )
}

export default HomePage
