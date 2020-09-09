package com.example.drawpolygon;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;

import java.util.ArrayList;

public class MapsActivity extends FragmentActivity implements
        OnMapReadyCallback,
        GoogleMap.OnMapClickListener,
        GoogleMap.OnMyLocationButtonClickListener,
        View.OnClickListener {

    private String TAG = "MapsActivity";
    private int mMarkerCount;
    private boolean isEditMaps = false;
    private ArrayList<LatLng> mLatLngList = new ArrayList<>();
    private ArrayList<Marker> mMarkerOptionsList = new ArrayList();

    private LocationManager mLocationManager;
    private Polygon mPolygon;
    private GoogleMap mMap;
    private Button drawClear;
    private Button drawStart;
    private Button drawFinish;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        drawClear = findViewById(R.id.btn_draw_clear);
        drawStart = findViewById(R.id.btn_draw_start);
        drawFinish = findViewById(R.id.btn_draw_finish);
        drawClear.setOnClickListener(this);
        drawStart.setOnClickListener(this);
        drawFinish.setOnClickListener(this);
        drawStart.setVisibility(View.VISIBLE);
        drawFinish.setVisibility(View.INVISIBLE);

        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        getCurrentLocation();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        mMap.getUiSettings().setMapToolbarEnabled(false);
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.setOnMapClickListener(this);
        mMap.setOnMyLocationButtonClickListener(this);
        permissionCheck();
    }

    @Override
    public void onMapClick(LatLng latLng) {
        if (latLng != null) {
            if (isEditMaps) {
                mLatLngList.add(latLng);
                addMarker();
                editMapsPolygon();
            }
        } else {
            Toast.makeText(this, "無法取得經緯度資訊", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onMyLocationButtonClick() {
        getCurrentLocation();
        return false;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_draw_clear:
                clear();
                break;
            case R.id.btn_draw_start:
                isEditMaps = true;
                drawStart.setVisibility(View.INVISIBLE);
                drawFinish.setVisibility(View.VISIBLE);
                break;
            case R.id.btn_draw_finish:
                isEditMaps = false;
                drawStart.setVisibility(View.VISIBLE);
                drawFinish.setVisibility(View.INVISIBLE);
                break;
        }
    }

    /**
     * 標記位置
     */
    private void addMarker() {
        if (mLatLngList.size() > 0 && mMarkerOptionsList != null) {
            // 標記數量++
            mMarkerCount++;
            // 標記點位
            LatLng latLng = mLatLngList.get(mLatLngList.size() - 1);
            // Marker設定
            MarkerOptions markerOptions = new MarkerOptions()
                    .position(latLng)
                    .title("標記" + mMarkerCount)
                    //.icon(BitmapDescriptorFactory.fromResource(R.drawable.pin)) // 可自訂icon
                    .snippet(String.format("%.6f", latLng.latitude) + ", " + String.format("%.6f", latLng.longitude));
            // 新增Marker
            mMarkerOptionsList.add(mMap.addMarker(markerOptions));
        }
    }

    /**
     * 畫圖畫線
     */
    private void editMapsPolygon() {
        // 清除上一次的紀錄
        if (mPolygon != null) {
            mPolygon.remove();
        }
        // 線的顏色
        int lineColor = Color.BLUE;
        // 填滿的顏色
        int fillColor = ContextCompat.getColor(this, R.color.colorDefaultFill);
        // 設定PolyGon
        PolygonOptions polygonOptions = new PolygonOptions()
                .addAll(mLatLngList)
                .strokeColor(lineColor)
                .strokeWidth(3f)
                .fillColor(fillColor)
                .zIndex(6);
        // 在地圖上新增Polygon
        mPolygon = mMap.addPolygon(polygonOptions);
    }

    /**
     * 清除地圖上的部分標記
     */
    public void clear() {
        if (mLatLngList.size() == 0) {
            return;
        }
        if (mLatLngList.size() == 1) {
            clearMaps();
            mMarkerCount--;
        } else {
            mLatLngList.remove(mLatLngList.size() - 1);
            editMapsPolygon();
            removeMarker();
        }
    }

    /**
     * 清除地圖上的所有標記
     */
    private void clearMaps() {
        // 清除點位的陣列
        mLatLngList.clear();
        // 清除PolyGon
        mPolygon.remove();
        // 清除Map上的所有標記
        for (Marker marker : mMarkerOptionsList) {
            marker.remove();
        }
        // 清除標記陣列
        mMarkerOptionsList.clear();
    }

    /**
     * 刪除Marker
     */
    private void removeMarker() {
        if (mMarkerOptionsList.size() > 0) {
            // 取出最後一個Marker 刪除
            mMarkerOptionsList.get(mMarkerOptionsList.size() - 1).remove();
            // 刪除陣列中最後一個Marker
            mMarkerOptionsList.remove(mMarkerOptionsList.size() - 1);
            // 標記數量--
            mMarkerCount--;
        }
    }

    /**
     * 檢查權限
     */
    private void permissionCheck() {
        ArrayList<String> permissionRequest = new ArrayList<>();
        ArrayList<String> permissionAskFromUser = new ArrayList<>();
        permissionRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        permissionRequest.add(Manifest.permission.ACCESS_FINE_LOCATION);

        for (String permission : permissionRequest) {
            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionAskFromUser.add(permission);
            }
        }
        if (permissionAskFromUser.size() == 0) {
            mMap.setMyLocationEnabled(true);
        } else {
            String[] reqPer = new String[permissionAskFromUser.size()];
            reqPer = permissionAskFromUser.toArray(reqPer);
            requestPermissions(reqPer, 0);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 0:
                if (grantResults.length > 0 && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                        return;
                    }
                    mMap.setMyLocationEnabled(true);
                }
                break;
        }
    }

    /**
     * 定位
     */
    private LocationListener mLocationListener = new LocationListener() {

        @Override
        public void onLocationChanged(Location location) {
            if (location != null) {
                Log.d(TAG, "onLocationChanged: " + String.format("%f, %f", location.getLatitude(), location.getLongitude()));
                currentLocation(location);
                mLocationManager.removeUpdates(mLocationListener);
            } else {
                Log.d(TAG, "onLocationChanged: location null");
            }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    };

    private void getCurrentLocation() {
        boolean isGPSEnabled = mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean isNetworkEnabled = mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        Location location = null;


        if (isGPSEnabled) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 10, mLocationListener);
            location = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        } else if (isNetworkEnabled) {
            mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000, 10, mLocationListener);
            location = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        } else {
            Toast.makeText(this, "GPS定位訊號不良", Toast.LENGTH_SHORT).show();
        }

        if (location != null) {
            currentLocation(location);
        }
    }

    private void currentLocation(Location location) {
        if (mMap != null) {
            LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        getCurrentLocation();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mLocationManager.removeUpdates(mLocationListener);
    }
}