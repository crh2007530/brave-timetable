package com.xingheyuzhuan.shiguangschedule.ui.campus

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import shiguangschedule.shared.generated.resources.Res
import shiguangschedule.shared.generated.resources.campus_laundry_open

@Composable
internal actual fun LaundryActions(onOpenProvider: () -> Unit) {
    TextButton(onClick = onOpenProvider) {
        Text(stringResource(Res.string.campus_laundry_open))
    }
}
