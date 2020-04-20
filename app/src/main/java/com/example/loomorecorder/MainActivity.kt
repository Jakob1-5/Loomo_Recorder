package com.example.loomorecorder

import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import com.example.loomorecorder.CameraUtils.bufferBaseSize
import com.example.loomorecorder.CameraUtils.colorBitmap
import com.example.loomorecorder.CameraUtils.colorFrameBuffer
import com.example.loomorecorder.CameraUtils.depthBitmap
import com.example.loomorecorder.CameraUtils.depthFrameBuffer
import com.example.loomorecorder.CameraUtils.fishEyeBitmap
import com.example.loomorecorder.CameraUtils.fishEyeFrameBuffer
import com.example.loomorecorder.loomoUtils.AllSensors
import com.example.loomorecorder.loomoUtils.LoomoRealSense
import com.example.loomorecorder.loomoUtils.LoomoSensor
import com.example.loomorecorder.loomoUtils.LoomoSensor.getAllSensors
import com.opencsv.CSVWriter
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"

    private val csvSavingLoop = NonBlockingInfLoop {
        if (recording) {
            csvBusy = true
            val fisheyeStamp = try {
                fishEyeFrameBuffer.peek().timeStamp.toString()
            } catch (e: EmptyBufferException) {
                "no frame"
            }
            val colorStamp = try {
                colorFrameBuffer.peek().timeStamp.toString()
            } catch (e: EmptyBufferException) {
                "no frame"
            }
            val depthStamp = try {
                depthFrameBuffer.peek().timeStamp.toString()
            } catch (e: EmptyBufferException) {
                "no frame"
            }
            val logEntry = arrayOf(*getAllSensors().toStringArray(), fisheyeStamp, colorStamp, depthStamp)
            saveTxt(logEntry, dirForThisRecording, csvName)
            csvBusy = false
        }
    }
    private val fisheyeSavingLoop = NonBlockingInfLoop {
        if (recording) {
            fisheyeBusy = true

            var frameCount = 0
            var avgframeSavingTime: Long = 0
            while (fishEyeFrameBuffer.isNotEmpty() or recording) {
                try {
                    val imgFrame = fishEyeFrameBuffer.dequeue()
                    val tic = System.currentTimeMillis()
                    saveImg(
                        imgFrame.frame,
                        dirForFisheye,
                        imgFrame.timeStamp.toString()
                    )
                    avgframeSavingTime += System.currentTimeMillis()-tic
                    frameCount++
                } catch (e: EmptyBufferException) {
                    //very unlikely to happen
                }
                if ((fishEyeFrameBuffer.freeSpace() < 30) and recording) {
                    fishEyeFrameBuffer.increaseBufferSizeBy(30)
                    Log.d(TAG, "Increasing fisheye buffer size")
                }
                runOnUiThread {
                    textView1.text = "Fisheye buffer size: ${fishEyeFrameBuffer.bufferSize}"
                    textView2.text =
                        if (recording) "Free space: ${fishEyeFrameBuffer.freeSpace()}"
                        else "Images remaining: ${fishEyeFrameBuffer.itemsInQueue}"
                }
            }
            runOnUiThread {
                textView1.text = "Fisheye frames took avg. of ${avgframeSavingTime/frameCount} ms to save"
                textView2.text = "Frames captured: $frameCount"
            }

            Log.d(TAG, "End of fisheyeSavingLoop")
            fisheyeBusy = false
        }
    }
    private val colorSavingLoop = NonBlockingInfLoop {
        if (recording) {
            colorBusy = true

            var frameCount = 0
            var avgframeSavingTime: Long = 0
            while (colorFrameBuffer.isNotEmpty() or recording) {
                try {
                    val imgFrame = colorFrameBuffer.dequeue()
                    val tic = System.currentTimeMillis()
                    saveImg(
                        imgFrame.frame,
                        dirForColor,
                        imgFrame.timeStamp.toString()
                    )
                    avgframeSavingTime += System.currentTimeMillis()-tic
                    frameCount++
                } catch (e: EmptyBufferException) {
                    //very unlikely to happen
                }
                if ((colorFrameBuffer.freeSpace() < 30) and recording) {
                    colorFrameBuffer.increaseBufferSizeBy(30)
                    Log.d(TAG, "Increasing color buffer size")
                }
                runOnUiThread {
                    textView3.text = "Color buffer size: ${colorFrameBuffer.bufferSize}"
                    textView4.text =
                        if (recording) "Free space: ${colorFrameBuffer.freeSpace()}"
                        else "Images remaining: ${colorFrameBuffer.itemsInQueue}"
                }
            }
            runOnUiThread {
                textView3.text = "Color frames took avg. of ${avgframeSavingTime/frameCount} ms to save"
                textView4.text = "Frames captured: $frameCount"
            }

            Log.d(TAG, "End of colorSavingLoop")
            colorBusy = false
        }
    }
    private val depthSavingLoop = NonBlockingInfLoop {
        if (recording) {
            depthBusy = true

            var frameCount = 0
            var avgframeSavingTime: Long = 0
            while (depthFrameBuffer.isNotEmpty() or recording) {
                try {
                    val imgFrame = depthFrameBuffer.dequeue()
                    val tic = System.currentTimeMillis()
                    saveImg(
                        imgFrame.frame,
                        dirForDepth,
                        imgFrame.timeStamp.toString()
                    )
                    avgframeSavingTime += System.currentTimeMillis()-tic
                    frameCount++
                } catch (e: EmptyBufferException) {
                    //very unlikely to happen
                }
                if ((depthFrameBuffer.freeSpace() < 30) and recording) {
                    depthFrameBuffer.increaseBufferSizeBy(30)
                    Log.d(TAG, "Increasing depth buffer size")
                }
                runOnUiThread {
                    textView5.text = "Depth buffer size: ${depthFrameBuffer.bufferSize}"
                    textView6.text =
                        if (recording) "Free space: ${depthFrameBuffer.freeSpace()}"
                        else "Images remaining: ${depthFrameBuffer.itemsInQueue}"
                }
            }
            runOnUiThread {
                textView5.text = "Depth frames took avg. of ${avgframeSavingTime/frameCount} ms to save"
                textView6.text = "Frames captured: $frameCount"
            }

            Log.d(TAG, "End of depthSavingLoop")
            depthBusy = false
        }
    }

    private var recordingsCounter = 0
    private val baseDir = "recordings"
    private val fisheyeDir = "fisheye"
    private val colorDir = "color"
    private val depthDir = "depth"
    private lateinit var appPath: String

    init {
        csvSavingLoop.pause()
        fisheyeSavingLoop.pause()
        colorSavingLoop.pause()
        depthSavingLoop.pause()
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
            Log.i(
                TAG, getAllSensors().toString()
//                        + "\n${fishEyeFrameBuffer.peek().info.imuTimeStamp}"
//                        + "\n${fishEyeFrameBuffer.peek().info.platformTimeStamp}" //correct timestamp info
            )
        }
        btn3.setOnClickListener {
            var a = ""
            for (i in getAllSensors().toStringArray()) a += i
            Log.i(TAG, a)
        }
        var recording = false
        btn4.setOnClickListener {
            when {
                !busySaving and !recording -> {
                    startRecording()
                    recording = true
                    btn4.text = getString(R.string.stopRecording)
                }
                busySaving and recording -> {
                    stopRecording()
                    recording = false
                    btn4.text = getString(R.string.startRecording)
                }
                busySaving and !recording -> {
                    // cant't start new recording session yet
                    Toast.makeText(
                        applicationContext,
                        "Busy saving, cant start new recording yet",
                        Toast.LENGTH_LONG
                    ).show()
                    recording = false
                }
                !busySaving and recording -> {
                    // shouldn't happen
                    recording = false
                }


                //            SavingUtils.saveImg(fishEyeFrameBuffer.peek().frame.toBitmap(), application.getExternalFilesDir("Images"), "01Fisheye")
            }


//            SavingUtils.saveImg(fishEyeFrameBuffer.peek().frame.toBitmap(), application.getExternalFilesDir("Images"), "01Fisheye")
        }

    }

    override fun onResume() {
        Log.i(TAG, "Activity resumed")


        LoomoRealSense.bind(this)
        LoomoRealSense.startCameras { streamType, frame ->
            CameraUtils.onNewFrame(streamType, frame, recording)
            runOnUiThread {
                camViewFishEye.setImageBitmap(fishEyeBitmap)
                camViewColor.setImageBitmap(colorBitmap)
                camViewDepth.setImageBitmap(depthBitmap)
            }
        }

        LoomoSensor.bind(this)


        // Hacky trick to make the app fullscreen:
        textView2.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        super.onResume()
    }


    // stuff for recording


    private var busySaving = false
    var recording = false
    private var csvBusy = false
    private var fisheyeBusy = false
    private var colorBusy = false
    private var depthBusy = false
    private var dirForThisRecording = ""
    private var dirForFisheye = ""
    private var dirForColor = ""
    private var dirForDepth = ""
    private val csvName = "test5"

    fun startRecording() {
        if (recording or busySaving) {
            // can't start recording
        } else {
            recording = true
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
            dirForFisheye = application.getExternalFilesDir(
                "$baseDir/${String.format(
                    "%04d",
                    recordingsCounter
                )}/$fisheyeDir"
            ).toString()
            dirForColor = application.getExternalFilesDir(
                "$baseDir/${String.format(
                    "%04d",
                    recordingsCounter
                )}/$colorDir"
            ).toString()
            dirForDepth = application.getExternalFilesDir(
                "$baseDir/${String.format(
                    "%04d",
                    recordingsCounter
                )}/$depthDir"
            ).toString()


            val header = arrayOf<String>(*AllSensors.getStringArrayHeader(), "fishEyeIndex", "colorIndex", "depthIndex")

            saveTxt(header, dirForThisRecording, csvName)
            csvSavingLoop.resume()
            fisheyeSavingLoop.resume()
            colorSavingLoop.resume()
            depthSavingLoop.resume()
        }
    }

    fun stopRecording() {
        recording = false
        csvSavingLoop.pause()
        fisheyeSavingLoop.pause()
        colorSavingLoop.pause()
        depthSavingLoop.pause()
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
            fishEyeFrameBuffer.clearAndResetBufferSize(bufferBaseSize)
        }
        busySaving = false
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


    fun saveImg(bitmap: Bitmap?, dir: String, name: String) {
        if (bitmap == null) {
            return
        }

//        GlobalScope.launch {
        var instanceCounter = 1

        val bmp = bitmap.copy(
            if (bitmap.config == Bitmap.Config.ALPHA_8) Bitmap.Config.ARGB_8888
            else bitmap.config, false
        )


        try {
            var fileName = "$name.png"

            val fOut: FileOutputStream?
            var file = File(dir, fileName)
            while (!file.createNewFile()) {
                ++instanceCounter
                fileName = "${name}_${String.format("%03d", instanceCounter)}.png"
                file = File(dir, fileName)
            }
            fOut = FileOutputStream(file)

            bmp.compress(Bitmap.CompressFormat.PNG, 100, fOut)
            fOut.flush()
            fOut.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
//        }
    }


}
