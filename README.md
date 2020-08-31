# What is body_pix

A new flutter plugin project for demo of Google's Bodypix model in tensorflow lite.

# Getting Started

#### 1. Bodypix is tensorflowjs graph model. How did you convert the tensorflowjs graph model to tflite.
You can check **[this](https://github.com/hegman12/body_pix_tflite/blob/master/converter_example/bodypix_MobileNetV1_to_tflite_converter.ipynb)** jupyter notebook for the conversion process. This describes how to convert tensorflowjs to tflite.

#### 2. How to use this tflite model in android?
You can check **[this](https://github.com/hegman12/body_pix_tflite/blob/master/android/src/main/java/com/bodypix/body_pix/BodyPixPlugin.java)** java file to know how to use this model. Note that this java code is not optimized. So, use this as a reference.

#### 3. How this model perform? Can I use this for real time person masking with camera?
This is not quantized model or optimized java code. It takes around 300 to 600ms to detech 1 image as of now. With quantized model and further optimization of java code as well as environment(using custom built tflite interpretor etc) I believe its possible to improve the performance and use it in real time application with camera etc. I am using FlexDelegate and it is also adding significant overhead.


# Parts segmentation result

![result](https://github.com/hegman12/body_pix_tflite/blob/master/converter_example/bodypix_android.jpg)
