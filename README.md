# Hubitat
Place for drivers and apps I have developed

## MQTT Subscribe Presence.groovy

This sensor driver subscribes to an MQTT topic and as long as messages are received within a configurable interval it shows as Present.
There's quite a lot of old code in here that is never used, specifically
     checkMQTT_bySending - which used to try to see if the mqtt connection was alive by sending a msg to itself via mqtt
     didWeGetAnIsAlive - coupled with checkMQTT_bySending
     
It autocancels debug and trace level logging after 1800 seconds
Every 15 minutes it checks the mqtt connection status and reconnects if it shows as not connected
