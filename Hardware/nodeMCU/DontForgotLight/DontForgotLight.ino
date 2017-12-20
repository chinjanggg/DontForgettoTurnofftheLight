#include <Arduino.h>
#include <ArduinoJson.h>
#include <ESP8266WiFi.h>
#include <ESP8266WiFiMulti.h>

#include <ESP8266HTTPClient.h>
#include <math.h>


ESP8266WiFiMulti WiFiMulti;

int stat;
String url;
HTTPClient http;
int temp;
int toBoard;

void setup() {
  // put your setup code here, to run once:
  Serial.begin(9600);
  pinMode(D1, INPUT);
  pinMode(D3, OUTPUT);
  temp=0;
  WiFiMulti.addAP("My ASUS", "wasu1234");
}

void updateServer(){
  if((WiFiMulti.run() == WL_CONNECTED)) {
  
  // put your main code here, to run repeatedly:
  if(stat==0){
    url= "http://192.168.43.167/project/sample.php?value=OFF";  
   // int httpCode = http.GET();
  }
  else{
     url = "http://192.168.43.167/project/sample.php?value=ON";
     
   // int httpCode = http.GET();
  }
  Serial.print("[HTTP] begin...\n");
  http.begin(url);
  Serial.print("[HTTP] GET...\n");
  // start connection and send HTTP header
        int httpCode = http.GET();

        // httpCode will be negative on error
        if(httpCode > 0) {
            // HTTP header has been send and Server response header has been handled
            Serial.printf("[HTTP] GET... code: %d\n", httpCode);

            // file found at server
            if(httpCode == HTTP_CODE_OK) {
                String payload = http.getString();
                Serial.println(payload);
            }
        } else {
            Serial.printf("[HTTP] Code: %d ", httpCode);
            Serial.printf("[HTTP] GET... failed, error: %s\n", http.errorToString(httpCode).c_str());
        }

        http.end();
  }
}
void getServer(){
  if((WiFiMulti.run() == WL_CONNECTED)) {
 
  // put your main code here, to run repeatedly:
  
     url = "http://192.168.43.167/project/newfile.txt";
     
   // int httpCode = http.GET();
  
 // Serial.print("[HTTP] begin...\n");
  http.begin(url);
//  Serial.print("[HTTP] GET...\n");
  // start connection and send HTTP header
        int httpCode = http.GET();

        // httpCode will be negative on error
        if(httpCode > 0) {
            // HTTP header has been send and Server response header has been handled
         //   Serial.printf("[HTTP] GET... code: %d\n", httpCode);

            // file found at server
            if(httpCode == HTTP_CODE_OK) {
                String payload = http.getString();
                Serial.println(payload);
                if(payload.equals("ON")){
                  toBoard=1;
                }
                else{
                  toBoard=0;
                }
            }
        } else {
         //   Serial.printf("[HTTP] Code: %d ", httpCode);
         //   Serial.printf("[HTTP] GET... failed, error: %s\n", http.errorToString(httpCode).c_str());
        }

        http.end(); 
        
  }
}
void loop() {

    stat=digitalRead(D1);
  if(stat!=temp){
    temp=stat;
    Serial.print("Status :");
    Serial.println(stat);
    updateServer();
    delay(1000);
  }
  
  getServer();
   Serial.print("toBoard :");
  Serial.println(toBoard);
  digitalWrite(D3, toBoard);
  delay(1000);
}
