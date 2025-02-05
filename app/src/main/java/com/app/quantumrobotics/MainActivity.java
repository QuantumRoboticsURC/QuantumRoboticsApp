/*
 * Copyright (C) 2014 Oliver Degener.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.app.quantumrobotics;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import org.ros.address.InetAddressFactory;

import com.ros.android.BitmapFromCompressedImage;
import com.ros.android.RosActivity;
import com.ros.android.view.RosImageView;

import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;

import io.github.controlwear.virtual.joystick.android.JoystickView;
import sensor_msgs.CompressedImage;
import sensor_msgs.Image;

public class MainActivity extends RosActivity implements View.OnClickListener,AdapterView.OnItemSelectedListener {
    private static final String TAG = MainActivity.class.getSimpleName();
    private RosImageView<Image> rosImageView;
    public LocationPublisherNode locationPublisherNode = new LocationPublisherNode();
    //public LocationPublisherNode joint1Node = new LocationPublisherNode();
    //public Q4PublisherNode joint4Node = new Q4PublisherNode();
    public float leftval;
    public float rightval;
    public Boolean armMode= FALSE;
    public LinearLayout armbuttons;
    public Button RotIzqB;
    public Button RotDerB;
    public Button RotPhiL;
    public Button RotPhiR;
    public LinearLayout PhiButtons;
    //public String cam_topic="/usb_cam/image_raw/compressed";
    public String cam_topic="/usb_cam/image_raw/compressed";


    private EditText locationFrameIdView, imuFrameIdView;
    Button applyB;
    private OnFrameIdChangeListener locationFrameIdListener, imuFrameIdListener,joint4FrameIdListener;

    public MainActivity() {
        super("RosAndroidExample", "RosAndroidExample");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if(!armMode){
            locationPublisherNode.setTopic("/cmd_vel","twist");
        }else{
        locationPublisherNode.setTopic("arm_teleop/joint1","float64");}
        //locationPublisherNode.setTopic("/turtle1/cmd_vel","twist");
        //joint1Node.setTopic("arm_teleop/joint1","float64");
        //joint4Node.setTopic("arm_teleop/joint4","float64");
        //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        locationFrameIdListener = new OnFrameIdChangeListener() {
            @Override
            public void onFrameIdChanged(String newFrameId) {
                Log.w(TAG, "Default location OnFrameIdChangedListener called");
            }
        };
        joint4FrameIdListener= new OnFrameIdChangeListener() {
            @Override
            public void onFrameIdChanged(String newFrameId) {
                Log.w(TAG, "Default joint4 OnFrameIdChangedListener called");
            }
        };
        imuFrameIdListener = new OnFrameIdChangeListener() {
            @Override
            public void onFrameIdChanged(String newFrameId) {
                Log.w(TAG, "Default IMU OnFrameIdChangedListener called");
            }
        };

        locationFrameIdView = findViewById(R.id.et_location_frame_id);
        imuFrameIdView = findViewById(R.id.et_imu_frame_id);

        SharedPreferences sp = getSharedPreferences("SharedPreferences", MODE_PRIVATE);
        locationFrameIdView.setText(sp.getString("locationFrameId", getString(R.string.default_location_frame_id)));
        imuFrameIdView.setText(sp.getString("imuFrameId", getString(R.string.default_imu_frame_id)));

        applyB = findViewById(R.id.b_apply);
        applyB.setOnClickListener(this);

        armbuttons= (LinearLayout) findViewById(R.id.armButtons);
        RotDerB= findViewById(R.id.RotDer);
        RotIzqB= findViewById(R.id.RotIzq);
        RotPhiL= findViewById(R.id.phi_lr);
        RotPhiR= findViewById(R.id.phi_rr);
        PhiButtons= findViewById(R.id.phi);

        //Joysticks
        JoystickView joystick = (JoystickView) findViewById(R.id.jV);
        joystick.setOnMoveListener(new JoystickView.OnMoveListener() {
            @Override
            public void onMove(int angle, int strength) {
                // Direccion del rover
                //Strength: de 0 a 100
                //Angle: 0 a 360 en donde una L es un angulo de 90
                // (El pulgar apuntando arriba)
                leftval= (float) (Math.sin(angle*Math.PI/180));
                //Set node values
                if(!armMode){
                    locationPublisherNode.setSpeedVars(leftval,rightval,true);
                    locationPublisherNode.setArmMode(1);
                }else{
                    locationPublisherNode.setJointVars(leftval,rightval,true);
                }
            }
        });

        JoystickView joystick2 = (JoystickView) findViewById(R.id.jV2);
        joystick2.setOnMoveListener(new JoystickView.OnMoveListener() {
            @Override
            public void onMove(int angle, int strength) {
                // Direccion del rover
                //Strength: de 0 a 100
                //Angle: 0 a 360 en donde una L es un angulo de 90
                // (El pulgar apuntando arriba)
                rightval= (float) (Math.sin(angle*Math.PI/180));
                //Send node values
                if(!armMode){
                    locationPublisherNode.setSpeedVars(leftval,rightval,true);
                }else{
                    locationPublisherNode.setJointVars(leftval,rightval,true);
                }

            }
        });

        //Camera
        rosImageView = findViewById(R.id.cam);
        rosImageView.setTopicName("/zed2/zed_node/left_raw/image_raw_color/compressed","/usb_cam_1/image_raw/compressed");
        //rosImageView.setTopicName("/usb_cam/image_raw/compressed","/usb_cam/image_raw/compressed");
        rosImageView.setMessageType(CompressedImage._TYPE);
        rosImageView.setMessageToBitmapCallable(new BitmapFromCompressedImage());

        //Camera Selector
        Spinner spinner= findViewById(R.id.cam_selec);
        ArrayAdapter<CharSequence> adapter= ArrayAdapter.createFromResource(this,R.array.cams, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(this);
    }

    @Override
    protected void init(NodeMainExecutor nodeMainExecutor) {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        Log.d(TAG, "init()");


        ImuPublisherNode imuPublisherNode = new ImuPublisherNode();

        MainActivity.this.locationFrameIdListener = locationPublisherNode.getFrameIdListener();
        //MainActivity.this.joint4FrameIdListener= joint4Node.getFrameIdListener();
        MainActivity.this.imuFrameIdListener = imuPublisherNode.getFrameIdListener();



        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setPowerRequirement(Criteria.POWER_LOW);
        criteria.setAltitudeRequired(false);
        criteria.setBearingRequired(false);
        criteria.setSpeedRequired(false);
        criteria.setCostAllowed(true);
        final String provider = LocationManager.GPS_PROVIDER;
        String svcName = Context.LOCATION_SERVICE;
        final LocationManager locationManager = (LocationManager) getSystemService(svcName);
        final int t = 500;
        final float distance = 0.1f;

        MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    boolean permissionFineLocation = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
                    boolean permissionCoarseLocation = checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
                    Log.d(TAG, "PERMISSION 1: " + String.valueOf(permissionFineLocation));
                    Log.d(TAG, "PERMISSION 2: " + String.valueOf(permissionCoarseLocation));
                    if (permissionFineLocation && permissionCoarseLocation) {
                        if (locationManager != null) {
                            Log.d(TAG, "Requesting location");
                            locationManager.requestLocationUpdates(provider, t, distance,
                                    locationPublisherNode.getLocationListener());
                        }
                    } else {
                        // Request permissions
                        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, PackageManager.GET_PERMISSIONS);
                    }
                } else {
                    locationManager.requestLocationUpdates(provider, t, distance, locationPublisherNode.getLocationListener());
                }
            }
        });

        SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        try {
            Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            sensorManager.registerListener(imuPublisherNode.getAccelerometerListener(), accelerometer, SensorManager.SENSOR_DELAY_FASTEST);
        } catch (NullPointerException e) {
            Log.e(TAG, e.toString());
            return;
        }

        SensorManager sensorManager1 = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        try {
            Sensor gyroscope = sensorManager1.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
            sensorManager1.registerListener(imuPublisherNode.getGyroscopeListener(), gyroscope, SensorManager.SENSOR_DELAY_FASTEST);
        } catch (NullPointerException e) {
            Log.e(TAG, e.toString());
            return;
        }

        SensorManager sensorManager2 = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        try {
            Sensor orientation = sensorManager2.getDefaultSensor(Sensor.TYPE_ORIENTATION);
            sensorManager2.registerListener(imuPublisherNode.getOrientationListener(), orientation, SensorManager.SENSOR_DELAY_FASTEST);
        } catch (NullPointerException e) {
            Log.e(TAG, e.toString());
            return;
        }



        // At this point, the user has already been prompted to either enter the URI
        // of a master to use or to start a master locally.

        // The user can easily use the selected ROS Hostname in the master chooser
        // activity.

        NodeConfiguration nodeConfiguration = NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback().getHostAddress());
        nodeConfiguration.setMasterUri(getMasterUri());


        if(!armMode){

            nodeMainExecutor.execute(locationPublisherNode, nodeConfiguration);
        }else{
            nodeMainExecutor.execute(locationPublisherNode,nodeConfiguration);
          //  nodeMainExecutor.execute(joint4Node,nodeConfiguration);
        }
        //nodeMainExecutor.execute(imuPublisherNode, nodeConfiguration);
        nodeMainExecutor.execute(rosImageView, nodeConfiguration.setNodeName("Cam_listener"));

        /*
        switch(cam_topic){
            case cam1:
                nodeMainExecutor.execute(rosImageView, nodeConfiguration.setNodeName("Cam_listener"));
                break;
            case cam2:
                nodeMainExecutor.execute(rosImageView2,nodeConfiguration.setNodeName("Cam_listener"));
                break;
        }*/
        onClick(null);
    }

    @Override
    public void onClick(View view) {
        Log.i(TAG, "Default IMU OnFrameIdChangedListener called");

        SharedPreferences sp = getSharedPreferences("SharedPreferences", MODE_PRIVATE);
        SharedPreferences.Editor spe = sp.edit();
        String newLocationFrameId = locationFrameIdView.getText().toString();
        if (!newLocationFrameId.isEmpty()) {
            locationFrameIdListener.onFrameIdChanged(newLocationFrameId);
            spe.putString("locationFrameId", newLocationFrameId);
        }
        String newImuFrameId = imuFrameIdView.getText().toString();
        if (!newLocationFrameId.isEmpty()) {
            imuFrameIdListener.onFrameIdChanged(newImuFrameId);
            spe.putString("imuFrameId", newImuFrameId);
        }
        spe.apply();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PackageManager.GET_PERMISSIONS) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Permissions granted!");
            } else {
                Log.e(TAG, "Permissions not granted.");
            }
        }
    }

    public void changeMode(View view) {
        armMode= !armMode;

        if(!armMode){
            locationPublisherNode.setTopic("/cmd_vel","twist");
            Log.w(TAG,"Nav");
            armbuttons.setVisibility(View.INVISIBLE);
            RotIzqB.setVisibility(View.INVISIBLE);
            RotDerB.setVisibility(View.INVISIBLE);
            PhiButtons.setVisibility(View.INVISIBLE);
            RotPhiL.setVisibility(View.INVISIBLE);
            RotPhiR.setVisibility(View.INVISIBLE);

        }else{
            Log.w(TAG,"arm");
            locationPublisherNode.setTopic("arm_teleop/joint1","float64");
            armbuttons.setVisibility(View.VISIBLE);
            RotIzqB.setVisibility(View.VISIBLE);
            RotDerB.setVisibility(View.VISIBLE);
            PhiButtons.setVisibility(View.VISIBLE);
            RotPhiL.setVisibility(View.VISIBLE);
            RotPhiR.setVisibility(View.VISIBLE);
        }

    }

    public void PhiUp(View view){ locationPublisherNode.rotatePhi(true);
    }
    public void PhiDown(View view){ locationPublisherNode.rotatePhi(false);
    }
    public void RotateRight(View view){ locationPublisherNode.rotate(true);
    }
    public void RotateLeft(View view){
        locationPublisherNode.rotate(false);
    }

    public void intermediate(View view) {
        locationPublisherNode.setArmMode(1);
        locationPublisherNode.setJointVars(0,0,true);
    }

    public void storage(View view) {
        locationPublisherNode.setArmMode(2);
        locationPublisherNode.setJointVars(0,0,true);
    }

    public void floor(View view) {
        locationPublisherNode.setArmMode(3);
        locationPublisherNode.setJointVars(0,0,true);
    }

    public void write(View view) {
        locationPublisherNode.setArmMode(4);
        locationPublisherNode.setJointVars(0,0,true);
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
        String text= adapterView.getItemAtPosition(position).toString();
        Toast.makeText(adapterView.getContext(),text,Toast.LENGTH_SHORT).show();
        cam_topic= text;
        rosImageView.setCurrent(position);
        rosImageView.setCamera();

    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }
}
