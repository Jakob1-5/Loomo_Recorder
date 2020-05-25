package com.example.loomorecorder

import android.graphics.Bitmap
import android.os.Bundle
import android.os.Process
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.loomorecorder.loomoUtils.AllSensors
import com.example.loomorecorder.loomoUtils.LoomoBase
import com.example.loomorecorder.loomoUtils.LoomoBase.mBase
import com.example.loomorecorder.loomoUtils.LoomoRealSense
import com.example.loomorecorder.loomoUtils.LoomoRealSense.COLOR_HEIGHT
import com.example.loomorecorder.loomoUtils.LoomoRealSense.COLOR_WIDTH
import com.example.loomorecorder.loomoUtils.LoomoRealSense.DEPTH_HEIGHT
import com.example.loomorecorder.loomoUtils.LoomoRealSense.DEPTH_WIDTH
import com.example.loomorecorder.loomoUtils.LoomoRealSense.FISHEYE_HEIGHT
import com.example.loomorecorder.loomoUtils.LoomoRealSense.FISHEYE_WIDTH
import com.example.loomorecorder.loomoUtils.LoomoSensor
import com.example.loomorecorder.loomoUtils.LoomoSensor.getAllSensors
import com.opencsv.CSVWriter
import com.segway.robot.algo.Pose2D
import com.segway.robot.algo.tf.AlgoTfData
import com.segway.robot.sdk.locomotion.sbv.Base
import com.segway.robot.sdk.perception.sensor.Sensor
import com.segway.robot.sdk.vision.frame.Frame
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter
import java.nio.ByteBuffer

class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"

    private var recordingsCounter = 0
    private val baseDir = "recordings"
    private val fisheyeDir = "fisheye"
    private val colorDir = "color"
    private val depthDir = "depth"
    private lateinit var appPath: String


    private val csvLoopedThread = LoopedThread("csvSaving", Process.THREAD_PRIORITY_DEFAULT)
    private val fisheyeLoopedThread = LoopedThread("fisheyeSaving", Process.THREAD_PRIORITY_DEFAULT)
    private val colorLoopedThread = LoopedThread("colorSaving", Process.THREAD_PRIORITY_DEFAULT)
    private val depthLoopedThread = LoopedThread("depthSaving", Process.THREAD_PRIORITY_DEFAULT)

    init {
//        csvSavingLoop.pause()
//        fisheyeSavingLoop.pause()
//        colorSavingLoop.pause()
//        depthSavingLoop.pause()
        csvLoopedThread.start()
        fisheyeLoopedThread.start()
        colorLoopedThread.start()
        depthLoopedThread.start()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)



        appPath = application.getExternalFilesDir(baseDir).toString()



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
            GlobalScope.launch {
                displayData()
            }
        }
        btn3.setOnClickListener {
            var a = ""
            for (i in getAllSensors().toStringArray()) a += i
            Log.i(TAG, a)
        }
//        recording = false
        btn4.setOnClickListener {
            when {
                !busySaving and !recording -> {
                    startRecording()
//                    recording = true
                    btn4.text = getString(R.string.stopRecording)
                }
                busySaving and recording -> {
                    stopRecording()
//                    recording = false
                    btn4.text = getString(R.string.startRecording)
                }
                busySaving and !recording -> {
                    // cant't start new recording session yet
                    Toast.makeText(
                        applicationContext,
                        "Busy saving, cant start new recording yet",
                        Toast.LENGTH_LONG
                    ).show()
//                    recording = false
                }
                !busySaving and recording -> {
                    // shouldn't happen
//                    recording = false
                }
            }
        }
    }

    override fun onResume() {
        Log.i(TAG, "Activity resumed")



        LoomoRealSense.bind(this)
        LoomoRealSense.startFisheye { streamType, frame ->
            if (recording) {
                appendFrame(frame, fishEyeFile)
                fishEyeTimeStamp = frame.info.imuTimeStamp
            }
            else {
                runOnUiThread {
                    camViewFishEye.setImageBitmap(
                        frame.byteBuffer.toBitmap(
                            FISHEYE_WIDTH,
                            FISHEYE_HEIGHT,
                            Bitmap.Config.ALPHA_8
                        )
                    )
                }
            }
        }
        LoomoRealSense.startColor { streamType, frame ->
            if (recording) {
                appendFrame(frame, colorFile)
                colorTimeStamp = frame.info.imuTimeStamp
            } else {
                runOnUiThread {
                    camViewColor.setImageBitmap(
                        frame.byteBuffer.toBitmap(
                            COLOR_WIDTH,
                            COLOR_HEIGHT,
                            Bitmap.Config.ARGB_8888
                        )
                    )
                }
            }
        }
        LoomoRealSense.startDepth { streamType, frame ->
            if (recording) {
                appendFrame(frame, depthFile)
                depthTimeStamp = frame.info.imuTimeStamp
            } else {
                runOnUiThread {
                    camViewDepth.setImageBitmap(
                        frame.byteBuffer.toBitmap(
                            DEPTH_WIDTH,
                            DEPTH_HEIGHT,
                            Bitmap.Config.RGB_565
                        )
                    )
                }
            }
        }



        LoomoBase.bind(this)
        LoomoSensor.bind(this)

        // Hacky trick to make the app fullscreen:
        textView2.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        super.onResume()
    }


//    fun updateCamViews(streamType: Int) {
//        when (streamType) {
//            StreamType.FISH_EYE -> runOnUiThread { camViewFishEye.setImageBitmap(fishEyeBitmap) }
////            StreamType.COLOR -> runOnUiThread { camViewColor.setImageBitmap(colorBitmap) }
////            StreamType.DEPTH -> runOnUiThread { camViewDepth.setImageBitmap(depthBitmap) }
//        }
//    }


    // stuff for recording


    private var recording = false
    private var busySaving = false
    private var csvBusy = false
    private var fisheyeBusy = false
    private var colorBusy = false
    private var depthBusy = false
    private var dirForThisRecording = ""
    private val csvName = "sensor_data"

    private val fishEyeStreamInfo = VidStreamInfo(FISHEYE_WIDTH, FISHEYE_HEIGHT, 1)
    private val colorStreamInfo = VidStreamInfo(COLOR_WIDTH, COLOR_HEIGHT, 4)
    private val depthStreamInfo = VidStreamInfo(DEPTH_WIDTH, DEPTH_HEIGHT, 2)

    private var fishEyeTimeStamp = 0L
    private var colorTimeStamp = 0L
    private var depthTimeStamp = 0L

    private lateinit var fishEyeFile: File
    private lateinit var colorFile: File
    private lateinit var depthFile: File

    private fun startRecording() {
        if (recording or busySaving) {
            Log.d(TAG, "Can't start recording. recording = $recording, busySaving = $busySaving")
        } else {

            busySaving = true

            // create new dirs and files
            while (File("$appPath/${String.format("%04d", recordingsCounter)}").isDirectory) {
                recordingsCounter++
            }
            dirForThisRecording = application.getExternalFilesDir(
                "$baseDir/${String.format(
                    "%04d",
                    recordingsCounter
                )}"
            ).toString()

            fishEyeFile = File(dirForThisRecording, "fisheye.myvid")
            var counter = 0
            while (!fishEyeFile.createNewFile()) {
                ++counter
                fishEyeFile =
                    File(dirForThisRecording, "fisheye_${String.format("%03d", counter)}.myvid")
            }
            counter = 0

            colorFile = File(dirForThisRecording, "color.myvid")
            while (!colorFile.createNewFile()) {
                ++counter
                colorFile =
                    File(dirForThisRecording, "color_${String.format("%03d", counter)}.myvid")
            }
            counter = 0

            depthFile = File(dirForThisRecording, "depth.myvid")
            while (!depthFile.createNewFile()) {
                ++counter
                depthFile =
                    File(dirForThisRecording, "depth_${String.format("%03d", counter)}.myvid")
            }


            fishEyeFile.appendBytes(fishEyeStreamInfo.toByteArray())
            colorFile.appendBytes(colorStreamInfo.toByteArray())
            depthFile.appendBytes(depthStreamInfo.toByteArray())


            recording = true


            val header = arrayOf<String>(
                *AllSensors.getStringArrayHeader(),
                "fishEyeIndex",
                "colorIndex",
                "depthIndex"
            )

            saveTxt(header, dirForThisRecording, csvName)
            csvLoopedThread.handler.post { csvSavingLoop() }
//            fisheyeLoopedThread.handler.post { fisheyeSavingLoop() }
//            colorLoopedThread.handler.post { colorSavingLoop() }
//            depthLoopedThread.handler.post { depthSavingLoop() }
        }
    }

    fun stopRecording() {
        recording = false
        fishEyeTimeStamp = 0L
        colorTimeStamp = 0L
        depthTimeStamp = 0L
//        csvSavingLoop.pause()
//        fisheyeSavingLoop.pause()
//        colorSavingLoop.pause()
//        depthSavingLoop.pause()
//        while (csvBusy or fisheyeBusy or colorBusy or depthBusy) {
//        }
        GlobalScope.launch {
            while (csvBusy or fisheyeBusy or colorBusy or depthBusy) {
                runOnUiThread { btn4.text = "Busy saving" }
                delay(50)
            }
            runOnUiThread {
                btn4.text = "Start recording"
                Toast.makeText(applicationContext, "Done saving", Toast.LENGTH_LONG).show()
            }
            busySaving = false
        }
    }

    private fun displayData() {
        var prevTime = System.currentTimeMillis()
        while (!recording) {
            if ((System.currentTimeMillis() - prevTime) > 10) {
                prevTime = System.currentTimeMillis()
                val sensors = getAllSensors()
                runOnUiThread {
                    textView1.text =
                        "Pose2D: (${sensors.pose2D.x}, ${sensors.pose2D.y}, ${sensors.pose2D.theta})\nVel: ${sensors.pose2D.linearVelocity} m/s, ${sensors.pose2D.angularVelocity} rad/s"
                    textView2.text =
                        "Left tick: ${sensors.baseTick.left}, right tick: ${sensors.baseTick.right}"
                    textView7.text =
                        "IR_L: ${sensors.surroundings.IR_Left}, Ultrasound: ${sensors.surroundings.UltraSonic}, IR_R: ${sensors.surroundings.IR_Right}"
                }
            }
        }
    }

    private fun csvSavingLoop() {
        var prevTime = System.currentTimeMillis()
        while (recording) {
            csvBusy = true
            val sensors = getAllSensors()
            if ((System.currentTimeMillis() - prevTime) > 10) {
                prevTime = System.currentTimeMillis()
                runOnUiThread {
                    textView1.text =
                        "Pose2D: (${sensors.pose2D.x}, ${sensors.pose2D.y}, ${sensors.pose2D.theta})\nVel: ${sensors.pose2D.linearVelocity} m/s, ${sensors.pose2D.angularVelocity} rad/s"
                    textView2.text =
                        "Left tick: ${sensors.baseTick.left}, right tick: ${sensors.baseTick.right}"
                    textView7.text =
                        "IR_L: ${sensors.surroundings.IR_Left}, Ultrasound: ${sensors.surroundings.UltraSonic}, IR_R: ${sensors.surroundings.IR_Right}"
                }
            }
            val logEntry =
                arrayOf(
                    *sensors.toStringArray(),
                    fishEyeTimeStamp.toString(),
                    colorTimeStamp.toString(),
                    depthTimeStamp.toString()
                )
            saveTxt(logEntry, dirForThisRecording, csvName)
            csvBusy = false
        }
    }

    private fun appendFrame(frame: Frame, file: File) {
        file.appendBytes(frame.info.imuTimeStamp.toByteArray())
        file.appendBytes(frame.byteBuffer.toByteArray())
        frame.byteBuffer.rewind()
    }

    private fun saveTxt(txt: Array<String>, dir: String, name: String) {
        val fileName = "$name.csv"
        val file = File(dir, fileName)
        val writer: CSVWriter

        writer = if (file.exists() and !file.isDirectory) {
            CSVWriter(FileWriter(file.absolutePath, true))
        } else {
            CSVWriter(FileWriter(file.absolutePath))
        }
        writer.writeNext(txt)
        writer.close()
    }


    // Utility functions:


    fun ByteBuffer.toByteArray(): ByteArray {
        val bytesInBuffer = this.remaining()
        val tmpArr = ByteArray(bytesInBuffer) { this.get() }
        this.rewind()
        return tmpArr
    }

    fun ByteBuffer.toBitmap(width: Int, height: Int, config: Bitmap.Config): Bitmap {
        val bmp = Bitmap.createBitmap(width, height, config)
        bmp.copyPixelsFromBuffer(this)
        this.rewind()
        return bmp
    }

    fun Long.toByteArray(): ByteArray {
        return byteArrayOf(
            (this shr 56).toByte(),
            (this shr 48).toByte(),
            (this shr 40).toByte(),
            (this shr 32).toByte(),
            (this shr 24).toByte(),
            (this shr 16).toByte(),
            (this shr 8).toByte(),
            this.toByte()
        )
    }

    fun Int.toByteArray(): ByteArray {
        return byteArrayOf(
            (this shr 24).toByte(),
            (this shr 16).toByte(),
            (this shr 8).toByte(),
            this.toByte()
        )
    }

    data class VidStreamInfo(
        val width: Int,
        val height: Int,
        val channels: Int
    ) {
        fun Int.toByteArray(): ByteArray {
            return byteArrayOf(
                (this shr 24).toByte(),
                (this shr 16).toByte(),
                (this shr 8).toByte(),
                this.toByte()
            )
        }

        fun toByteArray(): ByteArray {
            return byteArrayOf(*width.toByteArray(), *height.toByteArray(), *channels.toByteArray())
        }
    }

//    private fun baseOriginReset() {
//        mBase.controlMode = Base.CONTROL_MODE_NAVIGATION
//        mBase.clearCheckPointsAndStop()
//        mBase.cleanOriginalPoint()
////        val newOriginPoint: Pose2D = mBase.getOdometryPose(-1)
//        val newOriginPoint: Pose2D = Pose2D(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, System.nanoTime())
//        mBase.setOriginalPoint(newOriginPoint)
//    }
}
