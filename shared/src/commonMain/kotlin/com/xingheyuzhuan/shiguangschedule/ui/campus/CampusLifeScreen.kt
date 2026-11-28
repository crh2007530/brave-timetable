package com.xingheyuzhuan.shiguangschedule.ui.campus

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.xingheyuzhuan.shiguangschedule.ui.components.LocalScaffoldPadding
import com.xingheyuzhuan.shiguangschedule.ui.schoolselection.web.XatuImport
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import shiguangschedule.shared.generated.resources.*

private data class CampusService(
    val title: StringResource,
    val detail: StringResource,
    val packages: List<String> = emptyList(),
    val webUrl: String? = null
)

private val dailyServices = listOf(
    CampusService(Res.string.campus_shower, Res.string.campus_cloudman, listOf("com.jhj.cloudman")),
    CampusService(
        Res.string.campus_laundry,
        Res.string.campus_ujing,
        listOf("com.midea.vm.washer"),
        "https://r.app.xiaomi.com/details?id=com.midea.vm.washer"
    ),
    CampusService(
        Res.string.campus_water,
        Res.string.campus_shehuitong,
        listOf("com.eg.android.AlipayGphone"),
        "https://app.mi.com/details?id=com.eg.android.AlipayGphone"
    ),
    CampusService(
        Res.string.campus_ujing_water,
        Res.string.campus_ujing,
        listOf("com.midea.vm.washer"),
        "https://r.app.xiaomi.com/details?id=com.midea.vm.washer"
    ),
    CampusService(
        Res.string.campus_hairdryer,
        Res.string.campus_ujing,
        listOf("com.midea.vm.washer"),
        "https://r.app.xiaomi.com/details?id=com.midea.vm.washer"
    )
)

private val studyServices = listOf(
    CampusService(Res.string.campus_grades, Res.string.campus_xatu, webUrl = XatuImport.loginUrl),
    CampusService(
        Res.string.campus_chaoxing, Res.string.campus_chaoxing_detail,
        listOf("com.chaoxing.mobile"), "https://app.chaoxing.com"
    ),
    CampusService(
        Res.string.campus_ucampus, Res.string.campus_ucampus_detail,
        listOf("cn.unipus.cloud", "cn.unipus.ucampus.student"), "https://uai.unipus.cn/official/download"
    ),
    CampusService(
        Res.string.campus_zhidao, Res.string.campus_zhidao_detail,
        listOf("com.able.wisdomtree"), "https://app.mi.com/details?id=com.able.wisdomtree"
    ),
    CampusService(
        Res.string.campus_pointhouse, Res.string.campus_pointhouse_detail,
        listOf("com.pointhouse.chiguan"), "https://www.pointhouse.cn/"
    ),
    CampusService(
        Res.string.campus_fif, Res.string.campus_fif_detail,
        listOf("com.fifedu.fifiplat"), "https://www.fifedu.com/iplat/html/index.html"
    )
)

@Composable
internal expect fun rememberCampusAppLauncher(): (List<String>) -> Boolean

@Composable
internal expect fun WaterBluetoothDiagnostic()

@Composable
internal expect fun WaterQrTest()

@Composable
internal expect fun LaundryActions(onOpenProvider: () -> Unit)

@Composable
internal fun CampusLifeScreen(onOpenDeadlines: () -> Unit) {
    val bottomPadding = LocalScaffoldPadding.current.calculateBottomPadding()
    val launchApp = rememberCampusAppLauncher()
    val uriHandler = LocalUriHandler.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var selectedService by rememberSaveable { mutableStateOf<Int?>(null) }
    val openService: (CampusService, String) -> Unit = { service, unavailable ->
        val opened = launchApp(service.packages) || service.webUrl?.let { url ->
            runCatching { uriHandler.openUri(url) }.isSuccess
        } == true
        if (!opened) scope.launch { snackbar.showSnackbar(unavailable) }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(Res.string.campus_title)) }) },
        snackbarHost = { SnackbarHost(snackbar, Modifier.padding(bottom = bottomPadding)) }
    ) { insets ->
        LazyColumn(
            modifier = Modifier.fillMaxWidth().padding(insets),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = bottomPadding + 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                CampusSectionTitle(Res.string.campus_daily)
            }
            items(dailyServices.size) { index ->
                val service = dailyServices[index]
                CampusServiceRow(service) {
                    selectedService = if (selectedService == index) null else index
                }
                if (selectedService == index) {
                    CampusDevicePanel(service, onRecharge = { openService(service, it) })
                }
            }
            item {
                CampusSectionTitle(Res.string.campus_study)
                CampusServiceRow(
                    CampusService(Res.string.campus_deadlines, Res.string.campus_deadlines_description)
                ) { onOpenDeadlines() }
            }
            items(studyServices) { service ->
                CampusServiceRow(service) { openService(service, it) }
            }
        }
    }
}

@Composable
private fun CampusDevicePanel(service: CampusService, onRecharge: (String) -> Unit) {
    val unavailable = stringResource(
        if (service.webUrl == null) Res.string.campus_install_cloudman else Res.string.campus_open_failed
    )
    Surface(
        modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth().padding(top = 4.dp),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (service.title == Res.string.campus_laundry) {
                LaundryActions { onRecharge(unavailable) }
                return@Column
            }
            Text(
                stringResource(Res.string.campus_device_connection_pending),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                stringResource(Res.string.campus_device_connection_explanation),
                style = MaterialTheme.typography.bodyMedium
            )
            TextButton(onClick = { onRecharge(unavailable) }) {
                Text(stringResource(Res.string.campus_recharge_provider))
            }
            if (service.detail == Res.string.campus_shehuitong) {
                WaterQrTest()
                Text(
                    stringResource(Res.string.campus_alipay),
                    style = MaterialTheme.typography.bodySmall
                )
                WaterBluetoothDiagnostic()
            }
        }
    }
}

@Composable
private fun CampusSectionTitle(title: StringResource) {
    Column(Modifier.widthIn(max = 600.dp).fillMaxWidth().padding(top = 8.dp)) {
        Text(
            text = stringResource(title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        HorizontalDivider()
    }
}

@Composable
private fun CampusServiceRow(service: CampusService, onOpen: (String) -> Unit) {
    val title = stringResource(service.title)
    val unavailable = stringResource(
        if (service.webUrl == null) Res.string.campus_install_cloudman else Res.string.campus_open_failed
    )
    Surface(
        onClick = { onOpen(unavailable) },
        modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        ListItem(
            headlineContent = { Text(title) },
            supportingContent = { Text(stringResource(service.detail)) },
            trailingContent = {
                Icon(vectorResource(Res.drawable.arrow_forward_24px), contentDescription = null)
            },
            colors = ListItemDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            )
        )
    }
}
