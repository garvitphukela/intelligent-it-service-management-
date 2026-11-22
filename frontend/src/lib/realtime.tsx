import { createContext, useContext, useEffect, useRef, useState, type ReactNode } from 'react'
import { Client } from '@stomp/stompjs'
import { useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { tokenStore } from './api'
import { useAuth, isStaff } from './auth'
import type { LiveEvent, NotificationView } from './types'

interface RealtimeState { connected: boolean; events: LiveEvent[] }
const RealtimeContext = createContext<RealtimeState>({ connected: false, events: [] })

/** STOMP over WebSocket: live dashboard events and per-user notifications. */
export function RealtimeProvider({ children }: { children: ReactNode }) {
  const { user } = useAuth()
  const qc = useQueryClient()
  const [connected, setConnected] = useState(false)
  const [events, setEvents] = useState<LiveEvent[]>([])
  const clientRef = useRef<Client | null>(null)

  useEffect(() => {
    if (!user) return
    const token = tokenStore.get()
    if (!token) return
    const proto = window.location.protocol === 'https:' ? 'wss' : 'ws'
    const client = new Client({
      brokerURL: `${proto}://${window.location.host}/ws`,
      connectHeaders: { Authorization: 'Bearer ' + token },
      reconnectDelay: 4000,
      heartbeatIncoming: 15000,
      heartbeatOutgoing: 15000,
    })
    let invalidateTimer: ReturnType<typeof setTimeout> | undefined
    const invalidateSoon = () => {
      clearTimeout(invalidateTimer)
      invalidateTimer = setTimeout(() => {
        qc.invalidateQueries({ queryKey: ['incidents'] })
        qc.invalidateQueries({ queryKey: ['incident'] })
        qc.invalidateQueries({ queryKey: ['reports'] })
        qc.invalidateQueries({ queryKey: ['workload'] })
        qc.invalidateQueries({ queryKey: ['intel'] })
      }, 400)
    }
    client.onConnect = () => {
      setConnected(true)
      client.subscribe('/user/queue/notifications', (m) => {
        const n = JSON.parse(m.body) as NotificationView
        qc.invalidateQueries({ queryKey: ['notifications'] })
        toast(n.title, { description: n.message.length > 140 ? n.message.slice(0, 140) + '…' : n.message })
        invalidateSoon()
      })
      if (isStaff(user.role)) {
        client.subscribe('/topic/events', (m) => {
          const e = JSON.parse(m.body) as LiveEvent
          setEvents((prev) => [e, ...prev].slice(0, 60))
          invalidateSoon()
        })
      }
    }
    client.onDisconnect = () => setConnected(false)
    client.onWebSocketClose = () => setConnected(false)
    client.activate()
    clientRef.current = client
    return () => { clearTimeout(invalidateTimer); client.deactivate(); setConnected(false) }
  }, [user, qc])

  return <RealtimeContext.Provider value={{ connected, events }}>{children}</RealtimeContext.Provider>
}

export const useRealtime = () => useContext(RealtimeContext)
