package se.linefeed.korjournal;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class GuideView extends SurfaceView implements SurfaceHolder.Callback {
    private SurfaceHolder mHolder;
    public GuideView(Context context,  AttributeSet attrs) {
        super(context, attrs);
    }
    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        mHolder = getHolder();
        mHolder.addCallback(this);
        mHolder.setFormat(PixelFormat.TRANSLUCENT);
        this.setZOrderMediaOverlay(true);
    }
    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int width, int height) {
        Surface surface = surfaceHolder.getSurface();
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.YELLOW);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(10);

        float left,right,top,bottom;
        if (width > height) {
            left=width*0.1f;
            right= width*0.9f;
            top=height*0.35f;
            bottom=height*0.65f;
        } else {
            left = width*0.2f;
            right = width*0.8f;
            top = height*0.45f;
            bottom =  height*0.55f;
        }
        Canvas canvas = surface.lockCanvas(null);
        try {
            Log.i("INFO", "drawCircleSurface: isHwAcc=" + canvas.isHardwareAccelerated());
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            Log.d("GuideView", "drawRect: l:" + left + " t:" + top + " r:" + right + " b:" + bottom);
            canvas.drawRect(left,top,right,bottom,paint);
        } finally {
            surface.unlockCanvasAndPost(canvas);
        }
    }
    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

    }

}
