package hk.bds.ibeaconlocator;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.RemoteException;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.service.RangedBeacon;

import java.util.Collection;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements BeaconConsumer {

    protected static final String TAG = "MainActivity";

    private MyBeacon b1, b2, b3, theNear;
    private String currentLocation;
    private TextView displayName1,
            displayName2,
            displayName3,
            displayDistance1,
            displayDistance2,
            displayDistance3,
            displayLocation;
    private BeaconManager beaconManager;
    private Thread displayThread;
    private TextToSpeech ttobj;
    private MyBeacon previousLocation = null;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        beaconManager = BeaconManager.getInstanceForApplication(this);
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"));
        RangedBeacon.setSampleExpirationMilliseconds(50000);    // The refresh interval
        beaconManager.bind(this);



        // Init displays
        displayName1 = (TextView) findViewById(R.id.beacon1);
        displayName2 = (TextView) findViewById(R.id.beacon2);
        displayName3 = (TextView) findViewById(R.id.beacon3);
        displayDistance1 = (TextView) findViewById(R.id.distance1);
        displayDistance2 = (TextView) findViewById(R.id.distance2);
        displayDistance3 = (TextView) findViewById(R.id.distance3);
        displayLocation = (TextView) findViewById(R.id.location);

        // Init beacon devices
        b1 = new MyBeacon("Class Room", "BC:6A:29:27:68:B4", displayName1, displayDistance1);
        b2 = new MyBeacon("Football Room", "BC:6A:29:25:14:01", displayName2, displayDistance2);
        b3 = new MyBeacon("The Sky", "BC:6A:29:27:71:A9", displayName3, displayDistance3);



        ttobj=new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {
                    ttobj.setLanguage(Locale.UK);
                }
            }
        });

        //Get location permission
        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(thisActivity,
                    Manifest.permission.READ_CONTACTS)) {

                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(thisActivity,
                        new String[]{Manifest.permission.READ_CONTACTS},
                        MY_PERMISSIONS_REQUEST_READ_CONTACTS);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_READ_CONTACTS: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        beaconManager.unbind(this);
    }

    @Override
    public void onBeaconServiceConnect() {
        beaconManager.setRangeNotifier( new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) { // This method will be executed many times.
                // Try to update the distance of the devices
                b1.updateDistance(beacons);
                b2.updateDistance(beacons);
                b3.updateDistance(beacons);
                theNear = getMinOne(getMinOne(b1, b2), b3);

                // Update Displays
                MainActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        if (theNear != null && theNear.distance < 35d) {
                            // You are now at theNear beacon
                            displayLocation.setText(theNear.name);

                            if (theNear != previousLocation) {
                                String toSpeak = "You are now at " + theNear.name;
                                ttobj.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null);
                            }

                        } else {
                            // You are not close enough
                            displayLocation.setText("UNCERTAIN");
                            previousLocation = null;
                        }
                        previousLocation = theNear;
                        b1.updateDisplayDistance();
                        b2.updateDisplayDistance();
                        b3.updateDisplayDistance();
                    }
                });
            }
        });

        try {
            beaconManager.startRangingBeaconsInRegion(new Region("myRangingUniqueId", null, null, null));
        } catch (RemoteException e) {    }

    }

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
    public TextView displayName;
    public TextView displayDistance;


    public MyBeacon(String _name, String _macAddress, TextView _displayName, TextView _displayDistance) {
        name = _name;
        macAddress = _macAddress;
        displayName = _displayName;
        displayDistance = _displayDistance;
        displayName.setText(name + " :  ");
    }

    public boolean updateDistance(Beacon _beacon) {
        if (_beacon.getBluetoothAddress().equals(this.macAddress)) {
            distance = _beacon.getDistance();
            return true;
        } else
            return false;
    }

    public void updateDistance(Collection<Beacon> beacons) {
        for (Beacon theBeacon : beacons) {
            if (updateDistance(theBeacon))
                return;
        }
        distance = 0d;
    }

    public void updateDisplayDistance() {
        String str;
        if (distance == 0d)
            str = "UNDETECTED";
        else
            str = String.format("%.2f", distance);
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