import { useState } from 'react'
import axios from 'axios'

// Fixed to match the seeded inventory rows. The assignment doesn't call
// for a GET endpoint to list products dynamically, so this is hardcoded.
const PRODUCTS = [
  { id: 'P100', label: 'P100 - Wireless Mouse' },
  { id: 'P200', label: 'P200 - Mechanical Keyboard' },
  { id: 'P300', label: 'P300 - USB-C Hub' },
]

const API_URL = 'http://localhost:8080/api/orders'

function App() {
  const [productId, setProductId] = useState(PRODUCTS[0].id)
  const [quantity, setQuantity] = useState(1)
  const [result, setResult] = useState(null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  async function handleSubmit(e) {
    e.preventDefault()
    setError('')
    setResult(null)

    if (!quantity || Number(quantity) <= 0) {
      setError('Quantity must be greater than zero.')
      return
    }

    setLoading(true)
    try {
      const response = await axios.post(API_URL, {
        productId,
        quantity: Number(quantity),
      })
      setResult(response.data)
    } catch (err) {
      if (err.response?.data?.message) {
        setError(err.response.data.message)
      } else {
        setError('Could not reach the server.')
      }
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="page-container">
      <h1>Place an Order</h1>

      <form onSubmit={handleSubmit}>
        <label htmlFor="product">Product</label>
        <select id="product" value={productId} onChange={(e) => setProductId(e.target.value)}>
          {PRODUCTS.map((p) => (
            <option key={p.id} value={p.id}>
              {p.label}
            </option>
          ))}
        </select>

        <label htmlFor="quantity">Quantity</label>
        <input
          id="quantity"
          type="number"
          min="1"
          value={quantity}
          onChange={(e) => setQuantity(e.target.value)}
        />

        <button type="submit" disabled={loading}>
          {loading ? 'Placing order...' : 'Submit Order'}
        </button>
      </form>

      {error && <div className="error-message">{error}</div>}

      {result && (
        <div className={`result-box ${result.status === 'CONFIRMED' ? 'result-confirmed' : 'result-rejected'}`}>
          <p>
            <strong>Status:</strong> {result.status}
          </p>
          {result.reason && (
            <p>
              <strong>Reason:</strong> {result.reason}
            </p>
          )}
          {result.inventory && (
            <p>
              <strong>Remaining stock for {result.inventory.name}:</strong> {result.inventory.stock}
            </p>
          )}
        </div>
      )}
    </div>
  )
}

export default App
