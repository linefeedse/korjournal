package se.linefeed.korjournal;

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import java.util.zip.InflaterOutputStream;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class CameraActivity extends AppCompatActivity {
    private Camera mCamera = null;
    private CameraView mCameraView = null;
    private GuideView mGuideView = null;
    private Context mContext = null;
    public int cameraOrientation = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContext = this;
        setContentView(R.layout.activity_camera);

        try{
            mCamera = Camera.open();//you can use open(int) to use different cameras
        } catch (Exception e){
            Log.d("ERROR", "Failed to get camera: " + e.getMessage());
        }

        FrameLayout camera_view = (FrameLayout)findViewById(R.id.camera_view);
        if(mCamera != null) {
            mCameraView = new CameraView(this, mCamera);//create a SurfaceView to show camera data
            camera_view.addView(mCameraView);//add the SurfaceView to the layout
        }
        mGuideView = (GuideView) findViewById(R.id.guideView);
        mGuideView.getHolder().addCallback(mGuideView);

        //btn to close the application
        ImageButton imageSnap = (ImageButton)findViewById(R.id.imageSnap);
        imageSnap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCamera.takePicture(null,rawCallback,jpegCallback);
            }
        });
        imageSnap.setClickable(true);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
    }
    Camera.PictureCallback rawCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            //new SavePictureTask(mContext).execute(data);
            //Log.i("INFO", "onPictureTaken - raw");
            //finish();
        }
    };
    Camera.PictureCallback jpegCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            Log.d("INFO", "onPictureTaken - jpeg data length "+ data.length);
            SavePictureTask spt = new SavePictureTask(mContext) {
                @Override
                protected void onPostExecute(Void v) {
                    ((AppCompatActivity) mContext).finish();
                }
            };
            spt.execute(data);
        }
    };
}
