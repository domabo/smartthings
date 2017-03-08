/**
 *  SmartThings SmartApp: Epson Projector
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
 *
 *  https://github.com/MattTW/BlinkMonitorProtocol
 *  https://github.com/terafin/SmartThingsPublic/tree/master/smartapps/terafin/blink.src
 *  https://community.smartthings.com/t/blink-wifi-camera-motion-sensor-on-kickstarter/3594/243
 */
 import groovy.json.JsonSlurper


definition(
  name: "Epson Projector",
  namespace: "domabot",
  author: "admin@domabot.com",
  description: "Espon Projector SmartApp",
  category: "My Apps",
  iconUrl: "http://cdn.device-icons.smartthings.com/Electronics/electronics7-icn.png",
  iconX2Url: "http://cdn.device-icons.smartthings.com/Electronics/electronics7-icn@2x.png",
  iconX3Url: "http://cdn.device-icons.smartthings.com/Electronics/electronics7-icn@3x.png",
  singleInstance: true
)

preferences {
  section("SmartThings Hub") {
    input "hostHub", "hub", title: "Select Hub", multiple: false, required: true
  }
  section("SmartThings Node Proxy") {
    input "proxyAddress", "text", title: "Proxy Address", description: "(ie. 192.168.1.10)", required: true
    input "proxyPort", "text", title: "Proxy Port", description: "(ie. 8080)", required: true, defaultValue: "8080"
    input "authCode", "password", title: "Auth Code", description: "", required: true, defaultValue: "secret-key"
  }
}

def installed() {
  subscribeToEvents()
}

def subscribeToEvents() {
  subscribe(location, null, lanResponseHandler, [filterEvents:false])
}

def uninstalled() {
    removeChildDevices()
}

def updated() {
  //remove child devices as we will reload
  removeChildDevices()

  //dynamically add child devices
  addChildDevices()
}

def lanResponseHandler(evt) {
  def map = stringToMap(evt.stringValue)

  //verify that this message is from STNP IP:Port
  //IP and Port are only set on HTTP GET response and we need the MAC
  if (map.ip == convertIPtoHex(settings.proxyAddress) &&
    map.port == convertPortToHex(settings.proxyPort)) {
      if (map.mac) {
        state.proxyMac = map.mac
      }
  }

  //verify that this message is from STNP MAC
  //MAC is set on both HTTP GET response and NOTIFY
  if (map.mac != state.proxyMac) {
    return
  }

  def headers = getHttpHeaders(map.headers);
  def body = getHttpBody(map.body);
  //log.trace "SmartThings Node Proxy: ${evt.stringValue}"
  //log.trace "Headers: ${headers}"
  //log.trace "Body: ${body}"

  //verify that this message is for this plugin
  if (headers.'stnp-plugin' != 'escvp21') {
    return
  }

  //log.trace "Generic event: ${evt.stringValue}"
  processEvent(body)
}

private sendCommand(path) {
  log.trace "Epson send command: ${path}"

  if (settings.proxyAddress.length() == 0 ||
    settings.proxyPort.length() == 0) {
    log.error "SmartThings Node Proxy configuration not set!"
    return
  }

  def host = getProxyAddress()
  def headers = [:]
  headers.put("HOST", host)
  headers.put("Content-Type", "application/json")
  headers.put("stnp-auth", settings.authCode)

  def hubAction = new physicalgraph.device.HubAction(
      method: "GET",
      path: path,
      headers: headers
  )
  sendHubCommand(hubAction)
}

private processEvent(evt) {
  if (evt.type == "command") {
    updateDevices(evt)
  }
}

private addChildDevices() {
  //just add one child device
  def deviceId = 'epson|projector1'
  if (!getChildDevice(deviceId)) {
    addChildDevice("domabot", "Epson Projector", deviceId, hostHub.id, ["name": "Epson Projector", label: "Epson Projector", completedSetup: true])
    //log.debug "Added device: ${deviceId}"
  }

  childDevices*.refresh()
}

private removeChildDevices() {
  getAllChildDevices().each { deleteChildDevice(it.deviceNetworkId) }
}

private updateDevices(evt) {
  log.debug "updateDevices: ${evt}"

  def device = getChildDevice("epson|projector${evt.deviceId}")
  if (device) {
    device.update(evt)
  }
}

private getHttpHeaders(headers) {
	def obj = [:]
	new String(headers.decodeBase64()).split("\r\n").each {param ->
    def nameAndValue = param.split(":")
    obj[nameAndValue[0]] = (nameAndValue.length == 1) ? "" : nameAndValue[1].trim()
	}
  return obj
}

private getHttpBody(body) {
  def obj = null;
  if (body) {
    def slurper = new JsonSlurper()
    obj = slurper.parseText(new String(body.decodeBase64()))
  }
  return obj
}

private getProxyAddress() {
	return settings.proxyAddress + ":" + settings.proxyPort
}

private getNotifyAddress() {
  return settings.hostHub.localIP + ":" + settings.hostHub.localSrvPortTCP
}

private String convertIPtoHex(ipAddress) {
  String hex = ipAddress.tokenize( '.' ).collect {  String.format( '%02x', it.toInteger() ) }.join().toUpperCase()
  return hex
}

private String convertPortToHex(port) {
  String hexport = port.toString().format( '%04x', port.toInteger() ).toUpperCase()
  return hexport
}
