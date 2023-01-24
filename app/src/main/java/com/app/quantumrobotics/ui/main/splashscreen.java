package com.app.quantumrobotics.ui.main;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;



import com.app.quantumrobotics.MainActivity;
import com.app.quantumrobotics.R;

public class splashscreen extends Activity {
    private static int SPLASH_SCREEN=2500;
    ImageView imageV;
    Animation logo;


    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splashscreen);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        imageV= (ImageView) findViewById(R.id.logo);
        logo= AnimationUtils.loadAnimation(this, R.anim.logo);
        imageV.setAnimation(logo);

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
