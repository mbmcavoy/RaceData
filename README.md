Concept development for motor sports data acquisition and telemetry data formats, protocols, and API.

This is exploring potential major updates to AutosportLabs' RaceCapture/Pro system.

RaceData
========
Contains the core classes to be used by client or server applications.
 
DataExerciser1
==============
This is a simple console app that exercises the RaceData library.

This reads a CSV-format data file ("rc_0.log") recorded with a current-version RaceCapture/Pro. This
data is used to mimic both data recording (storing to disk) and telemetry (storing in memory). The
resulting data sets are compared with the log file again and the results are displayed in the console.

TODO: implement basic client-server applications that use CoAP to communicate in a realistic simulation
of telemetry.