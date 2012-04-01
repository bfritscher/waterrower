var Rower = {};

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
      url: "http://" + document.location.host + "/session/" + filename,
      dataType: "json",
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
