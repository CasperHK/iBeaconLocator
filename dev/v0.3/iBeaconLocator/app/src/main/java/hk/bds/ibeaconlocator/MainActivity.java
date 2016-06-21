package hk.bds.ibeaconlocator;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.RemoteException;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.service.RangedBeacon;
import org.w3c.dom.Text;

import java.text.DecimalFormat;
import java.util.Collection;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements BeaconConsumer {

    protected static final String TAG = "MainActivity"; // This is used for debug
    private MyBeacon b1, b2, b3, theNear;
    private TextView displayName1,
            displayName2,
            displayName3,
            displayDistance1,
            displayDistance2,
            displayDistance3,
            displayLocation;
    private BeaconManager beaconManager;
    private TextToSpeech ttobj;
    private MyBeacon previousLocation = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Init displays
        setContentView(R.layout.activity_main);
        displayName1 = (TextView) findViewById(R.id.beacon1);
        displayName2 = (TextView) findViewById(R.id.beacon2);
        displayName3 = (TextView) findViewById(R.id.beacon3);
        displayDistance1 = (TextView) findViewById(R.id.distance1);
        displayDistance2 = (TextView) findViewById(R.id.distance2);
        displayDistance3 = (TextView) findViewById(R.id.distance3);
        displayLocation = (TextView) findViewById(R.id.location);

        // Init Beacon
        beaconManager = BeaconManager.getInstanceForApplication(this);
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24")); // Set the beacon brand
        //beaconManager.setRssiFilterImplClass(ArmaRssiFilter.class);
        RangedBeacon.setSampleExpirationMilliseconds(5000);    // The refresh interval
        beaconManager.setBackgroundScanPeriod(20);
        beaconManager.setBackgroundBetweenScanPeriod(10);
        beaconManager.setForegroundBetweenScanPeriod(10);
        beaconManager.bind(this);

        // Init beacon devices
        b1 = new MyBeacon("Room S505", "BC:6A:29:25:0F:52", displayName1, displayDistance1);
        b2 = new MyBeacon("Room S506", "BC:6A:29:27:A4:2D", displayName2, displayDistance2);
        b3 = new MyBeacon("Room S507", "BC:6A:29:28:01:BD", displayName3, displayDistance3);

        // Create Google TextToSpeech Object
        ttobj = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {
                    ttobj.setLanguage(Locale.UK); // Set to "UK" language
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        beaconManager.unbind(this);
    }

    @Override
    public void onBeaconServiceConnect() {
        //This method will be called when the Beacon Manager is binded.
        beaconManager.setRangeNotifier( new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
                // This method will be executed many times according to the size of the refresh interval.
                // Try to update the distance of the devices
                for (Beacon theBeacon : beacons) {
                    b1.updateDistance(theBeacon);
                    b2.updateDistance(theBeacon);
                    b3.updateDistance(theBeacon);
                }
                theNear = getMinOne(getMinOne(b1, b2), b3);

                // Update Displays
                MainActivity.this.runOnUiThread( new Runnable() {
                    public void run() {
                        if (theNear != null && theNear.distance < 0.30) {
                            // If the near beacon is inside the range, do this action
                            displayLocation.setText(theNear.name); // Update the largest text.

                            if (theNear != previousLocation) {
                                // If you just enter the area
                                String toSpeak = "" + theNear.name;
                                ttobj.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null); // Speak
                            }

                        } else {
                            // You are not close enough to the near one.
                            displayLocation.setText("UNCERTAIN");
                            previousLocation = null;
                        }
                        previousLocation = theNear; // Store the previous location
                        // Update the distance display
                        b1.updateDisplayDistance();
                        b2.updateDisplayDistance();
                        b3.updateDisplayDistance();
                    }
                });
            }
        });

        try {
            // Tells the BeaconService to start looking for beacons that match the passed Region object,
            // and providing updates on the estimated mDistance every seconds while beacons in the Region are visible.
            beaconManager.startRangingBeaconsInRegion(new Region("myRangingUniqueId", null, null, null));
        } catch (RemoteException e) {  /* Error is detected. */  }

    }

    // This method is used to compare which one is the nearest to you and return that object back.
    // "null" means that cannot detect any device or can detect them but do not know the answer.
    private MyBeacon getMinOne(MyBeacon a, MyBeacon b) {
        if (a != null && b != null) {
            if ((a.distance > 0d && b.distance > 0d) && !(a.distance == b.distance)) {
                if (b.distance < a.distance)
                    return b;
                else
                    return a;
            } else if ((a.distance <= 0d && b.distance <= 0d) || (a.distance == b.distance)) {
                return null;
            } else {
                if (a.distance == 0d)
                    return b;
                else
                    return a;
            }
        } else if ((a == null && b == null) || (a == null && b.distance <= 0d) || (b == null && a.distance <= 0d)){
            return null;
        } else {
            if (a != null)
                return a;
            else
                return b;
        }
    }

}

class MyBeacon {
    public String name;
    public String macAddress;
    public double distance = 0d; // initially the distance is 0.
    //public double predistance = 0d;

    // Display reference pointers.
    public TextView displayName;
    public TextView displayDistance;


    public MyBeacon(String _name, String _macAddress, TextView _displayName, TextView _displayDistance) {
        name = _name;
        macAddress = _macAddress;
        displayName = _displayName;
        displayDistance = _displayDistance;
        displayName.setText(name + " :  ");
    }

    public void updateDistance(Beacon _beacon) {
        if (_beacon.getBluetoothAddress().equals(this.macAddress)) {
            distance = _beacon.getDistance(); // Calculate the distance based on RSSI.
        }
    }

    /*
    public boolean updateDistance(Beacon _beacon) {
        if (_beacon.getBluetoothAddress().equals(this.macAddress)) {
            distance = _beacon.getDistance(); // Calculate the distance based on RSSI.
            return true;
        } else
            return false;
    }
    */

    /*
    public void updateDistance(Collection<Beacon> beacons) {
        for (Beacon theBeacon : beacons) {
            if (updateDistance(theBeacon))
                return;
        }
        distance = 0d;

    }
    */

    public void updateDisplayDistance() {
        String str;
        if (distance == 0d)
            str = "UNDETECTED";
        else
            str = String.format("%.4f", distance);
        displayDistance.setText(str);
    }

    public String toString() {
        String str = "";
        str += "\n===================================";
        str += "\nBeacon Name: " + this.name;
        str += "\nMac Address: " + this.macAddress;
        str += "\nDistance   : " + this.distance;
        str += "\n===================================";
        return str;
    }
}
