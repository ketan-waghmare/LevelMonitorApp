package com.example.levelmonitorapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.ColorUtils;
import androidx.interpolator.view.animation.FastOutLinearInInterpolator;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import androidx.interpolator.view.animation.LinearOutSlowInInterpolator;

import android.animation.TimeInterpolator;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.transition.Explode;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AnticipateInterpolator;
import android.view.animation.AnticipateOvershootInterpolator;
import android.view.animation.BounceInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.example.levelmonitorapp.Utils.ApiUtils;
import com.ramijemli.percentagechartview.PercentageChartView;
import com.ramijemli.percentagechartview.callback.AdaptiveColorProvider;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import me.itangqi.waveloadingview.WaveLoadingView;

import static com.ramijemli.percentagechartview.renderer.BaseModeRenderer.ACCELERATE;
import static com.ramijemli.percentagechartview.renderer.BaseModeRenderer.ACCELERATE_DECELERATE;
import static com.ramijemli.percentagechartview.renderer.BaseModeRenderer.ANTICIPATE;
import static com.ramijemli.percentagechartview.renderer.BaseModeRenderer.ANTICIPATE_OVERSHOOT;
import static com.ramijemli.percentagechartview.renderer.BaseModeRenderer.BOUNCE;
import static com.ramijemli.percentagechartview.renderer.BaseModeRenderer.DECELERATE;
import static com.ramijemli.percentagechartview.renderer.BaseModeRenderer.FAST_OUT_LINEAR_IN;
import static com.ramijemli.percentagechartview.renderer.BaseModeRenderer.FAST_OUT_SLOW_IN;
import static com.ramijemli.percentagechartview.renderer.BaseModeRenderer.LINEAR_OUT_SLOW_IN;
import static com.ramijemli.percentagechartview.renderer.BaseModeRenderer.OVERSHOOT;

public class MainActivity extends AppCompatActivity {

    private String str = "";
    private String message = "";
    private String state = "Connect";

    private String state2;
    private String strEnabled;
    private String strConnect = "Connect";
    private String strConnected = "Connected";
    private String strCannotSend = "Can not Send";
    private String strDisconnect = "Disconnect";
    private String strMissedConnection = "Missed Connection";
    private String strBluetoothTurnedOn;

    private BluetoothSocket socket;
    private BluetoothDevice bluetoothDevice;
    private BluetoothAdapter bluetoothAdapter;

    private InputStream inputStreamHeightReceiver;
    private OutputStream outputStreamHeightReceiver;

    private int shadowColor;
    private float blur, distX, distY;
    private WaveLoadingView mWaveLoadingView;

    private double value;
    private double percent;
    private MediaPlayer mp;
    PercentageChartView mChart;
    private TextView tvReceivedData;
    private ProgressDialog progressDialog;
    private boolean isAudioPlaying = false;
    private Context context = MainActivity.this;
    private SharedPreferences sharedPreferenceBluetoothAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupUI();
        initPreference();
        initializeData();
        setLayout();
    }

    private void startLeveLIndicator() {
        mWaveLoadingView.setShapeType(WaveLoadingView.ShapeType.CIRCLE);
        mWaveLoadingView.setTopTitle("90");
        mWaveLoadingView.setCenterTitleColor(Color.GRAY);
        mWaveLoadingView.setBottomTitleSize(18);
        mWaveLoadingView.setProgressValue(80);
        mWaveLoadingView.setBorderWidth(10);
        mWaveLoadingView.setAmplitudeRatio(60);
        mWaveLoadingView.setWaveColor(Color.GREEN);
        mWaveLoadingView.setBorderColor(Color.GRAY);
        mWaveLoadingView.setTopTitleStrokeColor(Color.BLUE);
        mWaveLoadingView.setTopTitleStrokeWidth(3);
        mWaveLoadingView.setAnimDuration(3000);
        mWaveLoadingView.pauseAnimation();
        mWaveLoadingView.resumeAnimation();
        mWaveLoadingView.cancelAnimation();
        mWaveLoadingView.startAnimation();
    }

    private void setLayout() {
    }

    private void setupUI() {
        tvReceivedData = findViewById(R.id.tv_received_data);
        mWaveLoadingView = (WaveLoadingView) findViewById(R.id.waveLoadingView);
        mp = MediaPlayer.create(context, R.raw.siran);

    }

    private void initPreference() {

    }

    private void initializeData() {
        try {
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            @SuppressLint("MissingPermission")
            Set<BluetoothDevice> bluetoothDevices = bluetoothAdapter.getBondedDevices();
            if (bluetoothDevices.size() > 0) {
                for (BluetoothDevice device : bluetoothDevices) {

                    Log.e("device_getName_Log", ":" + device.getName());
                    if (device.getName().equals("HC-05") ||
                            device.getName().equals("HC05")) {
                        storeBluetoothInformation(device);
                        new Connect().execute(new String[]{device.getAddress()});
                    }
                }
            } else {
                Toast.makeText(context, "Cannot connect to bluetooth device", Toast.LENGTH_SHORT);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

  /*  private boolean savedDeviceAlreadyExists() {
        return !sharedPreferenceBluetoothAddress.getString("address", "").equals("");
    }*/

    private String getStoredDeviceAddress() {
        return sharedPreferenceBluetoothAddress.getString("address", "");
    }

    private void storeBluetoothInformation(BluetoothDevice device) {
        sharedPreferenceBluetoothAddress = getSharedPreferences(ApiUtils.BLUTOOTH_ADDRESS, MODE_PRIVATE);

        SharedPreferences.Editor editor = sharedPreferenceBluetoothAddress.edit();
        if (sharedPreferenceBluetoothAddress.getString("address", "").equalsIgnoreCase("")) {
            editor.putString("address", device.getAddress());
            editor.commit();
        }
    }


    public class Receiver extends AsyncTask<String, String, String> {
        public Receiver() {
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        public String doInBackground(String... params) {
            message = "";
            byte[] buffer = new byte[128];

            while (strEnabled.equals("true")) {
                try {
                    int bytes = inputStreamHeightReceiver.read(buffer);
                    message = new String(buffer, 0, bytes);
                } catch (IOException e) {
                    message = "";
                    strEnabled = "false";
                }

                publishProgress(new String[]{message, strEnabled});
            }

            return message;
        }

        public void onProgressUpdate(String... params) {

            try {
                str = params[0];

                value = Double.parseDouble(str);

                percent = mapValues(value);

                int roundUp = (int) percent;

                Log.e("str_Log_First", ":" + str);

                mWaveLoadingView.setTopTitle("value  " + str);

                if(Double.parseDouble(str) < 0.005){
                    stopPlaying();
                    tvReceivedData.setText("No Saline");
                    tvReceivedData.setTextSize(25);
                    mWaveLoadingView.setProgressValue(0);
                    mWaveLoadingView.setTopTitle("Value = "+str);
                    mWaveLoadingView.setAnimDuration(3000);
                    mWaveLoadingView.setWaveColor(Color.RED);
                    mWaveLoadingView.startAnimation();
                }else if (Double.parseDouble(str) <= 0.050 && Double.parseDouble(str) > 0.005) {
                    tvReceivedData.setText("Please Remove saline");
                    tvReceivedData.setTextSize(25);
                    tvReceivedData.setBackground(getResources().getDrawable(R.drawable.round_btn_small));
                    mWaveLoadingView.setProgressValue(roundUp);
                    mWaveLoadingView.setTopTitle("Value = "+str);
                    mWaveLoadingView.setAnimDuration(3000);
                    mWaveLoadingView.setWaveColor(Color.RED);
                    mWaveLoadingView.startAnimation();
                    playsound();
                } else if((Double.parseDouble(str) > 0.050)){
                    stopPlaying();
                    tvReceivedData.setText("Saline is ON");
                    tvReceivedData.setTextSize(25);
                    mWaveLoadingView.setProgressValue(roundUp);
                    mWaveLoadingView.setTopTitle("Value = "+str);
                    mWaveLoadingView.setAnimDuration(3000);
                    mWaveLoadingView.setWaveColor(Color.GREEN);
                    mWaveLoadingView.startAnimation();
                } else if(Double.parseDouble(str) < 0.005){

                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (params[1].equals("false")) {
                state = strConnect;
                state2 = strConnect;
                strEnabled = "false";

            } else {
                strEnabled = "true";
                state2 = strDisconnect;
                state = strDisconnect;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            Log.e("result_Height_Log", ":" + result);

        }
    }


    private Double mapValues(Double v1){
        double value = (v1 * 100) / (1.000);
        Log.e("mapValue_log"," = "+String.valueOf(value));
        return value;
    }

    private void stopPlaying() {
        if (mp != null) {
            mp.stop();

            mp.reset();
            mp.release();
            mp = null;
        }
    }

    private void playsound() {
        if (!isAudioPlaying) {
            try {
                stopPlaying();
                mp = MediaPlayer.create(context, R.raw.siran);
                mp.start();
                isAudioPlaying = true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            isAudioPlaying = false;
            Toast.makeText(context, "Audio", Toast.LENGTH_SHORT).show();
        }
    }


    private class Connect extends AsyncTask<String, String, String> {
        public Connect() {
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            progressDialog = new ProgressDialog(MainActivity.this);
            progressDialog.setMessage("Connecting..");
            progressDialog.setCancelable(true);
            progressDialog.show();
        }

        @SuppressLint("MissingPermission")
        public String doInBackground(String... deviceAddresses) {
            String deviceAddress = deviceAddresses[0];

            if (deviceAddress.trim().length() == 0) {
                return "";
            }

            bluetoothDevice = bluetoothAdapter.getRemoteDevice(deviceAddress);

            try {
                socket = bluetoothDevice.createInsecureRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));

                if (!socket.isConnected()) {
                    socket.connect();
                }

                inputStreamHeightReceiver = socket.getInputStream();
                outputStreamHeightReceiver = socket.getOutputStream();

                return strConnected;
            } catch (Exception e) {
                e.printStackTrace();
                return "";
            }
        }

        public void onPostExecute(String result) {
            if (progressDialog.isShowing())
                progressDialog.dismiss();

            Toast.makeText(MainActivity.this, result, Toast.LENGTH_SHORT).show();

            Log.e("result_Logs", ":" + result);

            if (result.equals(strConnected)) {

                strEnabled = "true";
                state = strDisconnect;
                state2 = strDisconnect;

                new Receiver().execute(new String[]{strEnabled});
            } else {
                Toast.makeText(MainActivity.this, "Unable to connect device, try again.", Toast.LENGTH_SHORT).show();

            }

//            new Receiver().execute(new String[]{strEnabled});
        }
    }

   /* AdaptiveColorProvider colorProvider = new AdaptiveColorProvider() {
        @Override
        public int provideProgressColor(float progress) {
            if (progress <= 25)
                return colorOne;
            else if (progress <= 50)
                return colorTwo;
            else if (progress <= 75)
                return colorThree;
            else return colorFour;
        }

        @Override
        public int provideBackgroundColor(float progress) {
            //This will provide a bg color that is 80% darker than progress color.
            return ColorUtils.blendARGB(provideProgressColor(progress), Color.BLACK, .8f);
        }

        @Override
        public int provideTextColor(float progress) {
            return provideProgressColor(progress);
        }

        @Override
        public int provideBackgroundBarColor(float progress) {
            return ColorUtils.blendARGB(provideProgressColor(progress), Color.BLACK, .5f);
        }
    };*/

    private void setupInterpolator() {
        TimeInterpolator interpolator = new LinearOutSlowInInterpolator();
    }
}