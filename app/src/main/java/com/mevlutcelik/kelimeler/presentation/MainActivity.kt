package com.mevlutcelik.kelimeler.presentation

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.mevlutcelik.kelimeler.data.AppStartup
import com.mevlutcelik.kelimeler.data.backup.BackupRepository
import com.mevlutcelik.kelimeler.data.sync.SyncRepository
import com.mevlutcelik.kelimeler.di.ApplicationScope
import com.mevlutcelik.kelimeler.presentation.navigation.KelimelerApp
import com.mevlutcelik.kelimeler.presentation.theme.Background
import com.mevlutcelik.kelimeler.presentation.theme.KelimelerTheme
import com.mevlutcelik.kelimeler.presentation.theme.OnSurface
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var startup: AppStartup
    @Inject lateinit var backup: BackupRepository
    @Inject lateinit var sync: SyncRepository
    @Inject @ApplicationScope lateinit var appScope: CoroutineScope

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        startup.start()
        setContent {
            KelimelerTheme {
                // Kök yüzey metin ve simgelere açık içerik rengini verir.
                Surface(Modifier.fillMaxSize(), color = Background, contentColor = OnSurface) {
                    KelimelerApp()
                }
            }
        }
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
