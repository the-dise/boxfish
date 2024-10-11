package com.v2ray.ang.service

import android.content.Context
import android.util.Log
import com.v2ray.ang.AppConfig.ANG_PACKAGE
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ProcessService {
    private val s = ANG_PACKAGE
    private lateinit var process: Process

    fun runProcess(context: Context, cmd: MutableList<String>) {
        Log.d(s, cmd.toString())

        try {
            val proBuilder = ProcessBuilder(cmd)
            proBuilder.redirectErrorStream(true)
            process = proBuilder
                .directory(context.filesDir)
                .start()

            CoroutineScope(Dispatchers.IO).launch {
                Thread.sleep(50L)
                Log.d(s, "runProcess check")
                process.waitFor()
                Log.d(s, "runProcess exited")
            }
            Log.d(s, process.toString())

        } catch (e: Exception) {
            Log.d(s, e.toString())
        }
    }

    fun stopProcess() {
        try {
            Log.d(s, "runProcess destroy")
            process.destroy()
        } catch (e: Exception) {
            Log.d(s, e.toString())
        }
    }
}