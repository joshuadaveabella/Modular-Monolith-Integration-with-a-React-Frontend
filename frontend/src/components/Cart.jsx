import { useState } from 'react'

function Cart({ products, onSubmit, submitting }) {
  const [selectedProduct, setSelectedProduct] = useState('')
  const [quantity, setQuantity] = useState(1)
  const [lines, setLines] = useState([])
  const [localError, setLocalError] = useState('')

  const available = products.filter((p) => !lines.some((l) => l.productId === p.productId))

  function handleAdd() {
    setLocalError('')
    const productId = selectedProduct || available[0]?.productId
    if (!productId) {
      setLocalError('No more products to add.')
      return
    }
    if (!quantity || Number(quantity) <= 0) {
      setLocalError('Quantity must be greater than zero.')
      return
    }
    const product = products.find((p) => p.productId === productId)
    setLines([...lines, { productId, name: product?.name ?? productId, quantity: Number(quantity) }])
    setSelectedProduct('')
    setQuantity(1)
  }

  function handleRemove(productId) {
    setLines(lines.filter((l) => l.productId !== productId))
  }

  function handleQuantityChange(productId, newQuantity) {
    setLines(lines.map((l) => (l.productId === productId ? { ...l, quantity: Number(newQuantity) } : l)))
  }

  function handleSubmit(e) {
    e.preventDefault()
    setLocalError('')
    if (lines.length === 0) {
      setLocalError('Add at least one item to the cart.')
      return
    }
    onSubmit(lines.map((l) => ({ productId: l.productId, quantity: l.quantity })))
  }

  return (
    <section className="panel">
      <h2>Build an Order</h2>

      <div className="add-row">
        <select
          value={selectedProduct || available[0]?.productId || ''}
          onChange={(e) => setSelectedProduct(e.target.value)}
          disabled={available.length === 0}
        >
          {available.map((p) => (
            <option key={p.productId} value={p.productId}>
              {p.productId} - {p.name} (stock: {p.stock})
            </option>
          ))}
        </select>
        <input
          type="number"
          min="1"
          value={quantity}
          onChange={(e) => setQuantity(e.target.value)}
          disabled={available.length === 0}
        />
        <button type="button" onClick={handleAdd} disabled={available.length === 0}>
          Add to cart
        </button>
      </div>

      {lines.length === 0 ? (
        <p className="muted">Cart is empty.</p>
      ) : (
        <table className="cart-table">
          <thead>
            <tr>
              <th>Product</th>
              <th>Qty</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {lines.map((l) => (
              <tr key={l.productId}>
                <td>{l.productId} - {l.name}</td>
                <td>
                  <input
                    type="number"
                    min="1"
                    value={l.quantity}
                    onChange={(e) => handleQuantityChange(l.productId, e.target.value)}
                    className="qty-input"
                  />
                </td>
                <td>
                  <button type="button" className="link-button" onClick={() => handleRemove(l.productId)}>
                    remove
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      {localError && <p className="error-text">{localError}</p>}

      <button type="button" onClick={handleSubmit} disabled={submitting || lines.length === 0}>
        {submitting ? 'Submitting...' : 'Submit Order'}
      </button>
    </section>
  )
}

export default Cart
