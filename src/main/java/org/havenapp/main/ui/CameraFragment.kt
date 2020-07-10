/*
 * Copyright (c) 2017 Nathanial Freitas / Guardian Project
 *  * Licensed under the GPLv3 license.
 *
 * Copyright (c) 2013-2015 Marco Ziccardi, Luca Bonato
 * Licensed under the MIT license.
 */
package org.havenapp.main.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.ImageFormat
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.os.Message
import android.os.Messenger
import android.util.Log
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import androidx.camera.core.*
import androidx.camera.core.impl.VideoCaptureConfig
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import java.io.File
import java.lang.Runnable
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors
import kotlinx.coroutines.*
import org.havenapp.main.PreferenceManager
import org.havenapp.main.R
import org.havenapp.main.Utils
import org.havenapp.main.model.EventTrigger
import org.havenapp.main.sensors.motion.LuminanceMotionDetector
import org.havenapp.main.sensors.motion.MotionDetector
import org.havenapp.main.service.MonitorService
import org.havenapp.main.usecase.MotionAnalyser

class CameraFragment : Fragment() {
    private var prefs: PreferenceManager? = null

    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var videoCapture: VideoCapture? = null
    private var camera: Camera? = null

    private var recordingEvent = false

    private val job = SupervisorJob()
    private val uiScope = CoroutineScope(Dispatchers.Main + job)

    /**
     * Sensitivity of motion detection
     */
    private var motionSensitivity = LuminanceMotionDetector.MOTION_MEDIUM

    private val motionDetector = MotionDetector(motionSensitivity)

    private val analysisFrameSize = Size(640, 480)
    private val motionAnalyser = MotionAnalyser(
            ImageFormat.YUV_420_888,
            analysisFrameSize,
            motionDetector
    )

    /**
     * Messenger used to signal motion to the alert service
     */
    @Volatile
    private var serviceMessenger: Messenger? = null

    private val connection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            Log.i("CameraFragment", "SERVICE CONNECTED")
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            serviceMessenger = Messenger(service)
            motionAnalyser.setAnalyze(true)
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            Log.i("CameraFragment", "SERVICE DISCONNECTED")
            motionAnalyser.setAnalyze(false)
            serviceMessenger = null
        }
    }

    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private val cameraDispatcher = cameraExecutor.asCoroutineDispatcher()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.camera_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = PreferenceManager(requireContext())
        // We bind to the alert service
        requireContext().bindService(Intent(context, MonitorService::class.java),
                connection, Context.BIND_ABOVE_CLIENT)
        motionDetector.resultEventLiveData.observe(viewLifecycleOwner, Observer { event ->
            event?.consume()?.let {
                val iEvent = Intent("event").apply {
                    putExtra("type", EventTrigger.CAMERA)
                    putExtra("detected", it.motionDetected)
                    putExtra("changed", it.pixelsChanged)
                }
                LocalBroadcastManager.getInstance(requireActivity()).sendBroadcast(iEvent)
                if (it.motionDetected) {
                    captureCameraEvent()
                }
            }
        })
        prefs?.cameraLiveData?.observe(viewLifecycleOwner, Observer {
            initCamera(it)
        })
    }

    override fun onDestroyView() {
        requireContext().unbindService(connection)
        super.onDestroyView()
    }

    override fun onDestroy() {
        cameraExecutor.shutdown()
        job.cancel()
        super.onDestroy()
    }

    fun setMotionSensitivity(threshold: Int) {
        this.motionSensitivity = threshold
        motionDetector.setMotionSensitivity(motionSensitivity)
    }

    fun motionDetectorLiveData() = motionDetector.detectorResultLiveData

    fun analyseFrames(analyse: Boolean) = motionAnalyser.setAnalyze(analyse)

    @UiThread
    private fun initCamera(cameraPref: String) {
        val viewFinder = requireView().findViewById<PreviewView>(R.id.pv_preview)
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener(Runnable {
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val videoMonitoring = prefs?.videoMonitoringActive ?: false
            val simultaneousImageMonitoring = prefs?.isSimultaneousImageMonitoring ?: false

            // Preview
            preview = Preview.Builder().build()
            // image capture
            imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()
            // analysis
            imageAnalyzer = ImageAnalysis.Builder()
                    .setDefaultResolution(analysisFrameSize) // todo
                    .setMaxResolution(analysisFrameSize) // todo
                    .build()
                    .also {
                        it.setAnalyzer(cameraExecutor, motionAnalyser)
                    }
            // video capture
            videoCapture = VideoCaptureConfig.Builder()
                    .setTargetResolution(analysisFrameSize)
                    .build()

            // Select camera
            val lensFacing = when (cameraPref) {
                PreferenceManager.FRONT -> CameraSelector.LENS_FACING_FRONT
                PreferenceManager.BACK -> CameraSelector.LENS_FACING_BACK
                else -> CameraSelector.LENS_FACING_BACK // default
            }
            val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(lensFacing)
                    .build()

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                camera = cameraProvider.bindToLifecycle(
                        viewLifecycleOwner,
                        cameraSelector,
                        if (simultaneousImageMonitoring) imageCapture else preview,
                        if (videoMonitoring) videoCapture else imageCapture,
                        imageAnalyzer
                )
                preview?.setSurfaceProvider(viewFinder.createSurfaceProvider())
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    @UiThread
    private fun captureCameraEvent() {
        if (prefs?.videoMonitoringActive == true) {
            recordVideo()
            if (prefs?.isSimultaneousImageMonitoring == true) {
                takePhoto()
            }
        } else {
            takePhoto()
        }
    }

    @UiThread
    private fun takePhoto() {
        // if we are not connected to the service; we are not monitoring
        if (serviceMessenger == null) {
            return
        }
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        // Create timestamped output file to hold the image
        val fileImageDir = File(requireContext().getExternalFilesDir(null), prefs!!.defaultMediaStoragePath)
        fileImageDir.mkdirs()
        val ts = SimpleDateFormat(Utils.DATE_TIME_PATTERN, Locale.getDefault()).format(Date())
        val photoFile = File(fileImageDir, "$ts.detected.original.jpg")

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        // Setup image capture listener which is triggered after photo has been taken
        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(requireContext()),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onError(exc: ImageCaptureException) {
                        Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                    }

                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        val savedUri = Uri.fromFile(photoFile)
                        val msg = "Photo capture succeeded: ${savedUri.path}"
                        Log.d(TAG, msg)
                        val message = Message().apply {
                            what = EventTrigger.CAMERA
                            data.putString(MonitorService.KEY_PATH, savedUri.path)
                        }
                        serviceMessenger?.send(message) ?: kotlin.run {
                            Log.e(TAG, "Failed to send ${savedUri.path} to service")
                        }
                    }
                })
    }

    @UiThread
    private fun recordVideo() {
        // don't record if monitoring is not set or already recording event or service is not running
        if (prefs?.videoMonitoringActive != true || recordingEvent || serviceMessenger == null) {
            return
        }
        Log.d(TAG, "Start record video")
        // get the video monitoring length from prefs in ms
        val videoMonitoringLength = prefs?.monitoringTime?.let { it * 1_000L } ?: return
        uiScope.launch {
            videoCapture?.let {
                recordingEvent = true
                // Create timestamped output file to hold the image
                val fileImageDir = File(requireContext().getExternalFilesDir(null), prefs!!.defaultMediaStoragePath)
                fileImageDir.mkdirs()
                val ts = SimpleDateFormat(Utils.DATE_TIME_PATTERN, Locale.getDefault()).format(Date())
                val videoFile = File(fileImageDir, "$ts.detected.original.mp4")
                it.startRecording(videoFile, cameraExecutor, object : VideoCapture.OnVideoSavedCallback {
                    @WorkerThread
                    override fun onVideoSaved(file: File) {
                        Log.e(TAG, "Saved video with to $file")
                        val message = Message().apply {
                            what = EventTrigger.CAMERA_VIDEO
                            data.putString(MonitorService.KEY_PATH, videoFile.absolutePath)
                        }
                        serviceMessenger?.send(message) ?: kotlin.run {
                            Log.e(TAG, "Failed to send ${videoFile.absolutePath} to service")
                        }
                    }

                    @WorkerThread
                    override fun onError(videoCaptureError: Int, message: String, cause: Throwable?) {
                        Log.e(TAG, "Failed to save video with error $videoCaptureError, $message, $cause")
                    }
                })
                delay(videoMonitoringLength)
                it.stopRecording()
                recordingEvent = false
            }
        }
    }

    @UiThread
    fun stopMonitoring() {
        motionAnalyser.setAnalyze(false)
        videoCapture?.stopRecording()
        uiScope.launch {
            delay(3_000L)
            withContext(cameraDispatcher) {
                val message = Message().apply {
                    what = MonitorService.MSG_STOP_SELF
                }
                serviceMessenger?.send(message) ?: kotlin.run {
                    Log.e(TAG, "Failed to send $message to service")
                }
            }
        }
    }

    companion object {
        private val TAG = CameraFragment::class.java.simpleName
    }
}
