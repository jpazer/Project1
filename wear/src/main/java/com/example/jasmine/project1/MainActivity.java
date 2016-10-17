package com.example.jasmine.project1;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GestureDetectorCompat;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.io.InputStream;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static android.R.attr.path;

//***** Implement your GoogleApiClient, DataApi, MessageApi and 
//***** Location callbacks/listeners
public class MainActivity extends Activity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener, MessageApi.MessageListener, DataApi.DataListener {

    private static final int SPEECH_REQUEST_CODE = 0;
    private static final String TAG = "WearMainActivity";

    //***** use the same paths/keys as in mobile side
    private static final String DONE_PATH = "/done";
    private static final String FOUND_PATH = "/found-it";
    private static final String ITEM_PATH = "/item";
    private String ITEM_KEY = "item";
    private String LOCATION_KEY = "location";


    //***** create your GoogleApiClient
    private GoogleApiClient mGoogleAPIClient;

    private ImageView itemPhoto;
    private GestureDetectorCompat tapDetector;
    private double latitude, longitude;

    //***** You might want some sort of boolean flag as to whether you are
//***** currently looking for an item or not.
    private boolean working = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//***** You might want to keep the screen on:
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


//***** create and build you GoogleApiClient and add the Wearable and LocationServices APIs
//***** and callbacks
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                itemPhoto = (ImageView) stub.findViewById(R.id.photo);
                itemPhoto.setImageResource(R.drawable.photo_placeholder);
            }
        });

        if (mGoogleAPIClient == null) {
            mGoogleAPIClient = new GoogleApiClient.Builder(this)
                    .addApi(LocationServices.API)
                    .addApi(Wearable.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
        }
        if (!mGoogleAPIClient.isConnected() || mGoogleAPIClient.isConnecting()) {
            mGoogleAPIClient.connect();
        }
        //create a tap detector
        tapDetector = new GestureDetectorCompat(MainActivity.this, new MyGestureListener(MainActivity.this));
//***** You might want to set your flag that you aren't currently looking for and item
        working = false;

    } //onCreate

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (working) {
            return tapDetector.onTouchEvent(event);
        } else {
            return super.dispatchTouchEvent(event);
        }
    }

    //Custom GestureDetector.SimpleOnGestureListener
    public class MyGestureListener extends GestureDetector.SimpleOnGestureListener {

        final Context myContext;
        public MyGestureListener(Context context) {
            myContext = context;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            // Detected long press, showing exit widget
            Log.d(TAG, "Long Press");
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            Log.d(TAG, "single tap");
            doVoiceInput();
            return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent event) {
            //present a dialog box for "found it"
            DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int choice) {
                    switch (choice) {
                        case DialogInterface.BUTTON_POSITIVE:
                            //call code to send response
                            sendData(latitude, longitude, System.currentTimeMillis());
                            String msg = "Location sent";
                            showDialog(msg);
                            break;
                        case DialogInterface.BUTTON_NEGATIVE:
                            //ignore
                            break;
                    }
                }
            };

            LayoutInflater inflater = LayoutInflater.from(myContext);

            View dialogView = inflater.inflate(R.layout.myalert, null);
            TextView textMsg = (TextView) dialogView.findViewById(R.id.msgText);
            textMsg.setText("Did you find it?");
            AlertDialog.Builder builder = new AlertDialog.Builder(myContext);
            builder.setView(dialogView)
                    .setPositiveButton("Yes", dialogClickListener).show();

            return true;
        }

    }//MyGestureListener

    //***** override your onResume/on Pause methods and connect (onResume)
//***** or disconnect/remove listeners (onPause)
    @Override
    public void onPause() {
        if (mGoogleAPIClient != null && mGoogleAPIClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleAPIClient, this);
            Wearable.MessageApi.removeListener(mGoogleAPIClient, this);
            Wearable.DataApi.removeListener(mGoogleAPIClient, this);
            mGoogleAPIClient.disconnect();
        }
    }

    @Override
    public void onResume() {
        if (!mGoogleAPIClient.isConnected() || mGoogleAPIClient.isConnecting()) {
            mGoogleAPIClient.connect();
        }
        super.onResume();
    }

    //***** implement your GoogleApiClient connection methods. In the onConnected
//***** method, add your listeners for the message and data APIs and the
//***** location permissions and setup
//create a location request and register as a listener when connected
    @Override
    public void onConnected(Bundle connectionHint) {
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
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        //add listeners
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleAPIClient, locationRequest, this);
        Wearable.DataApi.addListener(mGoogleAPIClient, this);
        Wearable.MessageApi.addListener(mGoogleAPIClient, this);

    }//onConnected

    @Override
    public void onConnectionSuspended(int i) {
    }
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
    }
    @Override
    protected void onStart() {
        super.onStart();
    }

//***** implement your onDataChanged method, checking your path
//***** to see if it is the next item. This code will get the image from
//***** You may also want to set your working on an item flag to true here.
@Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        for (DataEvent event : dataEvents) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                //get the path
                String path = event.getDataItem().getUri().getPath();
                //check our path
                if (path.equals(ITEM_PATH)) {
                    working = true;
                    DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
                    Asset photoAsset = dataMapItem.getDataMap().getAsset(ITEM_KEY);
                    // Loads image on background thread.
                    new LoadBitmapAsyncTask().execute(photoAsset);
                    Log.d("pic", "The picture has been received and is loading");
                }
            }//changed type
        }//for
    }

    //***** Implement your onMessageReceived event here.
//***** The code below will set the image back to the placeholder and
//***** pop up a dialog box. You may want to put this code in a separate 
//***** function and call it from multiple places.
//***** You may also want to turn your working on an item flag to false here as well
    @Override
    public void onMessageReceived(final MessageEvent messageEvent) {
        //Check the path to see if one we want
        if (messageEvent.getPath().equalsIgnoreCase(DONE_PATH)) {
            working = false;
            itemPhoto.setImageResource(R.drawable.photo_placeholder);
            showDialog("You've found it all!");
        }

    }//onMessageReceived

//***** Implement the LocationListener callback for location updates

    @Override
    public void onLocationChanged(Location location) {
        latitude = location.getLatitude();
        longitude = location.getLongitude();
    }


//***** Finish this code for the voice input:
// This callback is invoked when the Speech Recognizer returns.
// This is where you process the intent and extract the speech text from the intent.
//***** See the link for doVoiceInput for more info on this method.
//Create an intent that can start the Speech Recognizer activity 

    private void displaySpeechRecognizer() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        //start the activity, the intent will be populated with the speech text
        startActivityForResult(intent, SPEECH_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK) {
            //check the text and if "found it", send the data and show a dialog
            //stating either, Location Sent or what they were supposed to say.
            List<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            String spokenText = results.get(0);

            // check to see if equals “Found it” and act accordingly
            String msg = "";
            if (spokenText.toLowerCase().equals("found it")) {
                sendData(latitude, longitude, System.currentTimeMillis());
                msg = "Location sent";
            } else {
                msg = "You have to say\n'Found it'";
            }
            showDialog(msg);
        }
        working = true;
        super.onActivityResult(requestCode, resultCode, data);
    }//onActivityResult

    private void showDialog(String msg) {
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int choice) {
                switch (choice) {
                    case DialogInterface.BUTTON_POSITIVE:
                        itemPhoto.setImageResource(R.drawable.photo_placeholder);
                        break;
                    case DialogInterface.BUTTON_NEGATIVE:
                        //ignore
                        break;
                }
            }
        };

        LayoutInflater inflater = this.getLayoutInflater();

        View dialogView = inflater.inflate(R.layout.myalert, null);
        TextView textMsg = (TextView) dialogView.findViewById(R.id.msgText);
        textMsg.setText(msg);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView)
                .setPositiveButton("OK", dialogClickListener).show();
    }
    //***** finish the code for free form voice input to allow "found it" to be said: see
    //***** https://developer.android.com/training/wearables/apps/voice.html
    private void doVoiceInput() {

        // Start the activity, the intent will be populated with the speech text
        displaySpeechRecognizer();
    }

    //***** There is where you will send the data back to the mobile side
    private void sendData(double latitude, double longitude, long timestamp) {
        //create a PutDataMapRequest and specify a path for it
        //The path here is used to reference this object and
        //make it unique compared to all the other once that we can
        //create.
        PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(FOUND_PATH);

        //Next we use put methods to add an String (location info as a string) 
        //and long key value pair for the time.
        String data = String.valueOf(latitude) + "," + String.valueOf(longitude);
        putDataMapRequest.getDataMap().putString(LOCATION_KEY, data);
        putDataMapRequest.getDataMap().putLong("timestamp", timestamp);

        //We then use putDataItem to submit the object,
        //and then we need to check for errors with the callback
        //to find underneath.
        PutDataRequest request = putDataMapRequest.asPutDataRequest();

        Wearable.DataApi.putDataItem(mGoogleAPIClient, request)
                .setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                    @Override
                    public void onResult(@NonNull DataApi.DataItemResult dataItemResult) {
                        if (!dataItemResult.getStatus().isSuccess()) {
                            Log.e("WATCH", "Failed to send found-it location data item /" + path + dataItemResult.getStatus());
                        } else {
                            //item been collected but not necessarily delivered
                            Log.d("WATCH", "Successfully sent found-it location data item /" + path + dataItemResult.getStatus());
                            //set back to placeholder image for feedback
                            itemPhoto.setImageResource(R.drawable.photo_placeholder);
                        }
                    }
                });


        //if successful, change to placeholder image again.

    }//send data

    /*
     * Extracts {@link android.graphics.Bitmap} data from the
     * {@link com.google.android.gms.wearable.Asset}
     */
    private class LoadBitmapAsyncTask extends AsyncTask<Asset, Void, Bitmap> {

        @Override
        protected Bitmap doInBackground(Asset... params) {

            if (params.length > 0) {

                Asset asset = params[0];

                InputStream assetInputStream = Wearable.DataApi.getFdForAsset(
                        mGoogleAPIClient, asset).await().getInputStream();

                if (assetInputStream == null) {
                    Log.w(TAG, "Requested an unknown Asset.");
                    return null;
                }
                return BitmapFactory.decodeStream(assetInputStream);

            } else {
                Log.e(TAG, "Asset must be non-null");
                return null;
            }
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {

            if (bitmap != null) {
                Log.d(TAG, "Setting background image");
                itemPhoto.setImageBitmap(bitmap);
            }
        }
    } //asynch task


}
