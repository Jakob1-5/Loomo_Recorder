package com.example.loomorecorder

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
import org.opencv.core.Mat

// Using a custom data class instead of the Pair-type/template for readability
data class FrameData(val frame: Mat, val info: FrameInfo)

var fishEyeFrameBuffer = RingBuffer<FrameData>(30, true)
var colorFrameBuffer = RingBuffer<FrameData>(30, true)
var depthFrameBuffer = RingBuffer<FrameData>(30, true)
var newFishEyeFrames = 0
var newColorFrames = 0
var newDepthFrames = 0


fun onNewFrame(streamType: Int, frame: Frame) {
//        val tic = System.currentTimeMillis()
    when (streamType) {
        StreamType.FISH_EYE -> {
            fishEyeFrameBuffer.enqueue(
                FrameData(
                    frame.byteBuffer.toMat(
                        FISHEYE_WIDTH, FISHEYE_HEIGHT,
                        CvType.CV_8UC1
                    ), frame.info
                )
            )
            ++newFishEyeFrames
        }
        StreamType.COLOR -> {
            colorFrameBuffer.enqueue(
                FrameData(
                    frame.byteBuffer.toMat(
                        COLOR_WIDTH, COLOR_HEIGHT,
                        CvType.CV_8UC4
                    ), frame.info
                )
            )
            ++newColorFrames
        }
        StreamType.DEPTH -> {
            depthFrameBuffer.enqueue(
                FrameData(
                    frame.byteBuffer.toMat(
                        DEPTH_WIDTH, DEPTH_HEIGHT,
                        CvType.CV_8UC2
                    ), frame.info
                )
            )
            ++newDepthFrames
        }
        else -> {
            throw IllegalStreamTypeException("Stream type not recognized in onNewFrame")
        }
    }
//        val toc = System.currentTimeMillis()
//        Log.d(TAG, "${streamTypeMap[streamType]} frame receive time: ${toc - tic}ms")
}


class IllegalStreamTypeException(msg: String) : RuntimeException(msg)