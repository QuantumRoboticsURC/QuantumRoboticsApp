package com.app.quantumrobotics.ui.main;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;


import com.app.quantumrobotics.MainActivity;
import com.app.quantumrobotics.R;

public class splashscreen extends Activity {
    private static int SPLASH_SCREEN=5500;
    ImageView imageV;
    LinearLayout layout;
    Animation logo;
    Animation fade_out;
    Animation fade_in;


    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splashscreen);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        imageV= (ImageView) findViewById(R.id.logo);
        layout=(LinearLayout) findViewById(R.id.sponsors);
        logo= AnimationUtils.loadAnimation(this, R.anim.logo);
        fade_out= AnimationUtils.loadAnimation(this,R.anim.fade_out);
        fade_in= AnimationUtils.loadAnimation(this,R.anim.fade_in);

        AnimationSet set1=new AnimationSet(false);
        AnimationSet set2=new AnimationSet(false);
        set1.addAnimation(logo);

        fade_out.setStartOffset(logo.getDuration());
        set1.addAnimation(fade_out);

        fade_in.setStartOffset(logo.getDuration()+fade_out.getDuration());
        set2.addAnimation(fade_in);

        set1.setFillAfter(true);
        set2.setFillAfter(true);
        imageV.startAnimation(set1);
        layout.startAnimation(set2);



        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent= new Intent(splashscreen.this,MainActivity.class);
                startActivity(intent);
                finish();
            }
        },SPLASH_SCREEN);
    }
}
