const TYPE_LABELS = {
  ORDER_CONFIRMED: 'confirmed',
  ORDER_REJECTED: 'rejected',
  LOW_STOCK: 'reorder',
  SUPPLIER_ORDER_DELIVERED: 'delivered',
  BACKORDER_RESOLVED: 'backorder',
}

function NotificationFeed({ notifications }) {
  return (
    <section className="panel">
      <h2>Activity Feed</h2>
      {notifications.length === 0 ? (
        <p className="muted">No activity yet.</p>
      ) : (
        <ul className="feed">
          {notifications.map((n) => (
            <li key={n.notificationId} className={`feed-item feed-${n.type.toLowerCase()}`}>
              <span className={`badge badge-${n.type.toLowerCase()}`}>{TYPE_LABELS[n.type] ?? n.type}</span>
              <span className="feed-message">{n.message}</span>
              <span className="muted small">{new Date(n.createdAt).toLocaleTimeString()}</span>
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}

export default NotificationFeed
