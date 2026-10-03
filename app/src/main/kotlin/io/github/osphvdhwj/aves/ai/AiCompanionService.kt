package io.github.osphvdhwj.aves.ai

import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import android.util.Log

class AiCompanionService : Service() {

    private val binder = object : IAvesAi.Stub() {
        override fun getInterfaceVersion(): Int = INTERFACE_VERSION

        override fun getCapabilities(): MutableList<String> = mutableListOf()

        override fun submit(request: Bundle?, cb: IAvesAiCallback?) {
            val id = request?.getLong("requestId", -1L) ?: -1L
            cb?.onError(id, ERR_INTERNAL, "not implemented")
        }

        override fun cancel(requestId: Long) {
            // no-op
        }
    }

    override fun onBind(intent: Intent?): IBinder {
        Log.i(TAG, "onBind action=${intent?.action}")
        return binder
    }

    companion object {
        private const val TAG = "AiCompanionService"
        const val INTERFACE_VERSION = 1
        const val ACTION_BIND = "io.github.osphvdhwj.aves.ai.BIND"
        const val ERR_INTERNAL = 5
    }
}
