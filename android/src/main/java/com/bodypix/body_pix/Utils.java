package com.bodypix.body_pix;

public class Utils {

    private Utils(){}

    static boolean to_valid_input_resolution(double in_resolution,double out_stride){
        return (in_resolution - 1) % out_stride == 0;
    }

    static int toValidInputResolution(int in_resolution,double out_stride) {
        if (to_valid_input_resolution(in_resolution, out_stride)) return in_resolution;
        return (int) (Math.floor(in_resolution / out_stride) * out_stride + 1);
    }

    static int[] calculatePaddingDims(int targetImageHeight,int targetImageWidth,int inputImageHeight,int inputImageWidth){

        int[] padDims=new int[4];

        final float targetAspect = (float)targetImageWidth / targetImageHeight;
        final float aspect = (float)inputImageWidth / inputImageHeight;
        System.out.println("targetAspect: "+targetAspect+", aspect"+aspect);

        int padT=0, padB=0, padL=0, padR=0;

        if (aspect < targetAspect) {
            padT = 0;
            padB = 0;
            final int t = (int) Math.round(0.5 * (targetAspect * inputImageHeight - inputImageWidth));
            padL = t;
            padR = t;
        }else{
            final int t = (int) Math.round(0.5 * ((1.0 / targetAspect) * inputImageWidth - inputImageHeight));
            padT = t;
            padB = t;
            padL = 0;
            padR = 0;
        }

        System.out.printf("PADDING - top: %d, bottom: %d, left: %d, right: %d\n", padT, padB, padL, padR);

        padDims[0]=padT;
        padDims[1]=padB;
        padDims[2]=padL;
        padDims[3]=padR;

        return padDims;

    }


}
