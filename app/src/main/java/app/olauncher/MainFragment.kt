package app.olauncher

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import kotlinx.android.synthetic.main.main_fragment.*
import java.lang.reflect.Method


class MainFragment : Fragment(), View.OnClickListener, View.OnLongClickListener {

    private lateinit var viewModel: MainViewModel
    private lateinit var prefs: Prefs

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.main_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        prefs = Prefs(requireContext())
        viewModel = activity?.run {
            ViewModelProvider(this).get(MainViewModel::class.java)
        } ?: throw Exception("Invalid Activity")

        populateHomeApps()
        initClickListeners()
        initObservers()

        mainLayout.setOnTouchListener(getSwipeGestureListener(requireContext()))
    }

    override fun onResume() {
        super.onResume()
        populateHomeApps()
        viewModel.isOlauncherDefault()
    }

    private fun populateHomeApps() {
        val pm = requireContext().packageManager
        if (isPackageInstalled(prefs.appPackage1, pm))
            homeApp1.text = prefs.appName1
        else {
            homeApp1.text = ""
            prefs.appName1 = ""
            prefs.appPackage1 = ""
        }
        if (isPackageInstalled(prefs.appPackage2, pm))
            homeApp2.text = prefs.appName2
        else {
            homeApp2.text = ""
            prefs.appName2 = ""
            prefs.appPackage2 = ""
        }
        if (isPackageInstalled(prefs.appPackage3, pm))
            homeApp3.text = prefs.appName3
        else {
            homeApp3.text = ""
            prefs.appName3 = ""
            prefs.appPackage3 = ""
        }
        if (isPackageInstalled(prefs.appPackage4, pm))
            homeApp4.text = prefs.appName4
        else {
            homeApp4.text = ""
            prefs.appName4 = ""
            prefs.appPackage4 = ""
        }

    }

    private fun initClickListeners() {
        homeApp1.setOnClickListener(this)
        homeApp2.setOnClickListener(this)
        homeApp3.setOnClickListener(this)
        homeApp4.setOnClickListener(this)
        homeApp1.setOnLongClickListener(this)
        homeApp2.setOnLongClickListener(this)
        homeApp3.setOnLongClickListener(this)
        homeApp4.setOnLongClickListener(this)

        date.setOnClickListener(this)
        clock.setOnClickListener(this)
        setDefaultLauncher.setOnClickListener(this)
    }

    private fun initObservers() {
        viewModel.refreshHome.observe(viewLifecycleOwner, Observer<Any> {
            populateHomeApps()
        })

        viewModel.isOlauncherDefault.observe(viewLifecycleOwner, Observer<Boolean> {
            if (it) setDefaultLauncher.visibility = View.GONE
            else setDefaultLauncher.visibility = View.VISIBLE
        })
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.homeApp1 -> if (prefs.appPackage1.isEmpty()) onLongClick(view)
            else launchAppEvent(prefs.appName1, prefs.appPackage1)

            R.id.homeApp2 -> if (prefs.appPackage2.isEmpty()) onLongClick(view)
            else launchAppEvent(prefs.appName2, prefs.appPackage2)

            R.id.homeApp3 -> if (prefs.appPackage3.isEmpty()) onLongClick(view)
            else launchAppEvent(prefs.appName3, prefs.appPackage3)

            R.id.homeApp4 -> if (prefs.appPackage4.isEmpty()) onLongClick(view)
            else launchAppEvent(prefs.appName4, prefs.appPackage4)

            R.id.clock -> openAlarmApp()
            R.id.date -> openCalendar()
            R.id.setDefaultLauncher -> showLauncherAppChooser()
        }
    }

    override fun onLongClick(view: View): Boolean {
        when (view.id) {
            R.id.homeApp1 -> showAppList(Constants.FLAG_SET_HOME_APP_1)
            R.id.homeApp2 -> showAppList(Constants.FLAG_SET_HOME_APP_2)
            R.id.homeApp3 -> showAppList(Constants.FLAG_SET_HOME_APP_3)
            R.id.homeApp4 -> showAppList(Constants.FLAG_SET_HOME_APP_4)
        }
        return true
    }

    private fun showAppList(flag: Int) {
        viewModel.getAppList()
        findNavController().navigate(
            R.id.action_mainFragment_to_appListFragment,
            bundleOf("flag" to flag)
        )
    }

    private fun openAlarmApp() {
        try {
            val intent = Intent(AlarmClock.ACTION_SHOW_ALARMS)
            startActivity(intent)
        } catch (e: java.lang.Exception) {
            Log.d("TAG", e.toString())
        }
    }

    private fun openCalendar() {
        try {
            val intent = Intent(Intent.ACTION_MAIN)
            intent.addCategory(Intent.CATEGORY_APP_CALENDAR)
            startActivity(intent)
        } catch (e: java.lang.Exception) {

        }
    }

    private fun launchAppEvent(appName: String, packageName: String) {
        viewModel.selectedApp(AppModel(appName, packageName), Constants.FLAG_LAUNCH_APP)
    }

    // Source: https://stackoverflow.com/a/51132142
    @SuppressLint("WrongConstant", "PrivateApi")
    private fun expandNotificationDrawer(context: Context) {
        try {
            val statusBarService = context.getSystemService("statusbar")
            val methodName = "expandNotificationsPanel"
            val statusBarManager: Class<*> = Class.forName("android.app.StatusBarManager")
            val method: Method = statusBarManager.getMethod(methodName)
            method.invoke(statusBarService)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun openDialerApp() {
        try {
            val sendIntent = Intent(Intent.ACTION_DIAL)
            startActivity(sendIntent)
        } catch (e: java.lang.Exception) {

        }
    }

    private fun openCameraApp() {
        try {
            val sendIntent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
            startActivity(sendIntent)
        } catch (e: java.lang.Exception) {

        }
    }

    private fun showLauncherAppChooser() {
        if (prefs.launcherReset) {
            resetDefaultLauncher(requireContext())
            val intent = Intent(Intent.ACTION_MAIN, null)
            intent.addCategory(Intent.CATEGORY_HOME)
            startActivity(intent)
            return
        }
        // Open settings because default launcher cannot be reset programmatically
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
        } else {
            Toast.makeText(
                requireContext(),
                "Search for launcher or home app",
                Toast.LENGTH_LONG
            ).show()
            Intent(Settings.ACTION_SETTINGS)
        }
        startActivity(intent)
    }

    private fun getSwipeGestureListener(context: Context): View.OnTouchListener {
        return object : OnSwipeTouchListener(context) {
            override fun onSwipeLeft() {
                super.onSwipeLeft()
                openCameraApp()
            }

            override fun onSwipeRight() {
                super.onSwipeRight()
                openDialerApp()
            }

            override fun onSwipeUp() {
                super.onSwipeUp()
                showAppList(Constants.FLAG_LAUNCH_APP)
            }

            override fun onSwipeDown() {
                super.onSwipeDown()
                expandNotificationDrawer(context)
            }

            override fun onLongClick() {
                super.onLongClick()
                openAppInfo(requireContext(), BuildConfig.APPLICATION_ID)
            }
        }
    }
}