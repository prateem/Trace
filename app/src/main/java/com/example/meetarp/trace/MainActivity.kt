package com.example.meetarp.trace

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.CheckBox
import androidx.appcompat.app.AppCompatDelegate
import com.meetarp.trace.TraceContainer

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        val toggle = findViewById<CheckBox>(R.id.toggle)
        val traceContainer = findViewById<TraceContainer>(R.id.traceContainer)

        toggle.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked)
                traceContainer.startShimmer()
            else
                traceContainer.stopShimmer()
        }
    }

}