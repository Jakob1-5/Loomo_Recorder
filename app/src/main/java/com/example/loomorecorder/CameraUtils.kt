package com.example.loomorecorder

import android.graphics.Bitmap
import com.example.loomorecorder.loomoUtils.LoomoRealSense.FISHEYE_WIDTH
import com.example.loomorecorder.loomoUtils.LoomoRealSense.FISHEYE_HEIGHT
import com.example.loomorecorder.loomoUtils.LoomoRealSense.COLOR_WIDTH
import com.example.loomorecorder.loomoUtils.LoomoRealSense.COLOR_HEIGHT
import com.example.loomorecorder.loomoUtils.LoomoRealSense.DEPTH_WIDTH
import com.example.loomorecorder.loomoUtils.LoomoRealSense.DEPTH_HEIGHT
import com.segway.robot.sdk.vision.frame.Frame
import com.segway.robot.sdk.vision.frame.FrameInfo
import com.segway.robot.sdk.vision.stream.StreamType
import org.opencv.core.CvType
import org.opencv.core.CvType.CV_8UC1
import org.opencv.core.Mat

object CameraUtils {
    // Using a custom data class instead of the Pair-type/template for readability
    data class FrameData(val frame: Bitmap, val timeStamp: Long)
    data class FrameData2(val frame: Mat, val info: FrameInfo)

    const val bufferBaseSize = 30

    val fishEyeFrameBuffer = RingBuffer<FrameData>(bufferBaseSize, true)
    val colorFrameBuffer = RingBuffer<FrameData>(bufferBaseSize, true)
    val depthFrameBuffer = RingBuffer<FrameData>(bufferBaseSize, true)
    var newFishEyeFrames = 0
    var newColorFrames = 0
    var newDepthFrames = 0

    val fishEyeBitmap = Bitmap.createBitmap(FISHEYE_WIDTH, FISHEYE_HEIGHT, Bitmap.Config.ALPHA_8)
    val colorBitmap = Bitmap.createBitmap(COLOR_WIDTH, COLOR_HEIGHT, Bitmap.Config.ARGB_8888)
    val depthBitmap = Bitmap.createBitmap(DEPTH_WIDTH, DEPTH_HEIGHT, Bitmap.Config.RGB_565)

    fun onNewFrame(streamType: Int, frame: Frame) {
//        val tic = System.currentTimeMillis()
        when (streamType) {
            StreamType.FISH_EYE -> {
                fishEyeBitmap.copyPixelsFromBuffer(frame.byteBuffer)
                fishEyeFrameBuffer.enqueue(
                    FrameData(
                        fishEyeBitmap,
                        frame.info.imuTimeStamp
                    )
                )
//                fishEyeFrameBuffer.enqueue(
//                    FrameData2(
//                        frame.byteBuffer.toMat(
//                            FISHEYE_WIDTH, FISHEYE_HEIGHT, CV_8UC1
//                        ),
//                        frame.info
//                    )
//                )
                ++newFishEyeFrames
            }
            StreamType.COLOR -> {
                colorBitmap.copyPixelsFromBuffer(frame.byteBuffer)
                colorFrameBuffer.enqueue(
                    FrameData(
                        colorBitmap,
                        frame.info.imuTimeStamp
                    )
                )
                ++newColorFrames
            }
            StreamType.DEPTH -> {
                depthBitmap.copyPixelsFromBuffer(frame.byteBuffer)
                depthFrameBuffer.enqueue(
                    FrameData(
                        depthBitmap,
                        frame.info.imuTimeStamp
                    )
                )
                ++newDepthFrames
            }
            else -> {
                throw IllegalStreamTypeException("Stream type not recognized in onNewFrame")
            }
        }
//        val toc = System.currentTimeMillis()
//        Log.d("onNewFrame()", "${streamTypeMap[streamType]} frame receive time: ${toc - tic}ms")
    }

}

class IllegalStreamTypeException(msg: String) : RuntimeException(msg)