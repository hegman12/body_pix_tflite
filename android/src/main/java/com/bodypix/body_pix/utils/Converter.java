package com.bodypix.body_pix.utils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

 public class Converter {
    public static Bitmap byteBufferToBitmap(ByteBuffer buffer, int[] shape) throws Exception{
        //Check if it is UINT8 or float buffer. converted bitmap will always be UINT8
        int numElements=buffer.capacity();
        int numBytes=buffer.rewind().remaining();

        int bytesPerElement=numBytes/numElements;

        if(bytesPerElement==1){
            //UINT8 type
            System.out.println("processing UINT8 ByteBuffer!");
        }
        //default configs
        int channel=3;
        boolean onlyAlpha=false;
        Bitmap.Config config=Bitmap.Config.ARGB_8888;

        try{
            channel=shape[2];
            if(channel==0){
                //mask image
                config= Bitmap.Config.ALPHA_8;
                onlyAlpha=true;
            }else if(channel==3 || channel==4){
                config= Bitmap.Config.ARGB_8888;
            }else{
                throw new Exception("Invalid alpha shape");
            }

        }catch (IndexOutOfBoundsException e){
            config= Bitmap.Config.ALPHA_8;
            onlyAlpha=true;
        }

        Bitmap image=Bitmap.createBitmap(shape[1],shape[0], config);
        buffer.rewind();
        IntBuffer intBuffer=buffer.asIntBuffer();
        for(int i=0;i<shape[0];i++){
            for(int j=0;j<shape[1];j++){
                if(onlyAlpha){
                    // mask image, get will return 1 byte uint8(or values from 0-255) which we will set as pixel.
                    // Bitmap.Config is ALPHA8 which represents only alpha channel for mask.
                    final int pixel=intBuffer.get();
                    if(pixel==1){
                        image.setPixel(j,i,255);
                    }else if(pixel==0){
                        image.setPixel(j,i,0);
                    }else{
                        image.setPixel(j,i,pixel);
                    }

                }else{
                    if(channel==3){
                        //we have 3 RGB int values in buffer, fourth will be alpha defaults to 255. So bitmap Config has to be RGBA for this
                        final int r=intBuffer.get();
                        final int g=intBuffer.get();
                        final int b=intBuffer.get();
                        //encode the RGBA into pixel of bitmap
                        final int color=(255 & 0xff) << 24 | (r & 0xff) << 16 | (g & 0xff) << 8 | (b & 0xff);
                        image.setPixel(j,i,color);
                    }else{
                        //we have 4 RGBA int values in buffer. So bitmap Config has to be RGBA for this
                        final int r=intBuffer.get();
                        final int g=intBuffer.get();
                        final int b=intBuffer.get();
                        final int a=intBuffer.get();
                        //encode the RGBA into pixel of bitmap
                        final int color=(a & 0xff) << 24 | (r & 0xff) << 16 | (g & 0xff) << 8 | (b & 0xff);
                        image.setPixel(j,i,color);
                    }
                }

            }
        }
        return image;
    }

    private int[] pixelToRGB(int pixel){
        //https://stackoverflow.com/questions/5669501/how-do-you-get-the-rgb-values-from-a-bitmap-on-an-android-device
            int R = (pixel >> 16) & 0xff;
            int G = (pixel >> 8) & 0xff;
            int B = pixel & 0xff;
            int A = (pixel >> 24) & 0xFF;
            return new int[]{R,G,B,A};

            /*
            Above can also be rewritten as. Notice the pattern in FF.
            int R = (p & 0xff0000) >> 16;
            int G = (p & 0x00ff00) >> 8;
            int B = (p & 0x0000ff) >> 0;

            OR can also be written as below, ommiting zeros.

            int R = (p & 0xff0000) >> 16;
            int G = (p & 0xff00) >> 8;
            int B = p & 0xff;

            Can also do below in android;

            int colour = bitmap.getPixel(x, y);
            int red = Color.red(colour);
            int blue = Color.blue(colour);
            int green = Color.green(colour);
            int alpha = Color.alpha(colour);
            */

    }

    public static ByteBuffer bitmapToByteBuffer(Bitmap image, DataType targetType, int channels){
        boolean isModelQuantized=false;
        int channelBytes=4;
        if(targetType==DataType.UINT8){
            isModelQuantized=true;
            channelBytes=1;
        }

        ByteBuffer imgData;
        int height=image.getHeight();
        int width=image.getWidth();

        imgData = ByteBuffer.allocateDirect(1 * height * width * channels * channelBytes);
        imgData.order(ByteOrder.nativeOrder());
        int[] intValues = new int[height * width];

        image.getPixels(intValues, 0, width, 0, 0, width, height);
        imgData.rewind();

        for (int i = 0; i < height; ++i) {
            for (int j = 0; j < width; ++j) {
                final int pixelValue = intValues[i * width + j];
                if (isModelQuantized) {
                    // Quantized model
                    imgData.put((byte) ((pixelValue >> 16) & 0xFF));
                    imgData.put((byte) ((pixelValue >> 8) & 0xFF));
                    imgData.put((byte) (pixelValue & 0xFF));
                } else { // Float model
//                    final int r = (pixelValue >> 16) & 0xFF;
//                    final int g = (pixelValue >> 8) & 0xFF;
//                    final int b = pixelValue & 0xFF;
//                    System.out.println("R: "+r+", G: "+g+", B: "+b);
                    imgData.putFloat((pixelValue >> 16) & 0xFF) ;
                    imgData.putFloat((pixelValue >> 8) & 0xFF) ;
                    imgData.putFloat(pixelValue & 0xFF);
                }
            }
        }
        imgData.rewind();
//        for(int i=0;i<10;i++){
//            //System.out.println("INSIDE PIXEL INT: "+i+" is "+imgData.getInt(i));
//            System.out.println("INSIDE PIXEL FLOAT: "+i+" is "+imgData.getFloat());
//        }
//        imgData.rewind();
        return imgData;
    }

    private static float correctPixel(int value){
        if(value==0){
            return 0.0f;
        }
        //System.out.println((float)value);
        return (float)value;
    }

   // public abstract int byteOffset();

     public static byte[] floatByteBufferToUint8(ByteBuffer image){

        byte[] outImage=new byte[image.rewind().remaining()/4];
        for(int i=0;i<outImage.length;i++){
            outImage[i]=(byte)image.getFloat();
        }
            return outImage;
     }

     static void convertBitmapToTensorBuffer(Bitmap bitmap, TensorBuffer buffer) {
         int w = bitmap.getWidth();
         int h = bitmap.getHeight();
         int[] intValues = new int[w * h];
         bitmap.getPixels(intValues, 0, w, 0, 0, w, h);
         int[] rgbValues = new int[w * h * 3];
         for (int i = 0, j = 0; i < intValues.length; i++) {
             rgbValues[j++] = ((intValues[i] >> 16) & 0xFF);
             rgbValues[j++] = ((intValues[i] >> 8) & 0xFF);
             rgbValues[j++] = (intValues[i] & 0xFF);
         }
         int[] shape = new int[] {h, w, 3};
         buffer.loadArray(rgbValues, shape);
     }

     static int scaleTo255(float value, float dataMin,float dataMax){
         int MAX=255;
         int MIN=0;
         return (int)(((value-dataMin)*(MAX-MIN))/(dataMax-dataMin));
     }

     static float scaleTo1(int value, int dataMin,int dataMax){
         float MAX=1.0f;
         float MIN=0.0f;
         return ((value-dataMin)*(MAX-MIN))/(dataMax-dataMin);
     }


 }
