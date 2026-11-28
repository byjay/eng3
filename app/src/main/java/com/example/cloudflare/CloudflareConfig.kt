package com.example.cloudflare

import com.example.BuildConfig

/**
 * Centralized Cloudflare Workers endpoint configuration.
 * - Uses public HTTPS Worker URL from BuildConfig.CLOUDFLARE_WORKER_URL (configured via .env / AI Studio Secrets)
 *   or an explicit user-provided override URL.
 * - Never embeds server-side Cloudflare API tokens or LLM secrets in the APK.
 */
object CloudflareConfig {

    private const val PLACEHOLDER_HOST = "your-worker.your-subdomain"

    @Volatile
    private var runtimeOverrideUrl: String = ""

    fun setRuntimeOverrideUrl(url: String) {
        runtimeOverrideUrl = url.trim().trimEnd('/')
    }

    fun resolveWorkerUrl(customUrl: String = ""): String {
        val candidate = customUrl.trim()
            .ifBlank { runtimeOverrideUrl }
            .ifBlank { BuildConfig.CLOUDFLARE_WORKER_URL.trim() }
        if (candidate.isBlank() || candidate.contains(PLACEHOLDER_HOST, ignoreCase = true)) {
            return ""
        }
        val normalized = candidate.trimEnd('/')
        return if (isAllowedWorkerUrl(normalized)) normalized else ""
    }

    fun isAllowedWorkerUrl(url: String): Boolean {
        val clean = url.trim().lowercase()
        if (clean.isBlank() || clean.contains(PLACEHOLDER_HOST)) return false
        if (clean.startsWith("https://")) return true
        return BuildConfig.DEBUG && (
            clean.startsWith("http://10.0.2.2:") ||
                clean.startsWith("http://localhost:") ||
                clean.startsWith("http://127.0.0.1:")
            )
    }

    fun isConfigured(customUrl: String = ""): Boolean = resolveWorkerUrl(customUrl).isNotBlank()
}
