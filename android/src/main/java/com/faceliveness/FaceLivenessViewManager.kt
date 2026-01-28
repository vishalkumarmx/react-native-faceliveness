package com.faceliveness

import com.facebook.react.uimanager.SimpleViewManager
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.annotations.ReactProp

class FaceLivenessViewManager : SimpleViewManager<FaceLivenessView>() {

    override fun getName(): String = "FaceLivenessView"

    override fun createViewInstance(reactContext: ThemedReactContext): FaceLivenessView {
        return FaceLivenessView(reactContext)
    }

    @ReactProp(name = "threshold", defaultFloat = 0.915f)
    fun setThreshold(view: FaceLivenessView, value: Float) {
        view.setThreshold(value)
    }

    @ReactProp(name = "cameraFacing")
    fun setCameraFacing(view: FaceLivenessView, value: String?) {
        view.setCameraFacing(value)
    }

    @ReactProp(name = "analysisIntervalMs", defaultInt = 100)
    fun setAnalysisIntervalMs(view: FaceLivenessView, value: Int) {
        view.setAnalysisIntervalMs(value)
    }

    @ReactProp(name = "active")
    fun setActive(view: FaceLivenessView, value: Boolean?) {
        view.setActive(value)
    }

    override fun getExportedCustomDirectEventTypeConstants(): MutableMap<String, Any> {
        return mutableMapOf(
            "onLiveness" to mapOf("registrationName" to "onLiveness"),
            "onError" to mapOf("registrationName" to "onError")
        )
    }

    override fun getCommandsMap(): MutableMap<String, Int> {
        return mutableMapOf(
            "start" to 1,
            "stop" to 2
        )
    }

    override fun receiveCommand(view: FaceLivenessView, commandId: Int, args: com.facebook.react.bridge.ReadableArray?) {
        when (commandId) {
            1 -> view.start()
            2 -> view.stop()
        }
    }

    override fun receiveCommand(view: FaceLivenessView, commandId: String, args: com.facebook.react.bridge.ReadableArray?) {
        when (commandId) {
            "start" -> view.start()
            "stop" -> view.stop()
        }
    }
}
