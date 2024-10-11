package com.v2ray.ang.util

import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.v2ray.ang.AppConfig
import com.v2ray.ang.AppConfig.LOOPBACK
import com.v2ray.ang.R
import com.v2ray.ang.extension.responseLength
import kotlinx.coroutines.isActive
import libv2ray.Libv2ray
import java.io.IOException
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.Socket
import java.net.URL
import java.net.UnknownHostException
import kotlin.coroutines.coroutineContext

object SpeedtestUtil {

    private val tcpTestingSockets = ArrayList<Socket?>()

    suspend fun tcPing(url: String, port: Int): Long {
        var time = -1L
        for (k in 0 until 2) {
            val one = socketConnectTime(url, port)
            if (!coroutineContext.isActive) {
                break
            }
            if (one != -1L && (time == -1L || one < time)) {
                time = one
            }
        }
        return time
    }

    fun realPing(config: String): Long {
        return try {
            Libv2ray.measureOutboundDelay(config, Utils.getDelayTestUrl())
        } catch (e: Exception) {
            Log.d(AppConfig.ANG_PACKAGE, "realPing: $e")
            -1L
        }
    }

    private fun socketConnectTime(url: String, port: Int): Long {
        try {
            val socket = Socket()
            synchronized(this) {
                tcpTestingSockets.add(socket)
            }
            val start = System.currentTimeMillis()
            socket.connect(InetSocketAddress(url, port), 3000)
            val time = System.currentTimeMillis() - start
            synchronized(this) {
                tcpTestingSockets.remove(socket)
            }
            socket.close()
            return time
        } catch (e: UnknownHostException) {
            e.printStackTrace()
        } catch (e: IOException) {
            Log.d(AppConfig.ANG_PACKAGE, "socketConnectTime IOException: $e")
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return -1
    }

    fun closeAllTcpSockets() {
        synchronized(this) {
            tcpTestingSockets.forEach {
                it?.close()
            }
            tcpTestingSockets.clear()
        }
    }

    fun testConnection(context: Context, port: Int): Triple<Long, String, Int> {
        var result: String
        var elapsed = -1L
        var conn: HttpURLConnection? = null
        var iconResId = R.drawable.ic_test_web_remove_24

        try {
            val url = URL(Utils.getDelayTestUrl())

            conn = url.openConnection(
                Proxy(
                    Proxy.Type.HTTP,
                    InetSocketAddress(LOOPBACK, port)
                )
            ) as HttpURLConnection
            conn.connectTimeout = 30000
            conn.readTimeout = 30000
            conn.setRequestProperty("Connection", "close")
            conn.instanceFollowRedirects = false
            conn.useCaches = false

            val start = SystemClock.elapsedRealtime()
            val code = conn.responseCode
            elapsed = SystemClock.elapsedRealtime() - start

            if (code == 204 || code == 200 && conn.responseLength == 0L) {
                result = context.getString(R.string.connection_test_available, elapsed)
                iconResId = R.drawable.ic_test_web_check_24
            } else {
                throw IOException(
                    context.getString(
                        R.string.connection_test_error_status_code,
                        code
                    )
                )
            }
        } catch (e: IOException) {
            // network exception
            Log.d(
                AppConfig.ANG_PACKAGE,
                "testConnection IOException: " + Log.getStackTraceString(e)
            )
            result = context.getString(R.string.connection_test_error, e.message)

        } catch (e: Exception) {
            // library exception, eg samsung
            Log.d(AppConfig.ANG_PACKAGE, "testConnection Exception: " + Log.getStackTraceString(e))
            result = context.getString(R.string.connection_test_error, e.message)
        } finally {
            conn?.disconnect()
        }

        return Triple(elapsed, result, iconResId)
    }

    fun getLibVersion(): String {
        return Libv2ray.checkVersionX()
    }

}
