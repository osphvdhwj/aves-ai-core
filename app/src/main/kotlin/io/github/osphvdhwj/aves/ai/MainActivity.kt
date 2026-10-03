package io.github.osphvdhwj.aves.ai

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.IBinder
import android.text.method.ScrollingMovementMethod
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import org.json.JSONArray
import java.util.concurrent.atomic.AtomicLong

class MainActivity : Activity() {

    private lateinit var input: EditText
    private lateinit var log: TextView

    private var service: IAvesAi? = null
    private val requestCounter = AtomicLong(0)

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            service = IAvesAi.Stub.asInterface(binder)
            appendLog("bound: " + (name?.className ?: "?"))
            try {
                val v = service?.apiVersion
                val caps = service?.capabilities ?: emptyList<String>()
                appendLog("iface version: $v")
                appendLog("capabilities:  ${caps.joinToString(",")}")
            } catch (e: Exception) {
                appendLog("bind query failed: ${e.message}")
            }
            appendLog("---")
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
            appendLog("unbound")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }

        root.addView(TextView(this).apply {
            text = "Aves AI Companion"
            textSize = 20f
        })

        val palette = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        for (cmd in loadCommands()) {
            palette.addView(Button(this).apply {
                text = cmd.prefix + cmd.verb
                textSize = 12f
                setOnClickListener {
                    input.setText(cmd.prefix + cmd.verb + " ")
                    input.setSelection(input.text.length)
                }
            })
        }
        root.addView(HorizontalScrollView(this).apply { addView(palette) })

        val inputRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        input = EditText(this).apply {
            hint = "type /find dog, @deep ..."
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        inputRow.addView(input)
        inputRow.addView(Button(this).apply {
            text = "Run"
            setOnClickListener { onRun() }
        })
        root.addView(inputRow)

        log = TextView(this).apply {
            textSize = 12f
            typeface = Typeface.MONOSPACE
            setTextIsSelectable(true)
            movementMethod = ScrollingMovementMethod()
            setPadding(dp(8), dp(8), dp(8), dp(8))
            setBackgroundColor(Color.parseColor("#EEEEEE"))
        }
        val logScroll = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
            )
            addView(log)
        }
        root.addView(logScroll)

        setContentView(root)

        appendLog("Ready. Companion scaffold v0.1.0")
        appendLog("binding to companion service...")
        bindCompanion()
    }

    private fun bindCompanion() {
        val i = Intent(this, AiCompanionService::class.java)
        bindService(i, connection, Context.BIND_AUTO_CREATE)
    }

    private fun onRun() {
        val text = input.text.toString()
        val cmd = CommandParser.parse(text)
        appendLog("input:    " + cmd.raw)
        appendLog("prefix:   '" + cmd.prefix + "'")
        appendLog("verb:     " + cmd.verb)
        appendLog("freeText: " + cmd.freeText)

        val svc = service
        if (svc == null) {
            appendLog("ERR: service not bound")
            appendLog("---")
            return
        }

        val id = requestCounter.incrementAndGet()
        val req = Bundle().apply {
            putString(AiCompanionService.KEY_CAPABILITY, AiCompanionService.CAP_ECHO)
            putLong(AiCompanionService.KEY_REQUEST_ID, id)
            putString("raw", cmd.raw)
            putString("prefix", cmd.prefix)
            putString("verb", cmd.verb)
            putString("freeText", cmd.freeText)
        }

        try {
            svc.submit(req, callback)
            appendLog("submitted id=$id")
        } catch (e: Exception) {
            appendLog("submit failed: ${e.message}")
        }
        appendLog("---")
    }

    private val callback = object : IAvesAiCallback.Stub() {
        override fun onProgress(requestId: Long, percent: Int) {
            runOnUiThread { appendLog("progress id=$requestId pct=$percent") }
        }

        override fun onResult(requestId: Long, result: Bundle?) {
            runOnUiThread {
                appendLog("result id=$requestId")
                if (result == null) {
                    appendLog("  (null result)")
                } else {
                    appendLog("  verb:        " + (result.getString("verb") ?: "?"))
                    appendLog("  freeText:    " + (result.getString("freeText") ?: "?"))
                    appendLog("  serverTime:  " + result.getLong("serverTimeMs"))
                    appendLog("  servicePid:  " + result.getInt("servicePid"))
                }
                appendLog("---")
            }
        }

        override fun onError(requestId: Long, code: Int, message: String?) {
            runOnUiThread {
                appendLog("error id=$requestId code=$code msg=$message")
                appendLog("---")
            }
        }
    }

    override fun onDestroy() {
        try { unbindService(connection) } catch (_: Exception) {}
        super.onDestroy()
    }

    private fun appendLog(line: String) {
        log.append(line)
        log.append("\n")
    }

    private fun dp(v: Int): Int =
        (v * resources.displayMetrics.density).toInt()

    private fun loadCommands(): List<CommandDef> {
        val raw = assets.open("commands.json").bufferedReader().use { it.readText() }
        val arr = JSONArray(raw)
        val out = ArrayList<CommandDef>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out.add(CommandDef(
                o.getString("prefix"),
                o.getString("verb"),
                o.getString("description"),
            ))
        }
        return out
    }
}

data class CommandDef(val prefix: String, val verb: String, val description: String)
