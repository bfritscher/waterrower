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

  var startWorkout = function() {
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
      "stroke-rate": function(value) {
        setNumber('stroke-rate', 2, value);
      },
      "total-strokes": function(value) {
        setContents('total-strokes', value);
      },
      "avg-distance-cmps": function(value) {
        if (value > 0) {
          setContents('avg-mps', (value / 100).toFixed(2));
          var totalSeconds = (500 / (value / 100));
          setNumber('avg-500m-min', 2, Math.floor(totalSeconds / 60).toFixed(0));
          setNumber('avg-500m-sec', 2, Math.floor(totalSeconds % 60));
        }
      },
      "total-kcal": function(value) {
        if (value > 0) {
          setNumber("kcal", 4, (value / 1000).toFixed(2));
        }
      }
    }

    ws.onopen = function() {
      console.log("connected");
    };
    ws.onclose = function() {
      console.log("close");
    };
    ws.onmessage = function(msg) {
      data = JSON.parse(msg.data);
      command = commands[data.type];
      if (command != null) {
        command(data.value);
      }
    };
  };

  return {
    startWorkout: startWorkout
  }
})();
