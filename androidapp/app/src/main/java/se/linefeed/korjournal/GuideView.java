package se.linefeed.korjournal;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.support.v4.content.res.ResourcesCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import se.linefeed.korjournal.R;

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
        setWillNotDraw(false);
        this.setZOrderMediaOverlay(true);
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int width, int height) {
        redrawSurface(surfaceHolder, width, height);
    }

    private void redrawSurface(SurfaceHolder surfaceHolder, int width, int height) {
        Surface surface = surfaceHolder.getSurface();
        Log.d("INFO", "redrawSurface was called");
        Canvas canvas = null;
        try {
            canvas = surface.lockCanvas(null);
        }
        catch (IllegalStateException e) {
            return;
        }
        Paint paint = new Paint();
        paint.setARGB(0xAA/2, 0x34, 0x90, 0xE9);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(10);
        Paint fillPaint = new Paint();
        fillPaint.setARGB(0xAA, 0x34, 0x90, 0xE9);
        fillPaint.setStyle(Paint.Style.FILL);

        float left,right,top,bottom;
        if (width > height) {
            left =   Math.round(width*0.1f);
            right =  Math.round(width*0.9f);
            top =    Math.round(height*0.35f);
            bottom = Math.round(height*0.65f);
        } else {
            left =  Math.round(width*0.25f);
            right = Math.round(width*0.75f);
            top =   Math.round(height*0.45f);
            bottom= Math.round(height*0.55f);
        }
        RectF viewFinder = new RectF(left, top, right, bottom);
        try {
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            //canvas.drawRoundRect(viewFinder,10,10, paint);
            //canvas.drawRect(viewFinder, paint);
            Drawable d = ResourcesCompat.getDrawable(getResources(), R.drawable.corners, null);
            d.setBounds((int) left, (int) top, (int) right, (int) bottom);
            d.draw(canvas);

            canvas.drawRect(0.0f,0.0f,width*1f,top,fillPaint);
            canvas.drawRect(0.0f,top,left,bottom,fillPaint);
            canvas.drawRect(right,top,width*1f,bottom,fillPaint);
            canvas.drawRect(0.0f,bottom,width*1f,height*1f,fillPaint);
        } finally {
            surface.unlockCanvasAndPost(canvas);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
    }
}
