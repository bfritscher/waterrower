var Rower = {};

Rower.ui = (function($) {
  var startLinkClick = function(f) {
    return $("#start-workout").click(f);
  };

  var getDistance = function() {
    return parseInt($("#distance").val());
  };

  var setContents = function(id, value) {
    $("#" + id).text(value);
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

  return {
    startLinkClick: startLinkClick,
    getDistance: getDistance,
    setHour: function(v) {
      setNumber('hour', 2, v);
    },
    setMin: function(v) {
      setNumber('min', 2, v);
    },
    setSec: function(v) {
      setNumber('sec', 2, v);
    },
    setSecDec: function(v) {
      setNumber('sec-dec', 1, v);
    },
    setTotalDistance: function(v) {
      setNumber('total-distance-m', 5, v);
    },
    setMetersRemaining: function(v) {
      setNumber('total-meters-remaining', 5, v);
    },
    setStrokeRate: function(v) {
      setNumber('stroke-rate', 2, v);
    },
    setTotalStrokes: function(v) {
      setContents('total-strokes', v);
    },
    setAverageMps: function(v) {
      setContents('avg-mps', v);
    },
    setAverageFiveHundredSplit: function(m, s) {
      setNumber('avg-500m-min', 2, m);
      setNumber('avg-500m-sec', 2, s);
    },
    setKCal: function(v) {
      setNumber("kcal", 4, v);
    },
    status: function(msg, cssClass) {
      $("#status").html("<span class='" + cssClass + "'>" + msg + "</span>");
    },
  };

}(jQuery));

Rower.dashboard = (function($) {
  var ui = Rower.ui;
  var ws;
  var distance = 0;

  var commands = {
    "display-hr":       ui.setHour,
    "display-min":      ui.setMin,
    "display-sec":      ui.setSec,
    "display-sec-dec":  ui.setSecDec,
    "total-distance-m": function(v) {
      ui.setTotalDistance(v);
      ui.setMetersRemaining(distance - v);
    },
    "stroke-rate":      ui.setStrokeRate,
    "total-strokes":    ui.setTotalStrokes,
    "avg-distance-cmps": function(v) {
      if (v > 0) {
        ui.setAverageMps((v / 100).toFixed(2));
        var totalSeconds = (500 / (v / 100));
        var minutes = Math.floor(totalSeconds / 60).toFixed(0);
        var seconds = Math.floor(totalSeconds % 60);
        ui.setAverageFiveHundredSplit(minutes, seconds);
      }
    },
    "total-kcal": function(value) {
      if (value > 0) {
        ui.setKCal((value / 1000).toFixed(2));
      }
    }
  };

  var startWorkout = function() {
    distance = ui.getDistance();
    var data = JSON.stringify({type: "start-workout",
                               data: {type: "distance",
                                      units: "meters",
                                      value: distance}});
    console.log("starting: " + data);
    ui.setMetersRemaining(distance);
    ws.send(data);
  };

  var init = function() {
    ui.status("connecting...", "connecting");
    ws = new ReconnectingWebSocket('ws://' + document.location.host + '/ws');
    ws.onopen  = function() { ui.status("connected", "connected"); }
    ws.onclose = function() { ui.status("disconnected", "disconnected") };
    ws.onmessage = function(msg) {
      data = JSON.parse(msg.data);
      command = commands[data.type];
      if (command != null) {
        command(data.value);
      }
    };
    ui.startLinkClick(startWorkout);
  };

  return {
    init: init
  }
})(jQuery);

Rower.history = (function($) {

  var plotGraph = function($div, data) {
    $div.css("height", "200px")
      .css("margin", "10px 0 0 0")
      .css("padding", "10px");
    $.plot($div, data, {
      series: {
        lines: { show: true,
                 fill: true },
        points: { show: false },
        shadowSize: 0
      },
      xaxes: [{ position: "bottom", mode: "time", ticks: 30 }],
      yaxes: [{ position: "left", min: 350, max: 520 },
              { position: "right", min: 20, max: 32 }]
    });
  };

  var showSession = function(session) {
    return function(data, status, req) {
      plotGraph($(session.find(".graphs .avg-speed")),
                [{ label: "avg speed",
                   data: data["avg-speed"],
                   yaxis: 1 },
                 { label: "stroke rate",
                   data: data["stroke-rate"],
                   yaxis: 2}]);
    };
  };

  var expandSession = function() {
    var $session = $(this).parents(".session");
    var filename = $(this).attr("href");
    $.ajax({
      url: "http://" + document.location.host + "/session",
      dataType: "json",
      data: "filename=" + filename,
      success: showSession($session),
      error: function(req, txt, err) {
        console.log(err);
      }
    });
    return false;
  };

  var init = function() {
    $('.expand-link').click(expandSession);
  };

  return {
    init: init
  };
})(jQuery);
