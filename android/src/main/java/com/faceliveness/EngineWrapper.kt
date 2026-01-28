package com.faceliveness

import android.content.res.AssetManager
import com.mv.engine.FaceBox
import com.mv.engine.FaceDetector
import com.mv.engine.Live

internal class EngineWrapper(private val assetManager: AssetManager) {

    private val faceDetector: FaceDetector = FaceDetector()
    private val live: Live = Live()

    fun init(): Boolean {
        var ret = faceDetector.loadModel(assetManager)
        if (ret == 0) {
            ret = live.loadModel(assetManager)
            return ret == 0
        }
        return false
    }

    fun destroy() {
        faceDetector.destroy()
        live.destroy()
    }

    fun detect(yuv: ByteArray, width: Int, height: Int, orientation: Int): LivenessResult {
        val boxes = detectFace(yuv, width, height, orientation)
        if (boxes.isNotEmpty()) {
            val begin = System.currentTimeMillis()
            val box = boxes[0].apply {
                val c = detectLive(yuv, width, height, orientation, this)
                confidence = c
            }
            val end = System.currentTimeMillis()
            return LivenessResult(
                left = box.left,
                top = box.top,
                right = box.right,
                bottom = box.bottom,
                confidence = box.confidence,
                timeMs = end - begin,
                hasFace = true
            )
        }

        return LivenessResult()
    }

    private fun detectFace(
        yuv: ByteArray,
        width: Int,
        height: Int,
        orientation: Int
    ): List<FaceBox> = faceDetector.detect(yuv, width, height, orientation)

    private fun detectLive(
        yuv: ByteArray,
        width: Int,
        height: Int,
        orientation: Int,
        faceBox: FaceBox
    ): Float = live.detect(yuv, width, height, orientation, faceBox)
}
