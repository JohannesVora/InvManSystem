import React, { useState, useEffect, useCallback } from 'react'
import { api } from './api/client.js'

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
                <td style={styles.td(item.needsReorder)}>{item.name}</td>
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
    </div>
  )
}

// ─── App ──────────────────────────────────────────────────────────────────────
const TABS = [
  { id: 'order', label: 'Order' },
  { id: 'supplier', label: 'Supplier Configuration' },
  { id: 'smtp', label: 'SMTP Settings' },
]

export default function App() {
  const [activeTab, setActiveTab] = useState('order')

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
      {activeTab === 'order' && <OrderTab />}
      {activeTab === 'supplier' && <SupplierConfigTab />}
      {activeTab === 'smtp' && <SmtpSettingsTab />}
    </div>
  )
}
