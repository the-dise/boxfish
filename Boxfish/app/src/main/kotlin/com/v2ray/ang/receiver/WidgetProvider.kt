package com.v2ray.ang.receiver

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import com.v2ray.ang.AppConfig
import com.v2ray.ang.R
import com.v2ray.ang.service.V2RayServiceManager
import com.v2ray.ang.util.Utils

class WidgetProvider : AppWidgetProvider() {
    /**
     * This method is called every time the widget is updated.
     */
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        updateWidget(
            context,
            appWidgetManager,
            appWidgetIds,
            V2RayServiceManager.v2rayPoint.isRunning
        )
    }


    private fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
        isRunning: Boolean
    ) {
        val remoteViews = RemoteViews(context.packageName, R.layout.widget_switch)
        val intent = Intent(context, WidgetProvider::class.java)
        intent.action = AppConfig.BROADCAST_ACTION_WIDGET_CLICK
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            R.id.layout_switch,
            intent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        )
        remoteViews.setOnClickPendingIntent(R.id.layout_switch, pendingIntent)
        if (isRunning) {
            remoteViews.setInt(R.id.image_switch, "setImageResource", R.drawable.ic_widget_stop_24)
            remoteViews.setTextViewText(
                R.id.text_switch,
                context.getString(R.string.widget_service_stop)
            )
        } else {
            remoteViews.setInt(R.id.image_switch, "setImageResource", R.drawable.ic_widget_play_24)
            remoteViews.setTextViewText(
                R.id.text_switch,
                context.getString(R.string.widget_service_start)
            )
        }

        for (appWidgetId in appWidgetIds) {
            appWidgetManager.updateAppWidget(appWidgetId, remoteViews)
        }
    }

    /**
     * Receive broadcasts from widgets
     */
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (AppConfig.BROADCAST_ACTION_WIDGET_CLICK == intent.action) {
            if (V2RayServiceManager.v2rayPoint.isRunning) {
                Utils.stopVService(context)
            } else {
                Utils.startVServiceFromToggle(context)
            }
        } else if (AppConfig.BROADCAST_ACTION_ACTIVITY == intent.action) {
            AppWidgetManager.getInstance(context)?.let { manager ->
                when (intent.getIntExtra("key", 0)) {
                    AppConfig.MSG_STATE_RUNNING, AppConfig.MSG_STATE_START_SUCCESS -> {
                        updateWidget(
                            context,
                            manager,
                            manager.getAppWidgetIds(
                                ComponentName(
                                    context,
                                    WidgetProvider::class.java
                                )
                            ),
                            true
                        )
                    }

                    AppConfig.MSG_STATE_NOT_RUNNING, AppConfig.MSG_STATE_START_FAILURE, AppConfig.MSG_STATE_STOP_SUCCESS -> {
                        updateWidget(
                            context,
                            manager,
                            manager.getAppWidgetIds(
                                ComponentName(
                                    context,
                                    WidgetProvider::class.java
                                )
                            ),
                            false
                        )
                    }
                }
            }
        }
    }
}