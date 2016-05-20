package se.linefeed.korjournal;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Camera;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

class SavePictureTask extends AsyncTask<byte[], Void, Void> {
    private Context mContext;
    public SavePictureTask (Context context) {
        mContext = context;
    }
    @Override
    protected Void doInBackground(byte[]... data) {
        FileOutputStream outStream = null;

        byte[] jpegData = data[0];
        Bitmap bm = BitmapFactory.decodeByteArray(jpegData, 0, jpegData.length);

        int orientation = ((CameraActivity) mContext).cameraOrientation;

        Matrix matrix = new Matrix();
        matrix.preRotate(orientation);

        int width,height,bottom,left;
        if (orientation == 0) {
            width = (int) (bm.getWidth() * 0.8f);
            height = (int) (bm.getHeight() * 0.3f);
            left = (int) (bm.getWidth() * 0.1f);
            bottom = (int) (bm.getHeight() * 0.35f);
        } else {
            width = (int) (bm.getHeight() * 0.6f);
            height = (int) (bm.getWidth() * 0.1f);
            left = (int) (bm.getHeight() * 0.2f);
            bottom = (int) (bm.getWidth() * 0.45f);
        }

        Bitmap rotated = Bitmap.createBitmap(bm, 0,0, bm.getWidth(),bm.getHeight(), matrix, true);
        Bitmap cropped = Bitmap.createBitmap(rotated, left, bottom, width,height);

        // Write to SD Card
        try {
            File dir = new File(mContext.getExternalFilesDir(null),"korjournal");
            dir.mkdirs();

            String fileName = String.format("%d.jpg", System.currentTimeMillis());
            File outFile = new File(dir, fileName);

            outStream = new FileOutputStream(outFile);
            cropped.compress(Bitmap.CompressFormat.JPEG,90,outStream);
            outStream.flush();
            outStream.close();

            Log.i("INFO", "doInBackground wrote image " + width + " " + height + " to " + outFile.getAbsolutePath());

            //refreshGallery(outFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
        }
        return null;
    }

}
