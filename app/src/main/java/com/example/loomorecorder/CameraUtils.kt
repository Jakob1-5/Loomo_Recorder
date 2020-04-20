package com.example.loomorecorder

import android.graphics.Bitmap
import com.example.loomorecorder.loomoUtils.LoomoRealSense.FISHEYE_WIDTH
import com.example.loomorecorder.loomoUtils.LoomoRealSense.FISHEYE_HEIGHT
import com.example.loomorecorder.loomoUtils.LoomoRealSense.COLOR_WIDTH
import com.example.loomorecorder.loomoUtils.LoomoRealSense.COLOR_HEIGHT
import com.example.loomorecorder.loomoUtils.LoomoRealSense.DEPTH_WIDTH
import com.example.loomorecorder.loomoUtils.LoomoRealSense.DEPTH_HEIGHT
import com.segway.robot.sdk.vision.frame.Frame
import com.segway.robot.sdk.vision.stream.StreamType

object CameraUtils {
    // Using a custom data class instead of the Pair-type/template for readability
    data class FrameData(val frame: Bitmap, val timeStamp: Long)

    const val bufferBaseSize = 30

    val fishEyeFrameBuffer = RingBuffer<FrameData>(bufferBaseSize, true)
    val colorFrameBuffer = RingBuffer<FrameData>(bufferBaseSize, true)
    val depthFrameBuffer = RingBuffer<FrameData>(bufferBaseSize, true)

    val fishEyeBitmap = Bitmap.createBitmap(FISHEYE_WIDTH, FISHEYE_HEIGHT, Bitmap.Config.ALPHA_8)
    val colorBitmap = Bitmap.createBitmap(COLOR_WIDTH, COLOR_HEIGHT, Bitmap.Config.ARGB_8888)
    val depthBitmap = Bitmap.createBitmap(DEPTH_WIDTH, DEPTH_HEIGHT, Bitmap.Config.RGB_565)

    fun onNewFrame(streamType: Int, frame: Frame, pushToBuffer: Boolean) {
//        val tic = System.currentTimeMillis()
        when (streamType) {
            StreamType.FISH_EYE -> {
                fishEyeBitmap.copyPixelsFromBuffer(frame.byteBuffer)
                if(pushToBuffer) {
                    fishEyeFrameBuffer.enqueue(
                        FrameData(
                            fishEyeBitmap,
                            frame.info.imuTimeStamp
                        )
                    )
                }
            }
            StreamType.COLOR -> {
                colorBitmap.copyPixelsFromBuffer(frame.byteBuffer)
                if (pushToBuffer) {
                    colorFrameBuffer.enqueue(
                        FrameData(
                            colorBitmap,
                            frame.info.imuTimeStamp
                        )
                    )
                }
            }
            StreamType.DEPTH -> {
                depthBitmap.copyPixelsFromBuffer(frame.byteBuffer)
                if (pushToBuffer) {
                    depthFrameBuffer.enqueue(
                        FrameData(
                            depthBitmap,
                            frame.info.imuTimeStamp
                        )
                    )
                }
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