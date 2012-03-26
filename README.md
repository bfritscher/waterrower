# Rower

A Web UI for the [waterrower](http://www.waterrower.com/),
specifically with the S4 rowing computer.

The S4 must be connected via USB, you can then start the app with:

```bash
$ ./bin/rower
```

And open your browser to http://localhost:3000. You should then be
able to enter a distance and start the workout. All data will be
recorded to ./data/yyyy-mm-dd-hh_MM_distance_units.cap. 

Alternatively, you can start the application in a development mode
(you do not need to connect to the rowing machine):

```bash
$ ./bin/rower -dev
```

Now when you start the workout stub generated data will be sent to 
the UI.

