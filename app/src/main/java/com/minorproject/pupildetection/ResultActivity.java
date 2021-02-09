package com.minorproject.pupildetection;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import java.util.ArrayList;

public class ResultActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);
        Intent resIntent = this.getIntent();
        if(resIntent != null && resIntent.hasExtra("resList")) {
	        ArrayList<Double> resList = (ArrayList<Double>) resIntent.getSerializableExtra("resList");
	        int s = resList.size();
	        double avg = 0;
	        for(int i = 0; i < s; i ++) {
	        	avg += resList.get(i);
	        }
            String res;
	        if(s == 0) {
                res = "No pupil detected";
            } else {
                avg = avg / s;
                res = avg + "";
            }
	        TextView resView = findViewById(R.id.result);
	        resView.setText(res);
	    }
    }
}