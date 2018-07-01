package com.example.adammaziakowski.shimmer;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.shimmerresearch.android.Shimmer;
import com.shimmerresearch.android.guiUtilities.ShimmerBluetoothDialog;
import com.shimmerresearch.bluetooth.ShimmerBluetooth;
import com.shimmerresearch.driver.CallbackObject;
import com.shimmerresearch.driver.Configuration;
import com.shimmerresearch.driver.FormatCluster;
import com.shimmerresearch.driver.ObjectCluster;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import static com.shimmerresearch.android.guiUtilities.ShimmerBluetoothDialog.EXTRA_DEVICE_ADDRESS;

public class CalibrationActivity extends AppCompatActivity {

    private static final String TAG = "CalibrationActivity";


    private TextView countdownTV;
    private TextView commandTV;
    private double relaxed;
    private double stressed;
    private ArrayList<Double> list;

    Shimmer shimmer;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_callibration);

        shimmer = new Shimmer(mHandler);

        connectDevice();


        countdownTV = findViewById(R.id.countdownTV);
        commandTV = findViewById(R.id.commandTV);

        countdownTV.setText("60");
        commandTV.setText(R.string.prepare);


    }

    public void startAction(View view) {
        commandTV.setText(R.string.relax);
        relaxed = measureTime(60);
        commandTV.setText(R.string.prepare_stress);
        measureTime(30);
        commandTV.setText(R.string.stress);
        stressed = measureTime(60);

        float treshold = (float) (relaxed+stressed)/2;

        SharedPreferences sharedPreferences = this.getSharedPreferences("PREFS", Context.MODE_PRIVATE);
        sharedPreferences.edit().putFloat("TRESHOLD", treshold).apply();
        finish();

    }

    private double measureTime(int i) {
        //odliczanie i sekund, w tym czasie pomiar, list jest temp
        startStreaming();
        new CountDownTimer(1000 * i, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                countdownTV.setText(String.valueOf(millisUntilFinished/1000));
            }

            @Override
            public void onFinish() {

            }

        }.start();
        stopStreaming();
        if (list.size()>0) {
            double average = 0;
            for (Double d : list
                    ) {
                average += d;
            }
            average /= list.size();
            list.clear();
            return average;
        }else
            return 0.0;
    }


    @SuppressLint("HandlerLeak")
    Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {
                case ShimmerBluetooth.MSG_IDENTIFIER_DATA_PACKET:
                    if ((msg.obj instanceof ObjectCluster)) {

                        ObjectCluster objectCluster = (ObjectCluster) msg.obj;
                        Log.i(TAG, objectCluster.mSensorDataList.toString());

                        //Retrieve all possible formats for the current sensor device:
                        Collection<FormatCluster> allFormats = objectCluster.getCollectionOfFormatClusters(Configuration.Shimmer3.ObjectClusterSensorName.TIMESTAMP);
                        FormatCluster timeStampCluster = ((FormatCluster)ObjectCluster.returnFormatCluster(allFormats,"CAL"));
                        double timeStampData = timeStampCluster.mData;
                        Log.i(TAG, "Time Stamp: " + timeStampData);
                        allFormats = objectCluster.getCollectionOfFormatClusters(Configuration.Shimmer3.ObjectClusterSensorName.GSR_CONDUCTANCE);
                        FormatCluster conductanceCluster = ((FormatCluster)ObjectCluster.returnFormatCluster(allFormats, "CAL"));
                        if (conductanceCluster!=null) {
                            double conductance = conductanceCluster.mData;
                            list.add(conductance);
                            Log.i(TAG, "conductance: " + conductance);
                        }else{
                            Log.i(TAG, "null");
                        }
                    }
                    break;
                case Shimmer.MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString(Shimmer.TOAST), Toast.LENGTH_SHORT).show();
                    break;
                case ShimmerBluetooth.MSG_IDENTIFIER_STATE_CHANGE:
                    ShimmerBluetooth.BT_STATE state = null;
                    String macAddress = "";

                    if (msg.obj instanceof ObjectCluster) {
                        state = ((ObjectCluster) msg.obj).mState;
                        macAddress = ((ObjectCluster) msg.obj).getMacAddress();
                    } else if (msg.obj instanceof CallbackObject) {
                        state = ((CallbackObject) msg.obj).mState;
                        macAddress = ((CallbackObject) msg.obj).mBluetoothAddress;
                    }

                    assert state != null;
                    switch (state) {
                        case CONNECTED:
                            break;
                        case CONNECTING:
                            break;
                        case STREAMING:
                            break;
                        case STREAMING_AND_SDLOGGING:
                            break;
                        case SDLOGGING:
                            break;
                        case DISCONNECTED:
                            break;
                    }
                    break;
            }
            super.handleMessage(msg);
        }
    };

    public void connectDevice() {
        Intent intent = new Intent(getApplicationContext(), ShimmerBluetoothDialog.class);
        startActivityForResult(intent, ShimmerBluetoothDialog.REQUEST_CONNECT_SHIMMER);
    }

    public void startStreaming(){
        shimmer.startStreaming();
    }

    public void stopStreaming(){
        shimmer.stopStreaming();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == 2) {
            if (resultCode == Activity.RESULT_OK) {
                //Get the Bluetooth mac address of the selected device:
                String macAdd = data.getStringExtra(EXTRA_DEVICE_ADDRESS);
                shimmer = new Shimmer(mHandler);
                shimmer.connect(macAdd, "default");                  //Connect to the selected device
            }

        }
        super.onActivityResult(requestCode, resultCode, data);
    }

}
