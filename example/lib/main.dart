import 'package:flutter/material.dart';
import 'dart:async';
import 'package:body_pix/body_pix.dart';

import 'dart:io';
import 'dart:typed_data';
import 'package:image_picker/image_picker.dart';


void main() {
  runApp(MyApp());
}

class MyApp extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Bodypix Demo',
      theme: ThemeData(
        primarySwatch: Colors.blue,
        visualDensity: VisualDensity.adaptivePlatformDensity,
      ),
      home: WelcomePage(),
    );
  }
}

class WelcomePage extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return Scaffold(body: ListView(children: [RaisedButton(child: Text("Demo"),onPressed: (){
      Navigator.push(context, MaterialPageRoute(builder: (context)=>MyHomePage(title: "Demo",)));

    },),Text("Welcome!"),],) );
  }
}

class MyHomePage extends StatefulWidget {
  MyHomePage({Key key, this.title}) : super(key: key);
  final String title;

  @override
  _MyHomePageState createState() => _MyHomePageState();
}

class _MyHomePageState extends State<MyHomePage> {
  String text;
  String _im;
  var _recognitions;
  double resolution = 0.75;
  double threshold = 0.3;
  double stride = 16.0;
  double rotation = 0;

  Future getImage() async {
    File f = await ImagePicker.pickImage(source: ImageSource.gallery);
    _recognitions = BodyPix.segmentBodyParts(
        f.readAsBytesSync(), resolution, stride, threshold, rotation.round());

    setState(() {
      _im = "a";
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(widget.title),
      ),
      body: ListView(children: [
        //resolution
        Row(
          children: [
            Text(
              "Resolution: $resolution",
              style: TextStyle(fontSize: 17),
            ),
            Expanded(
                child: Slider(
              value: resolution,
              onChanged: (v) => {
                setState(() {
                  resolution = double.parse(v.toStringAsPrecision(2));
                })
              },
              min: 0.25,
              max: 2.0,
              label: '$resolution',
            ))
          ],
        ),
        //stride
        Row(
          children: [
            Text(
              "Stride: $stride",
              style: TextStyle(fontSize: 17),
            ),
            Expanded(
                child: Slider(
              value: stride,
              onChanged: (v) => {
                setState(() {
                  stride = v;
                })
              },
              min: 8.0,
              max: 32.0,
              label: '$stride',
              divisions: 3,
            ))
          ],
        ),
        //threshold
        Row(
          children: [
            Text(
              "Rotation: $rotation",
              style: TextStyle(fontSize: 17),
            ),
            Expanded(
                child: Slider(
              value: rotation,
              onChanged: (v) => {
                setState(() {
                  rotation = v;
                })
              },
              min: 0.0,
              max: 360.0,
              label: '$rotation',
              divisions: 4,
            ))
          ],
        ),
        Row(
          children: [
            Text(
              "Threshold: $threshold",
              style: TextStyle(fontSize: 17),
            ),
            Expanded(
                child: Slider(
              value: threshold,
              onChanged: (v) => {
                setState(() {
                  threshold = double.parse(v.toStringAsPrecision(2));
                })
              },
              min: 0.0,
              max: 1.0,
              label: '$threshold',
            ))
          ],
        ),
        _im == null
            ? Center(
                child: Text("Your selected image will appear here. Tap floating button to select Image",style: TextStyle(fontSize: 17),softWrap: true,),
              )
            : FutureBuilder<List<Uint8List>>(
                future: _recognitions,
                builder: (context, snapshot) {
                  if (snapshot.hasData) {
                    return Image.memory(snapshot.data[0]);
                  }
                  if (snapshot.hasError) {
                    return Text("Error getting original photo",style: TextStyle(fontSize: 17),);
                  }
                  return CircularProgressIndicator();
                },
              ),
        _im == null
            ? Center(
                child: Text("Part segmented image will appear here once you select image",style: TextStyle(fontSize: 17),),
              )
            : FutureBuilder<List<Uint8List>>(
                future: _recognitions,
                builder: (context, snapshot) {
                  if (snapshot.hasData) {
                    return Image.memory(snapshot.data[1]);
                  }
                  if (snapshot.hasError) {
                    return Text("Error getting parts");
                  }
                  return CircularProgressIndicator();
                },
              ),
        _im == null
            ? Center(
                child: Text("General segmentation will appear here after you select image",style: TextStyle(fontSize: 17),),
              )
            : FutureBuilder<List<Uint8List>>(
                future: _recognitions,
                builder: (context, snapshot) {
                  if (snapshot.hasData) {
                    return Image.memory(snapshot.data[2]);
                  }
                  if (snapshot.hasError) {
                    return Text("Error getting segmentation",style: TextStyle(fontSize: 17),);
                  }
                  return CircularProgressIndicator();
                },
              )
      ]),
      floatingActionButton: FloatingActionButton(
        onPressed: getImage,
        tooltip: 'Select Image',
        child: Icon(Icons.image),
      ),
    );
  }
}
