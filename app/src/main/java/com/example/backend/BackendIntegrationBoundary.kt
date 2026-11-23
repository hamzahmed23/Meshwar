package com.example.backend

/**
 * Defines the integration boundary for future phases (Phase 2+).
 *
 * Phase 1 intentionally contains no live backend connections, API keys, or mock network stubs.
 * Future phases will plug concrete adapters here for:
 * - Authentication & session management
 * - Realtime ride matching & atomic offer acceptance
 * - Maps & location services
 * - Push notifications
 */
interface BackendIntegrationBoundary {
  val isConfigured: Boolean
}

object UnconfiguredBackendBoundary : BackendIntegrationBoundary {
  override val isConfigured: Boolean = false
}
