package com.mtsu.table21.server

import java.util.concurrent.atomic.AtomicInteger

/** Small in-memory metrics shared by the server and authenticated admin screens. */
object ServerMetrics {
    @Volatile private var startedAtMillis = System.currentTimeMillis()
    private val activeConnections = AtomicInteger(0)

    fun serverStarted() { startedAtMillis = System.currentTimeMillis() }
    fun connectionOpened() { activeConnections.incrementAndGet() }
    fun connectionClosed() { activeConnections.updateAndGet { (it - 1).coerceAtLeast(0) } }

    fun info(port: Int): String {
        val uptime = (System.currentTimeMillis() - startedAtMillis).coerceAtLeast(0L)
        return "ADMIN_SERVER|$uptime|${activeConnections.get()}|$port"
    }
}
