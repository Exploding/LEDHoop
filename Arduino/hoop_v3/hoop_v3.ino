

#include <Adafruit_NeoPixel.h>
#include <SPI.h>
#include <SdFat.h>
#include <avr/power.h>
#include <avr/sleep.h>
#include <ReceiveOnlySoftwareSerial.h>

// TX/RX are the only "available" pins on OpenLog device:
#define LED_PIN 1          
#define RX_PIN 0

#define NUM_LEDS 147 

// Empty and full thresholds (millivolts) used for battery level display:
#define BATT_MIN_MV 3350 // Some headroom over battery cutoff near 2.9V
#define BATT_MAX_MV 5000 // And little below fresh-charged battery near 4.1V

#define NEXT_PATTERN 0
#define PREV_PATTERN 1
#define BRIGHT_UP 36
#define BRIGHT_DOWN 37
#define FASTER 38
#define SLOWER 39
#define RESTART 40
#define BATTERY 41
#define OFF 42
#define AUTO 43
#define STROBE 44
#define LOOP_MODE 45
#define SET_LOOP 46
#define SET_PATTERN 47

uint32_t CYCLE_TIME = 10;

SdFat sd;
File file;

void showBatteryLevel(void);
uint16_t readVoltage(void);

long setting_uart_speed = 115200; // MAX speed

// Microseconds per line for various speed settings
const uint16_t PROGMEM lineTable[] = {
  1000000L /  30, 
  1000000L /  60,
  1000000L /  120,
  1000000L /  144,
  1000000L /  240
};
uint16_t lineInterval = 1000000L / 60; // 60 fps
uint16_t lineIntervalIndex = 1;
uint32_t lastUpdate = 0;

const byte stat1 = 5; // status LED

uint8_t frames = 10,
        effectPos = 0;
uint8_t buff[3];
        
String fileName = "PAT0001.PAT";

boolean autoCycle = true;

uint16_t numPatterns = 1775;
uint16_t patternNumber = 486;

uint32_t lastImageTime = 0L, // Time of last image change
         lastLineTime  = 0L;

const uint8_t PROGMEM brightness[] = { 15, 31, 63, 127, 255 };
uint8_t bLevel = sizeof(brightness) - 1;

boolean strobeMode = true;
bool strobe = true;

bool loopMode = false;
int loopIndex = 0;
uint16_t loopIndices[10] = {1,2,3,4,5,6,7,8,9,10};

Adafruit_NeoPixel strip = Adafruit_NeoPixel(NUM_LEDS, LED_PIN);
ReceiveOnlySoftwareSerial serial(RX_PIN); // RX

void setup() {
  // set up LED strip
  strip.begin();
  strip.clear();
  strip.show();

  showBatteryLevel();
  
  // init serial
  serial.begin(9600);

  // set up SD & FAT
  if (!sd.begin(SS, SPI_FULL_SPEED)) {
    sd.initErrorHalt();
  }

  patternInit();
}

void showBatteryLevel(void) {
  // Display battery level bargraph on startup.  It's just a vague estimate
  // based on cell voltage (drops with discharge) but doesn't handle curve.
  uint16_t mV  = readVoltage();
  uint8_t  lvl = (mV >= BATT_MAX_MV) ? NUM_LEDS : // Full (or nearly)
                 (mV <= BATT_MIN_MV) ?        1 : // Drained
                 1 + ((mV - BATT_MIN_MV) * NUM_LEDS + (NUM_LEDS / 2)) /
                 (BATT_MAX_MV - BATT_MIN_MV + 1); // # LEDs lit (1-NUM_LEDS)
  for(uint8_t i=0; i<lvl; i++) {                  // Each LED to batt level...
    uint8_t g = (i * 5 + 2) / NUM_LEDS;           // Red to green
    strip.setPixelColor(i, 4-g, g, 0);
    strip.show();                                 // Animate a bit
    delay(250 / NUM_LEDS);
  }
  delay(1500);                                    // Hold last state a moment
  strip.clear();                                  // Then clear strip
  strip.show();
}

void patternInit() {
  if(file.isOpen()) {
    file.close();
  }
  String padding = "000";
  if(patternNumber > 9)
    padding = "00";
  if(patternNumber > 99)
    padding = "0";

  String filename = "PAT" + padding + String(patternNumber) + ".PAT";
  file.open(filename.c_str());
  frames = file.read();

  lastImageTime = millis();
}

void nextPattern(void) {
  if(loopMode) {
    if(++loopIndex >= 9) loopIndex = 0;
    patternNumber = loopIndices[loopIndex];
  } else {
    if(++patternNumber >= numPatterns) patternNumber = 1;
  }

  patternInit();
}

void prevPattern(void) {
  if(loopMode) {
    loopIndex = loopIndex ? loopIndex - 1 : 9;
    patternNumber = loopIndices[loopIndex];
  } else {
    patternNumber = patternNumber ? patternNumber - 1 : numPatterns - 1;
  }
  
  patternInit();
}

void loop() {
  uint32_t t = millis(); // Current time, milliseconds

  if(autoCycle) {
    if((t - lastImageTime) >= (CYCLE_TIME * 1000L)) nextPattern();
  }
  
  if(file.isOpen()) {
    file.seek((effectPos * NUM_LEDS * 3) + 1);
    for(int i = 0; i < NUM_LEDS; i++) {
      file.read(buff, sizeof(buff));
      strip.setPixelColor(i, strip.Color(buff[0], buff[1], buff[2]));
    }

    if(effectPos < frames - 1) {
      effectPos++;
    } else {
      effectPos = 0;
    }
  }

  
  while(((t = micros()) - lastLineTime) < lineInterval) {
    if(serial.available()) {
      int cmd = (int) serial.read();
      switch(cmd) {
        case NEXT_PATTERN: {
          nextPattern();
          break;
        }
        case PREV_PATTERN: {
          prevPattern();
          break;
        }
        case BRIGHT_UP: {
          if(bLevel < (sizeof(brightness) - 1))
            strip.setBrightness(pgm_read_byte(&brightness[++bLevel]));
          break;
        }
        case BRIGHT_DOWN: {
          if(bLevel)
            strip.setBrightness(pgm_read_byte(&brightness[--bLevel]));
          break;
        }
        case FASTER: {
          if(lineIntervalIndex < (sizeof(lineTable) / sizeof(lineTable[0]) - 1))
            lineInterval = pgm_read_word(&lineTable[++lineIntervalIndex]);
          break;
        }
        case SLOWER: {
          if(lineIntervalIndex)
            lineInterval = pgm_read_word(&lineTable[--lineIntervalIndex]);
          break;
        }
        case RESTART: {
          patternNumber = 1;
          patternInit();
          break;
        }
        case BATTERY: {
          strip.clear();
          strip.show();
          delay(250);
          strip.setBrightness(255);
          showBatteryLevel();
          strip.setBrightness(pgm_read_byte(&brightness[bLevel]));
          break;
        }
        case OFF: {
          strip.setBrightness(0);
          break;
        }
        case AUTO: {
          autoCycle = !autoCycle;
          break;
        }
        case STROBE: {
          strobeMode = !strobeMode;
          break;
        }
        case LOOP_MODE: {
          loopMode = !loopMode;
          loopIndex = 0;
          break;
        }
        case SET_LOOP: {
          for(int i = 0; i < 10; i++) {
            loopIndices[i] = serial.read();
          }
          break;
        }
        case SET_PATTERN: {
          patternNumber = serial.read();
          patternInit();
          autoCycle = false;
          break;
        }
      }
    }
  }

  if(strobeMode) {
    if(strobe) {
      strip.setBrightness(pgm_read_byte(&brightness[bLevel]) / 2);
    } else {
      strip.setBrightness(pgm_read_byte(&brightness[bLevel]));
    }

    strobe = !strobe;
  }
  
  strip.show();
  lastLineTime = t;
}

uint16_t readVoltage() {
  int      i, prev;
  uint8_t  count;
  uint16_t mV;

  // Select AVcc voltage reference + Bandgap (1.8V) input
  ADMUX  = _BV(REFS0) |
           _BV(MUX3)  | _BV(MUX2) | _BV(MUX1);
  ADCSRA = _BV(ADEN)  |                          // Enable ADC
           _BV(ADPS2) | _BV(ADPS1) | _BV(ADPS0); // 1/128 prescaler (125 KHz)
  // Datasheet notes that the first bandgap reading is usually garbage as
  // voltages are stabilizing.  It practice, it seems to take a bit longer
  // than that.  Tried various delays, but still inconsistent and kludgey.
  // Instead, repeated readings are taken until four concurrent readings
  // stabilize within 10 mV.
  for(prev=9999, count=0; count<4; ) {
    for(ADCSRA |= _BV(ADSC); ADCSRA & _BV(ADSC); ); // Start, await ADC conv.
    i  = ADC;                                       // Result
    mV = i ? (1100L * 1023 / i) : 0;                // Scale to millivolts
    if(abs((int)mV - prev) <= 10) count++;   // +1 stable reading
    else                          count = 0; // too much change, start over
    prev = mV;
  }
  ADCSRA = 0; // ADC off
  return mV;
}
