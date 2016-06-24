package com.nirzvi.vuforiaimport;

import android.graphics.Camera;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.vuforia.CameraCalibration;
import com.vuforia.DataSet;
import com.vuforia.Matrix44F;
import com.vuforia.ObjectTracker;
import com.vuforia.STORAGE_TYPE;
import com.vuforia.State;
import com.vuforia.Tool;
import com.vuforia.Trackable;
import com.vuforia.TrackableResult;
import com.vuforia.Tracker;
import com.vuforia.TrackerManager;
import com.vuforia.Vuforia;
import com.vuforia.VuforiaBase;
import com.vuforia.CameraDevice;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements Vuforia.UpdateCallbackInterface {

    Object shutdownLock = new Object();
    String LOGTAG = "Vuforia FIXIT";

    InitVuforiaTask mInitTask;
    LoadTrackerTask mLoadTask;

    Matrix44F mProjectionMatrix;
    DataSet mCurrentDataset;

    private int mCurrentDatasetSelectionIndex = 0;
    private int mStartDatasetsIndex = 0;
    private int mDatasetsNumber = 0;
    private ArrayList<String> mDatasetStrings = new ArrayList<String>();

    boolean mStarted = false;
    boolean mCameraRunning = false;

    TextView text;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initVuforia();
        text = (TextView) findViewById(R.id.target_status);
        text.setText("HELLO");


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();

        Vuforia.onResume();

        if (mStarted) {
            startVuforiaCamera();
        }

    }

    @Override
    protected void onPause() {

        super.onPause();

        if (mStarted) {
            stopCamera();
        }

        Vuforia.onPause();
    }

    public boolean doInitTrackers() {

        TrackerManager tManager = TrackerManager.getInstance();
        Tracker track = tManager.initTracker(ObjectTracker.getClassType());

        if (track == null) {
            return false;
        } else {
            return true;
        }

    }

    public boolean doStartTrackers() {
        Tracker track = TrackerManager.getInstance().getTracker(ObjectTracker.getClassType());

        if (track != null) {
            track.start();
        } else {
            return false;
        }

        return true;
    }

    public boolean doStopTrackers()
    {
        // Indicate if the trackers were stopped correctly
        boolean result = true;

        Tracker objectTracker = TrackerManager.getInstance().getTracker(
                ObjectTracker.getClassType());
        if (objectTracker != null)
            objectTracker.stop();

        return result;
    }

    public boolean doLoadTrackersData()
    {
        TrackerManager tManager = TrackerManager.getInstance();
        ObjectTracker objectTracker = (ObjectTracker) tManager
                .getTracker(ObjectTracker.getClassType());
        if (objectTracker == null)
            return false;

        if (mCurrentDataset == null)
            mCurrentDataset = objectTracker.createDataSet();

        if (mCurrentDataset == null)
            return false;

        if (!mCurrentDataset.load("Calculator_OT.xml",
                STORAGE_TYPE.STORAGE_APPRESOURCE))
            return false;

        if (!objectTracker.activateDataSet(mCurrentDataset))
            return false;

        int numTrackables = mCurrentDataset.getNumTrackables();
        for (int count = 0; count < numTrackables; count++)
        {
            Trackable trackable = mCurrentDataset.getTrackable(count);


            String name = "Current Dataset : " + trackable.getName();
            trackable.setUserData(name);
            Log.d(LOGTAG, "UserData:Set the following user data "
                    + (String) trackable.getUserData());
        }

        return true;
    }

    public boolean doUnloadTrackersData()
    {
        // Indicate if the trackers were unloaded correctly
        boolean result = true;

        TrackerManager tManager = TrackerManager.getInstance();
        ObjectTracker objectTracker = (ObjectTracker) tManager
                .getTracker(ObjectTracker.getClassType());
        if (objectTracker == null)
            return false;

        if (mCurrentDataset != null && mCurrentDataset.isActive())
        {
            if (objectTracker.getActiveDataSet().equals(mCurrentDataset)
                    && !objectTracker.deactivateDataSet(mCurrentDataset))
            {
                result = false;
            } else if (!objectTracker.destroyDataSet(mCurrentDataset))
            {
                result = false;
            }

            mCurrentDataset = null;
        }

        return result;
    }



    public void initVuforia() {

        try {
            mInitTask = new InitVuforiaTask();
            mInitTask.execute();
        } catch (Exception e) {
            e.printStackTrace();
            onInitDone(false);
        }

    }


    public boolean onInitDone(boolean success) {

        if (success) {

            startVuforiaCamera();

            CameraDevice.getInstance().setFocusMode(CameraDevice.FOCUS_MODE.FOCUS_MODE_CONTINUOUSAUTO);

            return true;
        }//if

        return false;
    }//onInitDone

    public void setProjectionMatrix() {
        CameraCalibration camCal = CameraDevice.getInstance()
                .getCameraCalibration();
        mProjectionMatrix = Tool.getProjectionGL(camCal, 10.0f, 5000.0f);
    }

    public boolean startVuforiaCamera() {
        if (!CameraDevice.getInstance().init(CameraDevice.CAMERA_DIRECTION.CAMERA_DIRECTION_DEFAULT)) {
            return false;
        }

        if (!CameraDevice.getInstance().selectVideoMode(CameraDevice.MODE.MODE_DEFAULT)) {
            return false;
        }

        if (!CameraDevice.getInstance().start()) {
            return false;
        }

        setProjectionMatrix();

        doStartTrackers();

        if (!CameraDevice.getInstance().setFocusMode(CameraDevice.FOCUS_MODE.FOCUS_MODE_CONTINUOUSAUTO)) {

            if (!CameraDevice.getInstance().setFocusMode(CameraDevice.FOCUS_MODE.FOCUS_MODE_TRIGGERAUTO)) {

                CameraDevice.getInstance().setFocusMode(CameraDevice.FOCUS_MODE.FOCUS_MODE_NORMAL);

            }//if
        }//if

        mCameraRunning = true;
        return true;
    }

    public void stopCamera() {

        if(mCameraRunning) {
            doStopTrackers();
            CameraDevice.getInstance().stop();
            CameraDevice.getInstance().deinit();
            mCameraRunning = false;
        }//if

    }//stopCamera

    public void setTextOfView(final String data) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (text != null) {
                    text.setText(data);
                }
            }
        });
    }

    @Override
    public void Vuforia_onUpdate(State s) {
        int numResults = s.getNumTrackableResults();

        TrackableResult result = s.getTrackableResult(0);

        if (result != null) {
            float[] data = result.getPose().getData();
            float[][] rotation = {{data[0], data[1], data[2]},
                    {data[4], data[5], data[6]},
                    {data[8], data[9], data[10]}};

            double thetaX = Math.atan2(rotation[2][1], rotation[2][2]);
            double thetaY = Math.atan2(-rotation[2][0], Math.sqrt(rotation[2][1] * rotation[2][1] + rotation[2][2] * rotation[2][2]));
            double thetaZ = Math.atan2(rotation[1][0], rotation[0][0]);

            String totalData = "AngleX: " + thetaX + "\n AngleY: " + thetaY + "\n AngleZ: " + thetaZ;
            totalData += "\n DistX: " + data[3] + "\n DistY: " + data[7] + "\n distZ: " + data[11];


            setTextOfView(totalData);
        }
    }

    private class InitVuforiaTask extends AsyncTask<Void, Integer, Boolean>
    {
        // Initialize with invalid value:
        private int mProgressValue = -1;


        protected Boolean doInBackground(Void... params)
        {
            // Prevent the onDestroy() method to overlap with initialization:
            synchronized (shutdownLock)
            {
                Vuforia.setInitParameters(MainActivity.this, VuforiaBase.GL_20, "Ad0I0ir/////AAAAAfR3NIO1HkxSqM8NPhlEftFXtFAm6DC5w4Cjcy30WUdGozklFlAkxeHpjfWc4moeL2ZTPvZ+wAoyOnlZxyB6Wr1BRE9154j6K1/8tPvu21y5ke1MIbyoJ/5BAQuiwoAadjptZ8fpS7A0QGPrMe0VauJIM1mW3UU2ezYFSOcPghCOCvQ8zid1Bb8A92IkbLcBUcv3DEC6ia4SEkbRMY7TpOh2gzsXdsue4tqj9g7vj7zBU5Hu4WhkMDJRsThn+5QoHXqvavDsCElwmDHG3hlEYo7qN/vV9VcQUX9XnVLuDeZhkp885BHK5vAe8T9W3Vxj2H/R4oijQso6hEBaXsOpCHIWGcuphpoe9yoQlmNRRZ97");

                do
                {
                    // Vuforia.init() blocks until an initialization step is
                    // complete, then it proceeds to the next step and reports
                    // progress in percents (0 ... 100%).
                    // If Vuforia.init() returns -1, it indicates an error.
                    // Initialization is done when progress has reached 100%.
                    mProgressValue = Vuforia.init();

                    // Publish the progress value:
                    publishProgress(mProgressValue);

                    // We check whether the task has been canceled in the
                    // meantime (by calling AsyncTask.cancel(true)).
                    // and bail out if it has, thus stopping this thread.
                    // This is necessary as the AsyncTask will run to completion
                    // regardless of the status of the component that
                    // started is.
                } while (!isCancelled() && mProgressValue >= 0
                        && mProgressValue < 100);

                return (mProgressValue > 0);
            }
        }


        protected void onProgressUpdate(Integer... values)
        {
            // Do something with the progress value "values[0]", e.g. update
            // splash screen, progress bar, etc.
        }


        protected void onPostExecute(Boolean result)
        {
            // Done initializing Vuforia, proceed to next application
            // initialization status:

            if (result)
            {
                Log.d(LOGTAG, "InitVuforiaTask.onPostExecute: Vuforia "
                        + "initialization successful");

                boolean initTrackersResult;
                initTrackersResult = doInitTrackers();

                if (initTrackersResult)
                {
                    try
                    {
                        mLoadTask = new LoadTrackerTask();
                        mLoadTask.execute();
                    } catch (Exception e)
                    {
                        String logMessage = "Loading tracking data set failed";
                        Log.e(LOGTAG, logMessage);
                        onInitDone(true);
                    }

                } else {
                    onInitDone(false);
                }
            } else
            {
                onInitDone(false);
            }
        }
    }

    // An async task to load the tracker data asynchronously.
    private class LoadTrackerTask extends AsyncTask<Void, Integer, Boolean>
    {
        protected Boolean doInBackground(Void... params)
        {
            // Prevent the onDestroy() method to overlap:
            synchronized (shutdownLock)
            {
                // Load the tracker data set:
                return doLoadTrackersData();
            }
        }


        protected void onPostExecute(Boolean result)
        {

            if (!result)
            {
                onInitDone(false);
            } else
            {
                // Hint to the virtual machine that it would be a good time to
                // run the garbage collector:
                //
                // NOTE: This is only a hint. There is no guarantee that the
                // garbage collector will actually be run.
                System.gc();

                Vuforia.registerCallback(MainActivity.this);

                mStarted = true;

                onInitDone(true);
            }

        }
    }
}
