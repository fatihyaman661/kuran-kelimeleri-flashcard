package com.mevlutcelik.kelimeler.presentation

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mevlutcelik.kelimeler.data.AppStartup
import com.mevlutcelik.kelimeler.data.backup.BackupRepository
import com.mevlutcelik.kelimeler.data.settings.SettingsRepository
import com.mevlutcelik.kelimeler.data.settings.ThemeMode
import com.mevlutcelik.kelimeler.data.sync.SyncRepository
import com.mevlutcelik.kelimeler.di.ApplicationScope
import com.mevlutcelik.kelimeler.presentation.navigation.KelimelerApp
import com.mevlutcelik.kelimeler.presentation.theme.Background
import com.mevlutcelik.kelimeler.presentation.theme.KelimelerTheme
import com.mevlutcelik.kelimeler.presentation.theme.OnSurface
import com.mevlutcelik.kelimeler.presentation.theme.TextScales
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var startup: AppStartup
    @Inject lateinit var backup: BackupRepository
    @Inject lateinit var sync: SyncRepository
    @Inject lateinit var settings: SettingsRepository
    @Inject @ApplicationScope lateinit var appScope: CoroutineScope

    override fun onCreate(savedInstanceState: Bundle?) {
        applySystemBars(dark = true)
        super.onCreate(savedInstanceState)
        startup.start()
        setContent {
            // Yazı boyutu ayarı okunana kadar (ilk kare) yalnızca zemin çizilir; yazılar sonradan zıplamaz.
            val display by settings.display.collectAsStateWithLifecycle(initialValue = null)
            val scales = display?.let { TextScales(it.arabicScale, it.latinScale) }
            val dark = when (display?.theme ?: ThemeMode.Dark) {
                ThemeMode.System -> isSystemInDarkTheme()
                ThemeMode.Light -> false
                ThemeMode.Dark -> true
            }
            LaunchedEffect(dark) { applySystemBars(dark) }
            KelimelerTheme(dark, scales ?: TextScales()) {
                // Kök yüzey metin ve simgelere açık içerik rengini verir.
                Surface(Modifier.fillMaxSize(), color = Background, contentColor = OnSurface) {
                    if (scales != null) KelimelerApp()
                }
            }
        }
    }

    /** Durum ve gezinme çubuğu saydam kalır; simgeleri temaya göre açık ya da koyu çizilir. */
    private fun applySystemBars(dark: Boolean) {
        val style = if (dark) SystemBarStyle.dark(Color.TRANSPARENT) else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
    }

    override fun onStop() {
        super.onStop()
        // Uygulamadan çıkılınca ilerlemeyi Android yedeğine yaz ve girişliyse senkronla.
        appScope.launch {
            backup.writeAutoBackup()
            sync.sync()
        }
    }
}
