package com.wowwee.revandroidsampleproject.fragments;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.AndroidRuntimeException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.wowwee.bluetoothrobotcontrollib.rev.REVRobot;
import com.wowwee.bluetoothrobotcontrollib.rev.REVRobotFinder;
import com.wowwee.revandroidsampleproject.R;
import com.wowwee.revandroidsampleproject.fragments.BaseViewFragment;
import com.wowwee.revandroidsampleproject.fragments.DriveViewFragment;
import com.wowwee.revandroidsampleproject.fragments.FragmentHelper;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class ScanFragment extends BaseViewFragment {

    private static final int REQUEST_ENABLE_BT = 1;
    private boolean mScanning;
    private BluetoothAdapter mBluetoothAdapter;
    private Handler handler;

    // Connect logic
    private Timer tapTimer;
    private static final int CONNECTION_IDLE = 0;
    private static final int CONNECTION_SCANNING = 1;
    private static final int CONNECTION_SCAN_HOLD = 2;
    private static final int CONNECTION_CONNECTING = 3;
    private static final int CONNECTION_CONNECTED = 4;
    private int connectionState = CONNECTION_IDLE;
    private long connectTimestamp;
    private long closestTimestamp;
    private REVRobot closestRev = null;

    public ScanFragment() {
        super(R.layout.fragment_scan);
    }

    //================================================================================
    // Override
    //================================================================================

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (container == null)
            return null;

        View view = super.onCreateView(inflater, container, savedInstanceState);
        handler = new Handler();

        // Init bluetooth
        initBluetooth();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        getFragmentActivity().registerReceiver(mRevFinderBroadcastReceiver, REVRobotFinder.getRevRobotFinderIntentFilter());

        BluetoothManager btManager = (BluetoothManager)getFragmentActivity().getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter btAdapter = btManager.getAdapter();

        if (btAdapter != null && !btAdapter.isEnabled()) {
            try {
                Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            } catch (ActivityNotFoundException ax) {
                askBluetoothActivationManually();
            } catch (AndroidRuntimeException ax) {
                askBluetoothActivationManually();
            }
        }

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        REVRobotFinder.getInstance().clearFoundREVList();

        scanLeDevice(false);
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        scanLeDevice(true);

        // Start timer
        tapTimer = new Timer();
        int delay = 0;
        int period = 500;
        tapTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                tapTimerAction();
            }
        }, delay, period);

        // Connection state
        setConnectionState(CONNECTION_SCANNING);
    }

    @Override
    public void onPause() {
        super.onPause();
        scanLeDevice(false);
        tapTimer.cancel();
//        unregisterReceiver(mRevFinderBroadcastReceiver);
    }

    //================================================================================
    // Bluetooth
    //================================================================================

    private void askBluetoothActivationManually() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getFragmentActivity());

        builder.setCancelable(true);

        builder.setMessage("bluetooth_enable_question");
        builder.setTitle("bluetooth_enable_dialog_title");
        builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        BluetoothAdapter.getDefaultAdapter().enable();
                    }}
        );

        builder.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }}
        );

        builder.show();
    }

    private void initBluetooth(){
        final BluetoothManager bluetoothManager = (BluetoothManager) getFragmentActivity().getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        REVRobotFinder.getInstance().setBluetoothAdapter(mBluetoothAdapter);
        REVRobotFinder.getInstance().setApplicationContext(getFragmentActivity());
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            Log.d(getClass().getName(), "Scan Le device start");
            // Stops scanning after a pre-defined scan period.
//        	final double scanTime = System.currentTimeMillis();
            mScanning = true;
            REVRobotFinder.getInstance().scanForREVContinuous();
        }else{
            Log.d(getClass().getName(), "Scan Le device stop");
            mScanning = false;
            REVRobotFinder.getInstance().stopScanForREVContinuous();
        }
    }

    public boolean IsScanning() {
        return mScanning;
    }

    //================================================================================
    // Timer action
    //================================================================================

    private void tapTimerAction() {
        if(connectionState == CONNECTION_SCANNING || connectionState == CONNECTION_SCAN_HOLD) {
            List<REVRobot> revFound = REVRobotFinder.getInstance().getRevFoundList();
            int closeRSSI = -65;
            long connectWait = 1500;
            if(closestRev != null) {
                if(closestRev.rssi >= closeRSSI) {
                    // Check timestamp
                    if(System.currentTimeMillis() - closestTimestamp >= connectWait) {
                        setConnectionState(CONNECTION_CONNECTING);
                        closestRev.setCallbackInterface(this);
                        closestRev.connect(getFragmentActivity());
                        scanLeDevice(false);
                    }
                }
                else {
                    closestRev = null;
                }
            }

            if(closestRev == null) {
                for(REVRobot rev : revFound) {
                    if(rev.rssi >= closeRSSI) {
                        closestRev = rev;
                        closestTimestamp = System.currentTimeMillis();
                        break;
                    }
                }
            }

            if(connectionState == CONNECTION_SCANNING && closestRev != null) {
                setConnectionState(CONNECTION_SCAN_HOLD);
            }
            else if(connectionState == CONNECTION_SCAN_HOLD && closestRev == null) {
                setConnectionState(CONNECTION_SCANNING);
            }
        }
    }

    //================================================================================
    // Connect Logic
    //================================================================================

    private void setConnectionState(int state) {
        if(connectionState != state) {
            connectionState = state;
            switch (connectionState) {
                default:
                case CONNECTION_SCANNING:
                    Log.d(getClass().getName(), "CONNECTION_SCANNING");
                    break;
                case CONNECTION_SCAN_HOLD:
                    Log.d(getClass().getName(), "CONNECTION_SCAN_HOLD");
                    connectTimestamp = System.currentTimeMillis();
                    break;
                case CONNECTION_CONNECTING:
                    break;
                case CONNECTION_CONNECTED: {
                    long connectDeltaTime = System.currentTimeMillis() - connectTimestamp;
                    long delay = 1200 - connectDeltaTime;
                    if(delay < 0) {
                        delay = 0;
                    }

                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Log.d(getClass().getName(), "CONNECTION_CONNECTED");
                        }
                    }, delay);

                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            // Proceed to DriveView
                            getFragmentActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Log.d(getClass().getName(),"Go to game page.");
                                    FragmentHelper.switchFragment(getFragmentActivity().getSupportFragmentManager(), new DriveViewFragment(), R.id.view_id_content, false);
                                }
                            });
                        }
                    }, (delay + 1000));

                }
                break;
            }
        }
    }

    //================================================================================
    // REVRobotFinder broadcast receiver
    //================================================================================

    private final BroadcastReceiver mRevFinderBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (REVRobotFinder.REVRobotFinder_REVFound.equals(action)){
                BluetoothDevice device = (BluetoothDevice)(intent.getExtras().get("BluetoothDevice"));
                Log.d(getClass().getName(), "RevScanFragment broadcast receiver found REV: " + device.getName());
            }
        }
    };

    //================================================================================
    // REVRobot callback
    //================================================================================

    @Override
    public void revDeviceReady(REVRobot rev) {
        Log.d(getClass().getName(), "revDeviceReady!");

        // REV connected
        closestRev = null;

        // Set connection state
        if (getFragmentActivity() != null) {
            getFragmentActivity().runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    // Animate connected
                    setConnectionState(CONNECTION_CONNECTED);
                }
            });
        }
    }

    @Override
    public void revDeviceDisconnected(REVRobot rev) {
        Log.d(getClass().getName(), "revDeviceDisconnected!");
        setConnectionState(CONNECTION_SCANNING);
    }
}
