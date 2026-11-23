package com.xingheyuzhuan.shiguangschedule.ui.settings.notification

import android.app.NotificationManager
import android.content.Context
import android.os.PowerManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.core.app.NotificationManagerCompat
import com.xingheyuzhuan.shiguangschedule.data.model.AndroidAppSettingsModel
import com.xingheyuzhuan.shiguangschedule.data.model.AppSettingsModel
import com.xingheyuzhuan.shiguangschedule.data.model.AutoControlMode
import com.xingheyuzhuan.shiguangschedule.data.repository.AndroidAppSettingsRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.AppSettingsRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.CourseTableRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.koin.core.annotation.KoinViewModel
import kotlin.time.Duration.Companion.seconds

/**
 * 权限检查结果。
 */
data class PermissionsState(
    val notificationsEnabled: Boolean = false,
    val hasDndPermission: Boolean = false,
    val isIgnoringBatteryOptimizations: Boolean = false
)

data class NotificationSettingsUiState(
    val appSettings: AppSettingsModel = AppSettingsModel(),
    val androidSettings: AndroidAppSettingsModel = AndroidAppSettingsModel(),
    val isReady: Boolean = false
)

@KoinViewModel
class NotificationSettingsViewModel(
    private val appSettingsRepository: AppSettingsRepository,
    private val androidAppSettingsRepository: AndroidAppSettingsRepository,
    private val courseTableRepository: CourseTableRepository,
    private val context: Context
) : ViewModel() {

    val uiState: StateFlow<NotificationSettingsUiState> = combine(
        appSettingsRepository.getAppSettings(),
        androidAppSettingsRepository.getSettings()
    ) { appSettings, androidSettings ->
        NotificationSettingsUiState(
            appSettings = appSettings,
            androidSettings = androidSettings,
            isReady = true
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5.seconds),
        initialValue = NotificationSettingsUiState()
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val courseNames: StateFlow<List<String>> = appSettingsRepository.getAppSettings()
        .map { it.currentCourseTableId }
        .distinctUntilChanged()
        .flatMapLatest(courseTableRepository::getCoursesWithWeeksByTableId)
        .map { courses -> courses.map { it.course.name }.distinct().sorted() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5.seconds), emptyList())

    // 独立维护的权限状态，供 UI 监听
    private val _permissionsState = MutableStateFlow(PermissionsState())
    val permissionsState: StateFlow<PermissionsState> = _permissionsState.asStateFlow()

    /**
     * 刷新系统的权限状态（精确闹钟、勿扰、电池优化、实时动态）。
     * 建议在 UI 层的 ON_RESUME 时调用。
     */
    fun refreshPermissions() {
        viewModelScope.launch {
            val dnd = checkDndPermission()
            val battery = checkBatteryOptimization()

            _permissionsState.value = PermissionsState(
                notificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled(),
                hasDndPermission = dnd,
                isIgnoringBatteryOptimizations = battery
            )
        }
    }

    private fun checkDndPermission(): Boolean {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return notificationManager.isNotificationPolicyAccessGranted
    }

    private fun checkBatteryOptimization(): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    // 公共设置
    fun onReminderEnabledChanged(enabled: Boolean) {
        viewModelScope.launch {
            appSettingsRepository.updateReminderEnabled(enabled)
        }
    }

    fun onRemindBeforeMinutesChanged(minutes: Int) {
        viewModelScope.launch {
            appSettingsRepository.updateReminderLeadMinutes(minutes)
        }
    }

    // Android 专属设置
    fun onAutoControlModeChanged(mode: AutoControlMode) {
        viewModelScope.launch {
            androidAppSettingsRepository.updateSettings {
                it.copy(autoControlMode = mode)
            }
        }
    }

    fun onReminderCoursesChanged(courseNames: Set<String>?) {
        viewModelScope.launch {
            androidAppSettingsRepository.updateSettings {
                it.copy(reminderCourseNames = courseNames)
            }
        }
    }
}
