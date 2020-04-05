package com.example.meetarp.trace

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.ViewGroup
import com.meetarp.trace.Trace

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val target = findViewById<ViewGroup>(R.id.traceTarget)
        val container = findViewById<ViewGroup>(R.id.traceContainer)

        val trace = Trace(this)
            .of(target, exclusions = listOf())
            .colored(android.R.color.darker_gray)
            .also { it.startShimmer() }

        container.addView(trace)
    }

}