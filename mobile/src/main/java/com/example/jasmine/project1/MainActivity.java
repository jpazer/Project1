package com.example.jasmine.project1;

import android.*;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.Uri;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.games.event.Event;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.CapabilityApi;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

//***** Need to implement listeners for Message, Data and GoogleAPI
public class MainActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {


//***** Should declare constants for the PATH values for Message and
//***** Data API's and keys for bundles being passed back and forth
//***** And add variables for the GoogleApiClient
//***** You might also want a variable to hold the current item being
//***** searched for.
    private GoogleApiClient mGoogleAPIClient;
    private String currentItem;
    private static final String WEAR_MESSAGE_PATH = "/message";

    private ArrayList<HashMap<String,String>> huntObjects;
    private ArrayList<HashMap<String,String>> foundObjects;
    private SimpleAdapter adapter;
    private ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //initialize scavenger hunt objects
        huntObjects = new ArrayList<HashMap<String, String>>();
        HashMap<String, String> item = new HashMap<String, String>();

        //acorn
        item.put("name","acorn");
        item.put("picture",Integer.toString(R.drawable.acorn));
        huntObjects.add(item);

        //chipmunk
        item = new HashMap<String, String>();
        item.put("name","chipmunk");
        item.put("picture",Integer.toString(R.drawable.chipmunk));
        huntObjects.add(item);

        //dandelion
        item = new HashMap<String, String>();
        item.put("name","dandelion");
        item.put("picture",Integer.toString(R.drawable.dandelion));
        huntObjects.add(item);

        //maple leaf
        item = new HashMap<String, String>();
        item.put("name","maple leaf");
        item.put("picture",Integer.toString(R.drawable.maple));
        huntObjects.add(item);

        //pine-cone
        item = new HashMap<String, String>();
        item.put("name","pine-cone");
        item.put("picture",Integer.toString(R.drawable.pine_cone));
        huntObjects.add(item);

        //initialize foundObjects
        foundObjects = new ArrayList<HashMap<String, String>>();

        // Keys used in Hashmap
        String[] from = { "itemPic","foundAt" };

        // Ids of views in list_item layout
        int[] to = { R.id.itemPic,R.id.location};

        // Instantiating an adapter to store each items
        // R.layout.list_item defines the layout of each item
        adapter = new SimpleAdapter(getBaseContext(), foundObjects, R.layout.list_item, from, to);

        // Getting a reference to listview of activity_main.xml layout file
        listView = ( ListView ) findViewById(R.id.listView);

        // Setting the adapter to the listView
        listView.setAdapter(adapter);

        // display message on empty list
        TextView emptyText = (TextView) View.inflate(this,
                R.layout.empty, null);
        emptyText.setVisibility(View.GONE);

        ((ViewGroup)listView.getParent()).addView(emptyText);
        listView.setEmptyView(emptyText);
        
//**** Create your GoogleApiClient here....
        mGoogleAPIClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();
        mGoogleAPIClient.connect();
        
    } //onCreate

    //action for startHunt button
//***** replace this code with wear interaction - comment out
//***** adding the items to foundObjects and uncomment the code below
//***** it
    public void startHunt(View v) {
        HashMap<String, String> item = new HashMap<String, String>();

        //acorn
        item.put("itemPic",Integer.toString(R.drawable.acorn));
        item.put("foundAt","42.9120,-77.4556");
        foundObjects.add(item);

        //chipmunk
        item = new HashMap<String, String>();
        item.put("itemPic",Integer.toString(R.drawable.chipmunk));
        item.put("foundAt","42.9120,-77.4556");
        foundObjects.add(item);

        //dandelion
        item = new HashMap<String, String>();
        item.put("itemPic",Integer.toString(R.drawable.dandelion));
        item.put("foundAt","42.9120,-77.4556");
        foundObjects.add(item);

        //maple leaf
        item = new HashMap<String, String>();
        item.put("itemPic",Integer.toString(R.drawable.maple));
        item.put("foundAt","42.9120,-77.4556");
        foundObjects.add(item);

        //pine-cone
        item = new HashMap<String, String>();
        item.put("itemPic",Integer.toString(R.drawable.pine_cone));
        item.put("foundAt","42.9120,-77.4556");
        foundObjects.add(item);

        adapter.notifyDataSetChanged();
        
//         foundObjects.clear();
//         adapter.notifyDataSetChanged();
// 
//         Uri uri = new Uri.Builder().scheme(PutDataRequest.WEAR_URI_SCHEME).path("found").build();
//         Wearable.DataApi.deleteDataItems(mGoogleApiClient, uri, DataApi.FILTER_PREFIX);
//         currentItem = 0;
//         startButton.setEnabled(false);
//         Bitmap image = BitmapFactory.decodeResource(getResources(), Integer.parseInt(huntObjects.get(0).get("picture")));
//         sendPhoto(toAsset(image));

    }
    
        /**
     * Builds an {@link Asset} from a bitmap. The image that we get
     * back from the camera in "data" is a thumbnail size. Typically, your image should not exceed
     * 320x320 and if you want to have zoom and parallax effect in your app, limit the size of your
     * image to 640x400. Resize your image before transferring to your wearable device.
     */
    private static Asset toAsset(Bitmap bitmap) {
        ByteArrayOutputStream byteStream = null;
        try {
            byteStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteStream);
            return Asset.createFromBytes(byteStream.toByteArray());
        } finally {
            if (null != byteStream) {
                try {
                    byteStream.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }

    /**
     * Sends the asset that was created from the photo we took by adding it to the Data Item store.
     */
    private void sendPhoto(Asset asset) {
//***** Create your PutDataMapRequest using the path you defined above
//***** and putAsset() method of the PutDataMapRequest.getDataMap() object
//***** add a putLong to pass the date to the request as well.
 //****  Then do your putDataItem of the Wearable.DataApi

    }

    
//***** Add your onStart and onStop overrides to connect (onStart) /disconnect & remove 
//***** Listeners (onStop)
    @Override
    protected void onStart(){
        super.onStart();
    }

//***** Implement your connection callbacks for GoogleApiClient, adding your
//***** listeners in the onConnected method.
    @Override
    public void onConnected(Bundle connectionHint){
        //create location request object
        LocationRequest locationRequest = LocationRequest.create();

        //use high accuracy
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        //update every 2s
        locationRequest.setInterval(TimeUnit.SECONDS.toMillis(2));

        //set the fastest update interval to 2s
        locationRequest.setFastestInterval(TimeUnit.SECONDS.toMillis(2));

        //set minimum displacement for accuracy in meters
        locationRequest.setSmallestDisplacement(2);

        //register the listener
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleAPIClient,locationRequest,this);
    }//onConnected

    @Override
    public void onConnectionSuspended(int i){

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult){

    }

    @Override
    protected void onStop(){
        //disconnect from google play services
        if (mGoogleAPIClient.isConnected()){
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleAPIClient,this);
            mGoogleAPIClient.disconnect();
        }
        super.onStop();
    }

    @Override
    public void onLocationChanged(Location location){

    }

//***** Implement your onDataChanged callback method. Check to see
//***** the currentItem is the last or not and act accordingly (either send the
//***** next item or if last item, send a message to the Wear (on a new thread)
//***** that the hunt is finished.  
//***** Get the found item, add it to the foundObjects HashMap
    public void onDataChanged(DataEventBuffer dataEvents){

        for (DataEvent event:dataEvents){
            if (event.getType() == DataEvent.TYPE_CHANGED){


            }//changed type
        }//for

    }
   
} //class
