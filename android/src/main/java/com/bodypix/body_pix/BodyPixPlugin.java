package com.bodypix.body_pix;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import androidx.annotation.NonNull;

import com.bodypix.body_pix.utils.Converter;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.flex.FlexDelegate;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.image.ops.Rot90Op;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;
import org.tensorflow.lite.support.tensorbuffer.TensorBufferFloat;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.util.HashMap;

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
  private Context context;
  private Interpreter interpreter;
  private FlexDelegate delegate;
  private int new_height;
  private int new_width;

  public BodyPixPlugin(){

  }

  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {

    channel = new MethodChannel(flutterPluginBinding.getFlutterEngine().getDartExecutor(), "body_pix");
    context=flutterPluginBinding.getApplicationContext();
    channel.setMethodCallHandler(this);
  }

  public static void registerWith(Registrar registrar) {
    final MethodChannel channel = new MethodChannel(registrar.messenger(), "body_pix");
    channel.setMethodCallHandler(new BodyPixPlugin());
  }

  @Override
  public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
    if (call.method.equals("getSegmentation")) {

      byte[] im= (byte[])call.argument("image");
      double threshold = (double)call.argument("threshold");
      double resolution = (double)call.argument("resolution");
      double stride = (double)call.argument("stride");
      int rotate = (int)call.argument("rotate");

      result.success(runSegmentation(im,(float)resolution,(float)stride,rotate, (float)threshold));
    } else {
      result.notImplemented();
    }
  }


  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    delegate.close();
    interpreter.close();
    channel.setMethodCallHandler(null);
  }

  private boolean checkIfResizeRequired(int in_height,int in_width){
    if(in_height>1024 || in_width >1024){
      return true;
    }
    return false;
  }

  private static int adjustWidthForBMP322(int new_width){
    int offset=(new_width & 3);
    int[] adjustment=new int[]{0,3,2,1};
    return adjustment[offset];
  }

  private ByteBuffer preProcessInput(Bitmap bmp, int rotate){

    //TODO: Simplify the logic, extract calculation of height and width to new static method

    ImageProcessor inputProcessor=null;
    TensorImage image=new TensorImage();
    int in_height=bmp.getHeight();
    int in_width=bmp.getWidth();

    if(checkIfResizeRequired(in_height,in_width)){

      //Determine the target height and width based on source aspect ratio
      float aspect=(float)in_width/in_height;

      if(aspect>1){
        //width is bigger than height
        this.new_width=1024;
        this.new_height=(int)((1/aspect)*this.new_width);
      }else{
        //height is bigger
        this.new_height=1024;
        this.new_width=(int)(aspect*this.new_height);
        this.new_width+=adjustWidthForBMP322(this.new_width);
      }

      if(rotate!=0){
        Rot90Op rotateOp=new Rot90Op(rotate);
        ResizeOp resizeOp = new ResizeOp(this.new_height,this.new_width, ResizeOp.ResizeMethod.BILINEAR);
        inputProcessor=new ImageProcessor.Builder().add(resizeOp).add(rotateOp).build();
      }else{
        ResizeOp resizeOp = new ResizeOp(this.new_height,this.new_width, ResizeOp.ResizeMethod.BILINEAR);
        inputProcessor = new ImageProcessor.Builder().add(resizeOp).build();
      }
      image.load(bmp);
      image=inputProcessor.process(image);

    }else{
      this.new_height=in_height;
      this.new_width=in_width;
      this.new_width+=adjustWidthForBMP322(this.new_width);

      if(rotate != 0){
        Rot90Op rotateOp=new Rot90Op(rotate);
        ResizeOp resizeOp = new ResizeOp(this.new_height,this.new_width, ResizeOp.ResizeMethod.BILINEAR);
        if(this.new_width!=in_width){
          inputProcessor=new ImageProcessor.Builder().add(resizeOp).add(rotateOp).build();
        }else{
          inputProcessor=new ImageProcessor.Builder().add(rotateOp).build();
        }
        image.load(bmp);
        image=inputProcessor.process(image);
      }else{
        image.load(bmp);
        if(this.new_width!=in_width){
          ResizeOp resizeOp = new ResizeOp(this.new_height,this.new_width, ResizeOp.ResizeMethod.BILINEAR);
          inputProcessor=new ImageProcessor.Builder().add(resizeOp).build();
          image=inputProcessor.process(image);
        }
      }
    }
    //Unnecessary work
    return Converter.bitmapToByteBuffer(image.getBitmap(),DataType.FLOAT32,3);
  }

  private HashMap<String,Object> runSegmentation(byte[] uint8Image, float internal_resolution,float out_stride,int rotate, float threshold ){
    Bitmap bmp=BitmapFactory.decodeByteArray(uint8Image,0,uint8Image.length);
    HashMap<String,Object> inferenceResult = runInference(bmp,internal_resolution,out_stride,rotate,threshold);
    //int new_height=(int)inferenceResult.get("new_height");
    //int new_width=(int)inferenceResult.get("new_width");
      // take care of rotation here
    //Bitmap rescaltedImage=Bitmap.createScaledBitmap(bmp,new_width,new_height,true);
    inferenceResult.put("image",uint8Image);
    return inferenceResult;
  }

  private HashMap<String,Object> runInference(Bitmap uint8InImage, float internal_resolution,float out_stride,int rotate, float threshold ) {

    ByteBuffer segmentation=null;
    ByteBuffer partSegmentation=null;
    //Need to change the model to accept uint8 input image. Its unnecessary work to convert uint8 to float
    ByteBuffer float32ByteBufferImage=null;
    byte[] partSegmentationOut=null;
    byte[] segmentationOut=null;

    try {

      MappedByteBuffer mb=FileUtil.loadMappedFile(context,"flutter_assets/assets/model_tlite.tflite");
      Interpreter.Options options= new Interpreter.Options();
      delegate=new FlexDelegate();
      options.addDelegate(delegate);
      options.setNumThreads(4);
      interpreter=new Interpreter(mb,options);
      float32ByteBufferImage=preProcessInput(uint8InImage,rotate);

      TensorBuffer imageSize= TensorBufferFloat.createFixedSize(new int[]{2},DataType.FLOAT32);
      imageSize.loadArray(new float[]{(float) this.new_height,(float) this.new_width});
      TensorBuffer config=TensorBufferFloat.createFixedSize(new int[]{3},DataType.FLOAT32);
      config.loadArray(new float[]{internal_resolution, out_stride, threshold});

      int[] imDims = new int[3];
      imDims[0]=this.new_height;
      imDims[1] =this.new_width;
      imDims[2] = 3;

      int inputIndex = interpreter.getInputIndex("image");
      interpreter.resizeInput(inputIndex, imDims);

      Object[] inputs=new Object[3];
      inputs[1]=float32ByteBufferImage;
      inputs[0]=config.getBuffer();
      inputs[2]=imageSize.getBuffer();
      HashMap<Integer,Object> outputs=new HashMap<Integer,Object>();

      segmentation = ByteBuffer.allocateDirect(this.new_height*this.new_width*DataType.UINT8.byteSize());
      segmentation.order(ByteOrder.nativeOrder());
      partSegmentation = ByteBuffer.allocateDirect(this.new_height*this.new_width*DataType.UINT8.byteSize());
      partSegmentation.order(ByteOrder.nativeOrder());

      outputs.put(0,partSegmentation);
      outputs.put(1,segmentation);

      interpreter.runForMultipleInputsOutputs(inputs,outputs);

      int flatSize=this.new_height*this.new_width;
      segmentation.rewind();
      partSegmentation.rewind();
      //Can improve this withougt allocating all those memory
     partSegmentationOut=new byte[partSegmentation.remaining()];
     segmentationOut=new byte[segmentation.remaining()];
        int flatIndex=0;
        for(int h=this.new_height-1;h>=0;h--){
          for(int w=0;w<this.new_width;w++){
            final byte mask=segmentation.get(h*this.new_width+w);
            if(mask==0){
              //Its background pixel. No parts should be identified here. 255 stands for no part
              partSegmentationOut[flatIndex]=(byte)255;
              segmentationOut[flatIndex]=(byte)255;
              flatIndex++;
            }else{
              partSegmentationOut[flatIndex]=getColors(partSegmentation,h*this.new_width+w);
              segmentationOut[flatIndex]=mask;
              flatIndex++;
            }
          }
        }

    }catch (IOException e){
      System.out.println(e);
    }finally {
      interpreter.close();
    }

    HashMap<String,Object> out=new HashMap<>();
    out.put("segmentation",segmentationOut);
    out.put("parts",partSegmentationOut);
    out.put("new_height",this.new_height);
    out.put("new_width",this.new_width);

    return out;
  }

  private static byte getColors(ByteBuffer partIds,int index){

      final int id=partIds.get(index);
      byte color;

      switch (id){
          case 0:
              color=(byte)0;
              break;
          case 1:
              color=(byte)10;
              break;
          case 2:
              color=(byte)20;
              break;
          case 3:
              color=(byte)30;
              break;
          case 4:
              color=(byte)40;
              break;
          case 5:
              color=(byte)50;
              break;
          case 6:
              color=(byte)60;
              break;
          case 7:
              color=(byte)70;
              break;
          case 8:
              color=(byte)80;
              break;
          case 9:
              color=(byte)90;
              break;
          case 10:
              color=(byte)100;
              break;
          case 11:
              color=(byte)110;
              break;
          case 12:
              color=(byte)120;
              break;
          case 13:
              color=(byte)130;
              break;
          case 14:
              color=(byte)140;
              break;
          case 15:
              color=(byte)150;
              break;
          case 16:
              color=(byte)160;
              break;
          case 17:
              color=(byte)170;
              break;
          case 18:
              color=(byte)180;
              break;
          case 19:
              color=(byte)190;
              break;
          case 20:
              color=(byte)200;
              break;
          case 21:
              color=(byte)210;
              break;
          case 22:
              color=(byte)220;
              break;
          case 23:
              color=(byte)230;
              break;
          case 24:
              color=(byte)240;
              break;
          default:
              color=(byte)255;
              break;
      }

    return color;

  }

  private static byte[] reverse(byte[] array) {
    if (array == null) {
      return null;
    }
    int i = 0;
    int j = array.length - 1;
    byte tmp;
    while (j > i) {
      tmp = array[j];
      array[j] = array[i];
      array[i] = tmp;
      j--;
      i++;
    }
    return array;
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

}
