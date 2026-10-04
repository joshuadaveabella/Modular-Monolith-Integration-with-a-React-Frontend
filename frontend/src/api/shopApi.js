import axios from 'axios'

const api = axios.create({
  baseURL: 'http://localhost:8080/api',
})

export function fetchInventory() { return api.get('/inventory') }
export function fetchOrders() { return api.get('/orders') }
export function fetchNotifications() { return api.get('/notifications') }
export function placeOrder(items) { return api.post('/orders', { items }) }
export function cancelOrder(orderId) { return api.post(`/orders/${orderId}/cancel`) }
