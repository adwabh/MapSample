package com.example.myapplication;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.example.myapplication.utils.Utility;

import pb.ApiClient;
import pb.ApiException;
import pb.Configuration;
import pb.locationintelligence.LIAPIGeoEnrichServiceApi;
import pb.locationintelligence.model.Locations;

import static android.Manifest.permission.INTERNET;

public class MainActivity extends Activity {
    private static final int REQUEST_INTERNET = 121;
    private RelativeLayout relative_layout_top;
    private RelativeLayout relative_layout_progress;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        relative_layout_top = findViewById(R.id.relative_layout_top);
        relative_layout_progress = findViewById(R.id.relative_layout_progress);
        if(mayRequestInternet()){
            executeTask();
        }
    }

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

    private boolean mayRequestInternet() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        if (checkSelfPermission(INTERNET) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        if (shouldShowRequestPermissionRationale(INTERNET)) {
            Snackbar.make(relative_layout_top, R.string.internet_permission_rationale, Snackbar.LENGTH_INDEFINITE)
                    .setAction(android.R.string.ok, new View.OnClickListener() {
                        @Override
                        @TargetApi(Build.VERSION_CODES.M)
                        public void onClick(View v) {
                            requestPermissions(new String[]{INTERNET}, REQUEST_INTERNET);
                        }
                    });
        } else {
            requestPermissions(new String[]{INTERNET}, REQUEST_INTERNET);
        }
        return false;
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_INTERNET) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                executeTask();
            }
        }
    }

    private void executeTask() {
        new MyAsyncTask().execute();
    }

    private class MyAsyncTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            ApiClient defaultClient = Configuration.getDefaultApiClient();
            defaultClient.setoAuthApiKey(Utility.getStringMeta(MainActivity.this,"pb_api_key"));
            defaultClient.setoAuthSecret(Utility.getStringMeta(MainActivity.this,"pb_api_secret"));

            final LIAPIGeoEnrichServiceApi api = new LIAPIGeoEnrichServiceApi();

            String latitude = "35.0118";
            String longitude = "-81.9571";
            String searchRadius = "2640";
            String searchRadiusUnit = "feet";

            Locations resp = null;

            try {
                Log.i("GeoAPIs","getAddress");
                resp = api.getAddress(latitude, longitude, searchRadius, searchRadiusUnit);
            } catch (ApiException e) {
                e.printStackTrace();
                hideProgress();
            }
            return resp.toString();
        }

        @Override
        protected void onPostExecute(String result) {
            // might want to change "executed" for the returned string passed
            // into onPostExecute() but that is upto you*/
            Log.i("Result",result);
            try {
                hideProgress();
                Toast.makeText(MainActivity.this,result,Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        @Override
        protected void onPreExecute() {
            showProgress();
        }

        @Override
        protected void onProgressUpdate(Void... values) {


        }
    }
}
