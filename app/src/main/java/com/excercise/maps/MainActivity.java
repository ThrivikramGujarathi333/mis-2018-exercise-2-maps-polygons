package com.excercise.maps;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.maps.android.SphericalUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private EditText etName;
    private PolygonOptions polygonOptions;
    private ArrayList<Marker> markerArrayList = new ArrayList<>();
    private Polygon polygon;
    private Button btnPolyLine;
    private SharedPreferences preferences;

    private Set<String> latSet = new HashSet<>();
    private Set<String> longSet = new HashSet<>();
    private Set<String> location = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        etName = findViewById(R.id.etName);
        btnPolyLine = findViewById(R.id.btnPolyLine);
        btnPolyLine.setVisibility(View.GONE);
        preferences = getSharedPreferences(getPackageName(), Context.MODE_PRIVATE);
        latSet = preferences.getStringSet("LatSetKey", latSet);
        longSet = preferences.getStringSet("LongSetKey", longSet);
        location = preferences.getStringSet("LocationSetKey", location);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.g_map);
        mapFragment.getMapAsync(this);

        btnPolyLine.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (btnPolyLine.getText().toString().equalsIgnoreCase(getResources().getString(R.string.end_polyline))) {
                    clearAll();
                } else {
                    drawPolyLines();
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_item, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_clear:
                clearAll();
                return true;
            case R.id.action_ployline:
                drawPolyLines();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.mMap = googleMap;
        mMap.setOnMapLongClickListener(longClickListener);

        if (location != null && !location.isEmpty()) {
            String[] locationArray = location.toArray(new String[location.size()]);
            String[] latArray = latSet.toArray(new String[latSet.size()]);
            String[] longArray = longSet.toArray(new String[longSet.size()]);

            for (int i = 0; i < locationArray.length; i++) {
                MarkerOptions markerOptions = new MarkerOptions();
                markerOptions.position(new LatLng(Double.parseDouble(latArray[i]), Double.parseDouble(longArray[i])))
                        .title(locationArray[i])
                        .draggable(false);
                markerArrayList.add(mMap.addMarker(markerOptions));
            }
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    zoomToCoverAllMarkers();
                }
            }, 1000);
        }
    }

    private GoogleMap.OnMapLongClickListener longClickListener = new GoogleMap.OnMapLongClickListener() {

        @Override
        public void onMapLongClick(LatLng latLng) {

            if (TextUtils.isEmpty(etName.getText().toString())) {
                Toast.makeText(MainActivity.this, "Please write marker Name", Toast.LENGTH_SHORT).show();
                return;
            }

            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(latLng)
                    .title(etName.getText().toString())
                    .draggable(false);

            latSet.add(String.valueOf(latLng.latitude));
            longSet.add(String.valueOf(latLng.longitude));
            location.add(String.valueOf(markerOptions.getTitle()));

            saveStringSets();
            markerArrayList.add(mMap.addMarker(markerOptions));
            etName.setText("");
            zoomToCoverAllMarkers();
        }
    };

    private void drawPolyLines() {
        if (markerArrayList == null || markerArrayList.size() < 2) {
            Toast.makeText(this, "Add more than one marker to draw polyline", Toast.LENGTH_SHORT).show();
            return;
        }
        mMap.clear();
        polygonOptions = new PolygonOptions();

        for (int i = 0; i < markerArrayList.size(); i++) {
            Marker marker = markerArrayList.get(i);
            MarkerOptions option = new MarkerOptions();
            option.position(marker.getPosition())
                    .title(marker.getTitle())
                    .draggable(false);
            mMap.addMarker(option);
            polygonOptions.add(marker.getPosition());
            polygonOptions.strokeColor(Color.RED);
            polygonOptions.fillColor(getResources().getColor(R.color.semitransparent));
            polygon = mMap.addPolygon(polygonOptions);
        }

        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(getPolygonCenterPoint())
                .title(calculatePolygonArea() + " m2")
                .draggable(false);
        mMap.addMarker(markerOptions).showInfoWindow();
        zoomToCoverAllMarkers();
    }

    private double calculatePolygonArea() {
        List<LatLng> latLngs = new ArrayList<>();
        for (Marker marker : markerArrayList) {
            latLngs.add(marker.getPosition());
        }
        return SphericalUtil.computeArea(latLngs);
    }

    private LatLng getPolygonCenterPoint() {
        LatLng centerLatLng;
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (Marker marker : markerArrayList) {
            builder.include(marker.getPosition());
        }
        LatLngBounds bounds = builder.build();
        centerLatLng = bounds.getCenter();

        return centerLatLng;
    }

    private void saveStringSets() {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putStringSet("LatSetKey", latSet);
        editor.putStringSet("LongSetKey", longSet);
        editor.putStringSet("LocationSetKey", location);
        editor.apply();
    }

    private void clearAll() {
        SharedPreferences.Editor editor = preferences.edit();
        editor.clear();
        editor.apply();
        markerArrayList.clear();
        if (mMap != null) {
            polygon.remove();
            mMap.clear();
        }
    }

    private void zoomToCoverAllMarkers() {
        if (mMap == null || markerArrayList.size() < 2) return;
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (Marker marker : markerArrayList) {
            builder.include(marker.getPosition());
        }
        LatLngBounds bounds = builder.build();
        CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, 30);
        mMap.setPadding(50, 50, 50, 50);
        mMap.animateCamera(cu);
    }
}
