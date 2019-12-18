/*
available methods
void hubitat.helper.InterfaceUtils.alphaV1mqttConnect(DeviceWrapper device, String broker, String clientId, String username, String password)
void hubitat.helper.InterfaceUtils.alphaV1mqttDisconnect(DeviceWrapper device)
void hubitat.helper.InterfaceUtils.alphaV1mqttSubscribe(DeviceWrapper device, String topicFilter, int qos = 1)
void hubitat.helper.InterfaceUtils.alphaV1mqttUnsubscribe(DeviceWrapper device, String topicFilter)
void hubitat.helper.InterfaceUtils.alphaV1mqttPublish(DeviceWrapper device, String topic, String payload, int qos = 1, boolean retained = false)
Map hubitat.helper.InterfaceUtils.alphaV1parseMqttMessage(String stringToParse)
*/

metadata {
    definition(name: "MQTT Subscribe Presence", namespace: "rummyr", author: "Andrew Rowbottom") {
        capability "Sensor"
		capability "PresenceSensor" 
			//Attributes
			//	presence - ENUM ["present", "not present"]

		capability "Initialize" // so we can set the initial value to off
	
//		command "disconnect"
//		command "connect"
//		command "subscribe"
//		command "unsubscribe"
	
		command "present"
		command "notPresent"
        
//        command "dbgShowProperties"
	
	}
}



preferences {
    section("URIs") {
        input name: "broker", type: "text", title: "MQTT Broker", required: true, defaultValue: "192.168.11.110"
        input name: "port", type: "number", title: "MQTT port", required: true, defaultValue: 1883
        input name: "topic", type: "text", title: "topic to subscribe to", required: true
		// input (name: "mode", type: "enum", title: "Mode", required: true, defaultValue: "Heartbeat", options: ["Heartbeat", "As received"])
		input(name: "delay", type: "number", title:"Delay in seconds before silence indicates not present", required: true, defaultValue: 120)
        input(name: "onlyChangeStateWhenConnected", type: "bool", title:"Only change presence if connected", required: true, defaultValue: true)
        

		input (name: "logLevel", type: "enum", title: "logging", required: true, defaultValue: "Warnings Only", options: [2:"Warnings Only", 3:"Informational", 4:"debug", 5:"trace"])
    }
}

//def dbgShowProperties() {
//    log.debug  "this.metaClass.properties is ${this.metaClass.properties}"
//}

def initialize() {
	info "setting initial state to not present (with stateChange:false)"
	
    reconnect();
    unschedule() // cancel all scheduled tasks!
    runIn(1800, logsOff) 

    // although we now get a disconnect message 
    // we need to check every so often anyway 
    // because if the attempt to connect after a disconnect fails
    // we won't get a disconnected msg again
    runEvery15Minutes(checkMQTTIsConnected)
	
    // leave the present/not present state alone
    // but set to not present as per usual schedule
    // DONT DO THIS .. let the timer sort it out sendEvent(name: "presence", value: "not present", isStateChange: false) // set initial state to not present
    runIn(delay, notPresent)

    info "${device.displayName} - initialized"
    
}

def connect() {
    info "connecting to tcp://$broker:$port as ${device.displayName}"
    try {
    	interfaces.mqtt.connect("tcp://$broker:$port", device.displayName, "iot", "iot0arilan")
    } catch (org.eclipse.paho.client.mqttv3.MqttException e) {
        error "connecting had an error: ${e}";
        state.mqttConnected = "not connected"
    }
}

def disconnect() {
    if (!interfaces.mqtt.isConnected()) {
        info "not connected, so not disconnecting"
        return;
    }

	info "disconnecting"
	try {
		interfaces.mqtt.disconnect()
	} catch (Exception e) {
		error "disconnecting had an error: $e"
	}
}

def unsubscribe() {
    if (!interfaces.mqtt.isConnected()) {
        info "not connected, so not unsubscribing"
        return;
    }

	info "unsubscribing"
	try {
		interfaces.mqtt.unsubscribe(topic)
	} catch (Exception e) {
		error "unsubscribing had an error: $e"
	}
}
def subscribe() {
	if (topic) {
		info "subscribing to $topic"
        try {
    		interfaces.mqtt.subscribe(topic)
        } catch (Exception e) {
    		error "subscribing had an error: $e"
        }
            
	} else {
		error "please fill in topic"
	}
} 


def updated() {
    info "updated..."
	warn "logging level is set to $logLevel"
	trace "trace enabled"
	debug "debug enabled"
	info "info enabled"

    reconnect();
    if (logLevel > 3) runIn(1800, logsOff)
}

def reconnect() {
    state.mqttConnected = "unsubscribing"
	unsubscribe()
    state.mqttConnected = "disconnecting"
	disconnect()
    state.mqttConnected = "connecting"
	connect()
    state.mqttConnected = "subscribing"
	subscribe()
    if (interfaces.mqtt.isConnected()) {
        state.mqttConnected = "connected, subscribed"
    } else {
        state.mqttConnected = "not connected"
    }
}

def parse(String description) {
    trace "${device.displayName} - parsing $description"
	parsed = interfaces.mqtt.parseMessage(description)
	trace "${device.displayName} - parsed as $parsed"
    
    // check to see if it's our "isAlive" message
    if (parsed.payload == "mqttAlive?") {
        debug "mqttAlive? message was received, ignoring"
        state.isAliveReceivedTS = now()
        // cancel the "check" because we KNOW it's alive -- we just received a msg
        unschedule(didWeGetAnIsAlive)
        // call it anyway for debug messages
        didWeGetAnIsAlive()
        return;
    }
    
	// recieved msg .. set as present, and start a timer
	debug "msg received signalling present, going to not present in $delay seconds"
	present()
	
	runIn(delay, notPresent) // not doing [overwrite: false] because we want to effectively reset

	
}

/** called periodically, sends a "test" message to MQTT, should be received */
def checkMQTT_bySending() {
    trace  "sending mqttAlive test message to $topic"
    state.checkTS=now()
    interfaces.mqtt.publish(topic, "mqttAlive?")
    runInMillis(10*1000,didWeGetAnIsAlive);
}

def checkMQTTIsConnected() {
    trace  "checking to see if we're connected"
    if (interfaces.mqtt.isConnected()) {
        return;
    }
    error "checkMQTTIsConnected - MQTT is not connected, reconnecting"
    reconnect()

}

def didWeGetAnIsAlive() {
    debug "didWeGetAnIsAlive"
    if (state.isAliveReceivedTS > state.checkTS) {
        rtt = (state.isAliveReceivedTS - state.checkTS)/1000;
        info "mqtt is alive (RTT:${rtt})"
    } else {
        warn "didWeGetAnIsAlive - mqtt seems to be down check was at ${state.checkTS} last \"alive\" was at ${state.isAliveReceivedTS}, now is " + now()
        reconnect()
    }
}

//************************* ERROR handling? ************************************
/** The design of this driver is a sensor only, messages are only *sent* to test 
   if the connection is alive. So if we get a "send error" we know to reconnect
*/
def mqttClientStatus(String status){
    if (status.contains("Status: Connection succeeded")) {
        info "mqttClientStatus - ${status}"
        state.mqttConnected = "connected"
    } else if (status.contains("Error: Connection lost:")) {
        delaySecs = 1
        warn "mqttClientStatus - force disconnecting now and reconnecting in $delaySecs because of  $status"
        try {
            interfaces.mqtt.disconnect()
        } catch (Exception e) {
            // dont print an error here, we're expecting it!
            // error "disconnecting had an error: $e"
        }
        runIn(delaySecs,reconnect);
        // seems not to work if called immediately! reconnect()
    } else {
        error "mqttClientStatus - ${status}"
    }
	// what to do? we really want to re-send after connecting, but that's going to be tough!
    if (status && status.toLowerCase().contains("send error")) {
        // all sends in this driver are test only, so we dont need to worry about re-sending
        warn "mqttClientStatus - reconnecting on send error"
        reconnect()
    }
    
}

def present() {
	debug "present"
    if (interfaces.mqtt.isConnected() 
        || !onlyChangeStateWhenConnected) {
    	sendEvent(name: "presence", value: "present")
    }
}

def notPresent() {
	debug "not present"
    if (interfaces.mqtt.isConnected() 
        || !onlyChangeStateWhenConnected) {
    	sendEvent(name: "presence", value: "not present")
    }
}

//********************* logging ***************************
def logsOff() {
    log.warn "auto disabling debug and trace logging"
	if (logLevel > 3) {
	    device.updateSetting("logLevel", [value: "3", type: "int"])
		warn "disabling trace and debug logging"
	}
}

private error(String msg) {
	// 	if (logLevel >= "2") {
	log.error "${device.displayName} - $msg"
	// }
}

private warn(String msg) {
	// 	if (logLevel >= "2") {
	log.warn "${device.displayName} - $msg"
	// }
}


private info(String msg) {
	if (logLevel >= "3") { 
		log.info "${device.displayName} - $msg"
	}
}


private debug(String msg) {
	if (logLevel >= "4") { 
		log.debug "${device.displayName} - $msg"
	}
}

private trace(String msg) {
	if (logLevel >= "5") {
		log.trace "${device.displayName} - $msg"
	}
}



