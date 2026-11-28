package com.xingheyuzhuan.shiguangschedule.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xingheyuzhuan.shiguangschedule.Destination
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import shiguangschedule.shared.generated.resources.Res
import shiguangschedule.shared.generated.resources.nav_exams
import shiguangschedule.shared.generated.resources.nav_campus_life
import shiguangschedule.shared.generated.resources.home_24px
import shiguangschedule.shared.generated.resources.calendar_today_24px
import shiguangschedule.shared.generated.resources.account_circle_24px
import shiguangschedule.shared.generated.resources.account_circle_filled_24px
import shiguangschedule.shared.generated.resources.nav_course_schedule
import shiguangschedule.shared.generated.resources.nav_settings
import shiguangschedule.shared.generated.resources.nav_today_schedule
import shiguangschedule.shared.generated.resources.view_agenda_24px
import shiguangschedule.shared.generated.resources.view_agenda_filled_24px
import shiguangschedule.shared.generated.resources.view_week_24px
import shiguangschedule.shared.generated.resources.view_week_filled_24px

@Immutable
private data class NavItemData(
    val label: String,
    val destination: Destination,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

/**
 * 定义一个 Local 用于在组件树中隐式传递 Scaffold 的 PaddingValues，
 */
val LocalScaffoldPadding = compositionLocalOf { PaddingValues(0.dp) }

/**
 * 底部悬浮导航栏自身固定的内容高度
 */
val BottomNavContentHeight = 64.dp

/**
 * 自适应导航栏组件
 */
@Composable
fun AdaptiveNavigationScaffold(
    currentDestination: Destination,
    onTabSelected: (Destination) -> Unit,
    modifier: Modifier = Modifier,
    showNavigation: Boolean = true,
    navHideFractionProvider: () -> Float = { 0f },
    navigationModifier: Modifier = Modifier,
    content: @Composable (PaddingValues) -> Unit
) {
    val todayLabel = stringResource(Res.string.nav_today_schedule)
    val courseLabel = stringResource(Res.string.nav_course_schedule)
    val examsLabel = stringResource(Res.string.nav_exams)
    val examsIcon = vectorResource(Res.drawable.calendar_today_24px)
    val campusLabel = stringResource(Res.string.nav_campus_life)
    val campusIcon = vectorResource(Res.drawable.home_24px)
    val settingsLabel = stringResource(Res.string.nav_settings)

    val todaySelectedIcon = vectorResource(Res.drawable.view_agenda_filled_24px)
    val todayUnselectedIcon = vectorResource(Res.drawable.view_agenda_24px)
    val courseSelectedIcon = vectorResource(Res.drawable.view_week_filled_24px)
    val courseUnselectedIcon = vectorResource(Res.drawable.view_week_24px)
    val settingsSelectedIcon = vectorResource(Res.drawable.account_circle_filled_24px)
    val settingsUnselectedIcon = vectorResource(Res.drawable.account_circle_24px)

    val navItems = remember(
        todayLabel, courseLabel, settingsLabel,
        todaySelectedIcon, todayUnselectedIcon,
        courseSelectedIcon, courseUnselectedIcon,
        settingsSelectedIcon, settingsUnselectedIcon, examsLabel, examsIcon, campusLabel, campusIcon
    ) {
        listOf(
            NavItemData(
                label = todayLabel,
                destination = Destination.TodaySchedule,
                selectedIcon = todaySelectedIcon,
                unselectedIcon = todayUnselectedIcon
            ),
            NavItemData(
                label = courseLabel,
                destination = Destination.CourseSchedule,
                selectedIcon = courseSelectedIcon,
                unselectedIcon = courseUnselectedIcon
            ),
            NavItemData(examsLabel, Destination.MyExams, examsIcon, examsIcon),
            NavItemData(campusLabel, Destination.CampusLife, campusIcon, campusIcon),
            NavItemData(
                label = settingsLabel,
                destination = Destination.Settings,
                selectedIcon = settingsSelectedIcon,
                unselectedIcon = settingsUnselectedIcon
            )
        )
    }

    val layoutType = NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(currentWindowAdaptiveInfoV2())

    // 获取系统的安全区高度
    val systemBottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Box(modifier = modifier.fillMaxSize()) {
        when (layoutType) {
            NavigationSuiteType.NavigationRail -> {
                Row(modifier = Modifier.fillMaxSize()) {
                    AnimatedVisibility(
                        visible = showNavigation,
                        enter = slideInHorizontally(initialOffsetX = { -it }) + fadeIn(tween(300)),
                        exit = slideOutHorizontally(targetOffsetX = { -it }) + fadeOut(tween(300))
                    ) {
                        NavigationRail(
                            containerColor = NavigationBarDefaults.containerColor,
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            navItems.forEach { item ->
                                key(item.destination) {
                                    val isSelected = currentDestination::class == item.destination::class
                                    NavigationRailItem(
                                        selected = isSelected,
                                        onClick = { if (!isSelected) onTabSelected(item.destination) },
                                        icon = {
                                            Icon(
                                                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                                contentDescription = item.label,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        },
                                        label = { Text(item.label, fontSize = 12.sp) }
                                    )
                                }
                            }
                        }
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        content(
                            PaddingValues(
                                top = statusBarTop,
                                bottom = systemBottomInset
                            )
                        )
                    }
                }
            }

            NavigationSuiteType.NavigationBar -> {
                val density = LocalDensity.current

                val verticalPadding = 24.dp
                val totalNavBarHeight = BottomNavContentHeight + verticalPadding + systemBottomInset
                val navBarTotalHeightPx = with(density) { totalNavBarHeight.toPx() }

                Box(modifier = Modifier.fillMaxSize()) {
                    val calculatedPadding = PaddingValues(
                        top = statusBarTop,
                        bottom = totalNavBarHeight
                    )

                    content(calculatedPadding)

                    AnimatedVisibility(
                        visible = showNavigation,
                        enter = slideInHorizontally(initialOffsetX = { -it / 3 }, animationSpec = tween(300)) + fadeIn(tween(300)),
                        exit = slideOutHorizontally(targetOffsetX = { -it / 3 }, animationSpec = tween(300)) + fadeOut(tween(300)),
                        modifier = Modifier.align(Alignment.BottomCenter)
                    ) {
                        Box(
                            modifier = Modifier
                                .wrapContentSize()
                                .then(navigationModifier)
                                .graphicsLayer {
                                    val fraction = navHideFractionProvider()
                                    translationY = navBarTotalHeightPx * fraction
                                    alpha = (1f - fraction).coerceIn(0f, 1f)
                                }
                                .windowInsetsPadding(WindowInsets.navigationBars)
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = NavigationBarDefaults.containerColor,
                                shadowElevation = 0.dp,
                                tonalElevation = 3.dp
                            ) {
                                Row(
                                    modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth()
                                        .padding(horizontal = 6.dp, vertical = 5.dp).selectableGroup(),
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    navItems.forEach { item ->
                                        key(item.destination) {
                                            BottomNavTab(
                                                item = item,
                                                isSelected = currentDestination == item.destination,
                                                onSelect = { onTabSelected(item.destination) },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            else -> {
                content(
                    PaddingValues(
                        top = statusBarTop,
                        bottom = systemBottomInset
                    )
                )
            }
        }
    }
}

@Composable
private fun BottomNavTab(
    item: NavItemData,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val contentColor = if (isSelected) colors.onSecondaryContainer else colors.onSurfaceVariant
    Column(
        modifier = modifier.height(54.dp).clip(CircleShape)
            .selectable(selected = isSelected, onClick = onSelect, role = Role.Tab),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(width = 42.dp, height = 28.dp)
                .background(if (isSelected) colors.secondaryContainer else colors.surface.copy(alpha = 0f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
        }
        Text(
            text = item.label,
            color = contentColor,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            softWrap = false
        )
    }
}
