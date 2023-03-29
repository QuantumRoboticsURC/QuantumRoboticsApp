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
    private double x= 0.14;
    private double rot= 0;
    private double z= 0.75;
    private double phi= 0;

    public double q1= 0;
    public double q2= 135;
    public double q3= -90;
    public double q4= -45;

    private double l1 = 0;
    private double l2 = 2.6;
    private double l3 = 2.6;
    private double l4 = .9;

    private double[] limits_map_q1= {-95,90};
    private double[] limits_map_q2= {0,161};
    private double[] limits_map_q3= {-165.4,0};
    private double[] limits_map_q4= {-135,90};



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
    public void rotate(boolean right){
        if (right) {rot+=5;q1+=5;}
        if(!right){rot-=5;q1-=5;}
        double[] result=inverseKinematics(x,rot,z,phi);
        q1=result[0];
        q2=result[1];
        q3=result[2];
        q4=result[3];
        moving= true;
    }
    public void setJointVars(float val_l,float val_r, boolean onMove){
        x+= val_l * 0.2;
        z+= val_r * 0.2;
        double[] result=inverseKinematics(x,rot,z,phi);
        q1=result[0];
        q2=result[1];
        q3=result[2];
        q4=result[3];
        /*Restricciones que están en inverseKinem
        if(q1 > 90 || q1 < -90){q1-=joint1*5;}
        if (q4 > 90 || q4 < -90){q4-=joint4*5;}*/
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
                q1= 0;
                q2= 135;
                q3= -90;
                q4= -45;
                break;
            case 2:
                //Storage
                q1= 0;
                q2= 161;
                q3= -163.15;
                q4= 90;
                break;
            case 3:
                //Floor
                q1= 0;
                q2= 49.13;
                q3= -101.74;
                q4= 52.62;
                break;
            case 4:
                //Box
                q1= 0;
                q2= 49.13;
                q3= -101.74;
                q4= 52.62;
                break;
            default:
                //HOME
                q1= 0;
                q2= 161;
                q3= -163.15;
                q4= 2.15;
        }
    }

    public double qlimit(double[] l,double val){
        if(val < l[0]){ //inferior
            return l[0];
        }else if(val > l[1]){ //superior
            return l[1];
        }else{
            return val;
        }

    }
    public double[] inverseKinematics(double xm, double ym, double zm, double phi_int){
        //temp
        double values_map_joint1;
        double values_map_joint3;
        double values_map_joint4;
        double Q1 = 0;
    /*if (xm != 0 || ym != 0 || zm != 0){
      if(xm == 0){
        if(ym>0){
          xm = ym;
          Q1 = math.pi/2;
        }else if (ym<0){
          xm = ym;
          Q1 = math.pi/2;
        }else if(ym == 0){
          Q1 = 0;
        }
      }else if (xm < 0){
        if (ym == 0){
          Q1 = 0;
        }else if(ym < 0){
          //No lo he cambiado #real
          Q1 = math.re(math.atan2(xm, ym));
        }else{
          //Tampoco lo he cambiado #real
          Q1 = math.re(math.atan2(-xm,-ym));
        }

      }else{
        //Ni idea #real
        Q1 = math.re(math.atan2(ym,xm));
      }
    }    */
        Q1 = Math.atan2(ym,xm);
        System.out.println(Q1);
        //console.log("Q1",Q1)
        //Para q1
        Log.d(TAG, String.valueOf(q1)); //marca -5 en lugar de 0

        q1=qlimit(limits_map_q1,q1);
        //Para q2
        double hip=Math.sqrt(Math.pow(xm,2)+Math.pow((zm-l1),2));
        //console.log("zm",zm)
        //console.log("l1",l1)
        //console.log("xm",xm)
        //console.log("hip", hip)
        double phi = Math.atan2(zm-l1, xm);
        //console.log("phi",phi)

        //beta=acos((-l3^2+l2^2+hip^2)/(2*l2*hip))
        double beta=Math.acos((Math.pow(l2,2)+Math.pow(hip,2)-Math.pow(l3,2))/(2*l2*hip)); //da negativo cuando no funciona
        double Q2=phi+beta;//math.re(phi+beta);
        q2=Math.toDegrees(Q2);
        //console.log("beta",beta)
        //console.log("Q2",Q2)
        Log.d(TAG, String.valueOf(q2));
        q2=qlimit(limits_map_q2,q2);
        //Para q3
        double gamma=Math.acos((Math.pow(l2,2)+Math.pow(l3,2)-Math.pow(hip,2))/(2*l2*l3));
        double Q3=gamma-Math.PI;
        q3=Math.toDegrees(Q3);
        q3=qlimit(limits_map_q3,q3);
        //console.log("gamma",gamma)
        //  console.log("Q3",Q3)
        Log.d(TAG, String.valueOf(q3));

        q4 = phi_int - q2 -q3;
        q4=qlimit(limits_map_q4,q4);
        values_map_joint4 = q4+q2+q3;

        Log.d(TAG, String.valueOf(q4));


        double acum = Math.toRadians(q2);
        double x2 = l2*Math.cos(acum);
        double y2 = l2*Math.sin(acum);
        acum+=Math.toRadians(q3);
        double x3 = x2+l3*Math.cos(acum);
        double y3 = y2+l3*Math.sin(acum);
        acum+=Math.toRadians(q4);
        double x4 = x3+l4*Math.cos(acum);
        double y4 = y3+l4*Math.sin(acum);
        //console.log(y4); //NAN
        double[] res={q1,q2,q3,q4};


        if(y4> -4.2 && (x4> 1.1 || y4>=0)){
            values_map_joint1 = x3;
            values_map_joint3 = y3;
        }
            /* Bool return
            /*
            angles_map_q2=q2;
            angles_map_q3=q3;
            angles_map_q4=q4;
            arm_interface(angles_map.q2,angles_map.q3,angles_map.q4);
            return true;
        }else{
            return false;
        }*/
        return res;
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
