package io.github.osphvdhwj.aves.ai

import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import android.os.Process
import android.util.Log

class AiCompanionService : Service() {

    private val binder = object : IAvesAi.Stub() {

        override fun getInterfaceVersion(): Int = INTERFACE_VERSION

        override fun getCapabilities(): MutableList<String> =
            mutableListOf(CAP_ECHO)

        override fun submit(request: Bundle?, cb: IAvesAiCallback?) {
            if (request == null || cb == null) {
                Log.w(TAG, "submit: null request or callback")
                return
            }
            val id = request.getLong(KEY_REQUEST_ID, -1L)
            val capability = request.getString(KEY_CAPABILITY)
            Log.i(TAG, "submit id=$id capability=$capability caller=${callerDescription()}")

            when (capability) {
                CAP_ECHO -> handleEcho(id, request, cb)
                else -> cb.onError(id, ERR_UNSUPPORTED, "unsupported capability: $capability")
            }
        }

        override fun cancel(requestId: Long) {
            Log.i(TAG, "cancel id=$requestId")
            // no long-running jobs yet; nothing to cancel
        }
    }

    private fun handleEcho(requestId: Long, request: Bundle, cb: IAvesAiCallback) {
        cb.onProgress(requestId, 0)
        val out = Bundle().apply {
            putLong(KEY_REQUEST_ID, requestId)
            putString(KEY_CAPABILITY, CAP_ECHO)
            putString("raw", request.getString("raw"))
            putString("prefix", request.getString("prefix"))
            putString("verb", request.getString("verb"))
            putString("freeText", request.getString("freeText"))
            putLong("serverTimeMs", System.currentTimeMillis())
            putInt("interfaceVersion", INTERFACE_VERSION)
            putInt("servicePid", Process.myPid())
        }
        cb.onProgress(requestId, 100)
        cb.onResult(requestId, out)
    }

    private fun callerDescription(): String {
        val uid = android.os.Binder.getCallingUid()
        val pkgs = packageManager.getPackagesForUid(uid)
        return "uid=$uid pkgs=${pkgs?.joinToString(",") ?: "?"}"
    }

    override fun onBind(intent: Intent?): IBinder {
        Log.i(TAG, "onBind action=${intent?.action}")
        return binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.i(TAG, "onUnbind action=${intent?.action}")
        return super.onUnbind(intent)
    }

    companion object {
        private const val TAG = "AiCompanionService"

        const val INTERFACE_VERSION = 1
        const val ACTION_BIND = "io.github.osphvdhwj.aves.ai.BIND"

        const val CAP_ECHO = "echo"

        const val KEY_REQUEST_ID = "requestId"
        const val KEY_CAPABILITY = "capability"

        // error codes per docs/ai-companion-ipc.md
        const val ERR_OK = 0
        const val ERR_UNSUPPORTED = 1
        const val ERR_MODEL_MISSING = 2
        const val ERR_OOM = 3
        const val ERR_PERMISSION = 4
        const val ERR_INTERNAL = 5
        const val ERR_CANCELLED = 6
    }
}
