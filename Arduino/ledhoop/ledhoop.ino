#include <Adafruit_NeoPixel.h>
//#include <avr/power.h>

//#include <BlockDriver.h>
//#include <FreeStack.h>
//#include <MinimumSerial.h>
#include <SPI.h>
#include <SdFat.h>
#include <SdFatConfig.h>
#include <SysCall.h>

#define LED_PIN 5
//#define CTS_PIN 3
//#define RX_PIN 2
#define N_LEDS 147
#define FPS 50

//SoftwareSerial serial(RX_PIN, 1);
Adafruit_NeoPixel strip = Adafruit_NeoPixel(N_LEDS, LED_PIN);

const uint8_t SOFT_MISO_PIN = 16;
const uint8_t SOFT_MOSI_PIN = 14;
const uint8_t SOFT_SCK_PIN  = 15;
const uint8_t SD_CHIP_SELECT_PIN = 10;

//SdFatSoftSpi<SOFT_MISO_PIN, SOFT_MOSI_PIN, SOFT_SCK_PIN> sd;
SdFat sd;

File file;

uint8_t effectMode = 6,
        effectPos  = 0,
        frames = 10;
uint8_t color[3] = {0,0,255};
uint32_t prevTime = 0L;
String fileName = "PAT0014.PAT";
uint8_t buff[3];

void setup() {
  Serial.begin(9600);
  
  strip.begin();
  //strip.setBrightness(50);

  if (!sd.begin(SD_CHIP_SELECT_PIN, SD_SCK_MHZ(50))) {
    sd.initErrorHalt();
  }

  file.open(fileName.c_str());
}
 
void loop() {
  int cmd;
  uint32_t t;
  
  for(;;) {
    t = micros();
    if((t - prevTime) >= (1000000L / FPS)) {
      prevTime = t;
      break;  
    }
    if(Serial.available() > 0) {
      cmd = (int) Serial.read();  

      switch(cmd) {
        case 34:
          while(Serial.available() < 3 && t + 10000 > micros());
          if(Serial.available() >= 3) {
            color[0] = (uint8_t) Serial.read();
            color[1] = (uint8_t) Serial.read();
            color[2] = (uint8_t) Serial.read();
          }
          break;
        case 35:
          while(Serial.available() < 1 && t + 10000 > millis());
          if(Serial.available() >= 1) {
            effectMode = (uint8_t) Serial.read();
            effectPos = 0;
          }
          break;
        case 36:
            file.close();
            fileName = Serial.readStringUntil('!');
            Serial.println("Received: " + fileName);
            file.open(fileName.c_str());
            frames = file.read();
          break;
      }
    }
  }

  strip.show();

  switch(effectMode) {
    case 0:
      chase();
      break;
    case 1:
      rainbow();
      break;
    case 2:
      rainbowCycle();
      break;
    case 3:
      sparkle();
      break;
    case 4:
      strobe();
      break;
    case 5:
      solid();
      break;
    case 6:
      clearStrip();
      break;
    case 7:
      pattern();
      break;
  }
}

void chase() {
  strip.setPixelColor(effectPos, strip.Color(color[0], color[1], color[2]));
  strip.setPixelColor(effectPos-4, 0);
  effectPos = effectPos == N_LEDS + 3 ? 0 : effectPos + 1;
}

void rainbow() {
  for(int i = 0; i < N_LEDS; i++) {
    strip.setPixelColor(i, Wheel((i + effectPos) & 255));
  }
  effectPos = effectPos == 256 ? 0 : effectPos + 1;
}

void rainbowCycle() {
  for(int i = 0; i < N_LEDS; i++) {
    strip.setPixelColor(i, Wheel(((i * 256 / N_LEDS) + effectPos) & 255));
  }
  effectPos = effectPos == 256 ? 0 : effectPos + 1;
}

void sparkle() {
  strip.setPixelColor(effectPos, strip.Color(0,0,0));
  effectPos = random(N_LEDS);
  strip.setPixelColor(effectPos, strip.Color(color[0], color[1], color[2]));
}

void strobe() {
  if(effectPos % 3 == 0) {
    for(int i = 0; i < N_LEDS; i++) {
      strip.setPixelColor(i, strip.Color(color[0], color[1], color[2]));
    }
  } else {
    for(int i = 0; i < N_LEDS; i++) {
      strip.setPixelColor(i, strip.Color(0, 0, 0));
    }
  }
  effectPos++;
}

void solid() {
  for(int i = 0; i < N_LEDS; i++) {
    strip.setPixelColor(i, strip.Color(color[0], color[1], color[2]));
  }
}

void clearStrip() {
  for(int i = 0; i < N_LEDS; i++) {
    strip.setPixelColor(i, strip.Color(0,0,0));
  }
}

void pattern() {
  if(file.isOpen()) {
    file.seek((effectPos * N_LEDS * 3) + 1);
    for(int i = 0; i < N_LEDS; i++) {
      file.read(buff, sizeof(buff));
      strip.setPixelColor(i, strip.Color(buff[0], buff[1], buff[2]));
      //strip.setPixelColor(i, strip.Color((uint8_t)file.read(), (uint8_t)file.read(), (uint8_t)file.read()));
    }

    if(effectPos < frames - 1) {
      effectPos++;
    } else {
      effectPos = 0;
    }
  }
}

uint32_t Wheel(byte WheelPos) {
  WheelPos = 255 - WheelPos;
  if(WheelPos < 85) {
    return strip.Color(255 - WheelPos * 3, 0, WheelPos * 3);
  }
  if(WheelPos < 170) {
    WheelPos -= 85;
    return strip.Color(0, WheelPos * 3, 255 - WheelPos * 3);
  }
  WheelPos -= 170;
  return strip.Color(WheelPos * 3, 255 - WheelPos * 3, 0);
}
