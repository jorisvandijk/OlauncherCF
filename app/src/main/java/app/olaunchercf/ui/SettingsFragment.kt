package app.olaunchercf.ui

import SettingsTheme
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import app.olaunchercf.BuildConfig
import app.olaunchercf.MainActivity
import app.olaunchercf.MainViewModel
import app.olaunchercf.R
import app.olaunchercf.data.Constants
import app.olaunchercf.data.Constants.AppDrawerFlag
import app.olaunchercf.data.Constants.Theme.*
import app.olaunchercf.data.Prefs
import app.olaunchercf.databinding.FragmentSettingsBinding
import app.olaunchercf.helper.*
import app.olaunchercf.listener.DeviceAdmin
import app.olaunchercf.ui.compose.SettingsComposable.SettingsAppSelector
import app.olaunchercf.ui.compose.SettingsComposable.SettingsArea
import app.olaunchercf.ui.compose.SettingsComposable.SettingsItem
import app.olaunchercf.ui.compose.SettingsComposable.SettingsNumberItem
import app.olaunchercf.ui.compose.SettingsComposable.SettingsToggle
import app.olaunchercf.ui.compose.SettingsComposable.SettingsTopView
import app.olaunchercf.ui.compose.SettingsComposable.SimpleTextButton

class SettingsFragment : Fragment() {

    private lateinit var prefs: Prefs
    private lateinit var viewModel: MainViewModel
    private lateinit var deviceManager: DevicePolicyManager
    private lateinit var componentName: ComponentName

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val offset = 5

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        prefs = Prefs(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        if (prefs.firstSettingsOpen) {
            prefs.firstSettingsOpen = false
        }

        binding.testView.setContent {

            val isDark = when (prefs.appTheme) {
                Light -> false
                Dark -> true
                System -> isSystemInDarkTheme()
            }

            SettingsTheme(isDark) {
                Settings((prefs.textSize - offset).sp)
            }
        }
    }

    @Composable
    private fun Settings(fontSize: TextUnit = TextUnit.Unspecified) {
        val selected = remember { mutableStateOf("") }
        /*val fs = remember { mutableStateOf(fontSize) }

        val titleFs = if (fs.value.isSpecified) {
            (fs.value.value * 2).sp
        } else fs.value

        val iconFs = if (fs.value.isSpecified) {
            (fs.value.value * 1.5).sp
        } else fs.value */

        val changeLauncherText = if (isOlauncherDefault(requireContext())) {
            R.string.change_default_launcher
        } else {
           R.string.set_as_default_launcher
        }

        Column {
            SettingsTopView(
                stringResource(R.string.app_name),
                onClick = { openAppInfo(requireContext(), android.os.Process.myUserHandle(), BuildConfig.APPLICATION_ID) },
            ) {
                SimpleTextButton(stringResource(R.string.hidden_apps) ) {
                    showHiddenApps()
                }
                SimpleTextButton(stringResource(changeLauncherText) ) {
                    resetDefaultLauncher(requireContext())
                }
            }
            SettingsArea(
                title = stringResource(R.string.appearance),
                selected = selected,
                items = arrayOf(
                    { _, onChange ->
                        SettingsToggle(
                            title = stringResource(R.string.auto_show_keyboard),
                            onChange = onChange,

                            state = remember { mutableStateOf(prefs.autoShowKeyboard) },
                        ) { toggleKeyboardText() }
                    },
                    { _, onChange ->
                        SettingsToggle(
                            title = stringResource(R.string.status_bar),
                            onChange = onChange,

                            state = remember { mutableStateOf(prefs.showStatusBar) },
                        ) { toggleStatusBar() }
                    },
                    { open, onChange ->
                        SettingsItem(
                            title = stringResource(R.string.theme_mode),
                            open = open,
                            onChange = onChange,

                            currentSelection = remember { mutableStateOf(prefs.appTheme) },
                            values = arrayOf(System, Light, Dark),
                            onSelect = { j -> setTheme(j) }
                        )
                    },
                    { open, onChange ->
                        SettingsItem(
                            open = open,
                            onChange = onChange,

                            title = stringResource(R.string.app_language),
                            currentSelection = remember { mutableStateOf(prefs.language) },
                            values = Constants.Language.values(),
                            onSelect = { j -> setLang(j) }
                        )
                    },
                    { open, onChange ->
                        SettingsNumberItem(
                            title = stringResource(R.string.app_text_size),
                            open = open,

                            onChange = onChange,
                            currentSelection = remember { mutableStateOf(prefs.textSize) },
                            min = Constants.TEXT_SIZE_MIN,
                            max = Constants.TEXT_SIZE_MAX,
                            onValueChange = { }, // newSize -> fs.value = (newSize - offset).sp },
                            onSelect = { f -> setTextSize(f) }
                        )
                    }
                )
            )
            SettingsArea(title = stringResource(R.string.homescreen),
                selected = selected,
                items = arrayOf(
                    { open, onChange ->
                        SettingsNumberItem(
                            title = stringResource(R.string.apps_on_home_screen),
                            open = open,

                            onChange = onChange,
                            currentSelection = remember { mutableStateOf(prefs.homeAppsNum) },
                            min = 0,
                            max = Constants.MAX_HOME_APPS,
                            onSelect = { j -> updateHomeAppsNum(j) }
                        )
                    },
                    { _, onChange ->
                        SettingsToggle(
                            title = stringResource(R.string.show_time),
                            onChange = onChange,

                            state = remember { mutableStateOf(prefs.showTime) }
                        ) { toggleShowTime() }
                    },
                    { _, onChange ->
                        SettingsToggle(
                            title = stringResource(R.string.show_date),
                            onChange = onChange,

                            state = remember { mutableStateOf(prefs.showDate) }
                        ) { toggleShowDate() }
                    },
                    { _, onChange ->
                        SettingsToggle(
                            title = stringResource(R.string.lock_home_apps),
                            onChange = onChange,

                            state = remember { mutableStateOf(prefs.homeLocked) }
                        ) { prefs.homeLocked = !prefs.homeLocked }
                    },
                    { _, onChange ->
                        SettingsToggle(
                            title = stringResource(R.string.extend_home_apps_area),
                            onChange = onChange,

                            state = remember { mutableStateOf(prefs.extendHomeAppsArea) }
                        ) { prefs.extendHomeAppsArea = !prefs.extendHomeAppsArea }
                    },
                )
            )
            SettingsArea(title = stringResource(R.string.alignment),
                selected = selected,
                items = arrayOf(
                    { open, onChange ->
                        SettingsItem(
                            title = stringResource(R.string.home_alignment),
                            open = open,
                            onChange = onChange,

                            currentSelection = remember { mutableStateOf(prefs.homeAlignment) },
                            values = arrayOf(Constants.Gravity.Left, Constants.Gravity.Center, Constants.Gravity.Right),
                            onSelect = { gravity -> setHomeAlignment(gravity) }
                        )
                    },
                    { _, onChange ->
                        SettingsToggle(
                            title = stringResource(R.string.home_alignment_bottom),
                            onChange = onChange,

                            state = remember { mutableStateOf(prefs.homeAlignmentBottom) }
                        ) { toggleHomeAppsBottom() }
                    },
                    { open, onChange ->
                        SettingsItem(
                            title = stringResource(R.string.clock_alignment),
                            open = open,
                            onChange = onChange,

                            currentSelection = remember { mutableStateOf(prefs.clockAlignment) },
                            values = arrayOf(Constants.Gravity.Left, Constants.Gravity.Center, Constants.Gravity.Right),
                            onSelect = { gravity -> setClockAlignment(gravity) }
                        )
                    },
                    { open, onChange ->
                        SettingsItem(
                            title = stringResource(R.string.drawer_alignment),
                            open = open,
                            onChange = onChange,

                            currentSelection = remember { mutableStateOf(prefs.drawerAlignment) },
                            values = arrayOf(Constants.Gravity.Left, Constants.Gravity.Center, Constants.Gravity.Right),
                            onSelect = { j -> viewModel.updateDrawerAlignment(j) }
                        )
                    },
                )
            )
            SettingsArea(title = stringResource(R.string.gestures),
                selected = selected,
                items = arrayOf(
                    { _, _ ->
                        SettingsAppSelector(
                            title = stringResource(R.string.swipe_left_app),
                            currentSelection = remember {
                                mutableStateOf(prefs.appSwipeLeft.appLabel.ifEmpty { "Camera" })
                            },

                            onClick = { updateGesture(AppDrawerFlag.SetSwipeLeft) },
                            active = prefs.swipeLeftEnabled,
                        )
                    },
                    { _, _ ->
                        SettingsAppSelector(
                            title = stringResource(R.string.swipe_right_app),
                            currentSelection = remember {
                                mutableStateOf(prefs.appSwipeRight.appLabel.ifEmpty { "Phone" })
                            },

                            onClick = { updateGesture(AppDrawerFlag.SetSwipeRight) },
                            active = prefs.swipeRightEnabled,
                        )
                    },
                    { _, _ ->
                        SettingsAppSelector(
                            title = stringResource(R.string.clock_click_app),
                            currentSelection =
                                remember { mutableStateOf(prefs.appClickClock.appLabel.ifEmpty { "Clock" }) },
                            onClick = { updateGesture(AppDrawerFlag.SetClickClock) },

                            active = prefs.clickClockEnabled,
                        )
                    },
                    { _, _ ->
                        SettingsAppSelector(
                            title = stringResource(R.string.date_click_app),
                            currentSelection =
                                remember { mutableStateOf(prefs.appClickDate.appLabel.ifEmpty { "Calendar" }) },
                            onClick = { updateGesture(AppDrawerFlag.SetClickDate) },

                            active = prefs.clickDateEnabled,
                        )
                    },
                    { _, onChange ->
                        SettingsToggle(
                            title = stringResource(R.string.double_tap_to_lock_screen),
                            onChange = onChange,

                            state = remember { mutableStateOf(prefs.lockModeOn) }
                        ) { toggleLockMode() }
                    }
                )
            )
            Text(
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(10.dp, 5.dp),
                text = "Version: ${requireContext().packageManager.getPackageInfo(requireContext().packageName, 0).versionName}",

                color = Color.DarkGray
            )
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        prefs = Prefs(requireContext())
        viewModel = activity?.run {
            ViewModelProvider(this).get(MainViewModel::class.java)
        } ?: throw Exception("Invalid Activity")

        viewModel.isOlauncherDefault()

        deviceManager = context?.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        componentName = ComponentName(requireContext(), DeviceAdmin::class.java)
        checkAdminPermission()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setHomeAlignment(gravity: Constants.Gravity) {
        prefs.homeAlignment = gravity
        viewModel.updateHomeAppsAlignment(gravity, prefs.homeAlignmentBottom)
    }

    private fun toggleHomeAppsBottom() {
        val onBottom  = !prefs.homeAlignmentBottom

        prefs.homeAlignmentBottom = onBottom
        viewModel.updateHomeAppsAlignment(prefs.homeAlignment, onBottom)
    }

    private fun setClockAlignment(gravity: Constants.Gravity) {
        prefs.clockAlignment = gravity
        viewModel.updateClockAlignment(gravity)
    }

    private fun toggleSwipeLeft() {
        prefs.swipeLeftEnabled = !prefs.swipeLeftEnabled
        if (prefs.swipeLeftEnabled) {
            //binding.swipeLeftApp.setTextColor(requireContext().getColorFromAttr(R.attr.primaryColor))
            showToastShort(requireContext(), "Swipe left app enabled")
        } else {
            //binding.swipeLeftApp.setTextColor(requireContext().getColorFromAttr(R.attr.primaryColorTrans50))
            showToastShort(requireContext(), "Swipe left app disabled")
        }
    }

    private fun toggleSwipeRight() {
        prefs.swipeRightEnabled = !prefs.swipeRightEnabled
        if (prefs.swipeRightEnabled) {
            //binding.swipeRightApp.setTextColor(requireContext().getColorFromAttr(R.attr.primaryColor))
            showToastShort(requireContext(), "Swipe right app enabled")
        } else {
            //binding.swipeRightApp.setTextColor(requireContext().getColorFromAttr(R.attr.primaryColorTrans50))
            showToastShort(requireContext(), "Swipe right app disabled")
        }
    }

    private fun toggleStatusBar() {
        val showStatusbar = !prefs.showStatusBar
        prefs.showStatusBar = showStatusbar
        if (showStatusbar) showStatusBar(requireActivity()) else hideStatusBar(requireActivity())
    }

    private fun toggleShowDate() {
        prefs.showDate = !prefs.showDate
        viewModel.setShowDate(prefs.showDate)
    }

    private fun toggleShowTime() {
        prefs.showTime = !prefs.showTime
        viewModel.setShowTime(prefs.showTime)
    }

    private fun showHiddenApps() {
        viewModel.getHiddenApps()
        findNavController().navigate(
            R.id.action_settingsFragment_to_appListFragment,
            bundleOf("flag" to AppDrawerFlag.HiddenApps.toString())
        )
    }

    private fun checkAdminPermission() {
        val isAdmin: Boolean = deviceManager.isAdminActive(componentName)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P)
            prefs.lockModeOn = isAdmin
    }

    private fun toggleLockMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            when {
                prefs.lockModeOn -> {
                    prefs.lockModeOn = false
                    deviceManager.removeActiveAdmin(componentName) // for backward compatibility
                }
                isAccessServiceEnabled(requireContext()) -> prefs.lockModeOn = true
                else -> {
                    showToastLong(requireContext(), "Please turn on accessibility service for Olauncher")
                    startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }
            }
        } else {
            val isAdmin: Boolean = deviceManager.isAdminActive(componentName)
            if (isAdmin) {
                deviceManager.removeActiveAdmin(componentName)
                prefs.lockModeOn = false
                showToastShort(requireContext(), "Admin permission removed.")
            } else {
                val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
                intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName)
                intent.putExtra(
                    DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    getString(R.string.admin_permission_message)
                )
                activity?.startActivityForResult(intent, Constants.REQUEST_CODE_ENABLE_ADMIN)
            }
        }
        //populateLockSettings()
    }

    private fun updateHomeAppsNum(homeAppsNum: Int) {
        prefs.homeAppsNum = homeAppsNum
        viewModel.homeAppsCount.value = homeAppsNum
    }

    private fun toggleKeyboardText() {
        prefs.autoShowKeyboard = !prefs.autoShowKeyboard
    }

    private fun setTheme(appTheme: Constants.Theme) {
        // if (AppCompatDelegate.getDefaultNightMode() == appTheme) return // TODO find out what this did
        prefs.appTheme = appTheme
        requireActivity().recreate()
    }

    private fun setLang(lang_int: Constants.Language) {

        prefs.language = lang_int
        requireActivity().recreate()
    }
    private fun setTextSize(size: Int) {
        prefs.textSize = size

        // restart activity
        /* activity?.let {
            val intent = Intent(context, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
            it.startActivity(intent)
            it.finish()
        } */
    }

    /*private fun setAppTheme(theme: Constants.Theme) {
        // if (AppCompatDelegate.getDefaultNightMode() == theme) return // TODO: find out what this did

        requireActivity().recreate()
    }*/

    private fun updateGesture(flag: AppDrawerFlag) {
        if ((flag == AppDrawerFlag.SetSwipeLeft) and !prefs.swipeLeftEnabled) {
            prefs.swipeLeftEnabled = true
        }

        if ((flag == AppDrawerFlag.SetSwipeRight) and !prefs.swipeRightEnabled) {
            prefs.swipeRightEnabled = true
        }

        if ((flag == AppDrawerFlag.SetClickClock) and !prefs.clickClockEnabled) {
            prefs.clickClockEnabled = true
        }

        if ((flag == AppDrawerFlag.SetClickDate) and !prefs.clickDateEnabled) {
            prefs.clickDateEnabled = true
        }

        viewModel.getAppList()
        findNavController().navigate(
            R.id.action_settingsFragment_to_appListFragment,
            bundleOf("flag" to flag.toString())
        )
    }
}
