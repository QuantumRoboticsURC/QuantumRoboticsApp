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

    private Boolean moving= false;

    private double linearVelocity;
    private double angularVelocity;
    private double q1= -5;
    private double q2= 135;
    private double q3= -90;
    private double q4= -45;

    private String nombre_topico;
    private String tipo_topico;

    private final LocationListener locationListener;
    private Location cachedLocation;
    private String navSatFixFrameId;
    private OnFrameIdChangeListener locationFrameIdChangeListener;

    private Publisher<Message> finalLocationPublisher;
    private Publisher<Message> finalLocationPublisher1;
    private Publisher<Message> finalLocationPublisher2;
    private Publisher<Message> finalLocationPublisher3;
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
    public void setSpeedVars(float left_val, float right_val,boolean onMove){
        linearVelocity= ((left_val+right_val)/2) * 0.2;
        angularVelocity= ((left_val-right_val)/2) * 0.2;

        moving= onMove;
    }
    public void setJointVars(float joint1,float joint4, boolean onMove){
        q1+= joint1*5;
        q4+= joint4*5;
        if(q1 > 90 || q1 < -90){q1-=joint1*5;}
        if (q4 > 90 || q4 < -90){q4-=joint4*5;}
        moving= onMove;

    }
    public void setTopic(String name,String type){
        nombre_topico=name;
        tipo_topico=type;
    }
    public void setArmMode(Integer mode) {
        switch (mode){
            case 1:
                //Intermediate
                q1= -5;
                q2= 135;
                q3= -90;
                q4= -45;
                break;
            case 2:
                //Storage
                q1= -5;
                q2= 161;
                q3= -163.15;
                q4= 90;
                break;
            case 3:
                //Floor
                q1= -5;
                q2= 49.13;
                q3= -101.74;
                q4= 52.62;
                break;
            case 4:
                //Box
                q1= -94;
                q2= 134.95;
                q3= -89.91;
                q4= -45.05;
                break;
            default:
                //HOME
                q1= -5;
                q2= 135;
                q3= -90;
                q4= -45;
        }
    }
    @Override
    public void onStart(final ConnectedNode connectedNode) {
        //this.topic_name = "arm_teleop/joint1";
        //this.topic_name = "turtle1/cmd_vel";
        //this.topic_type= "geometry_msgs/Twist";
        Publisher<Message> locationPublisher;
        Publisher<Message> locationPublisher1;
        Publisher<Message> locationPublisher2;
        Publisher<Message> locationPublisher3;
        Publisher<Message> locationPublisher4;

        locationPublisher = connectedNode.newPublisher("cmd_vel", Twist._TYPE);
        locationPublisher1 = connectedNode.newPublisher("arm_teleop/joint1", "std_msgs/Float64");
        locationPublisher2 = connectedNode.newPublisher("arm_teleop/joint2", "std_msgs/Float64");
        locationPublisher3 = connectedNode.newPublisher("arm_teleop/joint3", "std_msgs/Float64");
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
        finalLocationPublisher2= locationPublisher2;
        finalLocationPublisher3= locationPublisher3;
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

                if ((isMessagePending
                                || (System.currentTimeMillis() - previousPublishTime) >= maxElapse // Or, is max elapse reached?
                        )) {
                    header.setStamp(connectedNode.getCurrentTime());
                    header.setFrameId(navSatFixFrameId);
                    header.setSeq(sequenceNumber);
                    Log.w(TAG,"Prueba");
                    //navSatFix.setHeader(header);

                    //navSatFix.setLatitude(cachedLocation.getLatitude());
                    //navSatFix.setLongitude(cachedLocation.getLongitude());
                    if(tipo_topico=="twist"){
                        geometry_msgs.Twist message = (Twist) finalLocationPublisher.newMessage();
                        message.getLinear().setX(linearVelocity);
                        message.getAngular().setZ(angularVelocity);
                        Log.d(TAG, nombre_topico);
                        if(moving){
                            finalLocationPublisher.publish(message);
                            moving=false;
                        }
                    }else if(tipo_topico=="float64"){
                        final std_msgs.Float64 message1= (Float64) finalLocationPublisher1.newMessage();
                        final std_msgs.Float64 message2= (Float64) finalLocationPublisher2.newMessage();
                        final std_msgs.Float64 message3= (Float64) finalLocationPublisher3.newMessage();
                        final std_msgs.Float64 message4= (Float64) finalLocationPublisher4.newMessage();
                        message1.setData(q1);
                        message2.setData(q2);
                        message3.setData(q3);
                        message4.setData(q4);
                        Log.d(TAG, nombre_topico);
                        if(moving){
                            finalLocationPublisher1.publish(message1);
                            finalLocationPublisher2.publish(message2);
                            finalLocationPublisher3.publish(message3);
                            finalLocationPublisher4.publish(message4);
                            moving=false;
                        }
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
