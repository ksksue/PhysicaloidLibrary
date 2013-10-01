Physicaloid Library
==================

Android Library for communicating with physical-computing boards (e.g.Arduino, mbed)

![Android x Arduino](https://lh5.googleusercontent.com/-weC-lA-1rdw/UeaCzIrWR3I/AAAAAAAACno/u-ZapAmzkz8/s640/android_arduino.jpg)


Users does not need to download an Arduino sketch from a web site.
![Download sketch](https://lh3.googleusercontent.com/-Hh-vISkTL6w/UeaC5moml2I/AAAAAAAACn8/g7Dozio1QrE/s640/physicaloid_download.png)


You (developer) can include Arduino firmwares in your Android app and upload to Google Play.
![Upload to Google Play](https://lh6.googleusercontent.com/-lzDrLOSohUY/UeaC5p7Z0uI/AAAAAAAACoA/hcqRjLUe6JQ/s640/physicaloid_upload.png)


Features
-----------------
- Android Java library project
- USB-Serial communication
- upload a firmware to an Arduino
- support on Android 3.1 or higher (need USB Host API feature)
- **does not require ROOT**
- support USB-Serial protocols : CDC-ACM, FTDI, Silicon Labs CP210x
- support uploading firmware protocols : STK500, STK500V2
- open-source(Apache License 2.0)


Code example
-----------------

### Upload a firmware from Android to Arduino ###
```java
Physicaloid mPhysicaloid = new Physicaloid(this);
mPhysicaloid.upload(Boards.ARDUINO_UNO, "/sdcard/arduino/Blink.hex");
```


### Write serial data to Arduino ###
```java
Physicaloid mPhysicaloid = new Physicaloid(this);
if(mPhysicaloid.open()) {
    byte[] buf = "moemoe".getBytes();
    mPhysicaloid.write(buf, buf.length);
    mPhysicaloid.close()
}
```


### Read serial data from Arduino ###
```java
Physicaloid mPhysicaloid = new Physicaloid(this);
TextView TextView1 = (TextView) findViewById(R.id.TextView1);// Android TextView

if(mPhysicaloid.open()) {
    byte[] buf = new byte[256];

    mPhysicaloid.read(buf, buf.length);
    String str = new String(buf);
    TextView1.append(str);

    mPhysicaloid.close();
}
```

How to use
-----------------
1. File -> import and select a PhysicaloidLibrary directory.
2. Right click your project -> Properties -> Android -> click Library's "Add" button -> select PhysicaloidLibrary


Special Thanks
-----------------
This code has built in knowledge of avrdude.
Thanks to all avrdude coders.


License
-----------------
Physicaloid Library is released under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0).
