# Nano Bluetooth

App Android e firmware Arduino Nano per comunicare con un modulo Bluetooth classico HC-06 tramite profilo seriale SPP.

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

Rimuovi i jumper ENA/ENB del modulo L298N se presenti, altrimenti i pin PWM del Nano non controlleranno la velocità. Lo sketch usa 9600 baud e controlla anche il LED integrato sul pin `LED_BUILTIN`.

## Comandi motori

Con HC-06 connesso, l'app mostra i pulsanti Avanti, Indietro, Sinistra, Destra e Stop, oltre allo slider velocità (0-255). La direzione resta attiva fino a un altro comando o a Stop.

Il protocollo seriale accetta `FORWARD`, `BACKWARD`, `LEFT`, `RIGHT`, `STOP` e `SPEED 0..255`, ciascuno terminato da newline. I comandi `LED ON` e `LED OFF` restano disponibili.

## Avvio

1. Apri `firmware/NanoBluetooth/NanoBluetooth.ino` con Arduino IDE e caricalo sul Nano.
2. Associa HC-06 dalle impostazioni Bluetooth del telefono. Il PIN predefinito dipende dal modulo, spesso `1234` o `0000`.
3. Apri il progetto `android-app` in Android Studio oppure esegui `./gradlew assembleDebug` dalla cartella `android-app`.
4. Nell'app concedi il permesso Dispositivi nelle vicinanze, aggiorna l'elenco e connettiti a HC-06.
5. Invia `LED ON` o `LED OFF`; puoi anche inviare altri comandi testuali terminati da newline.

L'app richiede un telefono Android con Bluetooth classico; l'emulatore Android non può sostituire un collegamento reale con HC-06.