package io.github.osphvdhwj.aves.ai

import android.app.Activity
import android.os.Bundle
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(TextView(this).apply {
            text = "Aves AI Companion\n\nNo models installed."
            textSize = 18f
            setPadding(64, 128, 64, 64)
        })
    }
}
