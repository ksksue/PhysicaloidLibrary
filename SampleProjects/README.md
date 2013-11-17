Physicaloid Library Sample Projects
==================
### PhysicaloidTest
- test project for me

### tutorial1 : how to use open/close/read/write
![tutorial1](https://lh3.googleusercontent.com/-PTd2dxUkHgo/Uokn1dZclZI/AAAAAAAACtA/QyxrLyy5m_k/s288/tutorial1.png)
- add <uses-feature android:name="android.hardware.usb.host" /> to AndroidManifest.xml
- create a new instance
- open a device
- close the device
- read from the device to a buffer and get read size
- write a buffer to the device

### tutorial2 : how to use upload
![tutorial](https://lh4.googleusercontent.com/-Qz1D1ZB3iwk/Uokn1fMlLiI/AAAAAAAACtE/dRvmMkhCcjg/s288/tutorial2.png)
- set board type and assets file.
- copy .hex file to porject_dir/assets directory.

### tutorial3 : how to use read/upload callbacks
![tutorial](https://lh6.googleusercontent.com/-TDILS9kE-xY/Uokn1VxnjiI/AAAAAAAACs8/YJ023Vp4uPA/s288/tutorial3.png)
- add read callback
- clear read callback
- create upload callback
- set upload callback

### tutorial4 : how to change UART configures
![tutorial](https://lh4.googleusercontent.com/-FMpSrLDGlUM/Uokn2WwVNvI/AAAAAAAACtU/jByDQYYSXv0/s288/tutorial4.png)
- set only baudrate
- set uart configurations

### tutorial5 : how to discover when user attach USB and open automatically
![tutorial](https://lh3.googleusercontent.com/-da_8a1vgIX4/Uokn2Vz889I/AAAAAAAACtQ/EmxRSjsvb5k/s288/tutorial5.png)
- register intent filtered actions for device being attached or detached
- unregister the intent filtered actions
- get intent when a USB device attached
- get intent when a USB device detached
- add usb device attached intent
- add device filter for usb device being attached
- add launchMode singleTask not to run multiple apps
- create rex/xml/device_filter.xml
