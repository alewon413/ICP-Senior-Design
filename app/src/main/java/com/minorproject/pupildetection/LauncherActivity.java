package com.minorproject.pupildetection;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class LauncherActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher);
        Button detectBtn = findViewById(R.id.detect_pupil) ;
        detectBtn.setOnClickListener(
            new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent detectIntent = new Intent(LauncherActivity.this, MainActivity.class) ;
                    startActivity(detectIntent);
                }
            }
        );
    }
}
