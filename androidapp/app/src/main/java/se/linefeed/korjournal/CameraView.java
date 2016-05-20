package se.linefeed.korjournal;

import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;
import java.util.Locale;

public class CameraView extends SurfaceView implements SurfaceHolder.Callback {

    private SurfaceHolder mHolder;
    private CameraActivity mContext;
    private Camera mCamera;

    public CameraView(Context context, Camera camera){
        super(context);

        mContext = (CameraActivity) context;
        mCamera = camera;
        mCamera.setDisplayOrientation(90);
        setZoom(2);
        //get the holder and set this class as the callback, so we can get camera data here
        mHolder = getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_NORMAL);
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        try{
            //when the surface is created, we can set the camera to draw images in this surfaceholder
            mCamera.setPreviewDisplay(surfaceHolder);
            mCamera.startPreview();
        } catch (IOException e) {
            Log.d("ERROR", "Camera error on surfaceCreated " + e.getMessage());
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int width, int height) {
        //before changing the application orientation, you need to stop the preview, rotate and then start it again
        if(mHolder.getSurface() == null)//check if the surface is ready to receive camera data
            return;

        try{
            mCamera.stopPreview();
        } catch (Exception e){
            //this will happen when you are trying the camera if it's not running
        }
        if (width>height) {
            // landscape mode
            mCamera.setDisplayOrientation(0);
            mContext.cameraOrientation = 0;
            setZoom(1);
        } else {
            mCamera.setDisplayOrientation(90);
            mContext.cameraOrientation = 90;
            setZoom(1.5f);
        }
        Log.d("CameraView","Orientation is now " + mContext.cameraOrientation);
        //now, recreate the camera preview
        try{
            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();
        } catch (IOException e) {
            Log.d("ERROR", "Camera error on surfaceChanged " + e.getMessage());
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        //our app has only one screen, so we'll destroy the camera in the surface
        //if you are unsing with more screens, please move this code your activity
        mCamera.stopPreview();
        mCamera.release();
    }

    private void setZoom(float level) {
        Camera.Parameters parameters = mCamera.getParameters();
        if (!parameters.isZoomSupported()) {
            Log.i("INFO", "Zoom is not supported");
        } else {
            int maxZoom = parameters.getMaxZoom();
            Log.i("INFO", String.format(Locale.getDefault(), "Max zoom is %d", maxZoom));
            if (maxZoom > level) {
                parameters.setZoom((int) (maxZoom / level));
                mCamera.setParameters(parameters);
            } else {
                Log.d("ERROR", "Max zoom is less than asked for");
            }
        }

    }
}
