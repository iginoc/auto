#include <SoftwareSerial.h>

const byte HC06_RX_PIN = 4;
const byte HC06_TX_PIN = 2;
const unsigned long HC06_BAUD = 9600;
const size_t COMMAND_BUFFER_SIZE = 64;
const byte MOTOR_A_ENABLE_PIN = 3;
const byte MOTOR_A_IN1_PIN = 5;
const byte MOTOR_A_IN2_PIN = 6;
const byte MOTOR_B_IN1_PIN = 10;
const byte MOTOR_B_IN2_PIN = 11;
const byte MOTOR_B_ENABLE_PIN = 9;

SoftwareSerial hc06(HC06_RX_PIN, HC06_TX_PIN);
char commandBuffer[COMMAND_BUFFER_SIZE];
size_t commandLength = 0;
byte motorSpeed = 180;

void setMotor(int speed, byte enablePin, byte in1Pin, byte in2Pin) {
  if (speed > 0) {
    digitalWrite(in1Pin, HIGH);
    digitalWrite(in2Pin, LOW);
  } else if (speed < 0) {
    digitalWrite(in1Pin, LOW);
    digitalWrite(in2Pin, HIGH);
  } else {
    digitalWrite(in1Pin, LOW);
    digitalWrite(in2Pin, LOW);
  }

  analogWrite(enablePin, abs(speed));
}

void driveMotors(int motorA, int motorB) {
  setMotor(motorA, MOTOR_A_ENABLE_PIN, MOTOR_A_IN1_PIN, MOTOR_A_IN2_PIN);
  setMotor(motorB, MOTOR_B_ENABLE_PIN, MOTOR_B_IN1_PIN, MOTOR_B_IN2_PIN);
}

void stopMotors() {
  driveMotors(0, 0);
}

void processCommand() {
  commandBuffer[commandLength] = '\0';

  if (strcmp(commandBuffer, "FORWARD") == 0) {
    driveMotors(motorSpeed, motorSpeed);
    hc06.println("OK: FORWARD");
  } else if (strcmp(commandBuffer, "BACKWARD") == 0) {
    driveMotors(-motorSpeed, -motorSpeed);
    hc06.println("OK: BACKWARD");
  } else if (strcmp(commandBuffer, "LEFT") == 0) {
    driveMotors(-motorSpeed, motorSpeed);
    hc06.println("OK: LEFT");
  } else if (strcmp(commandBuffer, "RIGHT") == 0) {
    driveMotors(motorSpeed, -motorSpeed);
    hc06.println("OK: RIGHT");
  } else if (strcmp(commandBuffer, "STOP") == 0) {
    stopMotors();
    hc06.println("OK: STOP");
  } else if (strncmp(commandBuffer, "SPEED ", 6) == 0) {
    char *endPointer;
    const long requestedSpeed = strtol(commandBuffer + 6, &endPointer, 10);
    if (*endPointer == '\0' && requestedSpeed >= 0 && requestedSpeed <= 255) {
      motorSpeed = static_cast<byte>(requestedSpeed);
      hc06.print("OK: SPEED ");
      hc06.println(motorSpeed);
    } else {
      hc06.println("ERR: SPEED deve essere 0..255");
    }
  } else if (strcmp(commandBuffer, "LED ON") == 0) {
    digitalWrite(LED_BUILTIN, HIGH);
    hc06.println("OK: LED ON");
  } else if (strcmp(commandBuffer, "LED OFF") == 0) {
    digitalWrite(LED_BUILTIN, LOW);
    hc06.println("OK: LED OFF");
  } else if (commandLength > 0) {
    hc06.println("ERR: comando sconosciuto");
  }

  commandLength = 0;
}

void setup() {
  pinMode(LED_BUILTIN, OUTPUT);
  digitalWrite(LED_BUILTIN, LOW);
  pinMode(MOTOR_A_ENABLE_PIN, OUTPUT);
  pinMode(MOTOR_A_IN1_PIN, OUTPUT);
  pinMode(MOTOR_A_IN2_PIN, OUTPUT);
  pinMode(MOTOR_B_ENABLE_PIN, OUTPUT);
  pinMode(MOTOR_B_IN1_PIN, OUTPUT);
  pinMode(MOTOR_B_IN2_PIN, OUTPUT);
  stopMotors();
  Serial.begin(115200);
  hc06.begin(HC06_BAUD);
  Serial.println("Nano pronto. HC-06 a 9600 baud.");
}

void loop() {
  while (hc06.available() > 0) {
    const char incoming = static_cast<char>(hc06.read());

    if (incoming == '\r') {
      continue;
    }
    if (incoming == '\n') {
      processCommand();
    } else if (commandLength < COMMAND_BUFFER_SIZE - 1) {
      commandBuffer[commandLength++] = incoming;
    } else {
      commandLength = 0;
      hc06.println("ERR: comando troppo lungo");
    }
  }
}