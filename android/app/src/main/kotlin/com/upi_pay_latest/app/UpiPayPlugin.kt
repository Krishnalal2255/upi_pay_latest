package com.upi_pay_latest.app

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.Base64
import android.util.Log
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.PluginRegistry.ActivityResultListener
import io.flutter.plugin.common.PluginRegistry.Registrar
import java.io.ByteArrayOutputStream

class UpiPayPlugin internal constructor(registrar: Registrar, channel: MethodChannel) : ActivityResultListener, MethodChannel.MethodCallHandler {
    private val activity: Activity? = registrar.activity()

    private var result: MethodChannel.Result? = null
    private var requestCodeNumber = 201119
    var hasResponded = false

   fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        hasResponded = false
        this.result = result

        when (call.method) {
            "initiateTransaction" -> this.initiateTransaction(call)
            "getInstalledUpiApps" -> this.getInstalledUpiApps()
            else -> result.notImplemented()
        }
    }

    private fun initiateTransaction(call: MethodCall) {
        val app: String? = call.argument("app")
        val pa: String? = call.argument("pa")
        val pn: String? = call.argument("pn")
        val mc: String? = call.argument("mc")
        val tr: String? = call.argument("tr")
        val tn: String? = call.argument("tn")
        val am: String? = call.argument("am")
        val cu: String? = call.argument("cu")
        val url: String? = call.argument("url")

        try {
            var uriStr: String? = "upi://pay?pa=" + pa +
                    "&pn=" + Uri.encode(pn) +
                    "&tr=" + Uri.encode(tr) +
                    "&am=" + Uri.encode(am) +
                    "&cu=" + Uri.encode(cu)
            if (url != null) {
                uriStr += ("&url=" + Uri.encode(url))
            }
            if (mc != null) {
                uriStr += ("&mc=" + Uri.encode(mc))
            }
            if (tn != null) {
                uriStr += ("&tn=" + Uri.encode(tn))
            }
            uriStr += "&mode=00" // &orgid=000000"
            val uri = Uri.parse(uriStr)

            val intent = Intent(Intent.ACTION_VIEW, uri)
            intent.setPackage(app)

            val packageManager: PackageManager = activity!!.packageManager
            if (intent.resolveActivity(activity!!.packageManager) == null) {
                this.success("activity_unavailable")
                return
            }

            activity!!.startActivityForResult(intent, requestCodeNumber)
        } catch (ex: Exception) {
            Log.e("upi_pay", ex.toString())
            this.success("failed_to_open_app")
        }
    }

    private fun getInstalledUpiApps() {
        val uriBuilder = Uri.Builder()
        uriBuilder.scheme("upi").authority("pay")

        val uri = uriBuilder.build()
        val intent = Intent(Intent.ACTION_VIEW, uri)

        // Check if the activity is not null before proceeding
        if (activity == null) {
            result?.error("getInstalledUpiApps", "activity_unavailable", null)
            return
        }

        val packageManager: PackageManager = activity.packageManager

        try {
            val activities = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)

            // Convert the activities into a response that can be transferred over the channel.
            val activityResponse = activities.map {
                val packageName = it.activityInfo.packageName
                val drawable = packageManager.getApplicationIcon(packageName)

                val bitmap = getBitmapFromDrawable(drawable)
                val icon = if (bitmap != null) {
                    encodeToBase64(bitmap)
                } else {
                    null
                }

                mapOf(
                    "packageName" to packageName,
                    "icon" to icon,
                    "priority" to it.priority,
                    "preferredOrder" to it.preferredOrder
                )
            }

            result?.success(activityResponse)
        } catch (ex: Exception) {
            Log.e("upi_pay", ex.toString())
            result?.error("getInstalledUpiApps", "exception", ex)
        }
    }

    private fun encodeToBase64(image: Bitmap): String? {
        val byteArrayOS = ByteArrayOutputStream()
        image.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOS)
        return Base64.encodeToString(byteArrayOS.toByteArray(), Base64.NO_WRAP)
    }

    private fun getBitmapFromDrawable(drawable: Drawable): Bitmap? {
        val bmp: Bitmap =
            Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bmp
    }

    private fun success(o: String) {
        if (!hasResponded) {
            hasResponded = true
            result?.success(o)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        if (requestCodeNumber == requestCode && result != null) {
            if (data != null) {
                try {
                    val response = data.getStringExtra("response") ?: "invalid_response"
                    this.success(response)
                } catch (ex: Exception) {
                    this.success("invalid_response")
                }
            } else {
                this.success("user_cancelled")
            }
        }
        return true
    }

    companion object {
        @JvmStatic
        fun registerWith(registrar: Registrar) {
            val channel = MethodChannel(registrar.messenger(), "upi_pay")
            val plugin = UpiPayPlugin(registrar, channel)
            registrar.addActivityResultListener(plugin)
            channel.setMethodCallHandler(plugin)
        }
    }
}
