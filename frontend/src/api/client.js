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
  getWppConnectSettings: () => request('GET', '/settings/wppconnect'),
  updateWppConnectSettings: (data) => request('PUT', '/settings/wppconnect', data),
  generateWppConnectToken: () => request('POST', '/settings/wppconnect/generate-token'),
  getWppConnectQrCode: () => request('GET', '/settings/wppconnect/qrcode'),

  // Supplier catalog
  getSupplierOffers:         (supplierId) => request('GET', `/suppliers/${supplierId}/offers`),
  createSupplierOffer:       (data)       => request('POST', '/supplier-offers', data),
  updateSupplierOffer:       (id, data)   => request('PUT', `/supplier-offers/${id}`, data),
  deleteSupplierOffer:       (id)         => request('DELETE', `/supplier-offers/${id}`),
  linkSupplierOfferItem:     (id, data)   => request('PUT', `/supplier-offers/${id}/link-item`, data),
  importSupplierOffersExcel: async (supplierId, file) => {
    const form = new FormData()
    form.append('file', file)
    const res = await fetch(`${BASE_URL}/suppliers/${supplierId}/offers/import-excel`, {
      method: 'POST',
      body: form,
    })
    if (!res.ok) {
      const text = await res.text()
      throw new Error(`${res.status}: ${text}`)
    }
    return res.json()
  },

  // POS mappings
  getSalesProducts:          ()           => request('GET', '/sales-products'),
  getSalesProductComponents: (spId)       => request('GET', `/sales-products/${spId}/components`),
  addItemComponent:          (itemId, data) => request('POST', `/inventory-items/${itemId}/components`, data),
  deleteItemComponent:       (id)         => request('DELETE', `/product-components/${id}`),
}
