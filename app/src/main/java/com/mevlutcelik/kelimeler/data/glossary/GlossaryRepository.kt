package com.mevlutcelik.kelimeler.data.glossary

import android.content.Context
import android.util.Log
import com.mevlutcelik.kelimeler.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Istılahat metninin tek kaynağı bu depodaki data/istilahat.md'dir. Derlemede uygulamanın içine konur;
 * açılışta GitHub'daki güncel hâli indirilip cihaza kaydedilir, böylece metindeki düzeltmeler
 * yeni sürüm beklemeden gelir. İnen dosya boş ya da bozuksa eldeki metin kalır.
 */
@Singleton
class GlossaryRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val http: OkHttpClient,
) {
    private val mutex = Mutex()
    private val cache get() = File(context.filesDir, FILE)

    private val _glossary = MutableStateFlow<Glossary?>(null)
    /** Henüz okunmadıysa null; [load] ile doldurulur. */
    val glossary: StateFlow<Glossary?> = _glossary.asStateFlow()

    /** İndirilmiş kopya varsa onu, yoksa uygulamayla geleni okur. */
    suspend fun load() = mutex.withLock {
        if (_glossary.value != null) return@withLock
        _glossary.value = withContext(Dispatchers.IO) {
            val downloaded = cache.takeIf { it.exists() }?.readText()?.let(GlossaryParser::parse)
            downloaded?.takeUnless { it.isEmpty }
                ?: GlossaryParser.parse(context.assets.open(FILE).bufferedReader().use { it.readText() })
        }
    }

    /** GitHub'daki güncel metni indirir; başarısızsa sessizce eldekiyle devam eder. */
    suspend fun refresh(): Boolean = mutex.withLock {
        try {
            val text = withContext(Dispatchers.IO) {
                val request = Request.Builder()
                    .url(BuildConfig.DECK_BASE_URL + FILE)
                    .cacheControl(CacheControl.FORCE_NETWORK)
                    .build()
                http.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw IOException("HTTP ${response.code}: $FILE")
                    response.body!!.string()
                }
            }
            val parsed = GlossaryParser.parse(text)
            if (parsed.isEmpty) return@withLock false
            withContext(Dispatchers.IO) {
                val temp = File(context.filesDir, "$FILE.tmp")
                temp.writeText(text)
                if (!temp.renameTo(cache)) {
                    temp.delete()
                    throw IOException("$FILE kaydedilemedi")
                }
            }
            _glossary.value = parsed
            true
        } catch (e: IOException) {
            Log.w(TAG, "Istılahat güncellenemedi", e)
            false
        }
    }

    private companion object {
        const val FILE = "istilahat.md"
        const val TAG = "GlossaryRepository"
    }
}
