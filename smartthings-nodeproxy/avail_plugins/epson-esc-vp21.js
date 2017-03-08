/**
 *  Epson ESC/VP21 Projector API
 *
 *  Author: admin@domabot.com
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
 */
var express = require('express');
var serialport = require("serialport");
var app = express();
var nconf = require('nconf');
nconf.file({ file: './config.json' });
var notify;
var logger = function(str) {
  mod = 'escvp21';
  console.log("[%s] [%s] %s", new Date().toISOString(), mod, str);
}

/**
 * Routes
 */
app.get('/', function (req, res) {
  res.status(200).json({ status: 'Epson Projector ESC/VP21 plugin running' });
});

app.get('/off', function (req, res) {
  vp21.command('PWR OFF');
  res.end();
});

app.get('/on', function (req, res) {
  vp21.command('PWR ON');
  res.end();
});


app.get('/status', function (req, res) {
  vp21.command('PWR?');
  res.end();
});

module.exports = function(f) {
  notify = f;
  return app;
};

/**
 * ESC/VP21
 */
var vp21 = new ESCVP21();
vp21.init();

function ESCVP21 () {
  var self = this;
  var device = null;
  var serialPorts = new Array();
  var queue = [];

  /**
   * init
   */
  this.init = function() {
    getSerialPorts();

    if (!nconf.get('espon:serialPort')) {
        logger('** NOTICE ** Epson serial port not set in config file!');
        return;
    }

    if (device && device.isOpen()) { return };

    device = new serialport(nconf.get('epson:serialPort'), {
        parser: serialport.parsers.readline('\n'),
        baudrate: 9600,
        autoOpen: false
      });

    device.on('data', function(data) {
      read(data);
    });

    device.open(function(error) {
      if (error) {
        logger('Epson Projector connection error: '+error);
        device = null;
        return;
      } else {
        logger('Connected to Epson Projector: '+nconf.get('epson:serialPort'));
      }
    });
  };

  // check connection every 60 secs
  setInterval(function() { self.init(); }, 60*1000);

  /**
   * write
   */
  function write(cmd) {
    if (!device || !device.isOpen()) {
      logger('Epson Projector not connected.');
      return;
    }

    if (!cmd || cmd.length == 0) { return; }
    device.write(cmd, function(err, results) {
      if (err) logger('Epson Projector write error: '+err);
    });
  }

  this.command = function(cmd) {
    write(cmd + "\r");
  };

  /**
   * read
   */
  function read(data) {
    if (data.length == 0) { return; }

    try {
      if (data.match(/^:/)) {
        response_handler(data);
      }
    } catch (err) {
      logger('Error: '+err);
    }
  }

  function response_handler(data) {
    notify({type: 'status', data: data.substr(1)});
  }

  /**
   * getSerialPorts
   */
  function getSerialPorts() {
    if (serialPorts.length > 0) { return; }
    serialport.list(function (err, ports) {
      ports.forEach(function(port) {
        serialPorts.push(port.comName);
      });
      logger('Detected serial ports: ' + JSON.stringify(serialPorts));
    });
  }

  /**
   * discover
   */
  this.discover = function() {
    if (nconf.get('epson:projectorConfig')) {
      notify(JSON.stringify(nconf.get('epson:projectorConfig')));
      logger('Completed projector discovery');
    } else {
      logger('** NOTICE ** Panel configuration not set in config file!');
    }
  };


}
