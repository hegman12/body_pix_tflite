package com.bodypix.body_pix;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import androidx.annotation.NonNull;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.Tensor;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.common.TensorProcessor;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;
import org.tensorflow.lite.support.tensorbuffer.TensorBufferFloat;

import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;

/** BodyPixPlugin */
public class BodyPixPlugin implements FlutterPlugin, MethodCallHandler, ActivityAware {
  /// The MethodChannel that will the communication between Flutter and native Android
  ///
  /// This local reference serves to register the plugin with the Flutter Engine and unregister it
  /// when the Flutter Engine is detached from the Activity
  private MethodChannel channel;
  private Activity activity;
  private Interpreter interpreter;


  static private double internal_resolution = 0.75;
  static private double out_stride=16;



  public BodyPixPlugin(){

  }

  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {

    channel = new MethodChannel(flutterPluginBinding.getFlutterEngine().getDartExecutor(), "body_pix");
    AssetManager am=flutterPluginBinding.getApplicationContext().getAssets();
    try {
      AssetFileDescriptor fd= am.openFd("flutter_assets/assets/bpix_model_stride16_with_unknown.tflite");
      MappedByteBuffer mb=FileUtil.loadMappedFile(flutterPluginBinding.getApplicationContext(),"flutter_assets/assets/bpix_model_stride16_with_unknown.tflite");
      interpreter=new Interpreter(mb);
    } catch (IOException e) {
      interpreter=null;
      e.printStackTrace();
    }
    channel.setMethodCallHandler(this);
    System.out.println("++++++++++++++++++++++++++++++++++++PLUGIN BINDING COMPLETE++++++++++++++++++++++++++++++++++++");
  }

  public static void registerWith(Registrar registrar) {
    final MethodChannel channel = new MethodChannel(registrar.messenger(), "body_pix");
    channel.setMethodCallHandler(new BodyPixPlugin());
    System.out.println("REGISTRATION COMPLETE");
  }

  @Override
  public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
    if (call.method.equals("getPlatformVersion")) {

      byte[] im= (byte[])call.argument("image");

      int image_width=(int)call.argument("width");
      int image_height=(int)call.argument("height");

      result.success(toInputResolutionHeightAndWidth(image_height,image_width,0.75,16,im));
    } else {
      result.notImplemented();
    }
  }


  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    interpreter.close();
    channel.setMethodCallHandler(null);
  }

  static int getCategory(double inputAspect){
    System.out.println("ASPECT: "+inputAspect);
    if(inputAspect<0.5){
      return 1;
    }else if(inputAspect>=0.5 && inputAspect<1){
      return 2;
    }else if(inputAspect>=1 && inputAspect < 1.5){
      return 3;
    }else{
      return 4;
    }

  }

void displayOutputShapeAndSize(){

    for(int i=0;i< interpreter.getOutputTensorCount();i++){
      Tensor t=interpreter.getOutputTensor(i);
      int[] shape=t.shape();
      String s=" SHAPE: ";
      for (int value : shape) {
        s = s + ":" + value;
      }
      System.out.println("TENSOR NAME: "+t.name());
      System.out.println("TENSOR SIZE: "+t.numBytes());
      System.out.println("TENSOR: "+i+s);
    }
  }

  HashMap<Integer,Object> getOutputs2(){

    HashMap<Integer,Object> outputs=new HashMap<Integer,Object>();
    outputs.put(0,TensorBufferFloat.createFixedSize(new int[]{1, 14, 14, 34},DataType.FLOAT32).getBuffer());
    outputs.put(1,TensorBufferFloat.createFixedSize(new int[]{1, 14, 14,  1},DataType.FLOAT32).getBuffer());
    outputs.put(2,TensorBufferFloat.createFixedSize(new int[]{1, 14, 14, 24},DataType.FLOAT32).getBuffer());
    outputs.put(3,TensorBufferFloat.createFixedSize(new int[]{1, 14, 14, 34},DataType.FLOAT32).getBuffer());
    outputs.put(4,TensorBufferFloat.createFixedSize(new int[]{1, 14, 14, 17},DataType.FLOAT32).getBuffer());
    outputs.put(5,TensorBufferFloat.createFixedSize(new int[]{1, 14, 14, 32},DataType.FLOAT32).getBuffer());
    outputs.put(6,TensorBufferFloat.createFixedSize(new int[]{1, 14, 14, 32},DataType.FLOAT32).getBuffer());
    outputs.put(7,TensorBufferFloat.createFixedSize(new int[]{1, 14, 14, 48},DataType.FLOAT32).getBuffer());

    return outputs;
  }


  HashMap<Integer,Object> getOutputs(){

    HashMap<Integer,Object> outputs=new HashMap<Integer,Object>();

    for(int i=0;i< interpreter.getOutputTensorCount();i++){
      Tensor t=interpreter.getOutputTensor(i);
      outputs.put(i,TensorBufferFloat.createFixedSize(t.shape(),DataType.FLOAT32).getBuffer());
    }
    return outputs;
  }


  private Map<String,TensorBuffer> allocateOutputsAndRun(Object[] inputs, int category){

    if(category==1){

      HashMap<Integer,Object> outputs=new HashMap<Integer,Object>();
      Map<String,TensorBuffer> returnValues=new HashMap<String,TensorBuffer>();
      TensorBuffer shortOffsets=TensorBufferFloat.createFixedSize(new int[]{1, 65, 17, 34},DataType.FLOAT32);
      TensorBuffer segments=TensorBufferFloat.createFixedSize(new int[]{1, 65, 17,  1},DataType.FLOAT32);
      TensorBuffer partHeatMaps=TensorBufferFloat.createFixedSize(new int[]{1, 65, 17, 24},DataType.FLOAT32);
      TensorBuffer longOffsets=TensorBufferFloat.createFixedSize(new int[]{1, 65, 17, 34},DataType.FLOAT32);
      TensorBuffer heatmaps=TensorBufferFloat.createFixedSize(new int[]{1, 65, 17, 17},DataType.FLOAT32);
      TensorBuffer displacement_fwd=TensorBufferFloat.createFixedSize(new int[]{1, 65, 17, 32},DataType.FLOAT32);
      TensorBuffer displacement_bwd=TensorBufferFloat.createFixedSize(new int[]{1, 65, 17, 32},DataType.FLOAT32);
      TensorBuffer partOffsets=TensorBufferFloat.createFixedSize(new int[]{1, 65, 17, 48},DataType.FLOAT32);

      outputs.put(0,shortOffsets.getBuffer());
      outputs.put(1,segments.getBuffer());
      outputs.put(2,partHeatMaps.getBuffer());
      outputs.put(3,longOffsets.getBuffer());
      outputs.put(4,heatmaps.getBuffer());
      outputs.put(5,displacement_fwd.getBuffer());
      outputs.put(6,displacement_bwd.getBuffer());
      outputs.put(7,partOffsets.getBuffer());

      interpreter.runForMultipleInputsOutputs(inputs,outputs);

      returnValues.put("shortOffsets",shortOffsets);
      returnValues.put("segments",segments);
      returnValues.put("partHeatMaps",partHeatMaps);
      returnValues.put("longOffsets",longOffsets);
      returnValues.put("heatmaps",heatmaps);
      returnValues.put("displacement_fwd",displacement_fwd);
      returnValues.put("displacement_bwd",displacement_bwd);
      returnValues.put("partOffsets",partOffsets);

      return returnValues;
    }

    if(category==2){

      HashMap<Integer,Object> outputs=new HashMap<Integer,Object>();
      Map<String,TensorBuffer> returnValues=new HashMap<String,TensorBuffer>();
      TensorBuffer shortOffsets=TensorBufferFloat.createFixedSize(new int[]{1, 63, 47, 34},DataType.FLOAT32);
      TensorBuffer segments=TensorBufferFloat.createFixedSize(new int[]{1, 63, 47,  1},DataType.FLOAT32);
      TensorBuffer partHeatMaps=TensorBufferFloat.createFixedSize(new int[]{1, 63, 47, 24},DataType.FLOAT32);
      TensorBuffer longOffsets=TensorBufferFloat.createFixedSize(new int[]{1, 63, 47, 34},DataType.FLOAT32);
      TensorBuffer heatmaps=TensorBufferFloat.createFixedSize(new int[]{1, 63, 47, 17},DataType.FLOAT32);
      TensorBuffer displacement_fwd=TensorBufferFloat.createFixedSize(new int[]{1, 63, 47, 32},DataType.FLOAT32);
      TensorBuffer displacement_bwd=TensorBufferFloat.createFixedSize(new int[]{1, 63, 47, 32},DataType.FLOAT32);
      TensorBuffer partOffsets=TensorBufferFloat.createFixedSize(new int[]{1, 63, 47, 48},DataType.FLOAT32);

      outputs.put(0,shortOffsets.getBuffer());
      outputs.put(1,segments.getBuffer());
      outputs.put(2,partHeatMaps.getBuffer());
      outputs.put(3,longOffsets.getBuffer());
      outputs.put(4,heatmaps.getBuffer());
      outputs.put(5,displacement_fwd.getBuffer());
      outputs.put(6,displacement_bwd.getBuffer());
      outputs.put(7,partOffsets.getBuffer());

      interpreter.runForMultipleInputsOutputs(inputs,outputs);

      returnValues.put("shortOffsets",shortOffsets);
      returnValues.put("segments",segments);
      returnValues.put("partHeatMaps",partHeatMaps);
      returnValues.put("longOffsets",longOffsets);
      returnValues.put("heatmaps",heatmaps);
      returnValues.put("displacement_fwd",displacement_fwd);
      returnValues.put("displacement_bwd",displacement_bwd);
      returnValues.put("partOffsets",partOffsets);

      return returnValues;
    }

    if(category==3){

      HashMap<Integer,Object> outputs=new HashMap<Integer,Object>();
      Map<String,TensorBuffer> returnValues=new HashMap<String,TensorBuffer>();
      TensorBuffer shortOffsets=TensorBufferFloat.createFixedSize(new int[]{1, 47, 63, 34},DataType.FLOAT32);
      TensorBuffer segments=TensorBufferFloat.createFixedSize(new int[]{1, 47, 63,  1},DataType.FLOAT32);
      TensorBuffer partHeatMaps=TensorBufferFloat.createFixedSize(new int[]{1, 47, 63, 24},DataType.FLOAT32);
      TensorBuffer longOffsets=TensorBufferFloat.createFixedSize(new int[]{1, 47, 63, 34},DataType.FLOAT32);
      TensorBuffer heatmaps=TensorBufferFloat.createFixedSize(new int[]{1, 47, 63, 17},DataType.FLOAT32);
      TensorBuffer displacement_fwd=TensorBufferFloat.createFixedSize(new int[]{1, 47, 63, 32},DataType.FLOAT32);
      TensorBuffer displacement_bwd=TensorBufferFloat.createFixedSize(new int[]{1, 47, 63, 32},DataType.FLOAT32);
      TensorBuffer partOffsets=TensorBufferFloat.createFixedSize(new int[]{1, 47, 63, 48},DataType.FLOAT32);

      outputs.put(0,shortOffsets.getBuffer());
      outputs.put(1,segments.getBuffer());
      outputs.put(2,partHeatMaps.getBuffer());
      outputs.put(3,longOffsets.getBuffer());
      outputs.put(4,heatmaps.getBuffer());
      outputs.put(5,displacement_fwd.getBuffer());
      outputs.put(6,displacement_bwd.getBuffer());
      outputs.put(7,partOffsets.getBuffer());

      interpreter.runForMultipleInputsOutputs(inputs,outputs);

      returnValues.put("shortOffsets",shortOffsets);
      returnValues.put("segments",segments);
      returnValues.put("partHeatMaps",partHeatMaps);
      returnValues.put("longOffsets",longOffsets);
      returnValues.put("heatmaps",heatmaps);
      returnValues.put("displacement_fwd",displacement_fwd);
      returnValues.put("displacement_bwd",displacement_bwd);
      returnValues.put("partOffsets",partOffsets);

      return returnValues;
    }

      HashMap<Integer,Object> outputs=new HashMap<Integer,Object>();
      Map<String,TensorBuffer> returnValues=new HashMap<String,TensorBuffer>();
      TensorBuffer shortOffsets=TensorBufferFloat.createFixedSize(new int[]{1, 17, 65, 34},DataType.FLOAT32);
      TensorBuffer segments=TensorBufferFloat.createFixedSize(new int[]{1, 17, 65,  1},DataType.FLOAT32);
      TensorBuffer partHeatMaps=TensorBufferFloat.createFixedSize(new int[]{1, 17, 65, 24},DataType.FLOAT32);
      TensorBuffer longOffsets=TensorBufferFloat.createFixedSize(new int[]{1, 17, 65, 34},DataType.FLOAT32);
      TensorBuffer heatmaps=TensorBufferFloat.createFixedSize(new int[]{1, 17, 65, 17},DataType.FLOAT32);
      TensorBuffer displacement_fwd=TensorBufferFloat.createFixedSize(new int[]{1, 17, 65, 32},DataType.FLOAT32);
      TensorBuffer displacement_bwd=TensorBufferFloat.createFixedSize(new int[]{1, 17, 65, 32},DataType.FLOAT32);
      TensorBuffer partOffsets=TensorBufferFloat.createFixedSize(new int[]{1, 17, 65, 48},DataType.FLOAT32);

      outputs.put(0,shortOffsets.getBuffer());
      outputs.put(1,segments.getBuffer());
      outputs.put(2,partHeatMaps.getBuffer());
      outputs.put(3,longOffsets.getBuffer());
      outputs.put(4,heatmaps.getBuffer());
      outputs.put(5,displacement_fwd.getBuffer());
      outputs.put(6,displacement_bwd.getBuffer());
      outputs.put(7,partOffsets.getBuffer());

      interpreter.runForMultipleInputsOutputs(inputs,outputs);

      returnValues.put("shortOffsets",shortOffsets);
      returnValues.put("segments",segments);
      returnValues.put("partHeatMaps",partHeatMaps);
      returnValues.put("longOffsets",longOffsets);
      returnValues.put("heatmaps",heatmaps);
      returnValues.put("displacement_fwd",displacement_fwd);
      returnValues.put("displacement_bwd",displacement_bwd);
      returnValues.put("partOffsets",partOffsets);

      return returnValues;

  }

  static int[] getTargetHeightAndWidth(int category){
    if (category==1){
      return new int[]{1025,257};
    }
    if(category==2){
      return new int[]{993,737};
    }
    if(category==3){
      return new int[]{737,993};
    }

    return new int[] {257,1025};

  }

  private String toInputResolutionHeightAndWidth(int image_height,int image_width,double internal_resolution,double out_stride, byte[] im) {
    System.out.println("input height: "+image_height+", input width: "+image_width);
    int category=getCategory((double)image_width/image_height);
    System.out.println("CATEGORY: "+category);

    int[] targetHeightAndWidth= getTargetHeightAndWidth(category);
    int target_height=targetHeightAndWidth[0];
    int target_width=targetHeightAndWidth[1];
    System.out.println("target_height: "+target_height+", target_width: "+target_width);

    //int target_height=  Utils.toValidInputResolution((int)(internal_resolution*image_height),out_stride); //height
    //int target_width= Utils.toValidInputResolution((int)(internal_resolution*image_width),out_stride); //width
    long startTime = System.nanoTime();
    int[] paddingDims= Utils.calculatePaddingDims(target_height,target_width,image_height,image_width);
    Pad3DOp paddingOp=new Pad3DOp(paddingDims[2],paddingDims[3],paddingDims[0],paddingDims[1]);
    ResizeOp resizeOp=new ResizeOp(target_height,target_width,ResizeOp.ResizeMethod.BILINEAR);
    NormalizeOp normalize=new NormalizeOp(0,1);

    Bitmap bmp= BitmapFactory.decodeByteArray(im,0,im.length);
    TensorImage image=new TensorImage(DataType.FLOAT32);
    image.load(bmp);
    ImageProcessor imageProcessor =new ImageProcessor.Builder().add(paddingOp).add(resizeOp).add(normalize).build();
    image=imageProcessor.process(image);
    int runtimeImageHeight=image.getHeight();
    int runtimeImageWidth=image.getWidth();

    System.out.println("runtimeImageHeight: "+runtimeImageHeight+", runtimeImageWidth: "+runtimeImageWidth);

    int[] imDims = new int[4];
    imDims[0]=1;
    imDims[1] = target_height;
    imDims[2] = target_width;
    imDims[3] = 3;

  int inputIndex = interpreter.getInputIndex("sub_2");

  interpreter.resizeInput(inputIndex, imDims);

    Object[] inputs=new Object[1];
    inputs[0]=image.getBuffer();

    Map<String,TensorBuffer> outputs= allocateOutputsAndRun(inputs,category);

    System.out.println("OUT0 BYTES: "+ Objects.requireNonNull(outputs.get("shortOffsets")).getFlatSize());
    System.out.println("OUT1 BYTES: "+ Objects.requireNonNull(outputs.get("segments")).getFlatSize());
    System.out.println("OUT2 BYTES: "+ Objects.requireNonNull(outputs.get("partHeatMaps")).getFlatSize());
    System.out.println("OUT3 BYTES: "+ Objects.requireNonNull(outputs.get("longOffsets")).getFlatSize());
    System.out.println("OUT4 BYTES: "+ Objects.requireNonNull(outputs.get("heatmaps")).getFlatSize());
    System.out.println("OUT5 BYTES: "+ Objects.requireNonNull(outputs.get("displacement_fwd")).getFlatSize());
    System.out.println("OUT6 BYTES: "+ Objects.requireNonNull(outputs.get("displacement_bwd")).getFlatSize());
    System.out.println("OUT7 BYTES: "+ Objects.requireNonNull(outputs.get("partOffsets")).getFlatSize());

    TensorBuffer heatmaps=outputs.get("heatmaps");
    System.out.println("FLOAT VALUE: "+heatmaps.getFloatValue(195));
    System.out.println("TOTAL FLOAT VALUE: "+heatmaps.getFlatSize());

    int paddedHeight=resizeOp.getOutputImageHeight(image_height,image_width);
    int paddedWidth=resizeOp.getOutputImageWidth(image_height,image_width);

    TensorProcessor processOut=new TensorProcessor.Builder().add(new SqueezeFirstDimOp()).add(new ResizeTensorBuffer(paddedHeight,paddedWidth)).add(new
            Crop3DTensorBuffer(paddingDims[2],paddingDims[3],paddingDims[0],paddingDims[1])
            ).add(new ResizeTensorBuffer(image_height,image_width)).build();

    TensorBuffer out_va=processOut.process(heatmaps);
    long endTime = System.nanoTime();
    long timeElapsed = endTime - startTime;
    System.out.println("Execution time in nanoseconds  : " + timeElapsed);
    System.out.println("Execution time in milliseconds : " +
            timeElapsed / 1000000);
    System.out.println("FLOAT VALUE AFTR CONV: "+out_va.getShape());

    return "OK";

  }

  @Override
  public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {

    this.activity=binding.getActivity();

  }

  @Override
  public void onDetachedFromActivityForConfigChanges() {

  }

  @Override
  public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {
    this.activity=binding.getActivity();
  }

  @Override
  public void onDetachedFromActivity() {

  }
//
//  private static Bitmap getOutputImage(ByteBuffer output, int im_width,int im_height){
//    output.rewind();
//
//    int outputWidth = im_width;
//    int outputHeight = im_height;
//    Bitmap bitmap = Bitmap.createBitmap(outputWidth, outputHeight, Bitmap.Config.ARGB_8888);
//    int [] pixels = new int[outputWidth * outputHeight];
//    for (int i = 0; i < outputWidth * outputHeight; i++) {
//      int a = 0xFF;
//
//      float r = output.getFloat() * 255.0f;
//      float g = output.getFloat() * 255.0f;
//      float b = output.getFloat() * 255.0f;
//
//      pixels[i] = a << 24 | ((int) r << 16) | ((int) g << 8) | (int) b;
//    }
//    bitmap.setPixels(pixels, 0, outputWidth, 0, 0, outputWidth, outputHeight);
//    return bitmap;
//  }
}
