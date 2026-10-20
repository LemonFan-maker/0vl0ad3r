package com.orionisli.ovl0ad3r.data.repo

import com.orionisli.ovl0ad3r.data.model.RepoSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicLong

class RepoRepository private constructor() {

    data class SourceSyncResult(
        val source: RepoSource,
        val index: RepoIndex?,
        val bytesDownloaded: Long,
        val error: String?,
        val rawIndex: String? = null
    ) {
        val ok: Boolean get() = error == null && index != null
    }

    suspend fun syncAll(
        sources: List<RepoSource>,
        onBytes: (Long) -> Unit = {}
    ): List<SourceSyncResult> = coroutineScope {
        val tally = ByteTally(onBytes)
        sources.filter { it.isEnabled }
            .map { source -> async(Dispatchers.IO) { syncOne(source, tally.forSource()) } }
            .awaitAll()
    }

    suspend fun downloadPackage(
        sourceBase: String,
        filename: String,
        expectedSha256: String,
        dest: File,
        onBytes: (Long) -> Unit = {}
    ): Long = withContext(Dispatchers.IO) {
        require(filename.isNotBlank()) { "包文件名为空" }
        val link = sourceBase.trimEnd('/') + "/" + filename
        val digest = MessageDigest.getInstance("SHA-256")
        val conn = openHttps(link)
        try {
            val code = conn.responseCode
            if (code !in 200..299) throw IOException("HTTP $code: $link")
            if (conn.contentLength > MAX_PACKAGE_BYTES) {
                throw IOException("包体过大: ${conn.contentLength} B")
            }
            var total = 0L
            conn.inputStream.use { input ->
                FileOutputStream(dest).use { output ->
                    val buf = ByteArray(64 * 1024)
                    while (true) {
                        val r = input.read(buf)
                        if (r <= 0) break
                        total += r
                        if (total > MAX_PACKAGE_BYTES) {
                            throw IOException("包体超过上限 $MAX_PACKAGE_BYTES B")
                        }
                        digest.update(buf, 0, r)
                        output.write(buf, 0, r)
                        onBytes(total)
                    }
                }
            }
            if (expectedSha256.isNotBlank()) {
                val actual = digest.digest().toHex()
                if (!actual.equals(expectedSha256, ignoreCase = true)) {
                    dest.delete()
                    throw IOException("sha256 校验失败: $filename")
                }
            }
            total
        } catch (e: Exception) {
            dest.delete()
            throw e
        } finally {
            conn.disconnect()
        }
    }

    private suspend fun syncOne(
        source: RepoSource,
        onBytes: (Long) -> Unit
    ): SourceSyncResult = try {
        val (raw, bytes) = fetchIndexText(source.url, onBytes)
        SourceSyncResult(source, RepoIndex.parse(raw), bytes, null, rawIndex = raw)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        SourceSyncResult(source, null, 0L, e.message ?: e.javaClass.simpleName)
    }

    companion object {
        const val INDEX_FILE = "index.json"

        fun indexUrlOf(sourceBase: String): String =
            sourceBase.trimEnd('/') + "/" + INDEX_FILE

        suspend fun fetchIndex(
            sourceBase: String,
            onBytes: (Long) -> Unit = {}
        ): Pair<RepoIndex, Long> {
            val (raw, n) = fetchIndexText(sourceBase, onBytes)
            return RepoIndex.parse(raw) to n
        }

        private suspend fun fetchIndexText(
            sourceBase: String,
            onBytes: (Long) -> Unit
        ): Pair<String, Long> {
            val body = ByteArrayOutputStream(INITIAL_BUFFER)
            val n = httpGet(indexUrlOf(sourceBase)) { chunk ->
                body.write(chunk, 0, chunk.size)
                onBytes(body.size().toLong())
            }
            return body.toString(Charsets.UTF_8.name()) to n
        }

        private fun openHttps(link: String): HttpURLConnection {
            val url = URL(link)
            if (url.protocol != "https") {
                throw IOException("仅支持 HTTPS 源, 拒绝: $link")
            }
            return (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                instanceFollowRedirects = true
                // HttpURLConnection 没有 requestProperties 的 setter, 只能逐个加。
                addRequestProperty("Accept-Encoding", "identity")
                addRequestProperty("User-Agent", USER_AGENT)
            }
        }

        private suspend fun httpGet(link: String, onChunk: (ByteArray) -> Unit): Long =
            withContext(Dispatchers.IO) {
                val conn = openHttps(link)
                try {
                    val code = conn.responseCode
                    if (code !in 200..299) throw IOException("HTTP $code")
                    if (conn.contentLength > MAX_BODY_BYTES) {
                        throw IOException("响应过大: ${conn.contentLength} B")
                    }
                    conn.inputStream.use { input ->
                        var total = 0L
                        val buf = ByteArray(8 * 1024)
                        while (true) {
                            val r = input.read(buf)
                            if (r <= 0) break
                            total += r
                            if (total > MAX_BODY_BYTES) {
                                conn.disconnect()
                                throw IOException("响应超过上限 $MAX_BODY_BYTES B")
                            }
                            onChunk(buf.copyOf(r))
                        }
                        total
                    }
                } finally {
                    conn.disconnect()
                }
            }

        private const val CONNECT_TIMEOUT_MS = 15_000
        private const val READ_TIMEOUT_MS = 60_000
        private const val INITIAL_BUFFER = 16 * 1024

        private const val MAX_BODY_BYTES = 4L * 1024 * 1024

        private const val MAX_PACKAGE_BYTES = 256L * 1024 * 1024
        private const val USER_AGENT = "0vl0ad3r/1.0 (Android)"

        internal fun sha256Hex(data: ByteArray): String =
            MessageDigest.getInstance("SHA-256").digest(data).toHex()

        val INSTANCE: RepoRepository by lazy { RepoRepository() }
    }
}

private fun ByteArray.toHex(): String =
    joinToString("") { "%02x".format(it) }

internal class ByteTally(private val onTotal: (Long) -> Unit) {

    private val total = AtomicLong(0L)

    fun forSource(): (Long) -> Unit {
        val reported = AtomicLong(0L)
        return { cumulative ->
            val delta = cumulative - reported.getAndSet(cumulative)
            if (delta > 0) onTotal(total.addAndGet(delta))
        }
    }
}
