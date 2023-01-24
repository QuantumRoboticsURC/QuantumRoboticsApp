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

    private final LocationListener locationListener;
    private Location cachedLocation;
    private String navSatFixFrameId;
    private OnFrameIdChangeListener locationFrameIdChangeListener;

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
    public void setVars(int angle, int strength,int direc){
        linearVelocity= strength * 0.01;
        angularVelocity= angle * direc * 0.01;
    }

    public void setTopic(String name,String type){
        this.topic_name = name;
        this.topic_type = type;

    }
    @Override
    public void onStart(final ConnectedNode connectedNode) {
        //this.topic_name = "arm_teleop/joint1";
        //this.topic_name = "turtle1/cmd_vel";
        //this.topic_type= "geometry_msgs/Twist";
        Publisher<Message> locationPublisher;
        if(this.topic_type=="twist"){
            locationPublisher = connectedNode.newPublisher(this.topic_name, Twist._TYPE);
        }else if (this.topic_type=="float64"){
            locationPublisher = connectedNode.newPublisher(this.topic_name, "std_msgs/Float64");
        }else{
            locationPublisher =null;
        }
        final String nombre_topico=this.topic_name;
        final String tipo_topico=this.topic_type;
        //final NavSatFix navSatFix = locationPublisher.newMessage();


        final Publisher<Message> finalLocationPublisher = locationPublisher;
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
                        final std_msgs.Float64 message= (Float64) finalLocationPublisher.newMessage();
                        message.setData(linearVelocity);
                        Log.d(TAG, nombre_topico);
                        finalLocationPublisher.publish(message);
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
        return GraphName.of("ros_android_sensors/location_publisher_node");
    }

    public LocationListener getLocationListener() {
        return locationListener;
    }

    public OnFrameIdChangeListener getFrameIdListener() {
        return locationFrameIdChangeListener;
    }
}
