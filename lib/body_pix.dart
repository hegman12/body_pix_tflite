import 'dart:async';
import 'dart:typed_data';

import 'package:flutter/services.dart';

//https://stackoverflow.com/questions/51315442/use-ui-decodeimagefromlist-to-display-an-image-created-from-a-list-of-bytes/51316489#51316489

class BMP332Header {
  int width; // NOTE: width must be multiple of 4 as no account is made for bitmap padding
  int height;
  int channel;

  Uint8List _bmp;
  int _totalHeaderSize;

  BMP332Header({this.width, this.height, this.channel})
      : assert(width & 3 == 0) {
    int baseHeaderSize = 54;
    _totalHeaderSize = baseHeaderSize + 1024; // base + color map
    int fileLength =
        _totalHeaderSize + width * height * channel; // header + bitmap
    _bmp = new Uint8List(fileLength);
    ByteData bd = _bmp.buffer.asByteData();
    bd.setUint8(0, 0x42);
    bd.setUint8(1, 0x4d);
    bd.setUint32(2, fileLength, Endian.little); // file length
    bd.setUint32(10, _totalHeaderSize, Endian.little); // start of the bitmap
    bd.setUint32(14, 40, Endian.little); // info header size
    bd.setUint32(18, width, Endian.little);
    bd.setUint32(22, height, Endian.little);
    bd.setUint16(26, 1, Endian.little); // planes
    bd.setUint32(28, 8, Endian.little); // bpp
    bd.setUint32(30, 0, Endian.little); // compression
    bd.setUint32(34, width * height * channel, Endian.little); // bitmap size
    // leave everything else as zero

    // there are 256 possible variations of pixel
    // build the indexed color map that maps from packed byte to RGBA32
    // better still, create a lookup table see: http://unwind.se/bgr233/
    for (int rgb = 0; rgb < 256; rgb++) {
      int offset = baseHeaderSize + rgb * 4;

      int red = rgb & 0xe0;
      int green = rgb << 3 & 0xe0;
      int blue = rgb & 6 & 0xc0;

      bd.setUint8(offset + 3, 255); // A
      bd.setUint8(offset + 2, red); // R
      bd.setUint8(offset + 1, green); // G
      bd.setUint8(offset, blue); // B
    }
  }

  /// Insert the provided bitmap after the header and return the whole BMP
  Uint8List appendBitmap(Uint8List bitmap) {
    int size = width * height * channel;
    print("size: " + size.toString());
    assert(bitmap.length == size);
    _bmp.setRange(_totalHeaderSize, _totalHeaderSize + size, bitmap);
    return _bmp;
  }
}

class BodyPix {
  static const MethodChannel _channel = const MethodChannel('body_pix');

  static Future<List<Uint8List>> segmentBodyParts(Uint8List image,
      double resolution, double stride, double threshold, int rotate) async {

    var out = await _channel.invokeMethod('getSegmentation', {
      "image": image,
      "resolution": resolution,
      "stride": stride,
      "threshold": threshold,
      "rotate": rotate
    });
    Uint8List parts = out["parts"];
    Uint8List segmentation = out["segmentation"];
    //Uint8List origImage = out["image"];
    final int newHeight = out["new_height"] as int;
    final int newWidth = out["new_width"] as int;
    //Sometimes we receive more than actual bytes. So, chop off extra bytes otherwise we get image codec error
    int numBytesParts = parts.lengthInBytes - (newHeight * newWidth);
    int numBytesSegmentation =
        segmentation.lengthInBytes - (newHeight * newWidth);
    //int numBytesOrigImage=origImage.lengthInBytes-(newHeight*newWidth*4);
    parts = parts.sublist(numBytesParts, parts.length);
    segmentation =
        segmentation.sublist(numBytesSegmentation, segmentation.length);
    //origImage=origImage.sublist(numBytesOrigImage,origImage.length);
    BMP332Header partsHeader =
        BMP332Header(height: newHeight, width: newWidth, channel: 1);
    BMP332Header segHeader =
        BMP332Header(height: newHeight, width: newWidth, channel: 1);
    //BMP332Header origImageHeader=BMP332Header(height: newHeight,width: newWidth, channel: 4);
    return [
      //origImageHeader.appendBitmap(origImage),
      image,
      partsHeader.appendBitmap(parts),
      segHeader.appendBitmap(segmentation)
    ];
  }
}
