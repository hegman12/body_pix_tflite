package com.bodypix.body_pix;

import org.tensorflow.lite.support.common.TensorOperator;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

public class SqueezeFirstDimOp implements TensorOperator {

    @Override
    public TensorBuffer apply(TensorBuffer tensorBuffer) {
        int[] shape=tensorBuffer.getShape();
        tensorBuffer.loadBuffer(tensorBuffer.getBuffer(),new int[]{shape[1],shape[2],shape[3]});
        return tensorBuffer;
    }
}
