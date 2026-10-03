package io.github.osphvdhwj.aves.ai

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import org.json.JSONArray

class MainActivity : Activity() {

    private lateinit var input: EditText
    private lateinit var log: TextView

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
        appendLog("Interface version: " + AiCompanionService.INTERFACE_VERSION)
        appendLog("Capabilities: " + AiCompanionService.CAP_ECHO)
        appendLog("---")
    }

    private fun onRun() {
        val text = input.text.toString()
        val cmd = CommandParser.parse(text)
        appendLog("input:    " + cmd.raw)
        appendLog("prefix:   '" + cmd.prefix + "'")
        appendLog("verb:     " + cmd.verb)
        appendLog("freeText: " + cmd.freeText)
        appendLog("---")
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
