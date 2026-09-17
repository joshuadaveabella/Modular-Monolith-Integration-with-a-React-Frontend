function OrderHistory({ orders, onCancel, cancellingId }) {
  if (orders.length === 0) {
    return (
      <section className="panel">
        <h2>Order History</h2>
        <p className="muted">No orders yet.</p>
      </section>
    )
  }

  return (
    <section className="panel">
      <h2>Order History</h2>

      <div className="order-list">
        {orders.map((order) => (
          <div key={order.orderId} className="order-card">
            <div className="order-head">
              <strong>Order #{order.orderId}</strong>
              <span className={`badge badge-${order.status.toLowerCase()}`}>{order.status}</span>
            </div>

            <ul className="order-lines">
              {order.items.map((line, i) => (
                <li key={i}>
                  {line.productId} x {line.quantity}
                </li>
              ))}
            </ul>

            {order.reason && <p className="order-reason">{order.reason}</p>}

            <p className="muted small">{new Date(order.createdAt).toLocaleString()}</p>

            {/* Only CONFIRMED orders can be cancelled - a REJECTED order
                never reserved stock, so there is nothing to return. */}
            {order.status === 'CONFIRMED' && (
              <button
                type="button"
                className="danger"
                onClick={() => onCancel(order.orderId)}
                disabled={cancellingId === order.orderId}
              >
                {cancellingId === order.orderId ? 'Cancelling...' : 'Cancel & Restock'}
              </button>
            )}
          </div>
        ))}
      </div>
    </section>
  )
}

export default OrderHistory
