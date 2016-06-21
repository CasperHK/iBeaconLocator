Beacons Layouts
===========================

The following shows the beason layouts used in the android beacon library

Beacon | Layout
:------| :-------
ALTBEACON | m:2-3=beac,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25
EDDYSTONE TLM | x,s:0-1=feaa,m:2-2=20,d:3-3,d:4-5,d:6-7,d:8-11,d:12-15
EDDYSTONE  UID | s:0-1=feaa,m:2-2=00,p:3-3:-41,i:4-13,i:14-19
EDDYSTONE  URL | s:0-1=feaa,m:2-2=10,p:3-3:-41,i:4-20v
IBEACON	| m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24