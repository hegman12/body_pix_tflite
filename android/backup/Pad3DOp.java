package com.bodypix.body_pix;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.graphics.Rect;

import org.tensorflow.lite.support.image.ImageOperator;
import org.tensorflow.lite.support.image.TensorImage;

public class Pad3DOp implements ImageOperator {

    private int padL;
    private int padR;
    private int padT;
    private int padB;

    public Pad3DOp(int padL, int padR,int padT, int padB){

        this.padL=padL;
        this.padR=padR;
        this.padT=padT;
        this.padB=padB;
    }

    @Override
    public TensorImage apply(TensorImage tensorImage) {

        int inputImageWidth = tensorImage.getWidth();
        int inputImageHeight = tensorImage.getHeight();
        System.out.println("Pad3DOp: inputImageWidth - "+inputImageWidth+", inputImageHeight - "+inputImageHeight);

        int srcL=0;
        int srcR=inputImageWidth;
        int srcT=0;
        int srcB=inputImageHeight;

        int dstL=this.padL;
        int dstR=inputImageWidth+padR;

        int dstT=padT;
        int dstB=inputImageHeight+padB;

        if((dstR+dstL == srcR) && (dstT+dstB == srcB)){
            //Not doing anything if we are not padding, just return
            return tensorImage;
        }

        Bitmap input = tensorImage.getBitmap();
        final Bitmap output = Bitmap.createBitmap(dstR+dstL, dstT+dstB, Bitmap.Config.ARGB_8888);
        Rect src = new Rect(srcL, srcT, srcR, srcB);
        Rect dst = new Rect(dstL, dstT, dstR, dstB);
        new Canvas(output).drawBitmap(input, src, dst, null);
        tensorImage.load(output);
        System.out.println("Pad3DOp: outputImageWidth - "+tensorImage.getWidth()+", outputImageHeight - "+tensorImage.getHeight());
        return tensorImage;
    }

    @Override
    public int getOutputImageWidth(int inputImageHeight, int inputImageWidth) {
        return inputImageWidth+this.padL+this.padB;
    }

    @Override
    public int getOutputImageHeight(int inputImageHeight, int inputImageWidth) {
        return inputImageHeight+this.padT+this.padB;
    }

    @Override
    public PointF inverseTransform(PointF pointF, int inputImageHeight, int inputImageWidth) {
        return pointF;
    }
}
