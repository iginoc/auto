package com.example.nanobluetooth.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BluetoothScreen(
  controller: BluetoothSerialController,
  onRefresh: () -> Unit,
  onPairDevice: () -> Unit,
  onEnableBluetooth: () -> Unit,
) {
  val state by controller.state.collectAsStateWithLifecycle()
  var message by remember { mutableStateOf("") }
  var motorSpeed by remember { mutableStateOf(180) }

  Scaffold(
    topBar = { TopAppBar(title = { Text("Nano Bluetooth") }) },
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Text(
        state.connectedName?.let { "Connesso a $it" } ?: state.status,
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.Medium,
      )
      if (state.connectedName == null) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedButton(onClick = onPairDevice, modifier = Modifier.weight(1f)) { Text("Associa HC-06") }
          OutlinedButton(onClick = onEnableBluetooth, modifier = Modifier.weight(1f)) { Text("Attiva Bluetooth") }
        }
        OutlinedButton(onClick = onRefresh, modifier = Modifier.fillMaxWidth()) { Text("Aggiorna elenco") }

        Text("DISPOSITIVI ASSOCIATI", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        if (state.pairedDevices.isEmpty()) {
          Text("Associa il modulo dalle impostazioni Bluetooth del telefono, poi aggiorna l'elenco.", style = MaterialTheme.typography.bodyMedium)
        } else {
          state.pairedDevices.forEach { paired ->
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(paired.name, fontWeight = FontWeight.SemiBold)
                Text(paired.address, style = MaterialTheme.typography.bodySmall)
              }
              Button(
                onClick = { controller.connect(paired.device) },
                enabled = !state.connecting,
              ) { Text(if (state.connecting) "Connessione..." else "Connetti") }
            }
            HorizontalDivider()
          }
        }
      }

      if (state.connectedName != null) {
        Text("COMANDI MOTORI L298N", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
          Button(onClick = { controller.sendLine("FORWARD") }, modifier = Modifier.fillMaxWidth(0.58f)) { Text("AVANTI") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Button(onClick = { controller.sendLine("LEFT") }, modifier = Modifier.weight(1f)) { Text("SINISTRA") }
          Button(onClick = { controller.sendLine("STOP") }, modifier = Modifier.weight(1f)) { Text("STOP") }
          Button(onClick = { controller.sendLine("RIGHT") }, modifier = Modifier.weight(1f)) { Text("DESTRA") }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
          Button(onClick = { controller.sendLine("BACKWARD") }, modifier = Modifier.fillMaxWidth(0.58f)) { Text("INDIETRO") }
        }
        Text("Velocità: $motorSpeed / 255", style = MaterialTheme.typography.bodyMedium)
        Slider(
          value = motorSpeed.toFloat(),
          onValueChange = { motorSpeed = it.roundToInt() },
          onValueChangeFinished = { controller.sendLine("SPEED $motorSpeed") },
          valueRange = 0f..255f,
          enabled = state.connectedName != null,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedButton(onClick = { controller.sendLine("LED ON") }, modifier = Modifier.weight(1f)) { Text("LED ON") }
          OutlinedButton(onClick = { controller.sendLine("LED OFF") }, modifier = Modifier.weight(1f)) { Text("LED OFF") }
        }
        OutlinedButton(onClick = controller::disconnect, modifier = Modifier.fillMaxWidth()) { Text("Disconnetti") }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = message,
            onValueChange = { message = it },
            modifier = Modifier.weight(1f),
            label = { Text("Comando") },
            singleLine = true,
          )
          Button(
            onClick = { controller.sendLine(message); message = "" },
            enabled = message.isNotBlank(),
          ) { Text("Invia") }
        }
      }

      Spacer(Modifier.height(2.dp))
      Text("SERIALE", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
      if (state.receivedLines.isEmpty()) {
        Text("In attesa di dati", style = MaterialTheme.typography.bodyMedium)
      } else {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          state.receivedLines.forEach { line ->
            Text(line, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodyMedium)
          }
        }
      }
    }
  }
}