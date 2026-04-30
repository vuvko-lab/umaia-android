package app.umaia.android.data.remote

import app.umaia.android.data.auth.AuthService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class PostgrestClient @Inject constructor(@PublishedApi internal val authService: AuthService) {

    @PublishedApi internal val client = OkHttpClient()
    @PublishedApi internal val json   = Json { ignoreUnknownKeys = true }

    val userId: String
        get() = authService.currentUserId ?: error("Not authenticated")

    // ── SELECT ────────────────────────────────────────────────────────────────

    suspend inline fun <reified T> select(
        table: String,
        columns: String = "*",
        filters: Map<String, String> = emptyMap(),
        rawFilters: List<Pair<String, String>> = emptyList(),
        single: Boolean = false
    ): T = withContext(Dispatchers.IO) {
        val urlBuilder = "${authService.baseUrl}/rest/v1/$table".toHttpUrl().newBuilder()
            .addQueryParameter("select", columns)
        filters.forEach { (k, v) -> urlBuilder.addQueryParameter(k, "eq.$v") }
        // rawFilters values are passed verbatim (e.g. "gte.2026-04-30T19:00:00Z").
        // List-of-pairs lets us send multiple operators on the same column,
        // which a Map<String, String> can't express (e.g. submitted_at gte X
        // AND submitted_at lt Y).
        rawFilters.forEach { (k, v) -> urlBuilder.addQueryParameter(k, v) }

        val request = Request.Builder()
            .url(urlBuilder.build())
            .apply { authService.authenticatedHeaders().forEach { (k, v) -> addHeader(k, v) } }
            .apply { if (single) addHeader("Accept", "application/vnd.pgrst.object+json") }
            .get()
            .build()

        val body = client.newCall(request).await().checkSuccess()
        json.decodeFromString<T>(body)
    }

    suspend inline fun <reified T> selectOptional(
        table: String,
        columns: String = "*",
        filters: Map<String, String> = emptyMap(),
        single: Boolean = false
    ): T? = runCatching { select<T>(table, columns, filters, single = single) }.getOrNull()

    // ── INSERT ────────────────────────────────────────────────────────────────

    suspend inline fun <reified T : Any> insert(value: T, table: String) =
        withContext(Dispatchers.IO) {
            val body = json.encodeToString(kotlinx.serialization.serializer<T>(), value)
            val request = Request.Builder()
                .url("${authService.baseUrl}/rest/v1/$table".toHttpUrl())
                .apply { authService.authenticatedHeaders().forEach { (k, v) -> addHeader(k, v) } }
                .addHeader("Prefer", "return=minimal")
                .post(body.toRequestBody("application/json".toMediaType()))
                .build()
            client.newCall(request).await().checkSuccess()
        }

    /** INSERT and return the created row decoded as [Out]. Uses `Prefer: return=representation`. */
    suspend inline fun <reified In : Any, reified Out> insertReturning(
        value: In,
        table: String,
        columns: String = "*"
    ): Out = withContext(Dispatchers.IO) {
        val urlBuilder = "${authService.baseUrl}/rest/v1/$table".toHttpUrl().newBuilder()
            .addQueryParameter("select", columns)
        val body = json.encodeToString(kotlinx.serialization.serializer<In>(), value)
        val request = Request.Builder()
            .url(urlBuilder.build())
            .apply { authService.authenticatedHeaders().forEach { (k, v) -> addHeader(k, v) } }
            .addHeader("Prefer", "return=representation")
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()
        val responseText = client.newCall(request).await().checkSuccess()
        // PostgREST returns an array even for single-row inserts
        json.decodeFromString<List<Out>>(responseText).first()
    }

    // ── RPC ───────────────────────────────────────────────────────────────────

    /** Call a Supabase SECURITY DEFINER RPC function and decode the result. */
    suspend inline fun <reified T> rpc(
        function: String,
        params: Map<String, String> = emptyMap()
    ): T = withContext(Dispatchers.IO) {
        val bodyJson = if (params.isEmpty()) "{}"
        else params.entries.joinToString(",", "{", "}") { (k, v) -> "\"$k\":\"$v\"" }
        val request = Request.Builder()
            .url("${authService.baseUrl}/rest/v1/rpc/$function".toHttpUrl())
            .apply { authService.authenticatedHeaders().forEach { (k, v) -> addHeader(k, v) } }
            .post(bodyJson.toRequestBody("application/json".toMediaType()))
            .build()
        val responseText = client.newCall(request).await().checkSuccess()
        json.decodeFromString<T>(responseText)
    }

    // ── UPSERT ────────────────────────────────────────────────────────────────

    suspend inline fun <reified T : Any> upsert(
        value: T,
        table: String,
        onConflict: String? = null
    ) = withContext(Dispatchers.IO) {
        val urlBuilder = "${authService.baseUrl}/rest/v1/$table".toHttpUrl().newBuilder()
        onConflict?.let { urlBuilder.addQueryParameter("on_conflict", it) }

        val body = json.encodeToString(kotlinx.serialization.serializer<T>(), value)
        val request = Request.Builder()
            .url(urlBuilder.build())
            .apply { authService.authenticatedHeaders().forEach { (k, v) -> addHeader(k, v) } }
            .addHeader("Prefer", "resolution=merge-duplicates,return=minimal")
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()
        client.newCall(request).await().checkSuccess()
    }

    /** UPSERT and return the resulting row decoded as [Out]. Uses
     *  `Prefer: resolution=merge-duplicates,return=representation` and
     *  `Accept: application/vnd.pgrst.object+json` so PostgREST returns a
     *  single object (not an array) — matches the iOS implementation. */
    suspend inline fun <reified In : Any, reified Out> upsertReturning(
        value: In,
        table: String,
        onConflict: String,
        columns: String = "*"
    ): Out = withContext(Dispatchers.IO) {
        val urlBuilder = "${authService.baseUrl}/rest/v1/$table".toHttpUrl().newBuilder()
            .addQueryParameter("on_conflict", onConflict)
            .addQueryParameter("select", columns)
        val body = json.encodeToString(kotlinx.serialization.serializer<In>(), value)
        val request = Request.Builder()
            .url(urlBuilder.build())
            .apply { authService.authenticatedHeaders().forEach { (k, v) -> addHeader(k, v) } }
            .addHeader("Prefer", "resolution=merge-duplicates,return=representation")
            .addHeader("Accept", "application/vnd.pgrst.object+json")
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()
        val responseText = client.newCall(request).await().checkSuccess()
        json.decodeFromString<Out>(responseText)
    }

    // ── PATCH ─────────────────────────────────────────────────────────────────

    suspend inline fun <reified T : Any> update(
        value: T,
        table: String,
        filters: Map<String, String>
    ) = withContext(Dispatchers.IO) {
        val urlBuilder = "${authService.baseUrl}/rest/v1/$table".toHttpUrl().newBuilder()
        filters.forEach { (k, v) -> urlBuilder.addQueryParameter(k, "eq.$v") }

        val body = json.encodeToString(kotlinx.serialization.serializer<T>(), value)
        val request = Request.Builder()
            .url(urlBuilder.build())
            .apply { authService.authenticatedHeaders().forEach { (k, v) -> addHeader(k, v) } }
            .addHeader("Prefer", "return=minimal")
            .patch(body.toRequestBody("application/json".toMediaType()))
            .build()
        client.newCall(request).await().checkSuccess()
    }

    fun Response.checkSuccess(): String {
        val text = body?.string() ?: ""
        if (!isSuccessful) error("PostgREST $code: $text")
        return text
    }
}

// OkHttp coroutine bridge
suspend fun Call.await(): Response = suspendCancellableCoroutine { cont ->
    enqueue(object : Callback {
        override fun onResponse(call: Call, response: Response) { cont.resume(response) }
        override fun onFailure(call: Call, e: IOException) { cont.resumeWithException(e) }
    })
    cont.invokeOnCancellation { cancel() }
}
