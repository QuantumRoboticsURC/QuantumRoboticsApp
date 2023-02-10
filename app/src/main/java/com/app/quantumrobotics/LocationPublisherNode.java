package com.app.quantumrobotics;

import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.util.Log;

import org.ros.concurrent.CancellableLoop;
import org.ros.internal.message.Message;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.topic.Publisher;

import std_msgs.Float64;
import std_msgs.Header;
import geometry_msgs.Twist;

public class LocationPublisherNode extends AbstractNodeMain {
    private static final String TAG = LocationPublisherNode.class.getSimpleName();

    private static float maxFrequency = 100.f;
    private static float minElapse = 1000 / maxFrequency;

    private static float minFrequency = 20.f;
    private static float maxElapse = 1000 / minFrequency;


    private long previousPublishTime = System.currentTimeMillis();
    private boolean isMessagePending;

    private String topic_name;
    private String topic_type;


    private double linearVelocity;
    private double angularVelocity;
    private double q1;
    private double q4;

    private String nombre_topico;
    private String tipo_topico;

    private final LocationListener locationListener;
    private Location cachedLocation;
    private String navSatFixFrameId;
    private OnFrameIdChangeListener locationFrameIdChangeListener;

    private Publisher<Message> finalLocationPublisher;
    private Publisher<Message> finalLocationPublisher1;
    private Publisher<Message> finalLocationPublisher4;
    public LocationPublisherNode() {
        isMessagePending = false;
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                if (location != null) {
                    cachedLocation = location;
                    isMessagePending = true;
                }
            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {
                Log.d(TAG, "Provider: " + s + ", Status: " + i + ", Extras: " + bundle);
            }

            @Override
            public void onProviderEnabled(String s) {
                Log.d(TAG, "Provider enabled: " + s);
            }

            @Override
            public void onProviderDisabled(String s) {
                Log.d(TAG, "Provider disabled: " + s);
            }
        };

        locationFrameIdChangeListener = new OnFrameIdChangeListener() {
            @Override
            public void onFrameIdChanged(String newFrameId) {
                navSatFixFrameId = newFrameId;
            }
        };
    }
    public void setSpeedVars(float left_val, float right_val){
        linearVelocity= ((left_val+right_val)/2) * 0.2;
        angularVelocity= ((left_val-right_val)/2) * 0.2;
    }
    public void setJointVars(float joint1,float joint4){
        q1= joint1*90;
        q4= joint4*90;
    }
    public void setTopic(String name,String type){
        nombre_topico=name;
        tipo_topico=type;
    }
    @Override
    public void onStart(final ConnectedNode connectedNode) {
        //this.topic_name = "arm_teleop/joint1";
        //this.topic_name = "turtle1/cmd_vel";
        //this.topic_type= "geometry_msgs/Twist";
        Publisher<Message> locationPublisher;
        Publisher<Message> locationPublisher1;
        Publisher<Message> locationPublisher4;

        locationPublisher = connectedNode.newPublisher("cmd_vel", Twist._TYPE);
        locationPublisher1 = connectedNode.newPublisher("arm_teleop/joint1", "std_msgs/Float64");
        locationPublisher4 = connectedNode.newPublisher("arm_teleop/joint4", "std_msgs/Float64");
        /*
        if(this.topic_type=="twist"){

            locationPublisher4= null;
        }else if (this.topic_type=="float64"){

        }else{
            locationPublisher =null;
            locationPublisher4= null;
        }*/

        //final NavSatFix navSatFix = locationPublisher.newMessage();


        finalLocationPublisher = locationPublisher;
        finalLocationPublisher1= locationPublisher1;
        finalLocationPublisher4= locationPublisher4;
        connectedNode.executeCancellableLoop(new CancellableLoop() {
            private int sequenceNumber;
            Header header = connectedNode.getTopicMessageFactory().newFromType(Header._TYPE);

            @Override
            protected void setup() {
            sequenceNumber = 0;
            }

            @Override
            protected void loop() throws InterruptedException {
                //double linearVelocity = 0.5;
                //double angularVelocity = -0.5;
                if ((cachedLocation != null) &&
                        (isMessagePending
                                || (System.currentTimeMillis() - previousPublishTime) >= maxElapse // Or, is max elapse reached?
                        )) {
                    header.setStamp(connectedNode.getCurrentTime());
                    header.setFrameId(navSatFixFrameId);
                    header.setSeq(sequenceNumber);
                    //navSatFix.setHeader(header);

                    //navSatFix.setLatitude(cachedLocation.getLatitude());
                    //navSatFix.setLongitude(cachedLocation.getLongitude());
                    if(tipo_topico=="twist"){
                        geometry_msgs.Twist message = (Twist) finalLocationPublisher.newMessage();
                        message.getLinear().setX(linearVelocity);
                        message.getAngular().setZ(angularVelocity);
                        Log.d(TAG, nombre_topico);
                        finalLocationPublisher.publish(message);
                    }else if(tipo_topico=="float64"){
                        final std_msgs.Float64 message1= (Float64) finalLocationPublisher1.newMessage();
                        final std_msgs.Float64 message4= (Float64) finalLocationPublisher4.newMessage();
                        message1.setData(q1);
                        message4.setData(q4);
                        Log.d(TAG, nombre_topico);
                        finalLocationPublisher1.publish(message1);
                        finalLocationPublisher4.publish(message4);
                    }






                    //Wait until minimum time has elapsed
                    long elapsed = System.currentTimeMillis() - previousPublishTime;
                    long remainingTime = (long) (minElapse - elapsed);
                    if (remainingTime > 0)
                        Thread.sleep(remainingTime);
                    previousPublishTime = System.currentTimeMillis();

                    isMessagePending = false;
                    ++this.sequenceNumber;
                } else {
                    Thread.sleep(1);
                }
            }
        });
    }

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of("ros_android_sensors/"+this.topic_name);
    }

    public LocationListener getLocationListener() {
        return locationListener;
    }

    public OnFrameIdChangeListener getFrameIdListener() {
        return locationFrameIdChangeListener;
    }
}
