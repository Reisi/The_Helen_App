package com.thomasR.helen.ui

import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.thomasR.helen.demo.DummyDevices
import com.thomasR.helen.repository.MainRepository
import com.thomasR.helen.repository.Theme
import com.thomasR.helen.repository.UserPreferencesRepository
import com.thomasR.helen.service.HelenService

enum class Screen() {
    SearchDeviceList,
    DeviceTap
}

@Composable
fun MainBase(
    modifier: Modifier = Modifier,
    connect: (String, String?) -> Unit,
    disconnect: (String) -> Unit,
    mainRepository: MainRepository,
    //serviceIBinder: HelenService.LocalBinder,
    //prefRepository: UserPreferencesRepository,
    baseViewModel: MainBaseViewModel // = viewModel(factory = MainBaseViewModel.Companion.Factory(prefRepository))
) {
    val navController: NavHostController = rememberNavController()
    //val mainRepository = serviceIBinder.getRepository()
    val baseData by baseViewModel.data.collectAsState()

    NavHost(
        modifier = modifier.fillMaxSize(),
        navController = navController,
        startDestination = "SearchDeviceList"
    ) {
        composable(route = "SearchDeviceList") {
            Log.d("MainBase", "mainRepository hash ${mainRepository.hashCode()}")
            SearchScreen (
                //modifier = Modifier.fillMaxSize(),
                repository = mainRepository,
                dummyDevices = DummyDevices().devices,
                addDummyDevice = {
                    mainRepository.addDevice(
                        device = it.helenData,
                        info = it.deviceInformationData,
                        hpsData = it.hpsData,
                        kd2Data = it.kD2Data
                    )
                },
                theme = baseData?.theme ?: Theme.SYSTEM_DEFAULT,
                changeTheme = { baseViewModel.changeTheme(it) },
                connectDevice = connect, //{address, name -> serviceIBinder.connectDevice(address, name) },
                disconnectDevice = disconnect //{ serviceIBinder.disconnectDevice(it) },
            ){
                navController.navigate(route = "device/${it.data.value.address}")
            }
        }

        composable(
            route = "device/{deviceAddress}",
            arguments = listOf(navArgument("deviceAddress") {type = NavType.StringType})
        ) { navBackStackEntry ->
            val deviceAddress = navBackStackEntry.arguments?.getString("deviceAddress")
            val deviceRepository =
                if (deviceAddress != null)
                    mainRepository.getDeviceRepository(deviceAddress)
                else null
            if (deviceRepository != null)
                TabScreen(
                    //modifier = Modifier.fillMaxSize(),
                    repository = deviceRepository,
                    navigateBack = { navController.navigateUp() },
                    reconnect = { connect(it, null) } // {serviceIBinder.connectDevice(it)}
                )
            else
                navController.navigate(route = "SearchDeviceList")
        }

        /*composable(
            route = "dfu/{deviceAddress}",
            arguments = listOf(navArgument("deviceAddress") {type = NavType.StringType})
        ) {
            val deviceAddress = it.arguments?.getString("deviceAddress")
            if (deviceAddress == null) {
                navController.navigateUp()
            } else {
                DfuScreen(address = deviceAddress)
            }
        //Text(text = "dfu screen for device $deviceAddress")
        }*/
    }
}