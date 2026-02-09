package com.debanshu.xcalendar.domain.llm

import android.os.Build
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single
import org.koin.mp.KoinPlatform

@Single(binds = [LocalLlmManager::class])
actual class PlatformLocalLlmManager : LocalLlmManager {
    private val koin = KoinPlatform.getKoin()
    private val context by lazy { koin.get<android.content.Context>() }
    private val runtimeFactory by lazy { koin.get<LocalLlmRuntimeFactory>() }
    private val downloadClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }
    private val localModelDir: File by lazy { File(context.filesDir, LOCAL_MODEL_DIR_NAME) }
    private val localModelFile: File by lazy { File(localModelDir, MODEL_FILE_NAME) }
    private val downloadMutex = Mutex()
    private val runtimeMutex = Mutex()
    private var localRuntime: LocalLlmRuntime? = null
    @Volatile private var modelIncompatible: Boolean = false
    @Volatile private var incompatibilityMessage: String? = null
    @Volatile private var cachedAssetAvailable: Boolean? = null
    @Volatile private var warnedGpuFallback: Boolean = false
    @Volatile private var pendingWarningMessage: String? = null
    private val warningLock = Any()

    override val isAvailable: Boolean
        get() = !modelIncompatible && (hasLocalModel() || isAssetAvailable())

    override fun getStatus(): LlmModelStatus {
        val source = resolveSource()
        val sizeBytes = if (source == LlmModelSource.LOCAL) localModelFile.length() else 0L
        val available = source != LlmModelSource.NONE && !modelIncompatible
        return LlmModelStatus(
            available = available,
            source = source,
            sizeBytes = sizeBytes,
            requiredBytes = PRIMARY_MODEL_ESTIMATED_BYTES,
            incompatibilityMessage = incompatibilityMessage,
        )
    }

    override suspend fun ensureModelAvailable(): Boolean {
        if (hasLocalModel()) return true
        return copyFromAssets()
    }

    override suspend fun downloadModel(onProgress: (Int) -> Unit): Boolean {
        return withContext(Dispatchers.IO) {
            downloadMutex.withLock {
                clearModelIncompatibility()
                if (hasLocalModel()) {
                    onProgress(100)
                    return@withLock true
                }
                if (!localModelDir.exists() && !localModelDir.mkdirs()) {
                    Log.e(TAG, "Failed to create local model directory at ${localModelDir.absolutePath}.")
                    return@withLock false
                }
                val tempFile = File(localModelDir, "$MODEL_FILE_NAME.part")
                if (tempFile.exists()) {
                    tempFile.delete()
                }
                val urls = listOf(PRIMARY_MODEL_URL, FALLBACK_MODEL_URL).distinct()
                for (url in urls) {
                    if (downloadToFile(url, tempFile, onProgress)) {
                        if (localModelFile.exists() && !localModelFile.delete()) {
                            Log.w(TAG, "Failed to delete existing local model before rename.")
                        }
                        if (!tempFile.renameTo(localModelFile)) {
                            Log.e(TAG, "Failed to move local model into place.")
                            tempFile.delete()
                            return@withLock false
                        }
                        resetRuntime()
                        onProgress(100)
                        return@withLock true
                    }
                }
                false
            }
        }
    }

    override suspend fun generate(
        prompt: String,
        sampling: LlmSamplingConfig,
    ): String? {
        if (modelIncompatible) return null
        return withContext(Dispatchers.Default) {
            val runtime = getOrCreateRuntime() ?: return@withContext null
            try {
                runtime.generate(prompt, sampling).trim()
            } catch (e: IllegalArgumentException) {
                Log.w(TAG, "Local LLM input validation failed: ${e.message}")
                if (isCompatibilityError(e)) {
                    markModelIncompatible(e)
                }
                null
            } catch (e: Exception) {
                Log.e(TAG, "Local LLM generation failed.", e)
                if (isCompatibilityError(e)) {
                    markModelIncompatible(e)
                }
                null
            }
        }
    }

    override fun deleteModel(): Boolean {
        return try {
            resetRuntime()
            clearModelIncompatibility()
            val tempFile = File(localModelDir, "$MODEL_FILE_NAME.part")
            if (tempFile.exists()) {
                tempFile.delete()
            }
            if (localModelFile.exists()) {
                localModelFile.delete()
            } else {
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete local model.", e)
            false
        }
    }

    override fun consumeWarningMessage(): String? {
        synchronized(warningLock) {
            val message = pendingWarningMessage
            pendingWarningMessage = null
            return message
        }
    }

    private fun resolveSource(): LlmModelSource {
        return when {
            hasLocalModel() -> LlmModelSource.LOCAL
            isAssetAvailable() -> LlmModelSource.ASSET
            else -> LlmModelSource.NONE
        }
    }

    private fun hasLocalModel(): Boolean {
        return localModelFile.exists() && localModelFile.length() > 0L
    }

    private fun isAssetAvailable(): Boolean {
        cachedAssetAvailable?.let { return it }
        val available = runCatching {
            context.assets.open(MODEL_ASSET_PATH).use { input ->
                input.available() >= 0
            }
            true
        }.getOrDefault(false)
        cachedAssetAvailable = available
        return available
    }

    private suspend fun copyFromAssets(): Boolean {
        if (!isAssetAvailable()) return false
        return withContext(Dispatchers.IO) {
            runCatching {
                clearModelIncompatibility()
                if (!localModelDir.exists()) {
                    localModelDir.mkdirs()
                }
                context.assets.open(MODEL_ASSET_PATH).use { input ->
                    FileOutputStream(localModelFile).use { output ->
                        input.copyTo(output)
                    }
                }
                resetRuntime()
                true
            }.getOrElse { e ->
                Log.e(TAG, "Failed to copy model from assets.", e)
                false
            }
        }
    }

    private fun downloadToFile(
        url: String,
        tempFile: File,
        onProgress: (Int) -> Unit,
    ): Boolean {
        return try {
            val request = Request.Builder().url(url).build()
            downloadClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Model download failed: HTTP ${response.code}.")
                    return false
                }
                val body = response.body ?: run {
                    Log.e(TAG, "Model download failed: empty body.")
                    return false
                }
                val contentLength = body.contentLength()
                val totalBytes = if (contentLength > 0) contentLength else PRIMARY_MODEL_ESTIMATED_BYTES
                onProgress(0)
                body.byteStream().use { input ->
                    FileOutputStream(tempFile).use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        var totalRead = 0L
                        var lastPercent = -1
                        while (true) {
                            val read = input.read(buffer)
                            if (read == -1) break
                            output.write(buffer, 0, read)
                            totalRead += read
                            val percent =
                                if (totalBytes > 0) ((totalRead * 100) / totalBytes).toInt()
                                else 0
                            if (percent != lastPercent) {
                                onProgress(percent.coerceIn(0, 100))
                                lastPercent = percent
                            }
                        }
                        output.flush()
                    }
                }
                val finalSize = tempFile.length()
                if (finalSize <= 0L) {
                    Log.e(TAG, "Model download failed: empty file.")
                    tempFile.delete()
                    return false
                }
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Model download failed for $url.", e)
            false
        }
    }

    private suspend fun getOrCreateRuntime(): LocalLlmRuntime? {
        if (modelIncompatible) return null
        return runtimeMutex.withLock {
            if (modelIncompatible) return@withLock null
            localRuntime?.let { return@withLock it }
            if (!ensureModelAvailable()) {
                Log.w(TAG, "Local model missing; cannot initialize runtime.")
                return@withLock null
            }
            if (!hasLocalModel()) {
                Log.w(TAG, "Local model unavailable after copy; cannot initialize runtime.")
                return@withLock null
            }
            val preferredBackend = resolvePreferredBackend()
            val baseConfig = LocalLlmRuntimeConfig(
                modelPath = localModelFile.absolutePath,
                backend = preferredBackend,
                maxTokens = MAX_TOKENS,
                cacheDir = null,
            )
            if (preferredBackend == LlmBackend.CPU) {
                return@withLock runCatching { runtimeFactory.create(baseConfig) }
                    .onFailure { markModelIncompatible(it as? Exception ?: Exception(it)) }
                    .getOrNull()
                    ?.also { localRuntime = it }
            }
            return@withLock try {
                runtimeFactory.create(baseConfig).also { localRuntime = it }
            } catch (gpuError: Exception) {
                Log.e(TAG, "GPU runtime init failed; falling back to CPU.", gpuError)
                notifyGpuFallbackOnce()
                try {
                    val cpuConfig = baseConfig.copy(backend = LlmBackend.CPU)
                    runtimeFactory.create(cpuConfig).also { localRuntime = it }
                } catch (cpuError: Exception) {
                    markModelIncompatible(cpuError)
                    null
                }
            }
        }
    }

    private fun resolvePreferredBackend(): LlmBackend {
        if (isProbablyEmulator()) {
            notifyGpuFallbackOnce()
            return LlmBackend.CPU
        }
        if (!isOpenClAvailable()) {
            notifyGpuFallbackOnce()
            return LlmBackend.CPU
        }
        return LlmBackend.GPU
    }

    private fun isProbablyEmulator(): Boolean {
        val fingerprint = Build.FINGERPRINT
        val model = Build.MODEL
        val brand = Build.BRAND
        val device = Build.DEVICE
        val product = Build.PRODUCT
        val manufacturer = Build.MANUFACTURER
        val hardware = Build.HARDWARE
        val abis = Build.SUPPORTED_ABIS
        return fingerprint.startsWith("generic") ||
            fingerprint.startsWith("unknown") ||
            model.contains("google_sdk", ignoreCase = true) ||
            model.contains("emulator", ignoreCase = true) ||
            model.contains("android sdk built for x86", ignoreCase = true) ||
            manufacturer.contains("genymotion", ignoreCase = true) ||
            (brand.startsWith("generic") && device.startsWith("generic")) ||
            product.contains("sdk_gphone", ignoreCase = true) ||
            product.contains("emulator", ignoreCase = true) ||
            hardware.contains("ranchu", ignoreCase = true) ||
            hardware.contains("goldfish", ignoreCase = true) ||
            abis.any { it.startsWith("x86") }
    }

    private fun isOpenClAvailable(): Boolean {
        val candidates = listOf(
            "/system/lib64/libOpenCL.so",
            "/system/lib/libOpenCL.so",
            "/vendor/lib64/libOpenCL.so",
            "/vendor/lib/libOpenCL.so",
        )
        val exists = candidates.any { path -> File(path).exists() }
        return if (exists) {
            runCatching {
                System.loadLibrary("OpenCL")
                true
            }.getOrElse { false }
        } else {
            false
        }
    }

    private fun notifyGpuFallbackOnce() {
        if (warnedGpuFallback) return
        warnedGpuFallback = true
        enqueueWarning("GPU unavailable; using CPU for offline AI.")
    }

    private fun enqueueWarning(message: String) {
        synchronized(warningLock) {
            pendingWarningMessage = message
        }
    }

    private fun isCompatibilityError(e: Exception): Boolean {
        val message = (e.message ?: "") + " " + (e.cause?.message ?: "")
        return message.contains("LiteRT", ignoreCase = true) ||
            message.contains("litertlm", ignoreCase = true) ||
            message.contains("model", ignoreCase = true) ||
            message.contains("signature", ignoreCase = true)
    }

    private fun markModelIncompatible(e: Exception) {
        modelIncompatible = true
        incompatibilityMessage = "Local AI model is not usable on this device."
        Log.e(TAG, "Local model marked incompatible: ${e.message}", e)
        resetRuntime()
    }

    private fun clearModelIncompatibility() {
        modelIncompatible = false
        incompatibilityMessage = null
    }

    private fun resetRuntime() {
        try {
            localRuntime?.close()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to close local LLM runtime.", e)
        } finally {
            localRuntime = null
        }
    }

    companion object {
        private const val TAG = "LocalLlmManager"
        private const val LOCAL_MODEL_DIR_NAME = "llm"
        private const val MODEL_FILE_NAME = "gemma3-1b-it-int4.litertlm"
        private const val MODEL_ASSET_PATH = "llm/$MODEL_FILE_NAME"
        private const val PRIMARY_MODEL_URL =
            "https://raw.githubusercontent.com/lefevre7/XCalendar/refs/heads/main/" +
                "gemma3-1b-it-int4.litertlm"
        private const val FALLBACK_MODEL_URL =
            "https://media.githubusercontent.com/media/lefevre7/XCalendar/refs/heads/main/" +
                "gemma3-1b-it-int4.litertlm?download=true"
        private const val PRIMARY_MODEL_ESTIMATED_BYTES = 600_000_000L
        private const val MAX_TOKENS = 1024
    }
}
