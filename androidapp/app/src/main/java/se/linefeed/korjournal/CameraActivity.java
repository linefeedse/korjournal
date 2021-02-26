package se.linefeed.korjournal;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

import java.util.List;

import se.linefeed.korjournal.R;
import se.linefeed.korjournal.api.RequestDoneInterface;

public class CameraActivity extends AppCompatActivity {
    private Camera mCamera = null;
    private CameraView mCameraView = null;
    private GuideView mGuideView = null;
    private Context mContext = null;
    private FrameLayout cameraFrame = null;
    public int cameraOrientation = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContext = this;
        setContentView(R.layout.activity_camera);

        cameraFrame = (FrameLayout)findViewById(R.id.camera_view);
        mGuideView = (GuideView) findViewById(R.id.guideView);
        mGuideView.getHolder().addCallback(mGuideView);

        Button noImage = (Button)findViewById(R.id.imgClose);
        noImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MyApplication app = (MyApplication) getApplicationContext();
                app.setPictureTaken(false);
                app.setNextFsmState(MyApplication.FSM_SELECT);
                finish();
            }
        });
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    1);
        }
    }

    private void initCamera() {
        try{
            mCamera = Camera.open();//you can use open(int) to use different cameras
        } catch (Exception e){
            Log.d("ERROR", "Failed to get camera: " + e.getMessage());
        }

        if(mCamera != null) {
            mCameraView = new CameraView(this, mCamera);//create a SurfaceView to show camera data
            mCameraView.setSiblingView(mGuideView);
            cameraFrame.addView(mCameraView);//add the SurfaceView to the layout
            Camera.Parameters params = mCamera.getParameters();
            if (params.getSupportedFocusModes().contains(
                    Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            }
            List<Camera.Size> picSizes = params.getSupportedPictureSizes();
            for (Camera.Size size: picSizes) {
                Log.d("INFO", "PictureSize: " + size.width + "x" + size.height);
                if (size.width <= 1280) {
                    params.setPictureSize(size.width,size.height);
                    break;
                }
            }
            mCamera.setParameters(params);
            Button imageSnap = (Button)findViewById(R.id.imageSnap);
            imageSnap.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    MyApplication app = (MyApplication) getApplicationContext();
                    try {
                        mCamera.takePicture(null, rawCallback, jpegCallback);
                    }
                    catch (RuntimeException e) {
                        e.printStackTrace();
                    }
                    app.setNextFsmState(MyApplication.FSM_SELECT);
                }
            });
            imageSnap.setClickable(true);

            Button imgClose = (Button)findViewById(R.id.imgClose);
            imgClose.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    MyApplication app = (MyApplication) getApplicationContext();
                    //Fixme: delete any old pictures
                    app.setNextFsmState(MyApplication.FSM_SELECT);
                    finish();
                }
            });


        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                  initCamera();
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request.
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        if (mCamera == null) {
            initCamera();
        }

    }

    @Override
    protected void onStart() {
        super.onStart();

        if (mCamera == null) {
            initCamera();
        }

        final MyApplication app = (MyApplication) getApplicationContext();
        app.requestOdosnaps(new RequestDoneInterface() {
            @Override
            public void done(int ignored) {}
            @Override
            public void error(String e) {}
        });
    }

    @Override
    protected void onStop() {
        super.onStop();

        cameraFrame.removeAllViews();
        mCamera = null;
        mCameraView = null;
    }

    Camera.PictureCallback rawCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            // noop
        }
    };

    Camera.PictureCallback jpegCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            Log.d("INFO", "onPictureTaken - jpeg data length "+ data.length);
            // Ignore the leakage warning, mContext is used with a WeakReference
            SavePictureTask spt = new SavePictureTask(mContext) {
                @Override
                protected void onPostExecute(Void v) {
                    MyApplication application = (MyApplication) getApplicationContext();
                    application.setPictureTaken(true);
                    ((CameraActivity) mContext).finish();
                }
            };
            spt.execute(data);
        }
    };
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return OptionsMenu.onCreateOptionsMenu(this, menu);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (OptionsMenu.handleOptionsItemSelected(this,item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
