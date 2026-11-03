# Nano Bluetooth

App Android e firmware Arduino Nano per comunicare con un modulo Bluetooth classico HC-06 tramite profilo seriale SPP.

Per il pinout completo, il protocollo, lo stato degli upload e le istruzioni per riprendere lo sviluppo, consulta [HANDOFF.md](HANDOFF.md).

## Collegamenti

- HC-06 TXD -> Arduino Nano D4 (RX di `SoftwareSerial`)
- HC-06 RXD <- Arduino Nano D2 (TX di `SoftwareSerial`), usando un partitore di tensione per ridurre il segnale a 3,3 V
- HC-06 GND -> Arduino GND
- HC-06 VCC -> alimentazione prevista dalla breakout del modulo
- L298N ENA -> Arduino Nano D3 (PWM)
- L298N IN1 -> Arduino Nano D5
- L298N IN2 -> Arduino Nano D6
- L298N IN3 -> Arduino Nano D10
- L298N IN4 -> Arduino Nano D11
- L298N ENB -> Arduino Nano D9 (PWM)
- D7 e D8 non sono usati con questa mappatura
- Collega GND dell'alimentazione motori, GND del L298N e GND del Nano in comune. Alimenta i motori dal driver, non dal pin 5 V del Nano.

In questo cablaggio ENA ed ENB sono collegati direttamente a D3 e D9 per il PWM. Non rimuovere altri ponticelli per questa configurazione: `5V_EN`, `CSA`, `CSB`, `UR1`, `UR2` e `UR3` non sono i jumper ENA/ENB. Lo sketch usa 9600 baud e controlla anche il LED integrato sul pin `LED_BUILTIN`.

## Comandi motori

Con HC-06 connesso, l'app offre un joystick: sinistra/destra controllano MotorA (sterzo), su/giù controllano MotorB (trazione), e le diagonali li combinano. La velocità segue la distanza dal centro; quando si rilascia il joystick, `STOP` arresta entrambi i motori.

Durante il movimento il joystick invia un frame `JOY <sterzo> <trazione>` con valori firmati da -255 a 255, senza attendere una risposta Bluetooth per ogni aggiornamento: così si riducono le collisioni sulla seriale software dell'HC-06.

L'app offre anche i pulsanti e i cursori manuali per i due ruoli:

- **MotorA / sterzo**: `STEER_LEFT`, `STEER_RIGHT`, `STEER_STOP` e velocità `STEER_SPEED 0..255`.
- **MotorB / trazione**: `DRIVE_FORWARD`, `DRIVE_BACKWARD`, `DRIVE_STOP` e velocità `TRACTION_SPEED 0..255`.
- `STOP` arresta entrambi i motori. `LED ON` e `LED OFF` controllano il LED integrato.

I comandi devono terminare con newline. Gli alias `LEFT`, `RIGHT`, `FORWARD`, `BACKWARD` e `SPEED 0..255` sono mantenuti: i primi due agiscono solo sullo sterzo, gli altri solo sulla trazione. Ogni motore resta attivo fino al proprio comando di stop o a `STOP` generale. Se il verso fisico dello sterzo è invertito, scambia i due fili del MotorA sulle uscite del driver.

## Avvio

1. Apri `firmware/NanoBluetooth/NanoBluetooth.ino` con Arduino IDE e caricalo sul Nano.
2. Associa HC-06 dalle impostazioni Bluetooth del telefono. Il PIN predefinito dipende dal modulo, spesso `1234` o `0000`.
3. Apri il progetto `android-app` in Android Studio oppure esegui `./gradlew assembleDebug` dalla cartella `android-app`.
4. Nell'app concedi il permesso Dispositivi nelle vicinanze, aggiorna l'elenco e connettiti a HC-06.
5. Usa i comandi separati per sterzo e trazione oppure invia `LED ON` o `LED OFF`.

L'app richiede un telefono Android con Bluetooth classico; l'emulatore Android non può sostituire un collegamento reale con HC-06.

## Sviluppo

Dalla directory `android-app/`, compila l'APK ed esegui i test unitari con:

```bash
./gradlew assembleDebug testDebugUnitTest
```

Il percorso dell'Arduino CLI incluso in Arduino IDE, i comandi completi di compilazione/upload e la variante bootloader usata sono riportati in [HANDOFF.md](HANDOFF.md).