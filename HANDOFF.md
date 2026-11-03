# Nano Bluetooth Control Handoff

## Project

This workspace contains an Android app and an Arduino Nano sketch for an HC-06 Bluetooth Classic serial link and an L298N motor driver.

- Android project: `android-app/`
- Arduino sketch: `firmware/NanoBluetooth/NanoBluetooth.ino`
- User-facing overview: `README.md`

## Hardware Wiring

HC-06 to Nano:

- HC-06 TXD -> Nano D4 (`SoftwareSerial` RX)
- HC-06 RXD <- Nano D2 (`SoftwareSerial` TX), through a voltage divider to keep the signal at 3.3 V
- HC-06 GND -> Nano GND
- HC-06 VCC -> the supply supported by the specific breakout board
- Serial baud rate: 9600

L298N to Nano:

- ENA -> D3 (PWM)
- IN1 -> D5
- IN2 -> D6
- IN3 -> D10
- IN4 -> D11
- ENB -> D9 (PWM)
- D7 and D8 are unused
- MotorA is steering; MotorB is traction.

Keep the Nano, L298N, and motor-supply grounds common. Power motors from the driver supply, not the Nano 5 V pin. The user reports ENA and ENB are wired directly to D3 and D9 and has no other removable jumpers. Do not remove or alter the board's `5V_EN`, `CSA`, `CSB`, `UR1`, `UR2`, or `UR3` jumpers based only on their labels.

## Bluetooth Protocol

Commands are UTF-8 text lines terminated by newline. The joystick sends one combined command:

```text
JOY <steering> <traction>
```

Each signed value is in `-255..255`. Zero stops that motor; positive MotorA is the app's left steering direction and positive MotorB is forward. The Android joystick maps left/right to MotorA and up/down to MotorB; diagonal positions control both. Speed follows displacement from center. Releasing the joystick sends `STOP` to stop both motors.

Valid `JOY` frames intentionally receive no HC-06 reply. This avoids continuous reply traffic while the Nano uses half-duplex `SoftwareSerial`. Manual commands still return `OK` or `ERR` replies:

- Steering: `STEER_LEFT`, `STEER_RIGHT`, `STEER_STOP`, `STEER_SPEED 0..255`
- Traction: `DRIVE_FORWARD`, `DRIVE_BACKWARD`, `DRIVE_STOP`, `TRACTION_SPEED 0..255`
- `STOP` stops both motors; `LED ON` and `LED OFF` control the built-in LED.
- Legacy aliases remain accepted: `LEFT`, `RIGHT`, `FORWARD`, `BACKWARD`, and `SPEED 0..255`.

## Build and Upload

Android build and unit tests:

```bash
./gradlew assembleDebug testDebugUnitTest
```

Run that command from `android-app/`. The debug APK is written to `android-app/app/build/outputs/apk/debug/app-debug.apk`.

Arduino CLI is bundled with Arduino IDE at `/home/igino/arduino-ide/resources/app/lib/backend/resources/arduino-cli`:

```bash
/home/igino/arduino-ide/resources/app/lib/backend/resources/arduino-cli compile --fqbn arduino:avr:nano --warnings all firmware/NanoBluetooth
/home/igino/arduino-ide/resources/app/lib/backend/resources/arduino-cli upload --verbose --fqbn arduino:avr:nano:cpu=atmega328old --port /dev/ttyUSB0 firmware/NanoBluetooth
```

Use the old-bootloader FQBN for this board. The latest recorded upload completed successfully: `avrdude` verified the ATmega328P and wrote 6,058 bytes to flash on `/dev/ttyUSB0`.

Install and launch the APK on the Pixel 7 when it is connected:

```bash
android run --apks=/home/igino/PlatformIO/arduino/auto/android-app/app/build/outputs/apk/debug/app-debug.apk --device=32081FDH2003S5
```

## Current Status

- Android `assembleDebug` and `testDebugUnitTest` passed after the atomic joystick protocol change.
- The updated APK was installed and launched on the Pixel 7.
- The updated firmware compiled and was uploaded successfully; `avrdude` reported 6,058 bytes written.
- The installed app may need to reconnect to HC-06 after the Nano resets during upload.
- End-to-end physical joystick movement was not confirmed after the latest protocol update. If commands are still rejected, check the app's outgoing `> JOY ...` line and verify the Nano was flashed with this sketch.