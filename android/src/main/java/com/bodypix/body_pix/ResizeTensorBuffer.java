package com.bodypix.body_pix;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ColorSpace;
import android.graphics.Matrix;
import android.os.Build;

import org.tensorflow.lite.TensorFlowLite;
import org.tensorflow.lite.annotations.UsedByReflection;
import org.tensorflow.lite.Tensor;

import androidx.annotation.RequiresApi;

import org.tensorflow.lite.Tensor;
import org.tensorflow.lite.support.common.TensorOperator;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.nio.ByteBuffer;

public class ResizeTensorBuffer implements TensorOperator {
    private int targetHeight;
    private int targetWidth;
    ResizeTensorBuffer(int targetHeight,int targetWidth){
        this.targetHeight=targetHeight;
        this.targetWidth=targetWidth;
    }



    public TensorBuffer apply(TensorBuffer tensorBuffer) {
        ByteBuffer byteBuffer=tensorBuffer.getBuffer();
        int size=byteBuffer.arrayOffset();
        int[] shape=tensorBuffer.getShape();
        int height=shape[0];
        int width=shape[1];
        int channels=shape[2];

        //TensorFlowLite tensorFlowLite=TensorFlowLite.init();
        int length=byteBuffer.remaining();
        System.out.println("LENGTH: "+length);
        System.out.println("SHAPE: "+shape[2]);
        System.out.println("offset: "+byteBuffer.arrayOffset());
        ByteBuffer target=ByteBuffer.allocateDirect(size*targetHeight*targetWidth*channels);
        byte[] tempBuffer=new byte[targetHeight*targetWidth*3];
        byteBuffer.order();

        for(int i=0;i<length;i+=shape[2]){
            for(int j=0;j<size;j++){
                tempBuffer[i+j]=(byte)(byteBuffer.getInt(i+j) & 0xFF);
                tempBuffer[i+j+1]=(byte)(byteBuffer.getInt(i+j) & 0xFF);
                tempBuffer[i+j+2]=(byte)(byteBuffer.getInt(i+j) & 0xFF);
            }
        }

        Bitmap buffer=BitmapFactory.decodeByteArray(tempBuffer,0,tempBuffer.length);
        if(buffer==null){
            System.out.println("IS NULL");
        }
        Bitmap resizedBitmap=Bitmap.createScaledBitmap(buffer,targetWidth,targetHeight,true);
//        Matrix matrix=new Matrix();
//        matrix.postScale(targetWidth, targetHeight);
//        Bitmap resizedBitmap = Bitmap.createBitmap(buffer, 0, 0,width, height, matrix, true);
        return TensorImage.fromBitmap(resizedBitmap).getTensorBuffer();
    }
}
