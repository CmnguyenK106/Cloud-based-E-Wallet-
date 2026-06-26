import axios from 'axios'
import { type FormEvent, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { authApi } from '../apis/authApi'
import { registerSchema, type RegisterForm } from '../schema/authSchema'

type RegisterErrors = Partial<Record<keyof RegisterForm, string>>

function RegisterPage() {
  const navigate = useNavigate()
  const [form, setForm] = useState<RegisterForm>({
    phone: '',
    password: '',
    fullName: '',
  })
  const [errors, setErrors] = useState<RegisterErrors>({})
  const [message, setMessage] = useState('')
  const [isSuccess, setIsSuccess] = useState(false)
  const [isLoading, setIsLoading] = useState(false)

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setMessage('')
    setIsSuccess(false)

    const result = registerSchema.safeParse(form)
    if (!result.success) {
      const fieldErrors = result.error.flatten().fieldErrors
      setErrors({
        phone: fieldErrors.phone?.[0],
        password: fieldErrors.password?.[0],
        fullName: fieldErrors.fullName?.[0],
      })
      return
    }

    setErrors({})
    setIsLoading(true)

    try {
      const { phone, password, fullName } = result.data
      console.log('Register payload:', { phone, password, fullName })
      const data = await authApi.register(result.data)
      setMessage(data.message || 'Register successfully')
      setIsSuccess(true)
      setTimeout(() => navigate('/login'), 1000)
    } catch (err) {
      console.error(err)
      if (axios.isAxiosError<{ message?: string }>(err)) {
        setMessage(err.response?.data?.message || err.message || 'Register failed')
      } else {
        setMessage('Register failed')
      }
    } finally {
      setIsLoading(false)
    }
  }

  return (
    <main className="auth-page">
      <section className="auth-card">
        <div className="auth-heading">
          <span className="eyebrow">Open your wallet</span>
          <h1>Create your account</h1>
          <p>Create your wallet account and receive 10 coins.</p>
        </div>

        <form onSubmit={handleSubmit} className="auth-form">
          <label>
            Full name
            <input
              value={form.fullName}
              onChange={(event) =>
                setForm({ ...form, fullName: event.target.value })
              }
              placeholder="Test User"
            />
            {errors.fullName && (
              <span className="field-error">{errors.fullName}</span>
            )}
          </label>

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

          {message && (
            <div className={`form-message ${isSuccess ? 'success' : 'error'}`}>
              {message}
            </div>
          )}

          <button className="primary-button full-width" disabled={isLoading}>
            {isLoading ? 'Creating...' : 'Create account'}
          </button>
        </form>

        <p className="auth-switch">
          Already registered? <Link to="/login">Login</Link>
        </p>
      </section>
    </main>
  )
}

export default RegisterPage
