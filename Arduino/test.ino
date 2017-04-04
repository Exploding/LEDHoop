#include <SoftwareSerial.h>
#include <Adafruit_NeoPixel.h>
#include <avr/power.h>

#define LED_PIN 0
#define CTS_PIN 3
#define RX_PIN 2
#define N_LEDS 69
#define FPS 30

SoftwareSerial serial(RX_PIN, 1);
Adafruit_NeoPixel strip = Adafruit_NeoPixel(N_LEDS, LED_PIN);

uint8_t effectMode = 0,
        effectPos  = 0;
uint8_t color[3] = {0,0,255};
uint32_t prevTime = 0L;

void setup() {
  if (F_CPU == 16000000) clock_prescale_set(clock_div_1);

  serial.begin(9600);
  
  strip.begin();
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
    if(serial.available() > 0) {
      cmd = (int) serial.read();

      switch(cmd) {
        case 0:
          while(serial.available() < 3 && t + 10000 > micros());
          if(serial.available() >= 3) {
            color[0] = (uint8_t) serial.read();
            color[1] = (uint8_t) serial.read();
            color[2] = (uint8_t) serial.read();
          }
          break;
        case 1:
          while(serial.available() < 1 && t + 10000 > millis());
          if(serial.available() >= 1) {
            effectMode = (uint8_t) serial.read();
            effectPos = 0;
          }
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
