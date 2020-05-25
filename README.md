# Loomo_Recorder

This is a very basic recording app. The data from various sensors are stored in a CSV-file with a timestamp for each row. The header of the CSV-file should make the content pretty self-explanatory. 

The app also records images from the RealSense's cameras. Sadly, I was not able to interface with any video I/O, so the raw video streams are stored in uncompressed binary files. The biggest draw-back is that the file-saving-process blocks the camera stream. Thus, the app will not record at full frame rate. The expected frame rates are as follows:
- 26-27 FPS for the depth camera
- 15-16 FPS for the fisheye camera
- 6-7 FPS for the color camera

## Accessing the recorded files

The recorded files are stored on the Loomo in `/storage/sdcard0/Android/data/com.example.loomoRecorder/files/Recordings`
Each recording is stored in a numbered sub-folder and consists of a .csv file and a binary file for each camera.

## Structure of the videos' binary files

The binary files are prefixed with 3 integers. They tell the width and height of the video frames. The third integer tells the number of bytes per pixel, and thus by extension which camera stream it is. The color camera is 4 bytes per pixel, the depth camera is 2 bytes (each pixel is 16 bits stored in little endian format), and the fisheye is 1 byte per pixel.
The pixels of a frame are stored in column-major order. Each video frame is prefixed with a timestamp (unsigned long int). The RealSense camera runs on a different clock, so this timestamp is different from the other sensors. However, the timestamp of the current camera frame is always written to the .csv file, which makes it possible to syncronize the video stream with the other sensors (the cameras' timestamp can be better thought of as a frame index).


![alt-text](https://github.com/Jakob1-5/Loomo_Recorder/blob/Save_raw_vid/myvid_structure.png "Structure of video binary files")
