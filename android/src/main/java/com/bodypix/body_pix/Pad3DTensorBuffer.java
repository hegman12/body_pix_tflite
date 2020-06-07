package com.bodypix.body_pix;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;

import org.tensorflow.lite.support.common.TensorOperator;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

public class Pad3DTensorBuffer implements TensorOperator {

    private int padL;
    private int padR;
    private int padT;
    private int padB;

    Pad3DTensorBuffer(int padL, int padR,int padT, int padB){
        this.padL=padL;
        this.padR=padR;
        this.padT=padT;
        this.padB=padB;
    }

    @Override
    public TensorBuffer apply(TensorBuffer tensorBuffer) {

        //TensorImage tensorImage= new TensorImage();
        //tensorImage.load(tensorBuffer.get);

        int[] shape=tensorBuffer.getShape();
        int inputImageWidth=shape[1];
        int inputImageHeight=shape[0];
        System.out.println("Pad3DTensorBuffer: inputImageWidth - "+inputImageWidth+", inputImageHeight - "+inputImageHeight);
        //int inputImageWidth = tensorImage.getWidth();
        //int inputImageHeight = tensorImage.getHeight();

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
        System.out.println("Pad3DTensorBuffer: outputImageWidth - "+tensorImage.getWidth()+", outputImageHeight - "+tensorImage.getHeight());
        return tensorImage.getTensorBuffer();
    }
}
