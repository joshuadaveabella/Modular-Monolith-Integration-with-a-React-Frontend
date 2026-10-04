function InventoryTable({ items, threshold }) {
  return (
    <section className="panel">
      <h2>Inventory</h2>
      <p className="muted">Rows below {threshold} units are flagged for reorder.</p>
      <table className="data-table">
        <thead><tr><th>Product ID</th><th>Name</th><th>Stock</th></tr></thead>
        <tbody>
          {items.map((item) => {
            const out = item.stock === 0
            const low = item.stock > 0 && item.stock < threshold
            return (
              <tr key={item.productId} className={out ? 'row-out' : low ? 'row-low' : ''}>
                <td>{item.productId}</td>
                <td>{item.name}</td>
                <td>
                  {item.stock}
                  {out && <span className="tag tag-out">out of stock</span>}
                  {low && <span className="tag tag-low">low</span>}
                </td>
              </tr>
            )
          })}
        </tbody>
      </table>
    </section>
  )
}

export default InventoryTable
