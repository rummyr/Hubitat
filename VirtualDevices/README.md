# Heartbeat Sensors

Heartbeat sensors are drivers that will automatically switch their state when they haven't received an update in a defined interval.
You could call them watchdogs.

These were written because I have home-made devices (and services) that only send "active" events, 
when they crash, power down, or just generally stop sending events, I want them to change state.

An example is a sensor that sends the RSSI for a bluetooth fob, when it's sending I'm home, when I'm not home there's no RSSI, so it doesn't send.

Another example is a motion sensor that only sends when there is motion, and simply powers down when there isn't, it never sends an "inactive" message!

And finally I have a number of services running on my RaspberryPi, I'd like to know when they crash. 


## VirtualHeartbeatPresence
This driver is a simple Presence Sensor that when not updated for a while automatically change to the selected state.

## VirtualHeartbeatMotionSensor
Coming Soon, it is almost identical to the VirtualHeartbeatPresence
