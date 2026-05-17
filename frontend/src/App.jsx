import React, { useState, useEffect, useCallback } from 'react'
import { api } from './api/client.js'
import PosConnectorSettings from './components/PosConnectorSettings.jsx'

const styles = {
  container: { maxWidth: 1100, margin: '0 auto', padding: '24px 16px' },
  header: { fontSize: 24, fontWeight: 700, marginBottom: 24, color: '#1a1a2e' },
  tabs: { display: 'flex', gap: 8, marginBottom: 24, borderBottom: '2px solid #e0e0e0' },
  tab: (active) => ({
    padding: '10px 20px',
    cursor: 'pointer',
    border: 'none',
    background: 'none',
    fontSize: 15,
    fontWeight: active ? 700 : 400,
    color: active ? '#3b5bdb' : '#555',
    borderBottom: active ? '2px solid #3b5bdb' : '2px solid transparent',
    marginBottom: -2,
  }),
  card: {
    background: '#fff',
    borderRadius: 8,
    padding: 20,
    marginBottom: 16,
    boxShadow: '0 1px 4px rgba(0,0,0,0.08)',
  },
  table: { width: '100%', borderCollapse: 'collapse' },
  th: { textAlign: 'left', padding: '10px 12px', background: '#f0f2ff', fontSize: 13, fontWeight: 600 },
  td: (needsReorder) => ({
    padding: '10px 12px',
    borderBottom: '1px solid #f0f0f0',
    background: needsReorder ? '#fff5f5' : 'transparent',
    fontSize: 14,
  }),
  btn: (variant) => ({
    padding: '8px 18px',
    borderRadius: 6,
    border: 'none',
    cursor: 'pointer',
    fontWeight: 600,
    fontSize: 14,
    background: variant === 'primary' ? '#3b5bdb' : variant === 'danger' ? '#e03131' : '#e9ecef',
    color: variant === 'primary' || variant === 'danger' ? '#fff' : '#333',
    marginRight: 8,
  }),
  input: {
    padding: '8px 10px',
    borderRadius: 6,
    border: '1px solid #ced4da',
    fontSize: 14,
    width: '100%',
  },
  label: { fontSize: 13, fontWeight: 600, marginBottom: 4, display: 'block', color: '#555' },
  formRow: { marginBottom: 14 },
  badge: (type) => ({
    display: 'inline-block',
    padding: '2px 10px',
    borderRadius: 12,
    fontSize: 12,
    fontWeight: 600,
    background: type === 'low' ? '#ffe0e0' : '#e6f4ea',
    color: type === 'low' ? '#c92a2a' : '#2b8a3e',
  }),
  alert: (type) => ({
    padding: '12px 16px',
    borderRadius: 6,
    marginBottom: 16,
    background: type === 'success' ? '#ebfbee' : '#fff5f5',
    color: type === 'success' ? '#2b8a3e' : '#c92a2a',
    fontWeight: 500,
  }),
  subTabs: { display: 'flex', gap: 8, marginBottom: 20, borderBottom: '1px solid #e0e0e0' },
  twoCol: { display: 'flex', gap: 16 },
  leftPanel: { width: 280, flexShrink: 0 },
  rightPanel: { flex: 1, minWidth: 0 },
  overlay: {
    position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.4)',
    display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000,
  },
  modal: {
    background: '#fff', borderRadius: 10, padding: 28, width: 480, maxWidth: '95vw',
    maxHeight: '90vh', overflowY: 'auto',
  },
  warningBox: {
    padding: '10px 14px', borderRadius: 6, marginBottom: 12,
    background: '#fff9db', color: '#7c5800', border: '1px solid #ffe066', fontSize: 13,
  },
  listItem: (active) => ({
    padding: '10px 14px', cursor: 'pointer', borderRadius: 6, marginBottom: 4,
    background: active ? '#e8f0ff' : 'transparent',
    fontWeight: active ? 600 : 400,
    color: active ? '#3b5bdb' : '#333',
  }),
}

// ─── Tab 1: Order ─────────────────────────────────────────────────────────────
function OrderTab() {
  const [items, setItems] = useState([])
  const [selected, setSelected] = useState({})
  const [qtys, setQtys] = useState({})
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [message, setMessage] = useState(null)

  const load = useCallback(async () => {
    try {
      const data = await api.getInventory()
      setItems(data)
      const initQtys = {}
      data.forEach(item => {
        if (item.needsReorder) {
          const qty = Math.max(0, (item.reorderTarget || 0) - (item.cachedStock || 0))
          initQtys[item.id] = qty.toFixed(2)
        }
      })
      setQtys(initQtys)
    } catch (e) {
      setMessage({ type: 'error', text: 'Failed to load inventory: ' + e.message })
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { load() }, [load])

  const toggleSelect = (id) => {
    setSelected(prev => ({ ...prev, [id]: !prev[id] }))
  }

  const selectAllLowStock = () => {
    const next = {}
    items.forEach(item => {
      if (item.needsReorder) next[item.id] = true
    })
    setSelected(next)
  }

  const handleSubmit = async () => {
    const lines = Object.entries(selected)
      .filter(([, v]) => v)
      .map(([id]) => ({
        inventoryItemId: parseInt(id),
        requestedQty: parseFloat(qtys[id] || 0),
      }))
      .filter(l => l.requestedQty > 0)

    if (lines.length === 0) {
      setMessage({ type: 'error', text: 'Please select at least one item with quantity > 0' })
      return
    }

    setSubmitting(true)
    setMessage(null)
    try {
      const order = await api.createReplenishmentOrder({ lines })
      setMessage({ type: 'success', text: `Order #${order.id} created with status ${order.status}` })
      setSelected({})
    } catch (e) {
      setMessage({ type: 'error', text: 'Failed to create order: ' + e.message })
    } finally {
      setSubmitting(false)
    }
  }

  if (loading) return <div>Loading inventory...</div>

  return (
    <div>
      {message && <div style={styles.alert(message.type)}>{message.text}</div>}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <h2 style={{ fontSize: 18, fontWeight: 600 }}>Inventory Items</h2>
        <div>
          <button style={styles.btn('secondary')} onClick={selectAllLowStock}>
            Select all low-stock
          </button>
          <button style={styles.btn('primary')} onClick={handleSubmit} disabled={submitting}>
            {submitting ? 'Submitting...' : 'Submit Order'}
          </button>
        </div>
      </div>
      <div style={styles.card}>
        <table style={styles.table}>
          <thead>
            <tr>
              <th style={styles.th}></th>
              <th style={styles.th}>Item</th>
              <th style={styles.th}>Unit</th>
              <th style={styles.th}>Current Stock</th>
              <th style={styles.th}>Min Level</th>
              <th style={styles.th}>Reorder Target</th>
              <th style={styles.th}>Status</th>
              <th style={styles.th}>Order Qty</th>
            </tr>
          </thead>
          <tbody>
            {items.map(item => (
              <tr key={item.id}>
                <td style={styles.td(item.needsReorder)}>
                  <input
                    type="checkbox"
                    checked={!!selected[item.id]}
                    onChange={() => toggleSelect(item.id)}
                  />
                </td>
                <td style={{ ...styles.td(item.needsReorder), background: !item.hasPreferredOffer ? '#fff9db' : (item.needsReorder ? '#fff5f5' : 'transparent') }}>
                  {!item.hasPreferredOffer && <span title="No supplier linked — will not be transmitted">⚠️ </span>}
                  {item.name}
                </td>
                <td style={styles.td(item.needsReorder)}>{item.unit}</td>
                <td style={styles.td(item.needsReorder)}>{item.cachedStock?.toFixed(2)}</td>
                <td style={styles.td(item.needsReorder)}>{item.minStockLevel?.toFixed(2)}</td>
                <td style={styles.td(item.needsReorder)}>{item.reorderTarget?.toFixed(2)}</td>
                <td style={styles.td(item.needsReorder)}>
                  <span style={styles.badge(item.needsReorder ? 'low' : 'ok')}>
                    {item.needsReorder ? 'Low Stock' : 'OK'}
                  </span>
                </td>
                <td style={styles.td(item.needsReorder)}>
                  <input
                    type="number"
                    min="0"
                    step="0.01"
                    style={{ ...styles.input, width: 90 }}
                    value={qtys[item.id] || ''}
                    onChange={e => setQtys(prev => ({ ...prev, [item.id]: e.target.value }))}
                    disabled={!selected[item.id]}
                  />
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <p style={{ fontSize: 12, color: '#7c5800', marginTop: 8 }}>
        ⚠️ Items marked with ⚠️ have no supplier linked and will not be transmitted.
      </p>
    </div>
  )
}

// ─── Inventory sub-tabs: History ──────────────────────────────────────────────
function GoodsReceiptDialog({ order, onClose, onBooked }) {
  // quantities are always stored in supplier units when conversionFactor present, else inventory units
  const [quantities, setQuantities] = useState(() => {
    const m = {}
    order.lines.forEach(l => {
      const cf = l.conversionFactor && l.conversionFactor > 1 ? l.conversionFactor : null
      m[l.inventoryItemId] = cf
        ? String(Math.ceil(l.requestedQty / cf))
        : String(l.requestedQty ?? '')
    })
    return m
  })
  const [submitting, setSubmitting] = useState(false)
  const [err, setErr] = useState(null)

  const confirm = async () => {
    setSubmitting(true)
    setErr(null)
    try {
      const lines = order.lines.map(l => {
        const cf = l.conversionFactor && l.conversionFactor > 1 ? l.conversionFactor : 1
        const supplierQty = parseFloat(quantities[l.inventoryItemId] || 0)
        return { inventoryItemId: l.inventoryItemId, receivedQty: supplierQty * cf }
      })
      const updated = await api.bookGoodsReceipt(order.id, { lines })
      onBooked(updated)
    } catch (e) {
      setErr(e.message)
      setSubmitting(false)
    }
  }

  return (
    <div style={styles.overlay}>
      <div style={styles.modal}>
        <h3 style={{ fontSize: 17, fontWeight: 700, marginBottom: 16 }}>Book Goods Receipt — Order #{order.id}</h3>
        {err && <div style={styles.alert('error')}>{err}</div>}
        <table style={styles.table}>
          <thead>
            <tr>
              <th style={styles.th}>Item</th>
              <th style={styles.th}>Ordered</th>
              <th style={styles.th}>Received</th>
            </tr>
          </thead>
          <tbody>
            {order.lines.map(l => {
              const cf = l.conversionFactor && l.conversionFactor > 1 ? l.conversionFactor : null
              const supplierQty = parseFloat(quantities[l.inventoryItemId] || 0)
              const inventoryQty = cf ? supplierQty * cf : supplierQty
              return (
                <tr key={l.inventoryItemId}>
                  <td style={styles.td(false)}>
                    <div style={{ fontWeight: 600 }}>{l.inventoryItemName}</div>
                    {l.supplierSku && <div style={{ fontSize: 12, color: '#888' }}>SKU: {l.supplierSku}</div>}
                  </td>
                  <td style={styles.td(false)}>
                    {l.requestedQty} {l.inventoryItemUnit}
                    {cf && <div style={{ fontSize: 12, color: '#888' }}>≈ {Math.ceil(l.requestedQty / cf)} {l.packageUnit}</div>}
                  </td>
                  <td style={styles.td(false)}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                      <input
                        type="number"
                        min="0"
                        step="1"
                        style={{ ...styles.input, width: 80 }}
                        value={quantities[l.inventoryItemId] ?? ''}
                        onChange={e => setQuantities(prev => ({ ...prev, [l.inventoryItemId]: e.target.value }))}
                      />
                      <span style={{ fontSize: 13, color: '#555' }}>{cf ? l.packageUnit : l.inventoryItemUnit}</span>
                    </div>
                    {cf && <div style={{ fontSize: 12, color: '#888', marginTop: 3 }}>= {inventoryQty.toFixed(0)} {l.inventoryItemUnit}</div>}
                  </td>
                </tr>
              )
            })}
          </tbody>
        </table>
        <div style={{ marginTop: 20, display: 'flex', gap: 8 }}>
          <button style={styles.btn('primary')} onClick={confirm} disabled={submitting}>
            {submitting ? 'Confirming...' : 'Confirm Receipt'}
          </button>
          <button style={styles.btn('secondary')} onClick={onClose} disabled={submitting}>Cancel</button>
        </div>
      </div>
    </div>
  )
}

function OrderHistorySubTab() {
  const [orders, setOrders] = useState([])
  const [loading, setLoading] = useState(true)
  const [message, setMessage] = useState(null)
  const [receiptDialog, setReceiptDialog] = useState(null)
  const [expandedId, setExpandedId] = useState(null)

  useEffect(() => {
    api.getAllOrders()
      .then(setOrders)
      .catch(e => setMessage({ type: 'error', text: e.message }))
      .finally(() => setLoading(false))
  }, [])

  const statusBadge = (status) => {
    const colors = {
      DRAFT:     { bg: '#e9ecef', color: '#555' },
      SUBMITTED: { bg: '#e8f0ff', color: '#3b5bdb' },
      CONFIRMED: { bg: '#e8f0ff', color: '#3b5bdb' },
      CANCELLED: { bg: '#ffe0e0', color: '#c92a2a' },
      RECEIVED:  { bg: '#e6f4ea', color: '#2b8a3e' },
    }
    const c = colors[status] || { bg: '#e9ecef', color: '#555' }
    return (
      <span style={{ ...c, padding: '2px 10px', borderRadius: 12, fontSize: 12, fontWeight: 600, display: 'inline-block' }}>
        {status}
      </span>
    )
  }

  if (loading) return <div>Loading orders...</div>

  return (
    <div>
      {message && <div style={styles.alert(message.type)}>{message.text}</div>}
      <h2 style={{ fontSize: 18, fontWeight: 600, marginBottom: 16 }}>Order History</h2>
      <div style={styles.card}>
        <table style={styles.table}>
          <thead>
            <tr>
              <th style={styles.th}></th>
              <th style={styles.th}>Order #</th>
              <th style={styles.th}>Date</th>
              <th style={styles.th}>Status</th>
              <th style={styles.th}>Items</th>
              <th style={styles.th}>Action</th>
            </tr>
          </thead>
          <tbody>
            {orders.map(order => {
              const expanded = expandedId === order.id
              return (
                <React.Fragment key={order.id}>
                  <tr
                    style={{ cursor: 'pointer' }}
                    onClick={() => setExpandedId(expanded ? null : order.id)}
                  >
                    <td style={styles.td(false)}>
                      <span style={{ fontSize: 11, color: '#888' }}>{expanded ? '▼' : '▶'}</span>
                    </td>
                    <td style={styles.td(false)}>#{order.id}</td>
                    <td style={styles.td(false)}>{new Date(order.createdAt).toLocaleDateString()}</td>
                    <td style={styles.td(false)}>{statusBadge(order.status)}</td>
                    <td style={styles.td(false)}>{order.lines?.length ?? 0}</td>
                    <td style={styles.td(false)} onClick={e => e.stopPropagation()}>
                      {order.status !== 'RECEIVED'
                        ? <button style={styles.btn('primary')} onClick={() => setReceiptDialog(order)}>📦 Book Goods Receipt</button>
                        : <span style={{ color: '#2b8a3e', fontSize: 13 }}>✓ Received{order.receivedAt ? ` on ${new Date(order.receivedAt).toLocaleDateString()}` : ''}</span>
                      }
                    </td>
                  </tr>
                  {expanded && (
                    <tr>
                      <td colSpan={6} style={{ padding: 0, background: '#f8f9ff' }}>
                        <table style={{ ...styles.table, margin: '0 0 0 32px', width: 'calc(100% - 32px)' }}>
                          <thead>
                            <tr>
                              <th style={{ ...styles.th, background: '#eef0fb' }}>Item</th>
                              <th style={{ ...styles.th, background: '#eef0fb' }}>SKU</th>
                              <th style={{ ...styles.th, background: '#eef0fb' }}>Ordered</th>
                              <th style={{ ...styles.th, background: '#eef0fb' }}>Received</th>
                            </tr>
                          </thead>
                          <tbody>
                            {order.lines.map(l => {
                              const cf = l.conversionFactor && l.conversionFactor > 1 ? l.conversionFactor : null
                              return (
                                <tr key={l.id}>
                                  <td style={styles.td(false)}>{l.inventoryItemName}</td>
                                  <td style={{ ...styles.td(false), color: '#888', fontSize: 13 }}>{l.supplierSku ?? '—'}</td>
                                  <td style={styles.td(false)}>
                                    {l.requestedQty} {l.inventoryItemUnit}
                                    {cf && <span style={{ color: '#888', fontSize: 12 }}> ({Math.ceil(l.requestedQty / cf)} {l.packageUnit})</span>}
                                  </td>
                                  <td style={styles.td(false)}>
                                    {l.receivedQty != null
                                      ? <>{l.receivedQty} {l.inventoryItemUnit}{cf && <span style={{ color: '#888', fontSize: 12 }}> ({(l.receivedQty / cf).toFixed(1)} {l.packageUnit})</span>}</>
                                      : <span style={{ color: '#aaa' }}>—</span>}
                                  </td>
                                </tr>
                              )
                            })}
                          </tbody>
                        </table>
                      </td>
                    </tr>
                  )}
                </React.Fragment>
              )
            })}
          </tbody>
        </table>
      </div>
      {receiptDialog && (
        <GoodsReceiptDialog
          order={receiptDialog}
          onClose={() => setReceiptDialog(null)}
          onBooked={(updated) => {
            setOrders(prev => prev.map(o => o.id === updated.id ? updated : o))
            setReceiptDialog(null)
          }}
        />
      )}
    </div>
  )
}

// ─── Inventory sub-tabs: Items ────────────────────────────────────────────────
function EditInventoryItemModal({ item, onClose, onSaved }) {
  const [name, setName] = useState(item.name)
  const [unit, setUnit] = useState(item.unit || '')
  const [minStockLevel, setMinStockLevel] = useState(String(item.minStockLevel ?? ''))
  const [reorderTarget, setReorderTarget] = useState(String(item.reorderTarget ?? ''))
  const [saving, setSaving] = useState(false)
  const [err, setErr] = useState(null)

  const save = async () => {
    setSaving(true)
    setErr(null)
    try {
      const updated = await api.updateInventoryItem(item.id, {
        name, unit,
        minStockLevel: parseFloat(minStockLevel) || 0,
        reorderTarget: parseFloat(reorderTarget) || 0,
      })
      onSaved(updated)
    } catch (e) {
      setErr(e.message)
      setSaving(false)
    }
  }

  return (
    <div style={styles.overlay}>
      <div style={styles.modal}>
        <h3 style={{ fontSize: 17, fontWeight: 700, marginBottom: 16 }}>Edit Item</h3>
        {err && <div style={styles.alert('error')}>{err}</div>}
        <div style={styles.formRow}>
          <label style={styles.label}>Name</label>
          <input style={styles.input} value={name} onChange={e => setName(e.target.value)} />
        </div>
        <div style={styles.formRow}>
          <label style={styles.label}>Unit</label>
          <input style={styles.input} value={unit} onChange={e => setUnit(e.target.value)} />
        </div>
        <div style={styles.formRow}>
          <label style={styles.label}>Min Stock Level</label>
          <input type="number" style={styles.input} value={minStockLevel} onChange={e => setMinStockLevel(e.target.value)} />
        </div>
        <div style={styles.formRow}>
          <label style={styles.label}>Reorder Target</label>
          <input type="number" style={styles.input} value={reorderTarget} onChange={e => setReorderTarget(e.target.value)} />
        </div>
        <div style={{ marginTop: 20, display: 'flex', gap: 8 }}>
          <button style={styles.btn('primary')} onClick={save} disabled={saving}>
            {saving ? 'Saving...' : 'Save'}
          </button>
          <button style={styles.btn('secondary')} onClick={onClose} disabled={saving}>Cancel</button>
        </div>
      </div>
    </div>
  )
}

function InventoryItemsSubTab() {
  const [items, setItems] = useState([])
  const [loading, setLoading] = useState(true)
  const [editModal, setEditModal] = useState(null)

  useEffect(() => {
    api.getInventory()
      .then(setItems)
      .finally(() => setLoading(false))
  }, [])

  if (loading) return <div>Loading items...</div>

  return (
    <div>
      <h2 style={{ fontSize: 18, fontWeight: 600, marginBottom: 16 }}>Inventory Items</h2>
      <div style={styles.card}>
        <table style={styles.table}>
          <thead>
            <tr>
              <th style={styles.th}>Name</th>
              <th style={styles.th}>Unit</th>
              <th style={styles.th}>Min Stock</th>
              <th style={styles.th}>Reorder Target</th>
              <th style={styles.th}>Current Stock</th>
              <th style={styles.th}>Supplier</th>
              <th style={styles.th}>Actions</th>
            </tr>
          </thead>
          <tbody>
            {items.map(item => (
              <tr key={item.id}>
                <td style={styles.td(false)}>{item.name}</td>
                <td style={styles.td(false)}>{item.unit}</td>
                <td style={styles.td(false)}>{item.minStockLevel?.toFixed(2)}</td>
                <td style={styles.td(false)}>{item.reorderTarget?.toFixed(2)}</td>
                <td style={styles.td(false)}>{item.cachedStock?.toFixed(2)}</td>
                <td style={styles.td(false)}>
                  {item.hasPreferredOffer
                    ? <span style={{ color: '#2b8a3e', fontWeight: 600 }}>✓ linked</span>
                    : <span style={{ color: '#e67700', fontWeight: 600 }}>⚠️ no supplier</span>}
                </td>
                <td style={styles.td(false)}>
                  <button style={styles.btn('secondary')} onClick={() => setEditModal(item)}>Edit</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {editModal && (
        <EditInventoryItemModal
          item={editModal}
          onClose={() => setEditModal(null)}
          onSaved={(updated) => {
            setItems(prev => prev.map(i => i.id === updated.id ? updated : i))
            setEditModal(null)
          }}
        />
      )}
    </div>
  )
}

// ─── InventoryTab wrapper ─────────────────────────────────────────────────────
function InventoryTab() {
  const [subTab, setSubTab] = useState('order')
  return (
    <div>
      <div style={styles.subTabs}>
        <button style={styles.tab(subTab === 'order')}   onClick={() => setSubTab('order')}>Inventory</button>
        <button style={styles.tab(subTab === 'history')} onClick={() => setSubTab('history')}>Orders</button>
        <button style={styles.tab(subTab === 'items')}   onClick={() => setSubTab('items')}>Items</button>
      </div>
      {subTab === 'order'   && <OrderTab />}
      {subTab === 'history' && <OrderHistorySubTab />}
      {subTab === 'items'   && <InventoryItemsSubTab />}
    </div>
  )
}

// ─── Tab 2: Supplier Config ───────────────────────────────────────────────────
function SupplierConfigTab() {
  const [suppliers, setSuppliers] = useState([])
  const [configs, setConfigs] = useState({})
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState({})
  const [messages, setMessages] = useState({})

  useEffect(() => {
    const load = async () => {
      try {
        const sups = await api.getSuppliers()
        setSuppliers(sups)
        const cfgs = {}
        await Promise.all(sups.map(async (s) => {
          try {
            const cfg = await api.getConnectorConfig(s.id)
            if (cfg) {
              let parsed = {}
              try { parsed = JSON.parse(cfg.configPayload || '{}') } catch {}
              cfgs[s.id] = {
                connectorTypeName: cfg.connectorTypeName || 'EMAIL',
                configPayload: cfg.configPayload || '{}',
                isActive: cfg.isActive,
                recipientEmail: parsed.recipientEmail || '',
                subjectTemplate: parsed.subjectTemplate || '',
                bodyTemplate: parsed.bodyTemplate || '',
                recipientPhone: parsed.recipientPhone || '',
                messageTemplate: parsed.messageTemplate || '',
              }
            } else {
              cfgs[s.id] = {
                connectorTypeName: 'EMAIL',
                configPayload: '{}',
                isActive: true,
                recipientEmail: '',
                subjectTemplate: '',
                bodyTemplate: '',
                recipientPhone: '',
                messageTemplate: '',
              }
            }
          } catch {
            cfgs[s.id] = {
              connectorTypeName: 'EMAIL',
              configPayload: '{}',
              isActive: true,
              recipientEmail: '',
              subjectTemplate: '',
              bodyTemplate: '',
              recipientPhone: '',
              messageTemplate: '',
            }
          }
        }))
        setConfigs(cfgs)
      } catch (e) {
        console.error(e)
      } finally {
        setLoading(false)
      }
    }
    load()
  }, [])

  const updateField = (supplierId, field, value) => {
    setConfigs(prev => ({
      ...prev,
      [supplierId]: { ...prev[supplierId], [field]: value }
    }))
  }

  const save = async (supplierId) => {
    setSaving(prev => ({ ...prev, [supplierId]: true }))
    setMessages(prev => ({ ...prev, [supplierId]: null }))
    try {
      const cfg = configs[supplierId]
      const payloadObj = cfg.connectorTypeName === 'WHATSAPP'
        ? { recipientPhone: cfg.recipientPhone, messageTemplate: cfg.messageTemplate }
        : { recipientEmail: cfg.recipientEmail, subjectTemplate: cfg.subjectTemplate, bodyTemplate: cfg.bodyTemplate }
      const payload = JSON.stringify(payloadObj)
      await api.updateConnectorConfig(supplierId, {
        connectorTypeName: cfg.connectorTypeName,
        configPayload: payload,
        isActive: cfg.isActive,
      })
      setMessages(prev => ({ ...prev, [supplierId]: { type: 'success', text: 'Saved!' } }))
    } catch (e) {
      setMessages(prev => ({ ...prev, [supplierId]: { type: 'error', text: e.message } }))
    } finally {
      setSaving(prev => ({ ...prev, [supplierId]: false }))
    }
  }

  if (loading) return <div>Loading suppliers...</div>

  return (
    <div>
      <h2 style={{ fontSize: 18, fontWeight: 600, marginBottom: 16 }}>Supplier Connector Configuration</h2>
      {suppliers.map(supplier => {
        const cfg = configs[supplier.id] || {}
        const msg = messages[supplier.id]
        return (
          <div key={supplier.id} style={styles.card}>
            <h3 style={{ fontSize: 16, fontWeight: 600, marginBottom: 4 }}>{supplier.name}</h3>
            <p style={{ color: '#888', fontSize: 13, marginBottom: 16 }}>{supplier.contactEmail}</p>
            {msg && <div style={styles.alert(msg.type)}>{msg.text}</div>}
            <div style={styles.formRow}>
              <label style={styles.label}>Connector Type</label>
              <select
                style={{ ...styles.input, width: 180 }}
                value={cfg.connectorTypeName || 'EMAIL'}
                onChange={e => updateField(supplier.id, 'connectorTypeName', e.target.value)}
              >
                <option value="EMAIL">EMAIL</option>
                <option value="WHATSAPP">WHATSAPP</option>
                <option value="EDI">EDI</option>
              </select>
            </div>
            {cfg.connectorTypeName === 'WHATSAPP' ? (
              <>
                <div style={styles.formRow}>
                  <label style={styles.label}>Recipient Phone</label>
                  <input
                    type="text"
                    style={styles.input}
                    value={cfg.recipientPhone || ''}
                    placeholder="4366000000001"
                    onChange={e => updateField(supplier.id, 'recipientPhone', e.target.value)}
                  />
                </div>
                <div style={styles.formRow}>
                  <label style={styles.label}>Message Template</label>
                  <textarea
                    style={{ ...styles.input, height: 100, resize: 'vertical' }}
                    value={cfg.messageTemplate || ''}
                    placeholder="Order {date} - {supplierName}:{'\n\n'}{orderLines}"
                    onChange={e => updateField(supplier.id, 'messageTemplate', e.target.value)}
                  />
                </div>
              </>
            ) : (
              <>
                <div style={styles.formRow}>
                  <label style={styles.label}>Recipient Email</label>
                  <input
                    type="email"
                    style={styles.input}
                    value={cfg.recipientEmail || ''}
                    onChange={e => updateField(supplier.id, 'recipientEmail', e.target.value)}
                  />
                </div>
                <div style={styles.formRow}>
                  <label style={styles.label}>Subject Template</label>
                  <input
                    type="text"
                    style={styles.input}
                    value={cfg.subjectTemplate || ''}
                    placeholder="Order for {supplierName} on {date}"
                    onChange={e => updateField(supplier.id, 'subjectTemplate', e.target.value)}
                  />
                </div>
                <div style={styles.formRow}>
                  <label style={styles.label}>Body Template</label>
                  <textarea
                    style={{ ...styles.input, height: 100, resize: 'vertical' }}
                    value={cfg.bodyTemplate || ''}
                    placeholder="Dear {supplierName},&#10;&#10;{orderLines}"
                    onChange={e => updateField(supplier.id, 'bodyTemplate', e.target.value)}
                  />
                </div>
              </>
            )}
            <button
              style={styles.btn('primary')}
              onClick={() => save(supplier.id)}
              disabled={saving[supplier.id]}
            >
              {saving[supplier.id] ? 'Saving...' : 'Save'}
            </button>
          </div>
        )
      })}
    </div>
  )
}

// ─── Tab 3: SMTP Settings ─────────────────────────────────────────────────────
function SmtpSettingsTab() {
  const [settings, setSettings] = useState({
    host: '', port: 587, username: '', password: '', from: ''
  })
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [message, setMessage] = useState(null)

  // WPPConnect state
  const [wpp, setWpp] = useState({ baseUrl: '', secretKey: '', session: '', tokenConfigured: false })
  const [wppLoading, setWppLoading] = useState(true)
  const [wppSaving, setWppSaving] = useState(false)
  const [wppMessage, setWppMessage] = useState(null)
  const [wppQrCode, setWppQrCode] = useState(null)
  const [wppGenerating, setWppGenerating] = useState(false)
  const [wppPolling, setWppPolling] = useState(false)

  useEffect(() => {
    api.getSmtpSettings()
      .then(data => setSettings({ ...data, password: '' }))
      .catch(e => setMessage({ type: 'error', text: e.message }))
      .finally(() => setLoading(false))

    api.getWppConnectSettings()
      .then(data => setWpp(data))
      .catch(e => setWppMessage({ type: 'error', text: e.message }))
      .finally(() => setWppLoading(false))
  }, [])

  const handleChange = (field, value) => {
    setSettings(prev => ({ ...prev, [field]: value }))
  }

  const handleSave = async () => {
    setSaving(true)
    setMessage(null)
    try {
      await api.updateSmtpSettings(settings)
      setMessage({ type: 'success', text: 'SMTP settings saved successfully!' })
      setSettings(prev => ({ ...prev, password: '' }))
    } catch (e) {
      setMessage({ type: 'error', text: 'Failed to save: ' + e.message })
    } finally {
      setSaving(false)
    }
  }

  const handleWppChange = (field, value) => {
    setWpp(prev => ({ ...prev, [field]: value }))
  }

  const handleWppSave = async () => {
    setWppSaving(true)
    setWppMessage(null)
    try {
      await api.updateWppConnectSettings({
        baseUrl: wpp.baseUrl,
        secretKey: wpp.secretKey,
        session: wpp.session,
      })
      setWppMessage({ type: 'success', text: 'WPPConnect settings saved!' })
    } catch (e) {
      setWppMessage({ type: 'error', text: 'Failed to save: ' + e.message })
    } finally {
      setWppSaving(false)
    }
  }

  const handleGenerateToken = async () => {
    setWppGenerating(true)
    setWppMessage(null)
    try {
      await api.generateWppConnectToken()
      setWppMessage({ type: 'success', text: 'Token generated successfully!' })
      setWpp(prev => ({ ...prev, tokenConfigured: true }))
    } catch (e) {
      setWppMessage({ type: 'error', text: 'Token generation failed: ' + e.message })
    } finally {
      setWppGenerating(false)
    }
  }

  // Poll for QR code every 3 seconds while wppPolling=true
  useEffect(() => {
    if (!wppPolling) return
    let cancelled = false
    const poll = async () => {
      try {
        const data = await api.getWppConnectQrCode()
        if (cancelled) return
        if (data.qrcode) {
          setWppQrCode(data.qrcode)
          setWppPolling(false)
          setWppMessage({ type: 'success', text: 'QR code ready — scan with WhatsApp!' })
        }
      } catch (e) {
        if (!cancelled) setWppMessage({ type: 'error', text: 'Failed to load QR code: ' + e.message })
      }
    }
    poll()
    const interval = setInterval(poll, 3000)
    return () => { cancelled = true; clearInterval(interval) }
  }, [wppPolling])

  const handleShowQrCode = () => {
    setWppQrCode(null)
    setWppMessage({ type: 'success', text: 'Starting WhatsApp session… this may take up to 40 seconds.' })
    setWppPolling(true)
  }

  const handleStopQrPolling = () => {
    setWppPolling(false)
    setWppMessage(null)
  }

  if (loading) return <div>Loading SMTP settings...</div>

  return (
    <div>
      <h2 style={{ fontSize: 18, fontWeight: 600, marginBottom: 16 }}>SMTP Settings</h2>
      <div style={{ ...styles.card, maxWidth: 500 }}>
        {message && <div style={styles.alert(message.type)}>{message.text}</div>}
        <div style={styles.formRow}>
          <label style={styles.label}>SMTP Host</label>
          <input
            type="text"
            style={styles.input}
            value={settings.host}
            placeholder="smtp.example.com"
            onChange={e => handleChange('host', e.target.value)}
          />
        </div>
        <div style={styles.formRow}>
          <label style={styles.label}>Port</label>
          <input
            type="number"
            style={{ ...styles.input, width: 120 }}
            value={settings.port}
            onChange={e => handleChange('port', parseInt(e.target.value))}
          />
        </div>
        <div style={styles.formRow}>
          <label style={styles.label}>Username</label>
          <input
            type="text"
            style={styles.input}
            value={settings.username}
            onChange={e => handleChange('username', e.target.value)}
          />
        </div>
        <div style={styles.formRow}>
          <label style={styles.label}>Password</label>
          <input
            type="password"
            style={styles.input}
            value={settings.password}
            placeholder="Leave blank to keep current"
            onChange={e => handleChange('password', e.target.value)}
          />
        </div>
        <div style={styles.formRow}>
          <label style={styles.label}>From Address</label>
          <input
            type="email"
            style={styles.input}
            value={settings.from}
            placeholder="orders@yourcompany.com"
            onChange={e => handleChange('from', e.target.value)}
          />
        </div>
        <button style={styles.btn('primary')} onClick={handleSave} disabled={saving}>
          {saving ? 'Saving...' : 'Save Settings'}
        </button>
      </div>

      <h2 style={{ fontSize: 18, fontWeight: 600, marginBottom: 16, marginTop: 32 }}>WhatsApp (WPPConnect)</h2>
      <div style={{ ...styles.card, maxWidth: 500 }}>
        {wppLoading ? <div>Loading WPPConnect settings...</div> : (
          <>
            {wppMessage && <div style={styles.alert(wppMessage.type)}>{wppMessage.text}</div>}
            <div style={styles.formRow}>
              <label style={styles.label}>WPPConnect Base URL</label>
              <input
                type="text"
                style={styles.input}
                value={wpp.baseUrl || ''}
                placeholder="http://wppconnect:21465"
                onChange={e => handleWppChange('baseUrl', e.target.value)}
              />
            </div>
            <div style={styles.formRow}>
              <label style={styles.label}>Secret Key</label>
              <input
                type="password"
                style={styles.input}
                value={wpp.secretKey || ''}
                placeholder="Enter secret key"
                onChange={e => handleWppChange('secretKey', e.target.value)}
              />
            </div>
            <div style={styles.formRow}>
              <label style={styles.label}>Session Name</label>
              <input
                type="text"
                style={styles.input}
                value={wpp.session || ''}
                placeholder="inventory-session"
                onChange={e => handleWppChange('session', e.target.value)}
              />
            </div>
            <div style={{ marginBottom: 14 }}>
              <span style={{ fontSize: 13, color: '#555' }}>
                Token status: {wpp.tokenConfigured ? '✓ Configured' : '✗ Not configured'}
              </span>
            </div>
            <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
              <button style={styles.btn('primary')} onClick={handleWppSave} disabled={wppSaving}>
                {wppSaving ? 'Saving...' : 'Save Settings'}
              </button>
              <button style={styles.btn('secondary')} onClick={handleGenerateToken} disabled={wppGenerating}>
                {wppGenerating ? 'Generating...' : 'Generate Token'}
              </button>
              {wppPolling
                ? <button style={styles.btn('danger')} onClick={handleStopQrPolling}>Stop Polling</button>
                : <button style={styles.btn('secondary')} onClick={handleShowQrCode}>Show QR Code</button>
              }
            </div>
            {wppQrCode && (
              <div style={{ marginTop: 16 }}>
                <img
                  src={wppQrCode.startsWith('data:') ? wppQrCode : `data:image/png;base64,${wppQrCode}`}
                  alt="WPPConnect QR Code"
                  style={{ maxWidth: 256, border: '1px solid #e0e0e0', borderRadius: 8 }}
                />
              </div>
            )}
          </>
        )}
      </div>

      <h2 style={{ fontSize: 18, fontWeight: 600, marginBottom: 16, marginTop: 32 }}>POS Connector (ready2order)</h2>
      <PosConnectorSettings />
    </div>
  )
}

// ─── Tab 4: Supplier Catalog ──────────────────────────────────────────────────

// --- OfferModal ---
function OfferModal({ offer, supplierId, onClose, onSaved }) {
  const [form, setForm] = useState(offer
    ? { supplierSku: offer.supplierSku || '', supplierProductName: offer.supplierProductName || '',
        packageUnit: offer.packageUnit || '', conversionFactor: offer.conversionFactor ?? 1, unitPrice: offer.unitPrice ?? '' }
    : { supplierSku: '', supplierProductName: '', packageUnit: '', conversionFactor: 1, unitPrice: '' }
  )
  const [saving, setSaving] = useState(false)
  const [err, setErr] = useState(null)

  const handleSave = async () => {
    setSaving(true)
    setErr(null)
    try {
      const data = { ...form, supplierId, conversionFactor: parseFloat(form.conversionFactor), unitPrice: parseFloat(form.unitPrice) }
      if (offer) {
        await api.updateSupplierOffer(offer.id, data)
      } else {
        await api.createSupplierOffer(data)
      }
      onSaved()
    } catch (e) {
      setErr(e.message)
    } finally {
      setSaving(false)
    }
  }

  return (
    <div style={styles.overlay}>
      <div style={styles.modal}>
        <h3 style={{ fontSize: 17, fontWeight: 700, marginBottom: 18 }}>{offer ? 'Edit Offer' : 'New Offer'}</h3>
        {err && <div style={styles.alert('error')}>{err}</div>}
        <div style={styles.formRow}>
          <label style={styles.label}>SKU</label>
          <input style={styles.input} value={form.supplierSku} onChange={e => setForm(p => ({ ...p, supplierSku: e.target.value }))} />
        </div>
        <div style={styles.formRow}>
          <label style={styles.label}>Product Name</label>
          <input style={styles.input} value={form.supplierProductName} onChange={e => setForm(p => ({ ...p, supplierProductName: e.target.value }))} />
        </div>
        <div style={styles.formRow}>
          <label style={styles.label}>Package Unit</label>
          <input style={styles.input} value={form.packageUnit} onChange={e => setForm(p => ({ ...p, packageUnit: e.target.value }))} />
        </div>
        <div style={styles.formRow}>
          <label style={styles.label}>Conversion Factor</label>
          <input type="number" step="0.001" style={styles.input} value={form.conversionFactor}
            onChange={e => setForm(p => ({ ...p, conversionFactor: e.target.value }))} />
          <div style={{ fontSize: 12, color: '#888', marginTop: 4 }}>
            1 {form.packageUnit || 'unit'} = {form.conversionFactor} × canonical unit
          </div>
        </div>
        <div style={styles.formRow}>
          <label style={styles.label}>Price</label>
          <input type="number" step="0.01" style={styles.input} value={form.unitPrice}
            onChange={e => setForm(p => ({ ...p, unitPrice: e.target.value }))} />
        </div>
        <div style={{ display: 'flex', gap: 8, marginTop: 8 }}>
          <button style={styles.btn('primary')} onClick={handleSave} disabled={saving}>
            {saving ? 'Saving...' : 'Save'}
          </button>
          <button style={styles.btn('secondary')} onClick={onClose}>Cancel</button>
        </div>
      </div>
    </div>
  )
}

// --- LinkDialog ---
function LinkDialog({ offer, inventoryItems, onClose, onLinked, onItemCreated }) {
  const [filter, setFilter] = useState('')
  const [selectedItem, setSelectedItem] = useState(null)
  const [isPreferred, setIsPreferred] = useState(false)
  const [saving, setSaving] = useState(false)
  const [err, setErr] = useState(null)
  const [showCreateForm, setShowCreateForm] = useState(false)
  const [newName, setNewName]               = useState('')
  const [newUnit, setNewUnit]               = useState('')
  const [newMinStock, setNewMinStock]       = useState(0)
  const [newReorderTarget, setNewReorderTarget] = useState(0)
  const [creating, setCreating]             = useState(false)
  const [createErr, setCreateErr]           = useState(null)

  const filtered = inventoryItems.filter(it =>
    it.name.toLowerCase().includes(filter.toLowerCase())
  )

  const showWarning = selectedItem
    && offer.conversionFactor === 1.0
    && offer.packageUnit
    && offer.packageUnit.toLowerCase() !== (selectedItem.unit || '').toLowerCase()

  const handleLink = async () => {
    if (!selectedItem) return
    setSaving(true)
    setErr(null)
    try {
      const res = await api.linkSupplierOfferItem(offer.id, { inventoryItemId: selectedItem.id, isPreferred })
      onLinked(res.warning)
    } catch (e) {
      setErr(e.message)
      setSaving(false)
    }
  }

  const handleShowCreate = () => {
    setNewName(offer.supplierProductName || '')
    setNewUnit('')
    setNewMinStock(0)
    setNewReorderTarget(0)
    setCreateErr(null)
    setShowCreateForm(true)
  }

  const handleCreateAndSelect = async () => {
    if (!newUnit.trim()) { setCreateErr('Canonical unit is required'); return }
    setCreating(true)
    setCreateErr(null)
    try {
      const created = await api.createInventoryItem({
        name: newName,
        unit: newUnit,
        minStockLevel: newMinStock || 0,
        reorderTarget: newReorderTarget || 0,
      })
      onItemCreated(created)
      setSelectedItem(created)
      setShowCreateForm(false)
    } catch (e) {
      setCreateErr(e.message)
    } finally {
      setCreating(false)
    }
  }

  return (
    <div style={styles.overlay}>
      <div style={{ ...styles.modal, width: 520 }}>
        <h3 style={{ fontSize: 17, fontWeight: 700, marginBottom: 4 }}>Link: {offer.supplierProductName || offer.supplierSku}</h3>
        <p style={{ fontSize: 13, color: '#888', marginBottom: 14 }}>Select an inventory item to link this offer to</p>
        {err && <div style={styles.alert('error')}>{err}</div>}
        <input
          style={{ ...styles.input, marginBottom: 10 }}
          placeholder="Filter items..."
          value={filter}
          onChange={e => setFilter(e.target.value)}
        />
        <div style={{ maxHeight: 220, overflowY: 'auto', border: '1px solid #e0e0e0', borderRadius: 6, marginBottom: 12 }}>
          {filtered.map(it => (
            <div key={it.id} style={styles.listItem(selectedItem?.id === it.id)}
              onClick={() => setSelectedItem(it)}>
              {it.name} <span style={{ color: '#888', fontSize: 12 }}>({it.unit})</span>
            </div>
          ))}
        </div>
        {/* Create new item */}
        {!showCreateForm && (
          <button style={{ ...styles.btn('secondary'), marginBottom: 12, fontSize: 13 }}
            onClick={handleShowCreate}>
            ＋ Create new item
          </button>
        )}
        {showCreateForm && (
          <div style={{ border: '1px solid #d0e4ff', borderRadius: 6, padding: 12, marginBottom: 12, background: '#f8faff' }}>
            <div style={{ fontWeight: 600, fontSize: 13, marginBottom: 8 }}>New inventory item</div>
            {createErr && <div style={styles.alert('error')}>{createErr}</div>}
            <input style={{ ...styles.input, marginBottom: 6 }} placeholder="Name *"
              value={newName} onChange={e => setNewName(e.target.value)} />
            <input style={{ ...styles.input, marginBottom: 6 }} placeholder="Canonical unit *"
              value={newUnit} onChange={e => setNewUnit(e.target.value)} />
            <input type="number" style={{ ...styles.input, marginBottom: 6 }} placeholder="Min stock level (default 0)"
              value={newMinStock} onChange={e => setNewMinStock(e.target.value)} />
            <input type="number" style={{ ...styles.input, marginBottom: 8 }} placeholder="Reorder target (default 0)"
              value={newReorderTarget} onChange={e => setNewReorderTarget(e.target.value)} />
            <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
              <button style={styles.btn('primary')} onClick={handleCreateAndSelect} disabled={creating}>
                {creating ? 'Creating...' : 'Create & Select'}
              </button>
              <span style={{ fontSize: 13, color: '#888', cursor: 'pointer' }}
                onClick={() => setShowCreateForm(false)}>Cancel</span>
            </div>
          </div>
        )}
        {selectedItem && (
          <div style={{ fontSize: 13, color: '#555', marginBottom: 10 }}>
            Preview: 1 {offer.packageUnit || 'package'} = {offer.conversionFactor} × {selectedItem.unit}
          </div>
        )}
        {showWarning && (
          <div style={styles.warningBox}>
            Warning: Conversion factor is 1.0 but supplier unit '{offer.packageUnit}' ≠ canonical unit '{selectedItem.unit}'. Please check!
          </div>
        )}
        <div style={{ marginBottom: 14 }}>
          <label style={{ fontSize: 14, cursor: 'pointer' }}>
            <input type="checkbox" checked={isPreferred} onChange={e => setIsPreferred(e.target.checked)} style={{ marginRight: 8 }} />
            Set as preferred supplier
          </label>
        </div>
        <div style={{ display: 'flex', gap: 8 }}>
          <button style={styles.btn('primary')} onClick={handleLink} disabled={saving || !selectedItem}>
            {saving ? 'Linking...' : 'Link'}
          </button>
          <button style={styles.btn('secondary')} onClick={onClose}>Cancel</button>
        </div>
      </div>
    </div>
  )
}

// --- AddComponentModal ---
function AddComponentModal({ salesProduct, inventoryItems, onClose, onAdded }) {
  const [selectedItemId, setSelectedItemId] = useState('')
  const [qty, setQty] = useState('')
  const [saving, setSaving] = useState(false)
  const [err, setErr] = useState(null)

  const selectedItem = inventoryItems.find(it => it.id === parseInt(selectedItemId))

  const handleAdd = async () => {
    if (!selectedItemId || !qty) return
    setSaving(true)
    setErr(null)
    try {
      await api.addItemComponent(parseInt(selectedItemId), { salesProductId: salesProduct.id, qtyRequired: parseFloat(qty) })
      onAdded()
    } catch (e) {
      if (e.message.startsWith('409')) {
        setErr('This mapping already exists')
      } else {
        setErr(e.message)
      }
      setSaving(false)
    }
  }

  return (
    <div style={styles.overlay}>
      <div style={styles.modal}>
        <h3 style={{ fontSize: 17, fontWeight: 700, marginBottom: 18 }}>Add Ingredient</h3>
        {err && <div style={styles.alert('error')}>{err}</div>}
        <div style={styles.formRow}>
          <label style={styles.label}>Inventory Item</label>
          <select style={styles.input} value={selectedItemId} onChange={e => setSelectedItemId(e.target.value)}>
            <option value="">— select item —</option>
            {inventoryItems.map(it => (
              <option key={it.id} value={it.id}>{it.name} ({it.unit})</option>
            ))}
          </select>
        </div>
        <div style={styles.formRow}>
          <label style={styles.label}>
            Qty per sold '{salesProduct.name}' in {selectedItem?.unit ?? '…'}
          </label>
          <input type="number" step="0.001" style={styles.input} value={qty}
            onChange={e => setQty(e.target.value)} />
        </div>
        <div style={{ display: 'flex', gap: 8 }}>
          <button style={styles.btn('primary')} onClick={handleAdd} disabled={saving || !selectedItemId || !qty}>
            {saving ? 'Adding...' : 'Add'}
          </button>
          <button style={styles.btn('secondary')} onClick={onClose}>Cancel</button>
        </div>
      </div>
    </div>
  )
}

function SupplierCatalogTab() {
  const [suppliers, setSuppliers] = useState([])
  const [selectedSupplierId, setSelectedSupplierId] = useState('')
  const [offers, setOffers] = useState([])
  const [inventoryItems, setInventoryItems] = useState([])
  const [loading, setLoading] = useState(false)
  const [message, setMessage] = useState(null)
  const [offerModal, setOfferModal] = useState(null) // null | { offer? }
  const [linkDialog, setLinkDialog] = useState(null) // null | offer
  const fileInputRef = React.useRef(null)

  useEffect(() => {
    api.getSuppliers().then(setSuppliers).catch(() => {})
    api.getInventory().then(setInventoryItems).catch(() => {})
  }, [])

  const loadOffers = useCallback(async (supplierId) => {
    if (!supplierId) return
    setLoading(true)
    try {
      const data = await api.getSupplierOffers(supplierId)
      setOffers(data)
    } catch (e) {
      setMessage({ type: 'error', text: 'Failed to load offers: ' + e.message })
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { loadOffers(selectedSupplierId) }, [selectedSupplierId, loadOffers])

  const handleSupplierChange = (e) => {
    setSelectedSupplierId(e.target.value)
    setOffers([])
  }

  const handleImportExcel = async (e) => {
    const file = e.target.files[0]
    if (!file || !selectedSupplierId) return
    e.target.value = ''
    setMessage(null)
    try {
      const result = await api.importSupplierOffersExcel(selectedSupplierId, file)
      setMessage({ type: 'success', text: `Import done: ${result.imported} imported, ${result.updated} updated, ${result.skipped} skipped` })
      await loadOffers(selectedSupplierId)
    } catch (e) {
      setMessage({ type: 'error', text: 'Import failed: ' + e.message })
    }
  }

  const handleDeleteOffer = async (id) => {
    if (!window.confirm('Delete this offer?')) return
    try {
      await api.deleteSupplierOffer(id)
      setOffers(prev => prev.filter(o => o.id !== id))
    } catch (e) {
      setMessage({ type: 'error', text: 'Delete failed: ' + e.message })
    }
  }

  return (
    <div>
      {message && <div style={styles.alert(message.type)}>{message.text}</div>}
      {(
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 16 }}>
            <select style={{ ...styles.input, width: 240 }} value={selectedSupplierId} onChange={handleSupplierChange}>
              <option value="">— select supplier —</option>
              {suppliers.map(s => <option key={s.id} value={s.id}>{s.name}</option>)}
            </select>
            <input ref={fileInputRef} type="file" accept=".xlsx" style={{ display: 'none' }} onChange={handleImportExcel} />
            <button style={styles.btn('secondary')} onClick={() => fileInputRef.current?.click()} disabled={!selectedSupplierId}>
              📤 Import Excel
            </button>
            <button style={styles.btn('primary')} onClick={() => setOfferModal({})} disabled={!selectedSupplierId}>
              + New Offer
            </button>
          </div>
          {loading ? <div>Loading offers...</div> : (
            <div style={styles.card}>
              <table style={styles.table}>
                <thead>
                  <tr>
                    <th style={styles.th}>SKU</th>
                    <th style={styles.th}>Product Name</th>
                    <th style={styles.th}>Unit</th>
                    <th style={styles.th}>Factor</th>
                    <th style={styles.th}>Price</th>
                    <th style={styles.th}>Linked Item</th>
                    <th style={styles.th}>Preferred</th>
                    <th style={styles.th}>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {offers.map(offer => (
                    <tr key={offer.id}>
                      <td style={styles.td(false)}>{offer.supplierSku}</td>
                      <td style={styles.td(false)}>{offer.supplierProductName}</td>
                      <td style={styles.td(false)}>{offer.packageUnit}</td>
                      <td style={styles.td(false)}>{offer.conversionFactor}</td>
                      <td style={styles.td(false)}>{offer.unitPrice != null ? offer.unitPrice.toFixed(2) : ''}</td>
                      <td style={styles.td(false)}>
                        <span
                          style={{ color: offer.inventoryItemName ? '#3b5bdb' : '#bbb', cursor: 'pointer', textDecoration: 'underline' }}
                          onClick={() => setLinkDialog(offer)}
                        >
                          {offer.inventoryItemName ?? '— not linked —'}
                        </span>
                      </td>
                      <td style={styles.td(false)}>{offer.isPreferred ? '✓' : '–'}</td>
                      <td style={styles.td(false)}>
                        <button style={{ ...styles.btn('secondary'), marginRight: 4 }} onClick={() => setOfferModal({ offer })}>Edit</button>
                        <button style={styles.btn('danger')} onClick={() => handleDeleteOffer(offer.id)}>Delete</button>
                      </td>
                    </tr>
                  ))}
                  {offers.length === 0 && (
                    <tr><td colSpan={8} style={{ ...styles.td(false), color: '#aaa', textAlign: 'center' }}>No offers</td></tr>
                  )}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}

      {offerModal !== null && (
        <OfferModal
          offer={offerModal.offer}
          supplierId={parseInt(selectedSupplierId)}
          onClose={() => setOfferModal(null)}
          onSaved={() => { setOfferModal(null); loadOffers(selectedSupplierId) }}
        />
      )}

      {linkDialog !== null && (
        <LinkDialog
          offer={linkDialog}
          inventoryItems={inventoryItems}
          onClose={() => setLinkDialog(null)}
          onLinked={(warning) => {
            setLinkDialog(null)
            loadOffers(selectedSupplierId)
            if (warning) setMessage({ type: 'error', text: warning })
          }}
          onItemCreated={(newItem) => setInventoryItems(prev => [...prev, newItem])}
        />
      )}
    </div>
  )
}

// ─── Add Sales Product Modal ──────────────────────────────────────────────────
function AddSalesProductModal({ onClose, onAdded }) {
  const [name, setName] = useState('')
  const [externalId, setExternalId] = useState('')
  const [posSystem, setPosSystem] = useState('MANUAL')
  const [saving, setSaving] = useState(false)
  const [err, setErr] = useState(null)

  const handleSave = async () => {
    if (!name.trim() || !externalId.trim()) { setErr('Name and External ID are required'); return }
    setSaving(true)
    try {
      const sp = await api.createSalesProduct({ name: name.trim(), externalId: externalId.trim(), posSystem })
      onAdded(sp)
    } catch (e) {
      setErr(e.message)
      setSaving(false)
    }
  }

  return (
    <div style={styles.overlay}>
      <div style={styles.modal}>
        <h3 style={{ fontSize: 17, fontWeight: 700, marginBottom: 18 }}>Add Sales Product</h3>
        {err && <div style={styles.alert('error')}>{err}</div>}
        <div style={styles.formRow}>
          <label style={styles.label}>Name</label>
          <input style={styles.input} value={name} onChange={e => setName(e.target.value)} placeholder="e.g. Cola 0.33L" />
        </div>
        <div style={styles.formRow}>
          <label style={styles.label}>External ID</label>
          <input style={styles.input} value={externalId} onChange={e => setExternalId(e.target.value)} placeholder="e.g. 45053813" />
        </div>
        <div style={styles.formRow}>
          <label style={styles.label}>POS System</label>
          <select style={styles.input} value={posSystem} onChange={e => setPosSystem(e.target.value)}>
            <option value="MANUAL">MANUAL</option>
            <option value="READY2ORDER">READY2ORDER</option>
          </select>
        </div>
        <div style={{ display: 'flex', gap: 8 }}>
          <button style={styles.btn('primary')} onClick={handleSave} disabled={saving}>{saving ? 'Saving...' : 'Save'}</button>
          <button style={styles.btn('secondary')} onClick={onClose}>Cancel</button>
        </div>
      </div>
    </div>
  )
}

// ─── POS tab ─────────────────────────────────────────────────────────────────
function PosMappingsTab() {
  const [salesProducts, setSalesProducts] = useState([])
  const [selectedProduct, setSelectedProduct] = useState(null)
  const [components, setComponents] = useState([])
  const [inventoryItems, setInventoryItems] = useState([])
  const [addComponentModal, setAddComponentModal] = useState(false)
  const [addProductModal, setAddProductModal] = useState(false)
  const [message, setMessage] = useState(null)
  const [importing, setImporting] = useState(false)
  const csvInputRef = React.useRef(null)

  const loadProducts = useCallback(() => {
    api.getSalesProducts().then(setSalesProducts).catch(() => {})
  }, [])

  useEffect(() => {
    loadProducts()
    api.getInventory().then(setInventoryItems).catch(() => {})
  }, [loadProducts])

  const loadComponents = useCallback(async (sp) => {
    if (!sp) return
    try {
      const data = await api.getSalesProductComponents(sp.id)
      setComponents(data)
    } catch (e) {
      setMessage({ type: 'error', text: 'Failed to load components: ' + e.message })
    }
  }, [])

  useEffect(() => { loadComponents(selectedProduct) }, [selectedProduct, loadComponents])

  const handleDeleteComponent = async (id) => {
    if (!window.confirm('Remove this ingredient?')) return
    try {
      await api.deleteItemComponent(id)
      setComponents(prev => prev.filter(c => c.id !== id))
    } catch (e) {
      setMessage({ type: 'error', text: 'Delete failed: ' + e.message })
    }
  }

  const handleDeleteProduct = async (e, sp) => {
    e.stopPropagation()
    if (!window.confirm(`Delete "${sp.name}"?`)) return
    try {
      await api.deleteSalesProduct(sp.id)
      setSalesProducts(prev => prev.filter(p => p.id !== sp.id))
      if (selectedProduct?.id === sp.id) { setSelectedProduct(null); setComponents([]) }
    } catch (e) {
      setMessage({ type: 'error', text: 'Delete failed: ' + e.message })
    }
  }

  const handleImportR2O = async () => {
    setImporting(true)
    setMessage(null)
    try {
      const result = await api.importSalesProductsR2O()
      setMessage({ type: 'success', text: `Imported ${result.imported} new, updated ${result.updated} products from ready2order` })
      loadProducts()
    } catch (e) {
      setMessage({ type: 'error', text: 'Import failed: ' + e.message })
    } finally {
      setImporting(false)
    }
  }

  const handleImportCsv = async (e) => {
    const file = e.target.files[0]
    if (!file) return
    e.target.value = ''
    setMessage(null)
    try {
      const result = await api.importSalesProductsCsv(file)
      setMessage({ type: 'success', text: `CSV: ${result.imported} imported, ${result.updated} updated, ${result.skipped} skipped` })
      loadProducts()
    } catch (e) {
      setMessage({ type: 'error', text: 'CSV import failed: ' + e.message })
    }
  }

  return (
    <div>
      {message && <div style={styles.alert(message.type)}>{message.text}</div>}
      <div style={styles.twoCol}>
        <div style={styles.leftPanel}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 10 }}>
            <h3 style={{ fontSize: 15, fontWeight: 600, margin: 0 }}>Sales Products</h3>
          </div>
          <div style={{ display: 'flex', gap: 6, marginBottom: 10, flexWrap: 'wrap' }}>
            <button style={styles.btn('secondary')} onClick={handleImportR2O} disabled={importing}>
              {importing ? 'Importing...' : '↓ From ready2order'}
            </button>
            <input ref={csvInputRef} type="file" accept=".csv" style={{ display: 'none' }} onChange={handleImportCsv} />
            <button style={styles.btn('secondary')} onClick={() => csvInputRef.current?.click()}>
              ↑ Import CSV
            </button>
            <button style={styles.btn('primary')} onClick={() => setAddProductModal(true)}>+ Add</button>
          </div>
          <div style={{ fontSize: 11, color: '#aaa', marginBottom: 8 }}>
            CSV format: <code>name,externalId,posSystem</code>
          </div>
          <div style={styles.card}>
            {salesProducts.map(sp => (
              <div key={sp.id} style={{ ...styles.listItem(selectedProduct?.id === sp.id), display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}
                onClick={() => setSelectedProduct(sp)}>
                <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                  {sp.name}
                  <span style={{ fontSize: 11, color: '#aaa', marginLeft: 6 }}>{sp.posSystem}</span>
                </span>
                <button
                  style={{ ...styles.btn('danger'), padding: '2px 8px', fontSize: 12, marginLeft: 6, flexShrink: 0 }}
                  onClick={(e) => handleDeleteProduct(e, sp)}>✕</button>
              </div>
            ))}
            {salesProducts.length === 0 && <div style={{ color: '#aaa', fontSize: 13 }}>No sales products</div>}
          </div>
        </div>
        <div style={styles.rightPanel}>
          {selectedProduct ? (
            <>
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 12 }}>
                <h3 style={{ fontSize: 15, fontWeight: 600 }}>Ingredients for: {selectedProduct.name}</h3>
                <button style={styles.btn('primary')} onClick={() => setAddComponentModal(true)}>+ Add Ingredient</button>
              </div>
              <p style={{ fontSize: 12, color: '#888', marginBottom: 12 }}>Quantities always in the canonical unit of the inventory item</p>
              <div style={styles.card}>
                <table style={styles.table}>
                  <thead>
                    <tr>
                      <th style={styles.th}>Inventory Item</th>
                      <th style={styles.th}>Qty</th>
                      <th style={styles.th}>Unit</th>
                      <th style={styles.th}></th>
                    </tr>
                  </thead>
                  <tbody>
                    {components.map(c => (
                      <tr key={c.id}>
                        <td style={styles.td(false)}>{c.inventoryItemName}</td>
                        <td style={styles.td(false)}>{c.qtyRequired}</td>
                        <td style={styles.td(false)}>{c.inventoryItemUnit}</td>
                        <td style={styles.td(false)}>
                          <button style={styles.btn('danger')} onClick={() => handleDeleteComponent(c.id)}>Delete</button>
                        </td>
                      </tr>
                    ))}
                    {components.length === 0 && (
                      <tr><td colSpan={4} style={{ ...styles.td(false), color: '#aaa', textAlign: 'center' }}>No ingredients</td></tr>
                    )}
                  </tbody>
                </table>
              </div>
            </>
          ) : (
            <div style={{ color: '#aaa', marginTop: 40, textAlign: 'center' }}>Select a sales product to view its ingredients</div>
          )}
        </div>
      </div>
      {addComponentModal && selectedProduct && (
        <AddComponentModal
          salesProduct={selectedProduct}
          inventoryItems={inventoryItems}
          onClose={() => setAddComponentModal(false)}
          onAdded={() => { setAddComponentModal(false); loadComponents(selectedProduct) }}
        />
      )}
      {addProductModal && (
        <AddSalesProductModal
          onClose={() => setAddProductModal(false)}
          onAdded={(sp) => { setSalesProducts(prev => [...prev, sp]); setAddProductModal(false) }}
        />
      )}
    </div>
  )
}

// ─── POS Transaction History ──────────────────────────────────────────────────
function PosTransactionHistoryTab() {
  const [transactions, setTransactions] = useState([])
  const [loading, setLoading] = useState(true)
  const [typeFilter, setTypeFilter] = useState('POS_SALE')

  const load = useCallback((type) => {
    setLoading(true)
    api.getTransactions(type)
      .then(setTransactions)
      .catch(() => setTransactions([]))
      .finally(() => setLoading(false))
  }, [])

  useEffect(() => { load(typeFilter) }, [typeFilter, load])

  const typeColor = (type) => ({
    POS_SALE:      { background: '#fff0f6', color: '#c2255c' },
    GOODS_RECEIPT: { background: '#ebfbee', color: '#2b8a3e' },
    ADJUSTMENT:    { background: '#fff9db', color: '#7c5c00' },
  }[type] || { background: '#f1f3f5', color: '#333' })

  return (
    <div>
      <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 16 }}>
        <select
          style={{ ...styles.input, width: 180 }}
          value={typeFilter}
          onChange={e => setTypeFilter(e.target.value)}
        >
          <option value="">All types</option>
          <option value="POS_SALE">POS Sale</option>
          <option value="GOODS_RECEIPT">Goods Receipt</option>
          <option value="ADJUSTMENT">Adjustment</option>
        </select>
        <button style={styles.btn('secondary')} onClick={() => load(typeFilter)}>↻ Refresh</button>
        <span style={{ fontSize: 13, color: '#888' }}>{transactions.length} entries</span>
      </div>

      {loading ? <div style={{ color: '#888' }}>Loading...</div> : (
        <div style={styles.card}>
          <table style={styles.table}>
            <thead>
              <tr>
                <th style={styles.th}>Date & Time</th>
                <th style={styles.th}>Type</th>
                <th style={styles.th}>Inventory Item</th>
                <th style={styles.th}>Delta</th>
                <th style={styles.th}>Unit</th>
                <th style={styles.th}>Reference / Invoice</th>
              </tr>
            </thead>
            <tbody>
              {transactions.map(tx => (
                <tr key={tx.id}>
                  <td style={styles.td(false)}>{tx.createdAt ? tx.createdAt.replace('T', ' ').substring(0, 19) : '—'}</td>
                  <td style={styles.td(false)}>
                    <span style={{ ...typeColor(tx.type), padding: '2px 8px', borderRadius: 10, fontSize: 12, fontWeight: 600 }}>
                      {tx.type}
                    </span>
                  </td>
                  <td style={styles.td(false)}>{tx.inventoryItem}</td>
                  <td style={{ ...styles.td(false), fontWeight: 600, color: tx.delta < 0 ? '#c92a2a' : '#2b8a3e' }}>
                    {tx.delta > 0 ? '+' : ''}{tx.delta}
                  </td>
                  <td style={styles.td(false)}>{tx.unit}</td>
                  <td style={{ ...styles.td(false), fontSize: 12, color: '#888' }}>{tx.referenceId || '—'}</td>
                </tr>
              ))}
              {transactions.length === 0 && (
                <tr><td colSpan={6} style={{ ...styles.td(false), color: '#aaa', textAlign: 'center' }}>No transactions</td></tr>
              )}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}

function PosTab() {
  const [subTab, setSubTab] = useState('pos-mappings')
  return (
    <div>
      <div style={styles.subTabs}>
        <button style={styles.tab(subTab === 'pos-mappings')} onClick={() => setSubTab('pos-mappings')}>POS Mappings</button>
        <button style={styles.tab(subTab === 'tx-history')}   onClick={() => setSubTab('tx-history')}>Transaction History</button>
      </div>
      {subTab === 'pos-mappings' && <PosMappingsTab />}
      {subTab === 'tx-history'   && <PosTransactionHistoryTab />}
    </div>
  )
}

// ─── SuppliersTab wrapper ─────────────────────────────────────────────────────
function SuppliersTab() {
  const [subTab, setSubTab] = useState('catalog')
  return (
    <div>
      <div style={styles.subTabs}>
        <button style={styles.tab(subTab === 'catalog')}       onClick={() => setSubTab('catalog')}>Catalog</button>
        <button style={styles.tab(subTab === 'configuration')} onClick={() => setSubTab('configuration')}>Configuration</button>
      </div>
      {subTab === 'catalog'       && <SupplierCatalogTab />}
      {subTab === 'configuration' && <SupplierConfigTab />}
    </div>
  )
}

// ─── App ──────────────────────────────────────────────────────────────────────
const TABS = [
  { id: 'order',     label: 'Inventory' },
  { id: 'suppliers', label: 'Suppliers' },
  { id: 'pos',       label: 'POS' },
  { id: 'settings',  label: 'Settings' },
]

export default function App() {
  const [activeTab, setActiveTab] = useState(() => {
    const tab = new URLSearchParams(window.location.search).get('tab')
    const validTabs = ['order', 'suppliers', 'pos', 'settings']
    return validTabs.includes(tab) ? tab : 'order'
  })

  return (
    <div style={styles.container}>
      <h1 style={styles.header}>Inventory Management</h1>
      <div style={styles.tabs}>
        {TABS.map(tab => (
          <button
            key={tab.id}
            style={styles.tab(activeTab === tab.id)}
            onClick={() => setActiveTab(tab.id)}
          >
            {tab.label}
          </button>
        ))}
      </div>
      {activeTab === 'order'     && <InventoryTab />}
      {activeTab === 'suppliers' && <SuppliersTab />}
      {activeTab === 'pos'       && <PosTab />}
      {activeTab === 'settings'  && <SmtpSettingsTab />}
    </div>
  )
}
