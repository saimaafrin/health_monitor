
#include <Adafruit_BMP085.h>
#include <Wire.h>


char PRESSURESHOW[4];// initializing a character of size 4 for showing the result
char TEMPARATURESHOW[4];// initializing a character of size 4 for showing the temparature result
Adafruit_BMP085 bmp;

#define USE_ARDUINO_INTERRUPTS true    // Set-up low-level interrupts for most acurate BPM math.
#include <PulseSensorPlayground.h>     // Includes the PulseSensorPlayground Library.   

//  Variables
const int PulseWire = A0;       // PulseSensor PURPLE WIRE connected to ANALOG PIN 0
const int LED13 = 13;          // The on-board Arduino LED, close to PIN 13.
int Threshold = 550;           // Determine which Signal to "count as a beat" and which to ignore.
                               // Use the "Gettting Started Project" to fine-tune Threshold Value beyond default setting.
                               // Otherwise leave the default "550" value. 
int temp = A1;

int temp_data;
                               
PulseSensorPlayground pulseSensor;  // Creates an instance of the PulseSensorPlayground object called "pulseSensor"


void setup() {   

  Serial.begin(9600);          // For Serial Monitor
  delay(1000);
pinMode(temp, INPUT);
if (!bmp.begin())
{
//Serial.println("ERROR");///if there is an error in communication
while (1) {}
}

  // Configure the PulseSensor object, by assigning our variables to it. 
  pulseSensor.analogInput(PulseWire);   
  pulseSensor.blinkOnPulse(LED13);       //auto-magically blink Arduino's LED with heartbeat.
  pulseSensor.setThreshold(Threshold);   
   if (pulseSensor.begin()) {
    //Serial.println("We created a pulseSensor Object !");  //This prints one time at Arduino power-up,  or on Arduino reset.  
  }
}



void loop() {


int prsr = pressure();
int tmp =  temperature();
int myBPM = pulseSensor.getBeatsPerMinute();  // Calls function on our pulseSensor object that returns BPM as an "int".

int bpm = 0;

pulseSensor.sawStartOfBeat()==true ? bpm=(int)myBPM : bpm=0;

String t1 = "{\"bpm\":\"";
String t2 = "\",";
String t3 = "\"tmp\":\"";
String t4 = "\"mmhg\":\"";
String t5 = "\"}";
String t6 = ",";

//String rs2 = tmp+t6+bpm+t6+prsr; 
String rs = t1+bpm+t2+t3+tmp+t2+t4+prsr+t5; //
Serial.println(rs);

delay(1000);

}

int temperature() {
  temp_data = analogRead(A1);
  int t = temp_data * 0.321;
 
  return t;
 
}

int pressure (){
  String PRESSUREVALUE = String(bmp.readPressure());
// convert the reading to a char array
PRESSUREVALUE.toCharArray(PRESSURESHOW, 4);

return (int)PRESSURESHOW;

}

  
