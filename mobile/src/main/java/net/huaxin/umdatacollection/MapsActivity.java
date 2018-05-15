package net.huaxin.umdatacollection;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.maps.android.MarkerManager;
import com.google.maps.android.clustering.ClusterItem;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.algo.Algorithm;
import com.google.maps.android.clustering.algo.NonHierarchicalDistanceBasedAlgorithm;
import com.google.maps.android.clustering.algo.StaticCluster;
import com.google.maps.android.clustering.view.ClusterRenderer;
import com.google.maps.android.heatmaps.HeatmapTileProvider;


import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.huaxin.umdatacollection.MyItem;


public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private ClusterManager<MyItem> mClusterManager;
    private Algorithm<MyItem> clusterManagerAlgorithm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Sydney and move the camera
        LatLng dearborn = new LatLng(42.31949, -83.23399);
        LatLng tmp;
        ArrayList<LatLng> topLocationData = new ArrayList<LatLng>();

        SQLiteDatabase mSQLiteDatabase = this.openOrCreateDatabase("location.db", MODE_PRIVATE, null);

        mClusterManager = new ClusterManager<MyItem>(this, mMap);

        // Instantiate the cluster manager algorithm as is done in the ClusterManager
        clusterManagerAlgorithm = new NonHierarchicalDistanceBasedAlgorithm();

        // Set this local algorithm in clusterManager
        mClusterManager.setAlgorithm(clusterManagerAlgorithm);
        mMap.setOnMarkerClickListener(mClusterManager);
        mMap.setOnCameraIdleListener(mClusterManager);

        try{
            Cursor cur = mSQLiteDatabase.rawQuery("SELECT * FROM ForTopLocations", null);
             if(cur != null){
                if(cur.moveToFirst()){
                    do{
                        tmp = new LatLng(cur.getFloat(cur.getColumnIndex("Lat")),cur.getFloat(cur.getColumnIndex("Lon")));
                        mMap.addMarker(new MarkerOptions().position(tmp).title("locations"));
                        MyItem offsetItem = new MyItem(cur.getFloat(cur.getColumnIndex("Lat")), cur.getFloat(cur.getColumnIndex("Lon")));
                        mClusterManager.addItem(offsetItem);
                        topLocationData.add(tmp);
                    }while(cur.moveToNext());
                }
            }

        }catch(Exception e){
            Log.e("error", e.toString());
        }

        /*
        //Points for testing
        InputStream inputStream = getResources().openRawResource(R.raw.radar_search);
        try {
            List<MyItem> items = new MyItemReader().read(inputStream);

        for (int i = 0; i < 10; i++) {
            double offset = i / 60d;
            for (MyItem item : items) {
                LatLng position = item.getPosition();
                double lat = position.latitude + offset;
                double lng = position.longitude + offset;
                MyItem offsetItem = new MyItem(lat, lng);
                mClusterManager.addItem(offsetItem);
            }
        }
        }catch(Exception e){}
        */

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(dearborn,17));
        HeatmapTileProvider mProvider = new HeatmapTileProvider.Builder()
                .data(topLocationData)
                .build();
        mMap.addTileOverlay(new TileOverlayOptions().tileProvider(mProvider));

        mClusterManager.cluster();
        Algorithm s = mClusterManager.getAlgorithm();
        MarkerManager.Collection mClusterMarker = mClusterManager.getClusterMarkerCollection();
        Collection a = mClusterMarker.getMarkers();
        /*try{
            mSQLiteDatabase.execSQL("DROP TABLE TopLocations");

        }catch(Exception e){}
        */
        try{
            mSQLiteDatabase.execSQL("create table TopLocations (" +
                    "Lat REAL, " +
                    "Lon REAL, " +
                    "CSize REAL)");
        }catch(Exception e){}
        for (Object element : s.getClusters(0)) {
            StaticCluster<MyItem> clu = (StaticCluster<MyItem>) element;
            ContentValues cv = new ContentValues();
            cv.put("Lat", clu.getPosition().latitude);
            cv.put("Lon", clu.getPosition().longitude);
            cv.put("CSize",clu.getSize());
            mSQLiteDatabase.insert("TopLocations", null, cv);
            Log.v("Size",String.valueOf(clu.getSize()));
        }
        mSQLiteDatabase.close();

    }

    public class MyItem1 implements ClusterItem {
        private final LatLng mPosition;

        public MyItem1(double lat, double lng) {
            mPosition = new LatLng(lat, lng);
        }

        @Override
        public LatLng getPosition() {
            return mPosition;
        }
    }
}
