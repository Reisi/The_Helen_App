package com.thomasR.helen

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModelProvider
import com.thomasR.helen.repository.MainRepository
import com.thomasR.helen.repository.Theme
import com.thomasR.helen.repository.UserPreferencesRepository
import com.thomasR.helen.service.HelenService
import com.thomasR.helen.ui.MainBase
import com.thomasR.helen.ui.MainBaseViewModel
import com.thomasR.helen.ui.theme.HelenTheme
import kotlinx.coroutines.flow.MutableStateFlow
import no.nordicsemi.android.dfu.DfuServiceInitiator.createDfuNotificationChannel


private const val USER_PREFERENCES_NAME = "user_preferences"

private val Context.dataStore by preferencesDataStore(
    name = USER_PREFERENCES_NAME
)

class MainActivity : ComponentActivity() {
    private var binder = MutableStateFlow<HelenService.LocalBinder?>(null)
    private var mainRepository: MainRepository? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(p0: ComponentName?, ibinder: IBinder?) {
            binder.value = ibinder as HelenService.LocalBinder
            if (mainRepository == null)
                mainRepository = binder.value!!.getRepository()
            else
                binder.value!!.setRepository(mainRepository!!)
        }

        override fun onServiceDisconnected(p0: ComponentName?) {
            binder.value = null
        }

    }

    override fun onStart() {
        super.onStart()

        val intent = Intent(this, HelenService::class.java)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onStop() {
        super.onStop()

        unbindService(serviceConnection)
    }

    override fun onDestroy() {
        super.onDestroy()

        // stop service on destroy to prevent it running without app
        val intent = Intent(this, HelenService::class.java)
        stopService(intent)
    }

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // limit screen orientation to portrait for now
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)

        val intent = Intent(this, HelenService::class.java)
        startService(intent)

        val prefRepository = UserPreferencesRepository(dataStore)
        val baseViewModel = ViewModelProvider(
            owner = this,
            factory = MainBaseViewModel.Companion.Factory(prefRepository)
        ).get(MainBaseViewModel::class.java)

        installSplashScreen().apply {
            setKeepOnScreenCondition{
                /*binder.value == null*/mainRepository == null || baseViewModel.data.value == null

            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createDfuNotificationChannel(this)
        }

        /*val isDarkTheme = when(baseViewModel.data.value?.theme ?: Theme.SYSTEM_DEFAULT) {
            Theme.SYSTEM_DEFAULT -> isSystemInDarkTheme()
            Theme.DARK -> true
            Theme.BRIGHT -> false
        }*/

        enableEdgeToEdge()
            /*statusBarStyle = SystemBarStyle.auto(Color.Transparent.toArgb(), Color.Transparent.toArgb()) {
                it.d
            }
        )*/

        setContent {
            val sBinder by binder.collectAsState()
            if (/*isBounded*/ sBinder != null) {
                val data by baseViewModel.data.collectAsState()
                HelenTheme (theme = data?.theme ?: Theme.SYSTEM_DEFAULT) {
                    // A surface container using the 'background' color from the theme
                    Surface(
                        //modifier = Modifier.width(200.dp),//.safeDrawingPadding(),// fillMaxSize(),// .windowInsetsTopHeight(WindowInsets.navigationBars),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        MainBase(
                            connect = { address: String, name: String? -> sBinder!!.connectDevice(address, name) },
                            disconnect = { address: String -> sBinder!!.disconnectDevice(address) },
                            mainRepository = mainRepository!!,
                            baseViewModel = baseViewModel
                        )
                    }
                }
            }
        }
    }
}

