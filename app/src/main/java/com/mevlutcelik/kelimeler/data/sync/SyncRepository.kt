package com.mevlutcelik.kelimeler.data.sync

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.mevlutcelik.kelimeler.BuildConfig
import com.mevlutcelik.kelimeler.data.backup.BackupReview
import com.mevlutcelik.kelimeler.data.local.ReviewDao
import com.mevlutcelik.kelimeler.data.settings.SettingsRepository
import com.mevlutcelik.kelimeler.data.study.StudyRepository
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

@Serializable
private data class AuthRequest(val idToken: String)

@Serializable
private data class AuthResponse(val token: String)

@Serializable
private data class SyncRequest(val cursor: Long, val reviews: List<BackupReview>)

@Serializable
private data class SyncResponse(val cursor: Long, val reviews: List<BackupReview>, val more: Boolean)

sealed interface SyncState {
    data object Idle : SyncState
    data object Running : SyncState
    data class Done(val pushed: Int, val pulled: Int) : SyncState
    data class Failed(val error: Throwable) : SyncState
}

class SessionExpiredException : IOException("Oturumun süresi doldu")

/**
 * Google hesabıyla giriş ve cihazlar arası senkron (sunucu: server/, Cloudflare Pages Functions + D1).
 *
 * Senkron yalnızca tekrar kayıtlarını taşır: gönderilmemiş kayıtlar yüklenir, sunucudaki imleçten
 * sonraki kayıtlar indirilir. Kayıtlar yalnızca eklendiği için birleştirmede çakışma olmaz.
 * Google kimlik jetonu bir kez sunucuya gösterilir; sunucu karşılığında uzun ömürlü bir oturum
 * jetonu verir, sonraki senkronlar bununla yapılır.
 */
@Singleton
class SyncRepository @Inject constructor(
    private val http: OkHttpClient,
    private val json: Json,
    private val settings: SettingsRepository,
    private val reviewDao: ReviewDao,
    private val study: StudyRepository,
) {
    val isConfigured = BuildConfig.SYNC_URL.isNotBlank() && BuildConfig.GOOGLE_CLIENT_ID.isNotBlank()

    private val _state = MutableStateFlow<SyncState>(SyncState.Idle)
    val state: StateFlow<SyncState> = _state.asStateFlow()

    private val mutex = Mutex()

    /** Google hesap seçicisini açar; [activityContext] bir Activity olmalı. */
    suspend fun signIn(activityContext: Context): Boolean {
        check(isConfigured)
        val option = GetSignInWithGoogleOption.Builder(BuildConfig.GOOGLE_CLIENT_ID).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        val credential = try {
            CredentialManager.create(activityContext).getCredential(activityContext, request).credential
        } catch (e: GetCredentialCancellationException) {
            return false
        }
        if (credential !is CustomCredential || credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            throw IOException("Beklenmeyen kimlik türü")
        }
        val google = GoogleIdTokenCredential.createFrom(credential.data)
        val auth = post("/api/auth", null, AuthRequest(google.idToken), AuthRequest.serializer(), AuthResponse.serializer())
        settings.setSyncAccount(auth.token, google.id)
        // Yeni hesapta bütün yerel kayıtlar yüklenmeli.
        reviewDao.markAllUnsynced()
        return true
    }

    suspend fun signOut() {
        settings.clearSyncAccount()
        reviewDao.markAllUnsynced()
        _state.value = SyncState.Idle
    }

    /** Sunucudaki bütün tekrar kayıtlarını ve oturumları siler, sonra çıkış yapar. Cihazdaki ilerleme kalır. */
    suspend fun deleteRemoteData() {
        val account = settings.syncAccount.first() ?: return
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url(BuildConfig.SYNC_URL + "/api/account")
                .delete()
                .header("Authorization", "Bearer ${account.token}")
                .build()
            http.newCall(request).execute().use { response ->
                // 401: oturum zaten yok, sunucuda bu jetona bağlı veri kalmamış.
                if (!response.isSuccessful && response.code != 401) throw IOException("Sunucu hatası (${response.code})")
            }
        }
        signOut()
    }

    /** Giriş yapılmamışsa hiçbir şey yapmaz. */
    suspend fun sync(): SyncState = mutex.withLock {
        val account = settings.syncAccount.first() ?: return@withLock SyncState.Idle
        _state.value = SyncState.Running
        val result = try {
            var cursor = account.cursor
            var pushed = 0
            var pulled = 0
            do {
                val batch = reviewDao.unsynced(PAGE)
                val response = post(
                    "/api/sync", account.token,
                    SyncRequest(cursor, batch.map(BackupReview::of)),
                    SyncRequest.serializer(), SyncResponse.serializer(),
                )
                batch.map { it.id }.chunked(500).forEach { reviewDao.markSynced(it) }
                pulled += study.mergeReviews(response.reviews.map { it.toEntity(synced = true) })
                pushed += batch.size
                cursor = response.cursor
                settings.setSyncProgress(cursor, System.currentTimeMillis())
            } while (batch.size == PAGE || response.more)
            SyncState.Done(pushed, pulled)
        } catch (e: SessionExpiredException) {
            settings.clearSyncAccount()
            SyncState.Failed(e)
        } catch (e: IOException) {
            SyncState.Failed(e)
        } catch (e: IllegalArgumentException) {
            SyncState.Failed(e)
        }
        _state.value = result
        result
    }

    private suspend fun <Req, Res> post(
        path: String,
        token: String?,
        body: Req,
        requestSerializer: KSerializer<Req>,
        responseSerializer: KSerializer<Res>,
    ): Res = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(BuildConfig.SYNC_URL + path)
            .post(json.encodeToString(requestSerializer, body).toRequestBody(JSON))
            .apply { if (token != null) header("Authorization", "Bearer $token") }
            .build()
        http.newCall(request).execute().use { response ->
            if (response.code == 401) throw SessionExpiredException()
            if (!response.isSuccessful) throw IOException("Sunucu hatası (${response.code})")
            json.decodeFromString(responseSerializer, response.body!!.string())
        }
    }

    private companion object {
        const val PAGE = 500
        val JSON = "application/json; charset=utf-8".toMediaType()
    }
}
