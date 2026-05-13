import React, { useState, useEffect } from 'react'
import { api } from '../api/client.js'

const styles = {
  card: {
    background: '#fff',
    borderRadius: 8,
    padding: 20,
    marginBottom: 16,
    boxShadow: '0 1px 4px rgba(0,0,0,0.08)',
    maxWidth: 500,
  },
  formRow: { marginBottom: 14 },
  label: { fontSize: 13, fontWeight: 600, marginBottom: 4, display: 'block', color: '#555' },
  input: {
    padding: '8px 10px',
    borderRadius: 6,
    border: '1px solid #ced4da',
    fontSize: 14,
    width: '100%',
  },
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
  alert: (type) => ({
    padding: '12px 16px',
    borderRadius: 6,
    marginBottom: 16,
    background: type === 'success' ? '#ebfbee' : '#fff5f5',
    color: type === 'success' ? '#2b8a3e' : '#c92a2a',
    fontWeight: 500,
  }),
  statusRow: {
    display: 'flex',
    alignItems: 'center',
    gap: 8,
    marginBottom: 12,
    fontSize: 14,
    color: '#333',
  },
  connectedBadge: {
    display: 'inline-block',
    padding: '2px 10px',
    borderRadius: 12,
    fontSize: 12,
    fontWeight: 600,
    background: '#e6f4ea',
    color: '#2b8a3e',
  },
}

export default function PosConnectorSettings() {
  const [settings, setSettings] = useState(null)
  const [loading, setLoading] = useState(true)
  const [message, setMessage] = useState(null)
  const [developerToken, setDeveloperToken] = useState('')
  const [manualAccountToken, setManualAccountToken] = useState('')
  const [pollingInterval, setPollingInterval] = useState('5')
  const [inboundMode, setInboundMode] = useState('polling')
  const [saving, setSaving] = useState(false)
  const [polling, setPolling] = useState(false)
  const [requesting, setRequesting] = useState(false)
  const [webhook, setWebhook] = useState(null)
  const [webhookUrl, setWebhookUrl] = useState('')
  const [webhookSaving, setWebhookSaving] = useState(false)

  const isConnected = settings && settings['r2o.accountToken'] && settings['r2o.accountToken'].length > 0

  useEffect(() => {
    // Detect OAuth callback success
    const params = new URLSearchParams(window.location.search)
    if (params.get('connected') === 'true') {
      setMessage({ type: 'success', text: 'ready2order account connected successfully!' })
    }

    api.getR2OSettings()
      .then(data => {
        setSettings(data)
        setPollingInterval(data['r2o.pollingIntervalMinutes'] || '5')
        setInboundMode(data['r2o.inboundMode'] || 'polling')
        return api.getR2OWebhook()
      })
      .then(wh => {
        setWebhook(wh)
        setWebhookUrl(wh?.webhookUrl || window.location.origin + '/api/inbound/r2o/webhook')
      })
      .catch(e => setMessage({ type: 'error', text: 'Failed to load settings: ' + e.message }))
      .finally(() => setLoading(false))
  }, [])

  const handleRequestGrant = async () => {
    if (!developerToken.trim()) {
      setMessage({ type: 'error', text: 'Please enter your Developer Token' })
      return
    }
    setRequesting(true)
    setMessage(null)
    try {
      const callbackUrl = window.location.origin + '/api/settings/r2o/callback'
      const res = await api.requestR2OGrant({ developerToken, callbackUrl })
      if (res.grantAccessUri) {
        window.open(res.grantAccessUri, '_blank')
        setMessage({ type: 'success', text: 'Grant page opened in a new tab. After connecting, return here.' })
      } else if (res.error) {
        setMessage({ type: 'error', text: res.error })
      }
    } catch (e) {
      setMessage({ type: 'error', text: 'Request failed: ' + e.message })
    } finally {
      setRequesting(false)
    }
  }

  const handleSaveManualToken = async () => {
    if (!manualAccountToken.trim()) {
      setMessage({ type: 'error', text: 'Please paste the account token' })
      return
    }
    try {
      await api.updateR2OSettings({ 'r2o.accountToken': manualAccountToken.trim(), 'r2o.enabled': 'true' })
      const data = await api.getR2OSettings()
      setSettings(data)
      setEnabled(true)
      setManualAccountToken('')
      setMessage({ type: 'success', text: 'Account token saved successfully!' })
    } catch (e) {
      setMessage({ type: 'error', text: 'Failed to save token: ' + e.message })
    }
  }

  const handleSaveSettings = async () => {
    setSaving(true)
    setMessage(null)
    try {
      await api.updateR2OSettings({
        'r2o.pollingIntervalMinutes': pollingInterval,
        'r2o.inboundMode': inboundMode,
      })
      setMessage({ type: 'success', text: 'Settings saved!' })
    } catch (e) {
      setMessage({ type: 'error', text: 'Failed to save: ' + e.message })
    } finally {
      setSaving(false)
    }
  }

  const handlePollNow = async () => {
    setPolling(true)
    setMessage(null)
    try {
      await api.triggerR2OPoll()
      setMessage({ type: 'success', text: 'Poll triggered successfully!' })
      const data = await api.getR2OSettings()
      setSettings(data)
    } catch (e) {
      setMessage({ type: 'error', text: 'Poll failed: ' + e.message })
    } finally {
      setPolling(false)
    }
  }

  const handleRegisterWebhook = async () => {
    if (!webhookUrl.trim()) {
      setMessage({ type: 'error', text: 'Please enter a webhook URL' })
      return
    }
    setWebhookSaving(true)
    setMessage(null)
    try {
      const result = await api.registerR2OWebhook({ webhookUrl: webhookUrl.trim() })
      setWebhook(result)
      setMessage({ type: 'success', text: 'Webhook registered successfully!' })
    } catch (e) {
      setMessage({ type: 'error', text: 'Failed to register webhook: ' + e.message })
    } finally {
      setWebhookSaving(false)
    }
  }

  const handleUnregisterWebhook = async () => {
    if (!window.confirm('Remove the webhook registration from ready2order?')) return
    setWebhookSaving(true)
    setMessage(null)
    try {
      await api.unregisterR2OWebhook()
      setWebhook({ webhookUrl: null, activeEvents: [] })
      setWebhookUrl(window.location.origin + '/api/inbound/r2o/webhook')
      setMessage({ type: 'success', text: 'Webhook unregistered.' })
    } catch (e) {
      setMessage({ type: 'error', text: 'Failed to unregister webhook: ' + e.message })
    } finally {
      setWebhookSaving(false)
    }
  }

  const handleDisconnect = async () => {
    if (!window.confirm('Disconnect ready2order? This will clear the account token and disable polling.')) return
    try {
      await api.updateR2OSettings({
        'r2o.accountToken': '',
        'r2o.enabled': 'false',
      })
      setSettings(prev => ({ ...prev, 'r2o.accountToken': '', 'r2o.enabled': 'false' }))
      setEnabled(false)
      setMessage({ type: 'success', text: 'Disconnected from ready2order.' })
    } catch (e) {
      setMessage({ type: 'error', text: 'Failed to disconnect: ' + e.message })
    }
  }

  if (loading) return <div style={{ color: '#888', fontSize: 14 }}>Loading POS connector settings...</div>

  return (
    <div style={styles.card}>
      {message && <div style={styles.alert(message.type)}>{message.text}</div>}

      {!isConnected ? (
        <>
          <div style={styles.formRow}>
            <label style={styles.label}>Developer Token</label>
            <input
              type="password"
              style={styles.input}
              value={developerToken}
              placeholder="Paste your ready2order developer token"
              onChange={e => setDeveloperToken(e.target.value)}
            />
          </div>
          <button style={styles.btn('primary')} onClick={handleRequestGrant} disabled={requesting}>
            {requesting ? 'Requesting...' : 'Connect ready2order Account'}
          </button>
          <p style={{ fontSize: 12, color: '#888', marginTop: 10 }}>
            This will open the ready2order authorization page. After granting access, you will be redirected back.
          </p>
          <hr style={{ margin: '16px 0', border: 'none', borderTop: '1px solid #e9ecef' }} />
          <div style={styles.formRow}>
            <label style={styles.label}>Or paste Account Token directly</label>
            <input
              type="password"
              style={styles.input}
              value={manualAccountToken}
              placeholder="Paste the token shown on the ready2order page"
              onChange={e => setManualAccountToken(e.target.value)}
            />
          </div>
          <button style={styles.btn('secondary')} onClick={handleSaveManualToken}>
            Save Account Token
          </button>
        </>
      ) : (
        <>
          <div style={styles.statusRow}>
            <span style={styles.connectedBadge}>Connected</span>
            <span style={{ color: '#555' }}>Token: {settings['r2o.accountToken']}</span>
          </div>

          <div style={styles.formRow}>
            <label style={styles.label}>Polling Interval (minutes)</label>
            <input
              type="number"
              min="1"
              style={{ ...styles.input, width: 120 }}
              value={pollingInterval}
              onChange={e => setPollingInterval(e.target.value)}
            />
          </div>

          <div style={styles.formRow}>
            <label style={styles.label}>Active Inbound Connector</label>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
              {[
                { value: 'polling', label: 'REST Polling', desc: 'Fetch invoices on a schedule' },
                { value: 'webhook', label: 'Webhook', desc: 'Receive real-time push events' },
                { value: 'escpos', label: 'EscPos (coming soon)', desc: 'ESC/POS printer integration', disabled: true },
              ].map(opt => (
                <label key={opt.value} style={{ display: 'flex', alignItems: 'center', gap: 10, cursor: opt.disabled ? 'default' : 'pointer', opacity: opt.disabled ? 0.45 : 1, fontSize: 14 }}>
                  <input
                    type="radio"
                    name="inboundMode"
                    value={opt.value}
                    checked={inboundMode === opt.value}
                    disabled={opt.disabled}
                    onChange={() => setInboundMode(opt.value)}
                  />
                  <span><strong>{opt.label}</strong> — {opt.desc}</span>
                </label>
              ))}
            </div>
          </div>

          {settings['r2o.lastPolledAt'] && settings['r2o.lastPolledAt'] !== '' && (
            <div style={{ fontSize: 13, color: '#888', marginBottom: 12 }}>
              Last polled: {settings['r2o.lastPolledAt']}
            </div>
          )}

          <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
            <button style={styles.btn('primary')} onClick={handleSaveSettings} disabled={saving}>
              {saving ? 'Saving...' : 'Save Settings'}
            </button>
            <button style={styles.btn('secondary')} onClick={handlePollNow} disabled={polling}>
              {polling ? 'Polling...' : 'Poll Now'}
            </button>
            <button style={styles.btn('danger')} onClick={handleDisconnect}>
              Disconnect
            </button>
          </div>

          <hr style={{ margin: '20px 0', border: 'none', borderTop: '1px solid #e9ecef' }} />
          <div style={{ fontSize: 14, fontWeight: 700, marginBottom: 12, color: '#333' }}>Webhook (Real-time)</div>

          <div style={{
            padding: '10px 14px',
            borderRadius: 6,
            background: '#fff9db',
            border: '1px solid #ffe066',
            fontSize: 13,
            color: '#7c5c00',
            marginBottom: 8,
          }}>
            The webhook URL must be <strong>publicly accessible</strong> from the internet — localhost will not work.
            Use a public domain or a tunnel like ngrok.
          </div>
          <div style={{
            padding: '10px 14px',
            borderRadius: 6,
            background: '#e7f5ff',
            border: '1px solid #74c0fc',
            fontSize: 13,
            color: '#1864ab',
            marginBottom: 14,
          }}>
            Only one inbound connector runs at a time. Select the active connector above and save — the webhook URL must still be registered separately with ready2order.
          </div>

          {webhook?.webhookUrl ? (
            <div style={{ marginBottom: 12 }}>
              <div style={{ fontSize: 13, color: '#555', marginBottom: 6 }}>
                Registered URL: <code style={{ background: '#f1f3f5', padding: '2px 6px', borderRadius: 4 }}>{webhook.webhookUrl}</code>
              </div>
              <div style={{ fontSize: 13, color: '#555', marginBottom: 10 }}>
                Events: <code style={{ background: '#f1f3f5', padding: '2px 6px', borderRadius: 4 }}>{(webhook.activeEvents || []).join(', ') || 'none'}</code>
              </div>
              <button style={styles.btn('danger')} onClick={handleUnregisterWebhook} disabled={webhookSaving}>
                {webhookSaving ? 'Removing...' : 'Remove Webhook'}
              </button>
            </div>
          ) : (
            <div>
              <div style={styles.formRow}>
                <label style={styles.label}>Webhook URL</label>
                <input
                  type="text"
                  style={styles.input}
                  value={webhookUrl}
                  onChange={e => setWebhookUrl(e.target.value)}
                  placeholder="https://your-domain.com/api/inbound/r2o/webhook"
                />
              </div>
              <button style={styles.btn('primary')} onClick={handleRegisterWebhook} disabled={webhookSaving}>
                {webhookSaving ? 'Registering...' : 'Register Webhook'}
              </button>
            </div>
          )}
        </>
      )}
    </div>
  )
}
