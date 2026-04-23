const BASE_URL = '/api'

async function request(method, path, body) {
  const options = {
    method,
    headers: { 'Content-Type': 'application/json' },
  }
  if (body !== undefined) {
    options.body = JSON.stringify(body)
  }
  const res = await fetch(`${BASE_URL}${path}`, options)
  if (!res.ok) {
    const text = await res.text()
    throw new Error(`${res.status}: ${text}`)
  }
  const text = await res.text()
  return text ? JSON.parse(text) : null
}

export const api = {
  getInventory: () => request('GET', '/inventory'),
  getSuppliers: () => request('GET', '/suppliers'),
  getConnectorConfig: (supplierId) => request('GET', `/suppliers/${supplierId}/connector`),
  updateConnectorConfig: (supplierId, data) => request('PUT', `/suppliers/${supplierId}/connector`, data),
  createReplenishmentOrder: (data) => request('POST', '/orders/replenishment', data),
  getSmtpSettings: () => request('GET', '/settings/smtp'),
  updateSmtpSettings: (data) => request('PUT', '/settings/smtp', data),
}
