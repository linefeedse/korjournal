package se.linefeed.korjournal;


import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;
import android.util.Log;

import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Comparator;

class OCREngine {
    private Bitmap originalImage = null;
    private TessBaseAPI tessAPI;
    private String datapath = "";
    private Context mContext;
    private String photoFile = null;
    private CropBounds[] cropBounds = null;

    OCREngine(Context context) {
        mContext = context;
        datapath = context.getFilesDir() + "/tesseract/";
        tessAPI = new TessBaseAPI();
        checkFile(new File(datapath + "tessdata/"));
        tessAPI.init(datapath, "eng");
        tessAPI.setVariable("tessedit_char_whitelist", "0123456789");
    }

    private class CropBounds {
        public int x1;
        public int y1;
        public int x2;
        public int y2;

        CropBounds(int[] bounds) {
            x1 = bounds[0];
            y1 = bounds[1];
            x2 = bounds[2];
            y2 = bounds[3];
        }
    }

    private void checkFile(File dir) {
        if (!dir.exists()&& dir.mkdirs()){
            copyFiles();
        }
        if(dir.exists()) {
            String datafilepath = datapath+ "/tessdata/eng.traineddata";
            File datafile = new File(datafilepath);

            if (!datafile.exists()) {
                copyFiles();
            }
        }
    }

    private void copyFiles() {
        try {
            String filepath = datapath + "/tessdata/eng.traineddata";
            AssetManager assetManager = mContext.getAssets();

            InputStream instream = assetManager.open("tessdata/eng.traineddata");
            OutputStream outstream = new FileOutputStream(filepath);

            byte[] buffer = new byte[1024];
            int read;
            while ((read = instream.read(buffer)) != -1) {
                outstream.write(buffer, 0, read);
            }

            outstream.flush();
            outstream.close();
            instream.close();

            File file = new File(filepath);
            if (!file.exists()) {
                throw new FileNotFoundException();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Boolean loadBitmap() {
        File dir = new File(mContext.getExternalFilesDir(null), "korjournal");
        File[] photoFiles = dir.listFiles();

        if (photoFiles == null || photoFiles.length < 1) {
            photoFile = null;
            return false;
        }
        if (photoFiles.length > 1) {
            Arrays.sort(photoFiles, new Comparator<File>() {
                @Override
                public int compare(File lhs, File rhs) {
                    return Long.valueOf(rhs.lastModified()).compareTo(lhs.lastModified());
                }
            });
            //FIXME delete old files
        }

        photoFile = photoFiles[0].getAbsolutePath();
        Log.d("loadLastOdoImage", "Found file: " + photoFile);
        Bitmap originalBitmap = BitmapFactory.decodeFile(photoFile);
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, originalBitmap.getWidth()/2, originalBitmap.getHeight()/2,false);
        originalImage = Bitmap.createScaledBitmap(resizedBitmap, originalBitmap.getWidth(),originalBitmap.getHeight(),false);
        originalBitmap.recycle();
        resizedBitmap.recycle();
        setupCropBounds(originalImage.getWidth(),originalImage.getHeight());
        return true;
    }

    public Bitmap getBitmap() {
        return originalImage;
    }

    private void setupCropBounds(int width, int height) {
            /*
     height, width = img.shape
        x1 = 0
        y1 = 0
        xleft = int(width * 0.17)
        xright = int(width * 0.83)
        ybottom = int(height * 0.83)
        ytop = int(height * 0.17)
        xmiddle1 = int(width*0.07)
        xmiddle2 = int(width*0.93)
        ymiddle1 = int(height*0.07)
        ymiddle2 = int(height*0.93)
        x2 = width
        y2 = height

        crops = [
            [y1, ybottom, xleft, x2],
            [ymiddle1, ymiddle2, xleft, x2],
            [ytop, y2, xleft, x2],
            [ytop, y2, x1, xright],
            [ymiddle1, ymiddle2, x1, xright],
            [y1, ybottom, x1, xright],
            [ymiddle1, ymiddle2, xmiddle1, xmiddle2]
        ]

     */
        int x1 = 0;
        int y1 = 0;
        int xleft = (int) Math.round(width * 0.17);
        int xright = (int) Math.round(width * 0.83);
        int ybottom = (int) Math.round(height * 0.83);
        int ytop = (int) Math.round(height * 0.17);
        int xmiddle1 = (int) Math.round(width*0.07);
        int xmiddle2 = (int) Math.round(width*0.93);
        int ymiddle1 = (int) Math.round(height*0.07);
        int ymiddle2 = (int) Math.round(height*0.93);
        int x2 = width;
        int y2 = height;

        cropBounds = new CropBounds[7];
        cropBounds[0] = new CropBounds(new int[] {xleft, y1,       x2,    ybottom});
        cropBounds[1] = new CropBounds(new int[] {xleft, ymiddle1, x2,    ymiddle2});
        cropBounds[2] = new CropBounds(new int[] {xleft, ytop,     x2,    y2});
        cropBounds[3] = new CropBounds(new int[] {x1,    ytop,     xright,y2});
        cropBounds[4] = new CropBounds(new int[] {x1,    ymiddle1, xright,ymiddle2});
        cropBounds[5] = new CropBounds(new int[] {x1,    y1,       xright,ybottom});
        cropBounds[6] = new CropBounds(new int[] {xmiddle1,ymiddle1,xmiddle2,ymiddle2});
    }

    int runOCR() {
        if (originalImage == null) {
            return -1;
        }

        tessAPI.setImage(originalImage);
        String OCRresult = tessAPI.getUTF8Text();
        OCRresult = OCRresult.replaceAll("[^0-9]", "");
        try {
            return Integer.parseInt(OCRresult);
        }
        catch (NumberFormatException e) {
            Log.d("OCR", "could not parse string" + OCRresult );
            return -2;
        }
    }

    int runOCR(int crop) {
        if (originalImage == null) {
            return -1;
        }
        return runOCR(crop, originalImage);
    }

    int runOCR(int crop, Bitmap originalImage) {
        if (crop < 0) {
            tessAPI.setImage(smartCrop(originalImage));
        } else {
            setupCropBounds(originalImage.getWidth(), originalImage.getHeight());
            tessAPI.setImage(cropBitmap(originalImage, cropBounds[crop]));
        }
        String OCRresult = tessAPI.getUTF8Text();
        OCRresult = OCRresult.replaceAll("[^0-9]", "");
        try {
            return Integer.parseInt(OCRresult);
        }
        catch (NumberFormatException e) {
            Log.d("OCR", "could not parse string" + OCRresult );
            return -2;
        }
    }

    private Bitmap cropBitmap(Bitmap bitmap, CropBounds cropBounds) {
        return Bitmap.createBitmap(bitmap, cropBounds.x1, cropBounds.y1, cropBounds.x2 - cropBounds.x1, cropBounds.y2 - cropBounds.y1);
    }

    private int scanVarianceY(@NonNull Bitmap bmp, int yStart, int yStep) {
        int width = bmp.getWidth();
        int height = bmp.getHeight();
        final int pixels[] = new int[width];
        bmp.getPixels(pixels, 0, width, 0, yStart, width, 1);
        double referenceVariance = pixelVariance(pixels);
        int finalY = (yStep > 0 ? height : 0);
        for (int yScan = yStart ;; yScan += yStep) {
            if (yStep > 0 && yScan >= height)
                break;
            if (yStep < 0 && yScan <= 0)
                break;
            bmp.getPixels(pixels, 0, width, 0,yScan, width, 1);
            if (pixelVariance(pixels) < (referenceVariance/10)) {
                finalY = (yStep > 0 ? yScan+1 : yScan-1);
                Log.i("OCREngine", "scanY " + yStep + " at y = " + yScan + " variance: " + pixelVariance(pixels));
                break;
            }
        }
        return finalY;
    }

    private int scanVarianceX(@NonNull Bitmap bmp, int xStart, int lowerY, int cellWidth, int cellHeight, int xStep) {
        int width = bmp.getWidth();
        final int pixels[] = new int[width];
        double referenceVariance = 0.0;
        int finalX = (xStep > 0 ? width : 0);
        for (int xScan = xStart ;; xScan+=xStep) {
            if (xStep > 0 && xScan >= (width-cellWidth))
                break;
            if (xStep < 0 && xScan <= 0)
                break;
            int offset = 0;
            int yScanStride = cellHeight*cellWidth/width + 1;
            for (int yScan = lowerY; yScan < lowerY+cellHeight; yScan+=yScanStride) {
                bmp.getPixels(pixels, offset, width, xScan, yScan, cellWidth, 1);
                offset += cellWidth;
            }
            double variance = pixelVariance(pixels);
            if (referenceVariance < variance) {
                referenceVariance = variance;
            } else if (variance < referenceVariance / 10) {
                finalX = (xStep > 0 ? xScan+1 : xScan-1);
                Log.i("OCREngine", "Hscan "+xStep+" at x = " + xScan + " variance: " + pixelVariance(pixels));
                break;
            }
        }
        return finalX;
    }

    private Bitmap smartCrop(Bitmap bmp) {
        int width = bmp.getWidth();
        int height = bmp.getHeight();
        int upperY = scanVarianceY(bmp, height/2, 2);
        int lowerY = scanVarianceY(bmp,height/2, -2);
        int cellWidth=width/16;

        // scan only upper half of number area, avoid "km" written low to the right of numbers
        int cellHeight=(upperY-lowerY)/2;
        int upperX = scanVarianceX(bmp, 2*width/3, lowerY, cellWidth, cellHeight, cellWidth/4);

        // scan almost whole height, ignore crap at lower 20% of image
        cellHeight=4*(upperY-lowerY)/5;
        int lowerX = scanVarianceX(bmp, width/2-cellWidth, lowerY, cellWidth, cellHeight, -cellWidth/4);

        // widely spaced numbers, use whole width
        if (width/(upperX-lowerX) > 2) {
            lowerX=0;
            upperX=width;
        }

        Log.i("OCREngine", "Smartcrop x1 = " + lowerX + " x2 = " + upperX + " y1 = " + lowerY + " y2 = " + upperY);
        return Bitmap.createBitmap(bmp,lowerX,lowerY,upperX-lowerX,upperY-lowerY);
    }

    private double pixelVariance(int[] pixels) {
        double meanSum = 0.0;
        int width = 0;
        for (int i : pixels) {
            meanSum += pixelGreyValue(i);
            width += 1;
        }
        double mean = meanSum / width;
        double varianceSum = 0.0;
        for (int i : pixels) {
            double greyValue = pixelGreyValue(i);
            varianceSum += (greyValue - mean) * (greyValue - mean);
        }
        return varianceSum / (width - 1);
    }

    private double pixelGreyValue(int pixel) {
        final int r = (pixel >> 16) & 0xFF;
        final int g = (pixel >> 8) & 0xFF;
        final int b = pixel & 0xFF;

        // see: https://en.wikipedia.org/wiki/Relative_luminance
        return (int) (0.2126 * r + 0.7152 * g + 0.0722 * b);
    }

    String getPhotoFile() {
        return photoFile;
    }
}
