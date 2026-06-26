import axios from 'axios'
import { type FormEvent, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { authApi } from '../apis/authApi'
import { loginSchema, type LoginForm } from '../schema/authSchema'
import { useAuthStore } from '../store/authStore'

type LoginErrors = Partial<Record<keyof LoginForm, string>>

function LoginPage() {
  const navigate = useNavigate()
  const setAuth = useAuthStore((state) => state.setAuth)
  const [form, setForm] = useState<LoginForm>({ phone: '', password: '' })
  const [errors, setErrors] = useState<LoginErrors>({})
  const [message, setMessage] = useState('')
  const [isLoading, setIsLoading] = useState(false)

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setMessage('')

    const result = loginSchema.safeParse(form)
    if (!result.success) {
      const fieldErrors = result.error.flatten().fieldErrors
      setErrors({
        phone: fieldErrors.phone?.[0],
        password: fieldErrors.password?.[0],
      })
      return
    }

    setErrors({})
    setIsLoading(true)

    try {
      const data = await authApi.login(result.data)
      if (data.token && data.user) {
        setAuth(data.token, data.user, data.wallet)
        navigate('/dashboard')
        return
      }
      setMessage(data.message || 'Login failed')
    } catch (err) {
      console.error(err)
      if (axios.isAxiosError<{ message?: string }>(err)) {
        setMessage(err.response?.data?.message || err.message || 'Login failed')
      } else {
        setMessage('Login failed')
      }
    } finally {
      setIsLoading(false)
    }
  }

  return (
    <main className="auth-page">
      <section className="auth-card">
        <div className="auth-heading">
          <span className="eyebrow">Welcome back</span>
          <h1>Login to your wallet</h1>
          <p>
            Log in to manage your balance, transfers, payments, and transaction
            history.
          </p>
        </div>

        <form onSubmit={handleSubmit} className="auth-form">
          <label>
            Phone
            <input
              value={form.phone}
              onChange={(event) =>
                setForm({ ...form, phone: event.target.value })
              }
              placeholder="0911111111"
            />
            {errors.phone && <span className="field-error">{errors.phone}</span>}
          </label>

          <label>
            Password
            <input
              value={form.password}
              onChange={(event) =>
                setForm({ ...form, password: event.target.value })
              }
              placeholder="123456"
              type="password"
            />
            {errors.password && (
              <span className="field-error">{errors.password}</span>
            )}
          </label>

          {message && <div className="form-message error">{message}</div>}

          <button className="primary-button full-width" disabled={isLoading}>
            {isLoading ? 'Logging in...' : 'Login'}
          </button>
        </form>

        <p className="auth-switch">
          New customer? <Link to="/register">Create an account</Link>
        </p>
      </section>
    </main>
  )
}

export default LoginPage
