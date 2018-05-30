package com.example.myapplication;

import android.app.Activity;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.RelativeLayout;

public class BaseActivity extends AppCompatActivity {
    protected RelativeLayout relative_layout_progress;

    void showProgress(){
        try {
            relative_layout_progress.setVisibility(View.VISIBLE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    void hideProgress(){
        try {
            relative_layout_progress.setVisibility(View.GONE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
