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
byte steeringSpeed = 180;
byte tractionSpeed = 180;
int steeringDirection = 0;
int tractionDirection = 0;

void setMotor(int speed, byte enablePin, byte in1Pin, byte in2Pin) {
  digitalWrite(in1Pin, LOW);
  digitalWrite(in2Pin, LOW);

  if (speed > 0) {
    digitalWrite(in1Pin, HIGH);
  } else if (speed < 0) {
    digitalWrite(in2Pin, HIGH);
  }

  analogWrite(enablePin, abs(speed));
}

void updateSteering() {
  setMotor(steeringDirection * steeringSpeed, MOTOR_A_ENABLE_PIN, MOTOR_A_IN1_PIN, MOTOR_A_IN2_PIN);
}

void updateTraction() {
  setMotor(tractionDirection * tractionSpeed, MOTOR_B_ENABLE_PIN, MOTOR_B_IN1_PIN, MOTOR_B_IN2_PIN);
}

void stopSteering() {
  steeringDirection = 0;
  updateSteering();
}

void stopTraction() {
  tractionDirection = 0;
  updateTraction();
}

void stopMotors() {
  stopSteering();
  stopTraction();
}

bool parseSpeed(const char *text, byte *speed) {
  char *endPointer;
  const long requestedSpeed = strtol(text, &endPointer, 10);
  if (*text == '\0' || *endPointer != '\0' || requestedSpeed < 0 || requestedSpeed > 255) {
    return false;
  }
  *speed = static_cast<byte>(requestedSpeed);
  return true;
}

bool parseJoystick(const char *text, int *steering, int *traction) {
  char *separator;
  const long steeringValue = strtol(text, &separator, 10);
  if (separator == text || *separator != ' ') {
    return false;
  }

  const char *tractionText = separator + 1;
  char *endPointer;
  const long tractionValue = strtol(tractionText, &endPointer, 10);
  if (endPointer == tractionText || *endPointer != '\0' ||
      steeringValue < -255 || steeringValue > 255 ||
      tractionValue < -255 || tractionValue > 255) {
    return false;
  }

  *steering = static_cast<int>(steeringValue);
  *traction = static_cast<int>(tractionValue);
  return true;
}

void processCommand() {
  commandBuffer[commandLength] = '\0';

  if (strncmp(commandBuffer, "JOY ", 4) == 0) {
    int steeringValue;
    int tractionValue;
    if (parseJoystick(commandBuffer + 4, &steeringValue, &tractionValue)) {
      steeringDirection = (steeringValue > 0) - (steeringValue < 0);
      steeringSpeed = static_cast<byte>(abs(steeringValue));
      tractionDirection = (tractionValue > 0) - (tractionValue < 0);
      tractionSpeed = static_cast<byte>(abs(tractionValue));
      updateSteering();
      updateTraction();
    } else {
      hc06.println("ERR: JOY richiede due valori tra -255 e 255");
    }
  } else if (strcmp(commandBuffer, "STEER_LEFT") == 0 || strcmp(commandBuffer, "LEFT") == 0) {
    steeringDirection = 1;
    updateSteering();
    hc06.println("OK: STEER_LEFT");
  } else if (strcmp(commandBuffer, "STEER_RIGHT") == 0 || strcmp(commandBuffer, "RIGHT") == 0) {
    steeringDirection = -1;
    updateSteering();
    hc06.println("OK: STEER_RIGHT");
  } else if (strcmp(commandBuffer, "STEER_STOP") == 0) {
    stopSteering();
    hc06.println("OK: STEER_STOP");
  } else if (strcmp(commandBuffer, "DRIVE_FORWARD") == 0 || strcmp(commandBuffer, "FORWARD") == 0) {
    tractionDirection = 1;
    updateTraction();
    hc06.println("OK: DRIVE_FORWARD");
  } else if (strcmp(commandBuffer, "DRIVE_BACKWARD") == 0 || strcmp(commandBuffer, "BACKWARD") == 0) {
    tractionDirection = -1;
    updateTraction();
    hc06.println("OK: DRIVE_BACKWARD");
  } else if (strcmp(commandBuffer, "DRIVE_STOP") == 0) {
    stopTraction();
    hc06.println("OK: DRIVE_STOP");
  } else if (strcmp(commandBuffer, "STOP") == 0) {
    stopMotors();
    hc06.println("OK: STOP");
  } else if (strncmp(commandBuffer, "SPEED ", 6) == 0) {
    if (parseSpeed(commandBuffer + 6, &tractionSpeed)) {
      updateTraction();
      hc06.print("OK: TRACTION_SPEED ");
      hc06.println(tractionSpeed);
    } else {
      hc06.println("ERR: SPEED deve essere 0..255");
    }
  } else if (strncmp(commandBuffer, "TRACTION_SPEED ", 15) == 0) {
    if (parseSpeed(commandBuffer + 15, &tractionSpeed)) {
      updateTraction();
      hc06.print("OK: TRACTION_SPEED ");
      hc06.println(tractionSpeed);
    } else {
      hc06.println("ERR: TRACTION_SPEED deve essere 0..255");
    }
  } else if (strncmp(commandBuffer, "STEER_SPEED ", 12) == 0) {
    if (parseSpeed(commandBuffer + 12, &steeringSpeed)) {
      updateSteering();
      hc06.print("OK: STEER_SPEED ");
      hc06.println(steeringSpeed);
    } else {
      hc06.println("ERR: STEER_SPEED deve essere 0..255");
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