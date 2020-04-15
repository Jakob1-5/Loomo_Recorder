package com.example.loomorecorder

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.SurfaceView
import android.view.View
import android.widget.ImageView
import com.example.loomorecorder.loomoUtils.LoomoBase
import com.example.loomorecorder.loomoUtils.LoomoRealSense
import com.example.loomorecorder.loomoUtils.LoomoSensor
import com.example.loomorecorder.loomoUtils.LoomoSensor.getAllSensors
import com.example.loomorecorder.loomoUtils.LoomoSensor.getSensBaseImu
import com.example.loomorecorder.loomoUtils.LoomoSensor.getSensBaseTick
import kotlinx.android.synthetic.main.activity_main.*
import org.opencv.android.BaseLoaderCallback
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.OpenCVLoader
import java.lang.NullPointerException

class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"
    lateinit var mLoaderCallback: BaseLoaderCallback

    init {
        //Load OpenCV
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "OpenCV not loaded")
        } else {
            Log.d(TAG, "OpenCV loaded")
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // Hacky trick to make the app fullscreen:
        textView2.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN




        mLoaderCallback = object : BaseLoaderCallback(this) {
            override fun onManagerConnected(status: Int) {
                when (status) {
                    LoaderCallbackInterface.SUCCESS -> Log.d(TAG, "OpenCV loaded successfully")
                    else -> super.onManagerConnected(status)
                }
            }
        }



        camViewColor.visibility = ImageView.GONE
        camViewFishEye.visibility = ImageView.VISIBLE
        camViewDepth.visibility = ImageView.GONE

        // OnClicklisteners
        var camViewState = 0
        btnCycleCam.setOnClickListener {
//            Log.d(TAG, "CamStartBtn clicked")
            ++camViewState
            when (camViewState) {
                1 -> {
                    camViewColor.visibility = ImageView.GONE
                    camViewFishEye.visibility = ImageView.GONE
                    camViewDepth.visibility = ImageView.VISIBLE
                }
                2 -> {
                    camViewColor.visibility = ImageView.VISIBLE
                    camViewFishEye.visibility = ImageView.GONE
                    camViewDepth.visibility = ImageView.GONE
                }
                else -> {
                    camViewState = 0
                    camViewColor.visibility = ImageView.GONE
                    camViewFishEye.visibility = ImageView.VISIBLE
                    camViewDepth.visibility = ImageView.GONE
                }
            }
        }
        btn2.setOnClickListener {
            Log.i(TAG, getAllSensors().toString())
        }
        btn3.setOnClickListener {
            Log.i(TAG, getSensBaseTick().toString())
        }
        btn4.setOnClickListener {
            Log.i(TAG, getSensBaseImu().toString())
        }

    }

    override fun onResume() {
        Log.i(TAG, "Activity resumed")
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization")
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback)
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!")
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS)
        }


        LoomoRealSense.bind(this)
        LoomoRealSense.startCameras { streamType, frame ->
            onNewFrame(streamType, frame)
            updateImgViews()
        }

        LoomoSensor.bind(this)

        LoomoBase.bind(this)


        super.onResume()
    }

    private fun updateImgViews() {
//        val fishEye = fishEyeFrameBuffer.peek().frame.toBitmap()
//        val color = colorFrameBuffer.peek().frame.toBitmap()
//        val depth = depthFrameBuffer.peek().frame.toBitmap()
        runOnUiThread {
            try {
                camViewFishEye.setImageBitmap(fishEyeFrameBuffer.peek().frame.toBitmap())
                camViewColor.setImageBitmap(colorFrameBuffer.peek().frame.toBitmap())
                camViewDepth.setImageBitmap(depthFrameBuffer.peek().frame.toBitmap())
            } catch (e: NullPointerException) {

            }
        }
    }
}
