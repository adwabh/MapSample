/* Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.myapplication;

import android.graphics.Color;
import android.graphics.Point;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.v4.view.ViewCompat;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.myapplication.utils.Utility;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnCircleClickListener;
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerDragListener;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.Dash;
import com.google.android.gms.maps.model.Dot;
import com.google.android.gms.maps.model.Gap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PatternItem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import pb.ApiClient;
import pb.ApiException;
import pb.Configuration;
import pb.locationintelligence.LIAPIGeoEnrichServiceApi;
import pb.locationintelligence.model.Locations;


/**
 * This shows how to draw circles on a map.
 */
public class CircleDemoActivity extends BaseActivity
        implements OnSeekBarChangeListener, OnMarkerDragListener, OnMapLongClickListener,
        OnItemSelectedListener, OnMapReadyCallback {

    private static final LatLng SYDNEY = new LatLng(-33.87365, 151.20689);
    private static final double DEFAULT_RADIUS_METERS = 100;//1000000;
    private static final double RADIUS_OF_EARTH_METERS = 6371009;

    private static final int MAX_WIDTH_PX = 50;
    private static final int MAX_HUE_DEGREES = 360;
    private static final int MAX_ALPHA = 255;

    private static final int PATTERN_DASH_LENGTH_PX = 100;
    private static final int PATTERN_GAP_LENGTH_PX = 200;
    private static final Dot DOT = new Dot();
    private static final Dash DASH = new Dash(PATTERN_DASH_LENGTH_PX);
    private static final Gap GAP = new Gap(PATTERN_GAP_LENGTH_PX);
    private static final List<PatternItem> PATTERN_DOTTED = Arrays.asList(DOT, GAP);
    private static final List<PatternItem> PATTERN_DASHED = Arrays.asList(DASH, GAP);
    private static final List<PatternItem> PATTERN_MIXED = Arrays.asList(DOT, GAP, DOT, DASH, GAP);

    private GoogleMap mMap;

    private List<DraggableCircle> mCircles = new ArrayList<>(1);

    private View mFab;
    private int mFillColorArgb;
    private int mStrokeColorArgb;

    private SeekBar mFillHueBar;
    private SeekBar mFillAlphaBar;
    private SeekBar mStrokeWidthBar;
    private SeekBar mStrokeHueBar;
    private SeekBar mStrokeAlphaBar;
    private Spinner mStrokePatternSpinner;
    private CheckBox mClickabilityCheckbox;

    // string resource IDs as identifiers.
    // These are the options for stroke patterns. We use their

    private static final int[] PATTERN_TYPE_NAME_RESOURCE_IDS = {
            R.string.pattern_solid, // Default
            R.string.pattern_dashed,
            R.string.pattern_dotted,
            R.string.pattern_mixed,
    };
    private AppBarLayout flexible_example_appbar;

    private class DraggableCircle {
        private final Marker mCenterMarker;
        private final Marker mRadiusMarker;
        private final Circle mCircle;
        private double mRadiusMeters;

        public DraggableCircle(LatLng center, double radiusMeters) {
            mRadiusMeters = radiusMeters;
            mCenterMarker = mMap.addMarker(new MarkerOptions()
                    .position(center)
                    .draggable(true));
            mRadiusMarker = mMap.addMarker(new MarkerOptions()
                    .position(toRadiusLatLng(center, radiusMeters))
                    .draggable(true)
                    .icon(BitmapDescriptorFactory.defaultMarker(
                            BitmapDescriptorFactory.HUE_AZURE)));
            mCircle = mMap.addCircle(new CircleOptions()
                    .center(center)
                    .radius(radiusMeters)
                    .strokeWidth(mStrokeWidthBar.getProgress())
                    .strokeColor(mStrokeColorArgb)
                    .fillColor(mFillColorArgb)
                    .clickable(mClickabilityCheckbox.isChecked()));
        }

        public boolean onMarkerMoved(Marker marker, boolean end) {
            if (marker.equals(mCenterMarker)) {
                mCircle.setCenter(marker.getPosition());
                mRadiusMarker.setPosition(toRadiusLatLng(marker.getPosition(), mRadiusMeters));
                if (end) {
                    executeTask(marker,toRadiusMeters(marker.getPosition(), mRadiusMarker.getPosition()));
                }
                return true;
            }
            if (marker.equals(mRadiusMarker)) {
                mRadiusMeters =
                        toRadiusMeters(mCenterMarker.getPosition(), mRadiusMarker.getPosition());
                mCircle.setRadius(mRadiusMeters);
                if (end) {
                    executeTask(mCenterMarker,toRadiusMeters(mCenterMarker.getPosition(), mRadiusMarker.getPosition()));
                }
                return true;
            }
            return false;
        }

        public void onStyleChange() {
            mCircle.setStrokeWidth(mStrokeWidthBar.getProgress());
            mCircle.setStrokeColor(mStrokeColorArgb);
            mCircle.setFillColor(mFillColorArgb);
        }

        public void setStrokePattern(List<PatternItem> pattern) {
            mCircle.setStrokePattern(pattern);
        }

        public void setClickable(boolean clickable) {
            mCircle.setClickable(clickable);
        }
    }

    private void executeTask(Marker mCenterMarker, double radiusMeters) {
        LatLng center = mCenterMarker.getPosition();
        new MyAsyncTask().execute(String.valueOf(center.latitude),
                String.valueOf(center.longitude),
                String.valueOf(radiusMeters));
    }

    /** Generate LatLng of radius marker */
    private static LatLng toRadiusLatLng(LatLng center, double radiusMeters) {
        double radiusAngle = Math.toDegrees(radiusMeters / RADIUS_OF_EARTH_METERS) /
                Math.cos(Math.toRadians(center.latitude));
        return new LatLng(center.latitude, center.longitude + radiusAngle);
    }

    private static double toRadiusMeters(LatLng center, LatLng radius) {
        float[] result = new float[1];
        Location.distanceBetween(center.latitude, center.longitude,
                radius.latitude, radius.longitude, result);
        return result[0];
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.circle_demo);
        relative_layout_progress = findViewById(R.id.relative_layout_progress);
        mFab = findViewById(R.id.flexible_example_fab);
        flexible_example_appbar = findViewById(R.id.flexible_example_appbar);

        mFillHueBar = (SeekBar) findViewById(R.id.fillHueSeekBar);
        mFillHueBar.setMax(MAX_HUE_DEGREES);
        mFillHueBar.setProgress(MAX_HUE_DEGREES / 2);

        mFillAlphaBar = (SeekBar) findViewById(R.id.fillAlphaSeekBar);
        mFillAlphaBar.setMax(MAX_ALPHA);
        mFillAlphaBar.setProgress(MAX_ALPHA / 2);

        mStrokeWidthBar = (SeekBar) findViewById(R.id.strokeWidthSeekBar);
        mStrokeWidthBar.setMax(MAX_WIDTH_PX);
        mStrokeWidthBar.setProgress(MAX_WIDTH_PX / 3);

        mStrokeHueBar = (SeekBar) findViewById(R.id.strokeHueSeekBar);
        mStrokeHueBar.setMax(MAX_HUE_DEGREES);
        mStrokeHueBar.setProgress(0);

        mStrokeAlphaBar = (SeekBar) findViewById(R.id.strokeAlphaSeekBar);
        mStrokeAlphaBar.setMax(MAX_ALPHA);
        mStrokeAlphaBar.setProgress(MAX_ALPHA);

        mStrokePatternSpinner = (Spinner) findViewById(R.id.strokePatternSpinner);
        mStrokePatternSpinner.setAdapter(new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item,
                getResourceStrings(PATTERN_TYPE_NAME_RESOURCE_IDS)));

        mClickabilityCheckbox = (CheckBox) findViewById(R.id.toggleClickability);

        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        flexible_example_appbar.addOnOffsetChangedListener(new ScrollAnimationHandler());
    }

    private String[] getResourceStrings(int[] resourceIds) {
        String[] strings = new String[resourceIds.length];
        for (int i = 0; i < resourceIds.length; i++) {
            strings[i] = getString(resourceIds[i]);
        }
        return strings;
    }

    @Override
    public void onMapReady(GoogleMap map) {
        // Override the default content description on the view, for accessibility mode.
        map.setContentDescription(getString(R.string.map_circle_description));

        mMap = map;
        mMap.setOnMarkerDragListener(this);
        mMap.setOnMapLongClickListener(this);

        mFillColorArgb = Color.HSVToColor(
                mFillAlphaBar.getProgress(), new float[]{mFillHueBar.getProgress(), 1, 1});
        mStrokeColorArgb = Color.HSVToColor(
                mStrokeAlphaBar.getProgress(), new float[]{mStrokeHueBar.getProgress(), 1, 1});

        mFillHueBar.setOnSeekBarChangeListener(this);
        mFillAlphaBar.setOnSeekBarChangeListener(this);

        mStrokeWidthBar.setOnSeekBarChangeListener(this);
        mStrokeHueBar.setOnSeekBarChangeListener(this);
        mStrokeAlphaBar.setOnSeekBarChangeListener(this);

        mStrokePatternSpinner.setOnItemSelectedListener(this);

        DraggableCircle circle = new DraggableCircle(SYDNEY, DEFAULT_RADIUS_METERS);
        mCircles.add(circle);

        // Move the map so that it is centered on the initial circle
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(SYDNEY, 4.0f));

        // Set up the click listener for the circle.
        map.setOnCircleClickListener(new OnCircleClickListener() {
            @Override
            public void onCircleClick(Circle circle) {
                // Flip the red, green and blue components of the circle's stroke color.
                circle.setStrokeColor(circle.getStrokeColor() ^ 0x00ffffff);
            }
        });

        List<PatternItem> pattern = getSelectedPattern(mStrokePatternSpinner.getSelectedItemPosition());
        for (DraggableCircle draggableCircle : mCircles) {
            draggableCircle.setStrokePattern(pattern);
        }
    }

    private List<PatternItem> getSelectedPattern(int pos) {
        switch (PATTERN_TYPE_NAME_RESOURCE_IDS[pos]) {
            case R.string.pattern_solid:
                return null;
            case R.string.pattern_dotted:
                return PATTERN_DOTTED;
            case R.string.pattern_dashed:
                return PATTERN_DASHED;
            case R.string.pattern_mixed:
                return PATTERN_MIXED;
            default:
                return null;
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        if (parent.getId() == R.id.strokePatternSpinner) {
            for (DraggableCircle draggableCircle : mCircles) {
                draggableCircle.setStrokePattern(getSelectedPattern(pos));
            }
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // Don't do anything here.
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        // Don't do anything here.
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        // Don't do anything here.
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (seekBar == mFillHueBar) {
            mFillColorArgb =
                    Color.HSVToColor(Color.alpha(mFillColorArgb), new float[]{progress, 1, 1});
        } else if (seekBar == mFillAlphaBar) {
            mFillColorArgb = Color.argb(progress, Color.red(mFillColorArgb),
                    Color.green(mFillColorArgb), Color.blue(mFillColorArgb));
        } else if (seekBar == mStrokeHueBar) {
            mStrokeColorArgb =
                    Color.HSVToColor(Color.alpha(mStrokeColorArgb), new float[]{progress, 1, 1});
        } else if (seekBar == mStrokeAlphaBar) {
            mStrokeColorArgb = Color.argb(progress, Color.red(mStrokeColorArgb),
                    Color.green(mStrokeColorArgb), Color.blue(mStrokeColorArgb));
        }

        for (DraggableCircle draggableCircle : mCircles) {
            draggableCircle.onStyleChange();
        }
    }

    @Override
    public void onMarkerDragStart(Marker marker) {
        onMarkerMoved(marker);
    }

    @Override
    public void onMarkerDragEnd(Marker marker) {
        onMarkerMovedEnd(marker);
    }

    @Override
    public void onMarkerDrag(Marker marker) {
        onMarkerMoved(marker);
    }

    private void onMarkerMoved(Marker marker) {
        for (DraggableCircle draggableCircle : mCircles) {
            if (draggableCircle.onMarkerMoved(marker, false)) {
                break;
            }
        }
    }

    private void onMarkerMovedEnd(Marker marker) {
        for (DraggableCircle draggableCircle : mCircles) {
            if (draggableCircle.onMarkerMoved(marker, true)) {
                break;
            }
        }
    }

    @Override
    public void onMapLongClick(LatLng point) {
        // We know the center, let's place the outline at a point 3/4 along the view.
        View view = getSupportFragmentManager().findFragmentById(R.id.map).getView();
        LatLng radiusLatLng = mMap.getProjection().fromScreenLocation(new Point(
                view.getHeight() * 3 / 4, view.getWidth() * 3 / 4));

        // Create the circle.
//        DraggableCircle circle = new DraggableCircle(point, toRadiusMeters(point, radiusLatLng));
//        mCircles.add(circle);
    }

    public void toggleClickability(View view) {
        boolean clickable = ((CheckBox) view).isChecked();
        // Set each of the circles to be clickable or not, based on the
        // state of the checkbox.
        for (DraggableCircle draggableCircle : mCircles) {
            draggableCircle.setClickable(clickable);
        }
    }

    private class MyAsyncTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            ApiClient defaultClient = Configuration.getDefaultApiClient();
            defaultClient.setoAuthApiKey(Utility.getStringMeta(CircleDemoActivity.this,"pb_api_key"));
            defaultClient.setoAuthSecret(Utility.getStringMeta(CircleDemoActivity.this,"pb_api_secret"));

            final LIAPIGeoEnrichServiceApi api = new LIAPIGeoEnrichServiceApi();

            String latitude = params[0];//"35.0118";
            String longitude = params[1];//"-81.9571";
            String searchRadius = params[2];//"2640";
            String searchRadiusUnit = "meters";//"feet";

            Locations resp = null;

            try {
                Log.i("GeoAPIs","getAddress");
                resp = api.getAddress(latitude, longitude, searchRadius, searchRadiusUnit);
            } catch (ApiException e) {
                e.printStackTrace();
                hideProgress();
            }
            return resp!=null?resp.toString():"";
        }

        @Override
        protected void onPostExecute(String result) {
            // might want to change "executed" for the returned string passed
            // into onPostExecute() but that is upto you*/
            Log.i("Result",result);
            try {
                hideProgress();
                Toast.makeText(CircleDemoActivity.this,result,Toast.LENGTH_LONG).show();
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

    private class ScrollAnimationHandler implements AppBarLayout.OnOffsetChangedListener{
        private static final int PERCENTAGE_TO_SHOW_IMAGE = 20;
        private int mMaxScrollSize;
        private boolean mIsImageHidden;

        @Override
        public void onOffsetChanged(AppBarLayout appBarLayout, int i) {
            if (mMaxScrollSize == 0)
                mMaxScrollSize = appBarLayout.getTotalScrollRange();

            int currentScrollPercentage = (Math.abs(i)) * 100
                    / mMaxScrollSize;

            if (currentScrollPercentage >= PERCENTAGE_TO_SHOW_IMAGE) {
                if (!mIsImageHidden) {
                    mIsImageHidden = true;

                    ViewCompat.animate(mFab).scaleY(0).scaleX(0).start();
                }
            }

            if (currentScrollPercentage < PERCENTAGE_TO_SHOW_IMAGE) {
                if (mIsImageHidden) {
                    mIsImageHidden = false;
                    ViewCompat.animate(mFab).scaleY(1).scaleX(1).start();
                }
            }
        }
    }

}