package com.bodypix.body_pix;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.graphics.Rect;

import org.tensorflow.lite.support.image.ImageOperator;
import org.tensorflow.lite.support.image.TensorImage;

public class Crop3DOp implements ImageOperator {

    private int left;
    private int right;
    private int top;
    private int bottom;

    public Crop3DOp(int left, int right,int top, int bottom){

        this.left=left;
        this.right=right;
        this.top=top;
        this.bottom=bottom;
    }

    @Override
    public TensorImage apply(TensorImage tensorImage) {
        int inputImageWidth = tensorImage.getWidth();
        int inputImageHeight = tensorImage.getHeight();
        System.out.println("Crop3DOp: inputImageWidth - "+inputImageWidth+", inputImageHeight - "+inputImageHeight);

        int dstL=0;
        int dstR=inputImageWidth-this.left-this.right;
        int dstT=0;
        int dstB=inputImageHeight-this.bottom-this.top;

        int srcL=this.left;
        int srcR=inputImageWidth-this.right;

        int srcT=this.top;
        int srcB=inputImageHeight-this.bottom;

        if(((inputImageHeight-this.bottom-this.top) == inputImageHeight) && ((inputImageWidth-this.left-this.right) == inputImageWidth)){
            //Not doing anything if we are not cropping, just return
            return tensorImage;
        }

        Bitmap input = tensorImage.getBitmap();
        final Bitmap output = Bitmap.createBitmap(dstR+dstL, dstT+dstB, Bitmap.Config.ARGB_8888);
        Rect src = new Rect(srcL, srcT, srcR, srcB);
        Rect dst = new Rect(dstL, dstT, dstR, dstB);
        new Canvas(output).drawBitmap(input, src, dst, null);
        tensorImage.load(output);
        System.out.println("Crop3DOp: outputImageWidth - "+tensorImage.getWidth()+", outputImageHeight - "+tensorImage.getHeight());
        return tensorImage;
    }

    @Override
    public int getOutputImageWidth(int inputImageHeight, int inputImageWidth) {
        return inputImageWidth-this.left-this.right;
    }

    @Override
    public int getOutputImageHeight(int inputImageHeight, int inputImageWidth) {
        return inputImageHeight-this.top-this.bottom;
    }

    @Override
    public PointF inverseTransform(PointF pointF, int inputImageHeight, int inputImageWidth) {
        return pointF;
    }
}
