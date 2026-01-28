package com.faceliveness

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.Camera
import android.os.Build
import android.os.SystemClock
import android.util.AttributeSet
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.LifecycleEventListener
import com.facebook.react.bridge.ReactContext
import com.facebook.react.uimanager.events.RCTEventEmitter
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs

@Suppress("DEPRECATION")
class FaceLivenessView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs),
    SurfaceHolder.Callback,
    Camera.PreviewCallback,
    LifecycleEventListener {

    companion object {
        private const val TAG = "FaceLivenessView"
        private const val defaultThreshold = 0.915f
        private const val defaultAnalysisIntervalMs = 100L
        private const val defaultPreviewWidth = 640
        private const val defaultPreviewHeight = 480
    }

    private val reactContext: ReactContext? = context as? ReactContext

    private val surfaceView: SurfaceView = SurfaceView(context).apply {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
    }

    private var surfaceHolder: SurfaceHolder? = null

    private var engineWrapper: EngineWrapper? = null
    private var enginePrepared: Boolean = false

    private var camera: Camera? = null
    private var cameraId: Int = Camera.CameraInfo.CAMERA_FACING_FRONT
    private var previewWidth: Int = defaultPreviewWidth
    private var previewHeight: Int = defaultPreviewHeight

    private var threshold: Float = defaultThreshold
    private var analysisIntervalMs: Long = defaultAnalysisIntervalMs
    private var lastAnalysisTime: Long = 0L
    private var isActive: Boolean = true

    private val detectionExecutor = Executors.newSingleThreadExecutor()
    private val working = AtomicBoolean(false)

    init {
        addView(surfaceView)
        surfaceView.holder.setFormat(ImageFormat.NV21)
        surfaceView.holder.addCallback(this)
        reactContext?.addLifecycleEventListener(this)
    }

    fun setThreshold(value: Float) {
        threshold = value
    }

    fun setAnalysisIntervalMs(value: Int) {
        analysisIntervalMs = value.toLong().coerceAtLeast(0L)
    }

    fun setCameraFacing(value: String?) {
        val desired = if (value == "back") {
            Camera.CameraInfo.CAMERA_FACING_BACK
        } else {
            Camera.CameraInfo.CAMERA_FACING_FRONT
        }
        if (cameraId != desired) {
            cameraId = desired
            if (isActive) {
                restartCamera()
            }
        }
    }

    fun setActive(value: Boolean?) {
        if (value == null) return
        if (value) {
            start()
        } else {
            stop()
        }
    }

    fun start() {
        isActive = true
        if (!hasCameraPermission()) {
            emitError("NO_CAMERA_PERMISSION", "Camera permission not granted")
            return
        }
        prepareEngine()
        openCameraIfReady()
    }

    fun stop() {
        isActive = false
        stopCamera()
    }

    override fun onHostResume() {
        if (isActive) {
            start()
        }
    }

    override fun onHostPause() {
        stopCamera()
    }

    override fun onHostDestroy() {
        stop()
        releaseEngine()
        detectionExecutor.shutdown()
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        surfaceHolder = holder
        if (isActive) {
            openCameraIfReady()
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        surfaceHolder = holder
        if (isActive && camera != null) {
            configureCamera()
            startPreview()
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        stopCamera()
        surfaceHolder = null
    }

    override fun onPreviewFrame(data: ByteArray?, camera: Camera?) {
        if (!enginePrepared || !isActive || data == null) return

        val now = SystemClock.elapsedRealtime()
        if (analysisIntervalMs > 0 && now - lastAnalysisTime < analysisIntervalMs) {
            return
        }
        lastAnalysisTime = now

        if (!working.compareAndSet(false, true)) return

        val dataCopy = data.clone()
        val width = previewWidth
        val height = previewHeight
        val orientation = frameOrientation()

        detectionExecutor.execute {
            try {
                val result = engineWrapper?.detect(dataCopy, width, height, orientation)
                if (result != null) {
                    emitLiveness(result)
                }
            } catch (e: Exception) {
                emitError("DETECT_FAILED", e.message ?: "Detection failed")
            } finally {
                working.set(false)
            }
        }
    }

    private fun frameOrientation(): Int {
        // Match the working sample app: use fixed orientation for front camera.
        return if (cameraId == Camera.CameraInfo.CAMERA_FACING_FRONT) 7 else 6
    }

    private fun openCameraIfReady() {
        if (!isActive || camera != null) return
        val holder = surfaceHolder ?: return

        try {
            camera = Camera.open(cameraId)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to open camera $cameraId, trying fallback", e)
            cameraId = Camera.CameraInfo.CAMERA_FACING_BACK
            camera = try {
                Camera.open(cameraId)
            } catch (inner: Exception) {
                emitError("CAMERA_OPEN_FAILED", inner.message ?: "Failed to open camera")
                null
            }
        }

        try {
            camera?.setPreviewDisplay(holder)
        } catch (e: Exception) {
            emitError("CAMERA_PREVIEW_FAILED", e.message ?: "Failed to set preview")
            stopCamera()
            return
        }

        configureCamera()
        startPreview()
    }

    private fun startPreview() {
        val cam = camera ?: return
        try {
            cam.setPreviewCallback(this)
            cam.startPreview()
        } catch (e: Exception) {
            emitError("CAMERA_START_FAILED", e.message ?: "Failed to start preview")
        }
    }

    private fun stopCamera() {
        try {
            camera?.setPreviewCallback(null)
            camera?.stopPreview()
        } catch (_: Exception) {
            // Ignore
        }
        camera?.release()
        camera = null
    }

    private fun restartCamera() {
        stopCamera()
        openCameraIfReady()
    }

    private fun configureCamera() {
        val cam = camera ?: return
        val params = cam.parameters
        params.previewFormat = ImageFormat.NV21

        val size = choosePreviewSize(params)
        params.setPreviewSize(size.width, size.height)
        cam.parameters = params

        previewWidth = size.width
        previewHeight = size.height

        setCameraDisplayOrientation(cam)
    }

    private fun choosePreviewSize(params: Camera.Parameters): Camera.Size {
        val sizes = params.supportedPreviewSizes
        if (sizes == null || sizes.isEmpty()) {
            return params.previewSize
        }

        sizes.firstOrNull { it.width == defaultPreviewWidth && it.height == defaultPreviewHeight }?.let {
            return it
        }

        val targetRatio = defaultPreviewWidth.toFloat() / defaultPreviewHeight
        var best: Camera.Size = sizes[0]
        var bestScore = Float.MAX_VALUE
        for (size in sizes) {
            val ratio = size.width.toFloat() / size.height.toFloat()
            val ratioDiff = abs(ratio - targetRatio)
            val sizeDiff = abs(size.width - defaultPreviewWidth).toFloat() / 1000f
            val score = ratioDiff + sizeDiff
            if (score < bestScore) {
                bestScore = score
                best = size
            }
        }
        return best
    }

    private fun setCameraDisplayOrientation(cam: Camera) {
        val info = Camera.CameraInfo()
        Camera.getCameraInfo(cameraId, info)
        val rotation = reactContext?.currentActivity?.windowManager?.defaultDisplay?.rotation
            ?: Surface.ROTATION_0
        val degrees = when (rotation) {
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> 0
        }
        val result = if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            (360 - (info.orientation + degrees) % 360) % 360
        } else {
            (info.orientation - degrees + 360) % 360
        }
        cam.setDisplayOrientation(result)
    }

    private fun prepareEngine() {
        if (engineWrapper == null) {
            engineWrapper = EngineWrapper(context.assets)
        }
        enginePrepared = engineWrapper?.init() == true
        if (!enginePrepared) {
            emitError("ENGINE_INIT_FAILED", "Failed to load liveness models")
        }
    }

    private fun releaseEngine() {
        engineWrapper?.destroy()
        engineWrapper = null
        enginePrepared = false
    }

    private fun hasCameraPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun emitLiveness(result: LivenessResult) {
        val event = Arguments.createMap().apply {
            putBoolean("hasFace", result.hasFace)
            putDouble("confidence", result.confidence.toDouble())
            putDouble("threshold", threshold.toDouble())
            putInt("left", result.left)
            putInt("top", result.top)
            putInt("right", result.right)
            putInt("bottom", result.bottom)
            putDouble("timeMs", result.timeMs.toDouble())
        }
        sendEvent("onLiveness", event)
    }

    private fun emitError(code: String, message: String) {
        Log.w(TAG, "$code: $message")
        val event = Arguments.createMap().apply {
            putString("code", code)
            putString("message", message)
        }
        sendEvent("onError", event)
    }

    private fun sendEvent(eventName: String, event: com.facebook.react.bridge.WritableMap) {
        val ctx = reactContext ?: return
        ctx.getJSModule(RCTEventEmitter::class.java).receiveEvent(id, eventName, event)
    }
}
