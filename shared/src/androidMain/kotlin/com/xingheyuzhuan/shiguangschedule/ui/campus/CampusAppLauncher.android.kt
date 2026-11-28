package com.xingheyuzhuan.shiguangschedule.ui.campus

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
internal actual fun rememberCampusAppLauncher(): (List<String>) -> Boolean {
    val context = LocalContext.current
    return remember(context) {
        { packages ->
            packages.any { packageName ->
                val intent = context.packageManager.getLaunchIntentForPackage(packageName)
                if (intent == null) {
                    false
                } else {
                    try {
                        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        true
                    } catch (_: ActivityNotFoundException) {
                        false
                    } catch (_: SecurityException) {
                        false
                    }
                }
            }
        }
    }
}
