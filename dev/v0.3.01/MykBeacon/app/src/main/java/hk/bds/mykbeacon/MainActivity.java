package hk.bds.mykbeacon;

import android.os.RemoteException;
import android.speech.tts.TextToSpeech;
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

public class MainActivity extends AppCompatActivity implements BeaconConsumer {

    private TextView   displayName1,
                        displayName2,
                        displayName3,
                        displayDistance1,
                        displayDistance2,
                        displayDistance3,
                        displayLocation;

    private BeaconManager beaconManager;


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
        RangedBeacon.setSampleExpirationMilliseconds(1100);    // The refresh interval
        beaconManager.setBackgroundBetweenScanPeriod(20);
        beaconManager.setForegroundBetweenScanPeriod(20);
        beaconManager.bind(this);
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
                if (beacons.size() > 0) {

                }

            }
        });

        try {
            // Tells the BeaconService to start looking for beacons that match the passed Region object,
            // and providing updates on the estimated mDistance every seconds while beacons in the Region are visible.
            beaconManager.startRangingBeaconsInRegion(new Region("myRangingUniqueId", null, null, null));
        } catch (RemoteException e) {  /* Error is detected. */  }

    }
}