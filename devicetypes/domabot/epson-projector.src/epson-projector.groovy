/**
 *  SmartThings Device Handler: Epson Project ESC/VP21
 *
 *  Author: admin@domabo.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 */

metadata {
  definition (name: "Epson Projector", namespace: "domabot", author: "admin@domabo.com") {

    /**
     * List our capabilties. Doing so adds predefined command(s) which
     * belong to the capability.
     */
    capability "Switch"
    capability "Refresh"
    capability "Polling"

    /**
     * Define all commands, ie, if you have a custom action not
     * covered by a capability, you NEED to define it here or
     * the call will not be made.
     *
     * To call a capability function, just prefix it with the name
     * of the capability, for example, refresh would be "refresh.refresh"
     */
    // command "button1"
  }

  /**
   * Define the various tiles and the states that they can be in.
   * The 2nd parameter defines an event which the tile listens to,
   * if received, it tries to map it to a state.
   *
   * You can also use ${currentValue} for the value of the event
   * or ${name} for the name of the event. Just make SURE to use
   * single quotes, otherwise it will only be interpreted at time of
   * launch, instead of every time the event triggers.
   */
  tiles(scale: 2) {
    multiAttributeTile(name:"state", type:"generic", width:6, height:4) {
      tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
        attributeState "on", label:'On', action:"switch.off", icon:"st.Electronics.electronics7", backgroundColor:"#79b821", nextState:"off"
        attributeState "off", label:'Off', action:"switch.on", icon:"st.Electronics.electronics7", backgroundColor:"#ffffff", nextState:"on"
      }
      tileAttribute ("command", key: "SECONDARY_CONTROL") {
        attributeState "command", label:'${currentValue}'
      }
    }

    // Row 1
   standardTile("refresh", "device.status", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
      state "default", label:"", action:"refresh.refresh", icon:"st.secondary.refresh", backgroundColor:"#ffffff"
    }

    // Defines which tile to show in the overview
    main "state"

    // Defines which tile(s) to show when user opens the detailed view
    details([
      "state",
      "refresh"
    ])
  }
}

/**************************************************************************
 * The following section simply maps the actions as defined in
 * the metadata into onAction() calls.
 *
 * This is preferred since some actions can be dealt with more
 * efficiently this way. Also keeps all user interaction code in
 * one place.
 *
 */
def refresh() {
  sendCommand("status") 
}

/**************************************************************************/

/**
 * Called every so often (every 5 minutes actually) to refresh the
 * tiles so the user gets the correct information.
 */
def poll() {
  refresh()
}

def on() {
  sendCommand("on")
  sendEvent(name: "switch", value: "on")
}
def off() {
  sendCommand("off")
  sendEvent(name: "switch", value: "off")
}

def update(evt) {
  if (evt.containsKey("command")) {
    //toggle the switch on and off
    sendEvent(name: "switch", value: (device.switch == "on") ? "off" : "on")

    //log.debug "setting command to ${evt.command}"
    sendEvent(name: "command", value: "Received: ${evt.command}")
  }
}

private sendCommand(part) {
  parent.sendCommand("/plugins/escvp21/${part}")
}
