/* based on https://raw.githubusercontent.com/CobraVmax/Hubitat/master/Drivers/Switches/Switch%20Timer.groovy */
/**
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *
 *  
 *
 *
 */



metadata {

    definition (name: "Virtual Heartbeat Presence", namespace: "rummyr", author: "Andrew Rowbottom", description: "A description!") {
		capability "Sensor"
		capability "PresenceSensor"
		
        command "notPresent"
		command "present"
		// command "removeLegacyState"


	}


	preferences {
		section {
		// description("Simulates a presence sensor which is kept alive by receiving events. After a delay it will automatically change to present/not present")
		input(name: "Comment", type: "text", title:"Simulates a presence sensor which is kept alive by receiving events. After a delay it will automatically change to present/not present",
			  required: false, defaultValue:"")
	 	input(name: "defaultState", type: "enum", title: "State to go to after the delay.<br />\n*Save* to see extra options", required:false,  defaultValue: "",
			  options: ["keep last state", "present", "not present" ])          
	
		//section	{
		if(defaultState == "present" || defaultState == "not present"){
				input(name: "delayNum", type: "number", title:"Delay before present/not present", required: true, defaultValue: 0)
				input(name: "minSec", type: "bool", title: "Off = Seconds - On = Minutes", required: true, defaultValue: false)
		}
		//}
	 	input(name: "debugEnabled", type: "bool", title: "Enable debug logging", required:true,  defaultValue: false)          
		}
	} // end preferences
} // end metadata



def present() {
    checkDelay()
    sendEvent(name: "presence", value: "present")
	if (debugEnabled) log.debug "$version - present() executing"
	
    if(defaultState == "not present"){
      if (debugEnabled) log.debug "$version - present going to not present in $state.delay1 seconds"
      runIn(state.delay1, notPresent) // not doing [overwrite: false] because we want to effectively reset
    } 
}

def notPresent() {
    checkDelay()
	if (debugEnabled) log.debug "$version - notPresent() executing"
    sendEvent(name: "presence", value: "not present")

    if(defaultState == "present"){
      if (debugEnabled) log.debug "$version - not present going to present in $state.delay1 seconds"
      runIn(state.delay1, present) // not doing [overwrite: false] because we want to effectively reset
    } 
}

def removeLegacyState() {
	dbCleanUp();
}

def checkDelay(){
    if (minSec == true) {
       state.delay1 = 60 * delayNum as int    
    }
    else{
       state.delay1 = delayNum as int   
    }
}


// for future work!
private dbCleanUp() {
	unschedule()
	state.remove("PresenceSensor")
	state.remove("presenceSensor")
}


private getVersion() {
	"Virtual Heartbeat Presence 1.0"
}

