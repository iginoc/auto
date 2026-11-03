package com.example.nanobluetooth.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.math.hypot
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
  var steeringSpeed by remember { mutableStateOf(180) }
  var tractionSpeed by remember { mutableStateOf(180) }
  var joystickPosition by remember { mutableStateOf(Offset.Zero) }
  var lastSteeringCommandSpeed by remember { mutableStateOf(0) }
  var lastTractionCommandSpeed by remember { mutableStateOf(0) }

  fun updateJoystick(position: Offset) {
    joystickPosition = position
    val deadZone = 0.16f

    fun signedAxisSpeed(value: Float, isSteering: Boolean): Int {
      if (kotlin.math.abs(value) <= deadZone) return 0

      val magnitude = (((kotlin.math.abs(value) - deadZone) / (1f - deadZone)) * 255f)
        .roundToInt()
        .coerceAtLeast(48)
        .let { (((it + 8) / 16) * 16).coerceAtMost(255) }
      val positiveDirection = if (isSteering) value < 0f else value > 0f
      return if (positiveDirection) magnitude else -magnitude
    }

    val steering = signedAxisSpeed(position.x, isSteering = true)
    val traction = signedAxisSpeed(-position.y, isSteering = false)
    if (steering != lastSteeringCommandSpeed || traction != lastTractionCommandSpeed) {
      controller.sendLine("JOY $steering $traction")
      lastSteeringCommandSpeed = steering
      lastTractionCommandSpeed = traction
    }
  }

  fun releaseJoystick() {
    controller.sendLine("STOP")
    joystickPosition = Offset.Zero
    lastSteeringCommandSpeed = 0
    lastTractionCommandSpeed = 0
  }

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
        Text("JOYSTICK", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
          DrivingJoystick(
            position = joystickPosition,
            onPositionChange = ::updateJoystick,
            onRelease = ::releaseJoystick,
          )
        }
        Text("STERZO · MOTOR A", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Button(onClick = { controller.sendLine("STEER_LEFT") }, modifier = Modifier.weight(1f)) { Text("SINISTRA") }
          OutlinedButton(onClick = { controller.sendLine("STEER_STOP") }, modifier = Modifier.weight(1f)) { Text("STOP STERZO") }
          Button(onClick = { controller.sendLine("STEER_RIGHT") }, modifier = Modifier.weight(1f)) { Text("DESTRA") }
        }
        Text("Velocità sterzo: $steeringSpeed / 255", style = MaterialTheme.typography.bodyMedium)
        Slider(
          value = steeringSpeed.toFloat(),
          onValueChange = { steeringSpeed = it.roundToInt() },
          onValueChangeFinished = { controller.sendLine("STEER_SPEED $steeringSpeed") },
          valueRange = 0f..255f,
          enabled = state.connectedName != null,
        )

        HorizontalDivider()
        Text("TRAZIONE · MOTOR B", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Button(onClick = { controller.sendLine("DRIVE_BACKWARD") }, modifier = Modifier.weight(1f)) { Text("INDIETRO") }
          OutlinedButton(onClick = { controller.sendLine("DRIVE_STOP") }, modifier = Modifier.weight(1f)) { Text("STOP TRAZIONE") }
          Button(onClick = { controller.sendLine("DRIVE_FORWARD") }, modifier = Modifier.weight(1f)) { Text("AVANTI") }
        }
        Text("Velocità trazione: $tractionSpeed / 255", style = MaterialTheme.typography.bodyMedium)
        Slider(
          value = tractionSpeed.toFloat(),
          onValueChange = { tractionSpeed = it.roundToInt() },
          onValueChangeFinished = { controller.sendLine("TRACTION_SPEED $tractionSpeed") },
          valueRange = 0f..255f,
          enabled = state.connectedName != null,
        )

        Button(onClick = { controller.sendLine("STOP") }, modifier = Modifier.fillMaxWidth()) { Text("ARRESTA TUTTO") }
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

@Composable
private fun DrivingJoystick(
  position: Offset,
  onPositionChange: (Offset) -> Unit,
  onRelease: () -> Unit,
) {
  val surfaceColor = MaterialTheme.colorScheme.surfaceVariant
  val outlineColor = MaterialTheme.colorScheme.outline
  val thumbColor = MaterialTheme.colorScheme.primary
  val thumbOutlineColor = MaterialTheme.colorScheme.onPrimary

  Canvas(
    modifier = Modifier
      .size(240.dp)
      .pointerInput(Unit) {
        fun normalizedPosition(point: Offset): Offset {
          val centerX = size.width / 2f
          val centerY = size.height / 2f
          val maxTravel = minOf(size.width, size.height) * 0.32f
          val dx = (point.x - centerX) / maxTravel
          val dy = (point.y - centerY) / maxTravel
          val distance = hypot(dx, dy)
          val scale = if (distance > 1f) 1f / distance else 1f
          return Offset(dx * scale, dy * scale)
        }

        awaitEachGesture {
          val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
          down.consume()
          onPositionChange(normalizedPosition(down.position))

          var isPressed = true
          while (isPressed) {
            val event = awaitPointerEvent(pass = PointerEventPass.Initial)
            val change = event.changes.firstOrNull { it.id == down.id } ?: break
            onPositionChange(normalizedPosition(change.position))
            change.consume()
            isPressed = change.pressed
          }
          onRelease()
        }
      },
  ) {
    val center = Offset(size.width / 2f, size.height / 2f)
    val radius = size.minDimension * 0.44f
    val travel = size.minDimension * 0.32f
    val thumbRadius = size.minDimension * 0.12f
    val thumbCenter = center + Offset(position.x * travel, position.y * travel)

    drawCircle(color = surfaceColor, radius = radius, center = center)
    drawCircle(color = outlineColor, radius = radius, center = center, style = Stroke(width = 3.dp.toPx()))
    drawLine(
      color = outlineColor.copy(alpha = 0.45f),
      start = Offset(center.x - radius * 0.55f, center.y),
      end = Offset(center.x + radius * 0.55f, center.y),
      strokeWidth = 2.dp.toPx(),
    )
    drawLine(
      color = outlineColor.copy(alpha = 0.45f),
      start = Offset(center.x, center.y - radius * 0.55f),
      end = Offset(center.x, center.y + radius * 0.55f),
      strokeWidth = 2.dp.toPx(),
    )
    drawCircle(color = thumbColor, radius = thumbRadius, center = thumbCenter)
    drawCircle(color = thumbOutlineColor, radius = thumbRadius, center = thumbCenter, style = Stroke(width = 2.dp.toPx()))
  }
}