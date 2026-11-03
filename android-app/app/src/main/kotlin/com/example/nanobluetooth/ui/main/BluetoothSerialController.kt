package com.example.nanobluetooth.ui.main

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.IOException
import java.util.UUID
import java.util.concurrent.Executors

data class PairedBluetoothDevice(
  val name: String,
  val address: String,
  val device: BluetoothDevice,
)

data class BluetoothScreenState(
  val pairedDevices: List<PairedBluetoothDevice> = emptyList(),
  val status: String = "Associa HC-06 e aggiorna l'elenco",
  val connecting: Boolean = false,
  val connectedName: String? = null,
  val receivedLines: List<String> = emptyList(),
)

class BluetoothSerialController {
  private val worker = Executors.newCachedThreadPool()
  private val writer = Executors.newSingleThreadExecutor()
  private val mutableState = MutableStateFlow(BluetoothScreenState())
  val state: StateFlow<BluetoothScreenState> = mutableState

  @Volatile private var socket: BluetoothSocket? = null

  fun setStatus(status: String) {
    mutableState.value = mutableState.value.copy(status = status)
  }

  fun setPairedDevices(devices: List<BluetoothDevice>) {
    val paired = devices
      .map { device -> PairedBluetoothDevice(device.name ?: "Dispositivo Bluetooth", device.address, device) }
      .sortedBy { it.name.lowercase() }
    mutableState.value = mutableState.value.copy(
      pairedDevices = paired,
      status = if (paired.isEmpty()) "Nessun dispositivo associato: associa HC-06 dalle impostazioni" else "Seleziona HC-06 per connetterti",
    )
  }

  fun connect(device: BluetoothDevice) {
    mutableState.value = mutableState.value.copy(connecting = true, status = "Connessione a ${device.name ?: "HC-06"}... ")
    worker.execute {
      try {
        val candidate = device.createRfcommSocketToServiceRecord(SPP_UUID)
        candidate.connect()
        val oldSocket = socket
        socket = candidate
        oldSocket?.close()
        mutableState.value = mutableState.value.copy(
          connecting = false,
          connectedName = device.name ?: "HC-06",
          status = "Connesso tramite Bluetooth classico",
          receivedLines = emptyList(),
        )
        worker.execute { readLines(candidate) }
      } catch (error: IOException) {
        mutableState.value = mutableState.value.copy(connecting = false, connectedName = null, status = "Connessione non riuscita: ${error.message ?: "controlla HC-06"}")
      } catch (error: SecurityException) {
        mutableState.value = mutableState.value.copy(connecting = false, connectedName = null, status = "Permesso Bluetooth non disponibile")
      }
    }
  }

  fun disconnect() {
    val activeSocket = socket
    socket = null
    worker.execute {
      try {
        activeSocket?.close()
      } catch (_: IOException) {
      }
    }
    mutableState.value = mutableState.value.copy(connecting = false, connectedName = null, status = "Disconnesso")
  }

  fun sendLine(line: String) {
    val activeSocket = socket ?: return
    val message = line.trim()
    if (message.isEmpty()) return
    writer.execute {
      try {
        activeSocket.outputStream.write("$message\n".toByteArray(Charsets.UTF_8))
        activeSocket.outputStream.flush()
        appendLine("> $message")
      } catch (error: IOException) {
        mutableState.value = mutableState.value.copy(status = "Invio fallito: ${error.message ?: "connessione interrotta"}")
      }
    }
  }

  fun close() {
    try {
      socket?.close()
    } catch (_: IOException) {
    }
    socket = null
    worker.shutdownNow()
    writer.shutdownNow()
  }

  private fun readLines(activeSocket: BluetoothSocket) {
    try {
      val reader = BufferedReader(InputStreamReader(activeSocket.inputStream, Charsets.UTF_8))
      while (activeSocket == socket) {
        val line = reader.readLine() ?: break
        appendLine("< $line")
      }
    } catch (_: IOException) {
      // Socket closure ends the reader loop.
    } finally {
      if (activeSocket == socket) {
        socket = null
        mutableState.value = mutableState.value.copy(connectedName = null, status = "Connessione interrotta")
      }
    }
  }

  private fun appendLine(line: String) {
    val lines = mutableState.value.receivedLines + line
    mutableState.value = mutableState.value.copy(receivedLines = lines.takeLast(10))
  }

  private companion object {
    val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
  }
}