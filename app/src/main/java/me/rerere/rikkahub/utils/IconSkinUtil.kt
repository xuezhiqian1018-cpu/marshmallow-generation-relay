package me.rerere.rikkahub.utils

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import me.rerere.rikkahub.data.datastore.IconSkin

object IconSkinUtil {
    fun apply(context: Context, skin: IconSkin) {
        val pm = context.applicationContext.packageManager
        val packageName = context.applicationContext.packageName
        IconSkin.entries.forEach { option ->
            val state = if (option == skin) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
            pm.setComponentEnabledSetting(
                ComponentName(packageName, "$packageName.${option.aliasSuffix}"),
                state,
                PackageManager.DONT_KILL_APP
            )
        }
    }
}
