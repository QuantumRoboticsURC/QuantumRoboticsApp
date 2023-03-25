/*
 * Copyright (C) 2011 Google Inc.
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

package com.ros.android.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;

import com.ros.android.BitmapFromCompressedImage;
import com.ros.android.MessageCallable;

import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.NodeMain;
import org.ros.node.topic.Subscriber;

/**
 * Displays incoming sensor_msgs/CompressedImage messages.
 * 
 * @author ethan.rublee@gmail.com (Ethan Rublee)
 * @author damonkohler@google.com (Damon Kohler)
 */
public class RosImageView<T> extends android.support.v7.widget.AppCompatImageView implements NodeMain {

  private String topicName;
  private String topicName2;
  private String topicName3;
  private String topicName4;
  private int currentTopic;
  private String messageType;
  public Boolean started= false;
  public Subscriber<T> subscriber;
  public Subscriber<T> subscriber2;
  private MessageCallable<Bitmap, T> callable;

  public RosImageView(Context context) {
    super(context);
  }

  public RosImageView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public RosImageView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
  }

  public void setTopicName(String topicName,String topicName2) {
    this.topicName = topicName;
    this.topicName2= topicName2;
    //Extra
    this.topicName3= topicName2;
    this.topicName4= topicName2;
  }

  public void setCurrent(int topic){
    this.currentTopic= topic;
  }

  public void setMessageType(String messageType) {
    this.messageType = messageType;
  }

  public void setMessageToBitmapCallable(BitmapFromCompressedImage callable) {
    this.callable = (MessageCallable<Bitmap, T>) callable;
  }
  public void setCamera(){
    /*
    if(started){
    switch (currentTopic){
      case 0:
        setImageBitmap(callable.call(message1));
        break;
      case 1:
        setImageBitmap(callable.call(message2));
        break;
    }}*/
  }

  @Override
  public GraphName getDefaultNodeName() {
    return GraphName.of("ros_image_view");
  }

  @Override
  public void onStart(ConnectedNode connectedNode) {
    started= true;
    subscriber = connectedNode.newSubscriber(topicName, messageType);
    subscriber2 = connectedNode.newSubscriber(topicName2, messageType);
    //Switch de camaras
      subscriber.addMessageListener(new MessageListener<T>() {
        @Override
        public void onNewMessage(final T message) {
          post(new Runnable() {
            @Override
            public void run() {
              if(currentTopic==0){
              setImageBitmap(callable.call(message));}
            }
          });
          postInvalidate();
        }
      });
      subscriber2.addMessageListener(new MessageListener<T>() {
        @Override
        public void onNewMessage(final T message) {
          post(new Runnable() {
            @Override
            public void run() {
              if (currentTopic==1){
              setImageBitmap(callable.call(message));}
            }
          });
          postInvalidate();
        }
      });
      //More cases

  }

  @Override
  public void onShutdown(Node node) {
  }

  @Override
  public void onShutdownComplete(Node node) {
  }

  @Override
  public void onError(Node node, Throwable throwable) {
  }
}
