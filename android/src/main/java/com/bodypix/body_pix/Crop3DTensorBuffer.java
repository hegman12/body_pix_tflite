package com.bodypix.body_pix;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;

import org.tensorflow.lite.support.common.TensorOperator;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

public class Crop3DTensorBuffer implements TensorOperator {
    private int left;
    private int right;
    private int top;
    private int bottom;

    Crop3DTensorBuffer(int left, int right,int top, int bottom){
        this.left=left;
        this.right=right;
        this.top=top;
        this.bottom=bottom;
    }

    @Override
    public TensorBuffer apply(TensorBuffer tensorBuffer) {

        int[] shape=tensorBuffer.getShape();
        int inputImageWidth=shape[1];
        int inputImageHeight=shape[0];
        System.out.println("Crop3DTensorBuffer: inputImageWidth - "+inputImageWidth+", inputImageHeight - "+inputImageHeight);

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
            return tensorBuffer;
        }

        TensorImage tensorImage=new TensorImage();
        tensorImage.load(tensorBuffer);
        Bitmap input = tensorImage.getBitmap();
        final Bitmap output = Bitmap.createBitmap(dstR+dstL, dstT+dstB, Bitmap.Config.ARGB_8888);
        Rect src = new Rect(srcL, srcT, srcR, srcB);
        Rect dst = new Rect(dstL, dstT, dstR, dstB);
        new Canvas(output).drawBitmap(input, src, dst, null);
        tensorImage.load(output);
        System.out.println("Crop3DTensorBuffer: outputImageWidth - "+tensorImage.getWidth()+", outputImageHeight - "+tensorImage.getHeight());
        return tensorImage.getTensorBuffer();
    }
}
