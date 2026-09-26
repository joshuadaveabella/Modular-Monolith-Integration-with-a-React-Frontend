import { useCallback, useEffect, useState } from 'react'
import {
  fetchInventory,
  fetchOrders,
  fetchNotifications,
  placeOrder,
  cancelOrder,
} from './api/shopApi.js'
import Cart from './components/Cart.jsx'
import InventoryTable from './components/InventoryTable.jsx'
import OrderHistory from './components/OrderHistory.jsx'
import NotificationFeed from './components/NotificationFeed.jsx'

function App() {
  const [inventory, setInventory] = useState([])
  const [threshold, setThreshold] = useState(5)
  const [orders, setOrders] = useState([])
  const [notifications, setNotifications] = useState([])

  const [result, setResult] = useState(null)
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [cancellingId, setCancellingId] = useState(null)

  const refreshAll = useCallback(async () => {
    try {
      const [inv, ord, notes] = await Promise.all([
        fetchInventory(),
        fetchOrders(),
        fetchNotifications(),
      ])
      setInventory(inv.data.items)
      setThreshold(inv.data.lowStockThreshold)
      setOrders(ord.data)
      setNotifications(notes.data)
    } catch (err) {
      setError('Could not reach the server. Is the backend running on port 8080?')
    }
  }, [])

  useEffect(() => {
    refreshAll()
  }, [refreshAll])

  async function handlePlaceOrder(items) {
    setError('')
    setResult(null)
    setSubmitting(true)
    try {
      const response = await placeOrder(items)
      setResult(response.data)
      await refreshAll()
    } catch (err) {
      setError(err.response?.data?.message ?? 'Something went wrong placing the order.')
    } finally {
      setSubmitting(false)
    }
  }

  async function handleCancel(orderId) {
    setError('')
    setResult(null)
    setCancellingId(orderId)
    try {
      const response = await cancelOrder(orderId)
      setResult({ status: 'CANCELLED', reason: response.data.message, items: [] })
      await refreshAll()
    } catch (err) {
      setError(err.response?.data?.message ?? 'Could not cancel that order.')
    } finally {
      setCancellingId(null)
    }
  }

  return (
    <div className="app">
      <header>
        <h1>Order &amp; Inventory Dashboard</h1>
      </header>

      {error && <div className="banner banner-error">{error}</div>}

      {result && (
        <div className={`banner banner-${result.status.toLowerCase()}`}>
          <strong>
            {result.orderId ? `Order #${result.orderId}: ` : ''}
            {result.status}
          </strong>
          {result.reason && <div>{result.reason}</div>}
          {result.items?.length > 0 && (
            <ul className="outcome-list">
              {result.items.map((item, i) => (
                <li key={i}>{item.productId} x {item.quantity} - {item.outcome}</li>
              ))}
            </ul>
          )}
        </div>
      )}

      <div className="columns">
        <div className="col">
          <Cart products={inventory} onSubmit={handlePlaceOrder} submitting={submitting} />
          <InventoryTable items={inventory} threshold={threshold} />
        </div>
        <div className="col">
          <OrderHistory orders={orders} onCancel={handleCancel} cancellingId={cancellingId} />
          <NotificationFeed notifications={notifications} />
        </div>
      </div>
    </div>
  )
}

export default App
