package com.example.nanobluetooth

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.nanobluetooth.ui.main.BluetoothScreen
import com.example.nanobluetooth.ui.main.BluetoothSerialController
import com.example.nanobluetooth.ui.theme.NanoBluetoothTheme

class MainActivity : ComponentActivity() {
  private lateinit var controller: BluetoothSerialController
  private var pendingPermissionAction: (() -> Unit)? = null

  private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
    val action = pendingPermissionAction
    pendingPermissionAction = null
    if (granted) action?.invoke() else controller.setStatus("Permesso Bluetooth negato")
  }

  private val enableBluetoothLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
    refreshPairedDevices()
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    controller = BluetoothSerialController()

    setContent {
      NanoBluetoothTheme {
        BluetoothScreen(
          controller = controller,
          onRefresh = { withConnectPermission(::refreshPairedDevices) },
          onPairDevice = { startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) },
          onEnableBluetooth = { enableBluetooth() },
        )
      }
    }
  }

  override fun onResume() {
    super.onResume()
    if (::controller.isInitialized && hasConnectPermission()) refreshPairedDevices()
  }

  override fun onDestroy() {
    if (::controller.isInitialized) controller.close()
    super.onDestroy()
  }

  private fun enableBluetooth() {
    withConnectPermission {
      val adapter = bluetoothAdapter()
      when {
        adapter == null -> controller.setStatus("Questo dispositivo non supporta Bluetooth")
        adapter.isEnabled -> refreshPairedDevices()
        else -> enableBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
      }
    }
  }

  private fun refreshPairedDevices() {
    val adapter = bluetoothAdapter()
    when {
      adapter == null -> controller.setStatus("Questo dispositivo non supporta Bluetooth")
      !adapter.isEnabled -> controller.setStatus("Bluetooth disattivato. Attivalo per continuare")
      else -> try {
        controller.setPairedDevices(adapter.bondedDevices.toList())
      } catch (_: SecurityException) {
        controller.setStatus("Concedi il permesso Dispositivi nelle vicinanze")
      }
    }
  }

  private fun withConnectPermission(action: () -> Unit) {
    if (hasConnectPermission()) {
      action()
    } else {
      pendingPermissionAction = action
      permissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
    }
  }

  private fun hasConnectPermission(): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
      ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED

  private fun bluetoothAdapter(): BluetoothAdapter? =
    getSystemService(BluetoothManager::class.java)?.adapter
}
