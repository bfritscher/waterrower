var Rower = (function() {
  var setContents = function(id, value) {
    document.getElementById(id).innerHTML = value;
  };

  var setNumber = function(id, width, n) {
    setContents(id, padNumber(width, n));
  };

  var padNumber = function(width, n) {
    while (String(n).length < width) {
      n = "0" + n;
    }
    return n;
  };

  var init = function() {
    console.log("opening websocket");
    var ws = new WebSocket('ws://' + document.location.host + '/rower');

    var commands = {
      "display-hr": function(value) {
        setNumber('hour', 2, value);
      },
      "display-min": function(value) {
        setNumber('min', 2, value);
      },
      "display-sec": function(value) {
        setNumber('sec', 2, value);
      },
      "display-sec-dec": function(value) {
        setNumber('sec-dec', 1, value);
      },
      "total-distance-m": function(value) {
        setNumber('total-distance-m', 5, value);
      },
      "total-distance-dec": function(value) {
        setNumber('total-distance-dec', 1, value);
      },
      "stroke-rate": function(value) {
        setNumber('stroke-rate', 2, value);
      },
      "avg-distance-cmps": function(value) {
        if (value > 0) {
          setContents('avg-mps', (value / 100).toFixed(2));
          var totalSeconds = (500 / (value / 100));
          setNumber('avg-500m-min', 2, (totalSeconds / 60).toFixed(0));
          setNumber('avg-500m-sec', 2, Math.floor(totalSeconds % 60));
        }
      }
    }

    ws.onmessage = function(msg) {
      data = JSON.parse(msg.data);
      command = commands[data.type];
      if (command != null) {
        command(data.value);
      }
    };
  };

  return {
    init: init
  }
})();
