package id.paniclabs.android.inappupdate

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallState
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.tasks.Task
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {


    private val tag = "MainActivity"
    private val myRequestCode = 11

    private lateinit var appUpdateManager: AppUpdateManager
    private lateinit var appUpdateInfoTask: Task<AppUpdateInfo>

    private var appUpdateType: Int = AppUpdateType.IMMEDIATE

    // Handling flexible update only
    private val listener = { _: InstallState ->
        // FIXME
        // Show module progress, log state, or install the update.
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Creates instance of the manager.
        appUpdateManager = AppUpdateManagerFactory.create(applicationContext)

        // Returns an intent object that you use to check for an update.
        appUpdateInfoTask = appUpdateManager.appUpdateInfo

        // Button click listener
        buttonFlexible.setOnClickListener {
            Log.d(tag, "buttonFlexible")
            appUpdateType = AppUpdateType.FLEXIBLE
            checkForUpdate()
        }

        buttonImmediate.setOnClickListener {
            Log.d(tag, "buttonImmediate")
            appUpdateType = AppUpdateType.IMMEDIATE
            checkForUpdate()
        }
    }

    override fun onResume() {
        super.onResume()

        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            // If the update is downloaded but not installed,
            // notify the user to complete the update.
            if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                popupDialogDownloadCompleted()
            }
            // Handle an immediate update still in progress.
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                // If an in-app update is already running, resume the update.
                appUpdateManager.startUpdateFlowForResult(
                    appUpdateInfo,
                    AppUpdateType.IMMEDIATE,
                    this,
                    myRequestCode
                );
            }
        }
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterListenerForFlexibleUpdate()
    }

    // Handling immediate update only
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        super.onActivityResult(requestCode, resultCode, data)

        Log.d(tag, "onActivityResult requestCode $requestCode, resultCode $resultCode")

        if (requestCode == myRequestCode) {

            if (resultCode != RESULT_OK) {
                Toast.makeText(applicationContext, "[Type:1] Update failed! Result code: $resultCode", Toast.LENGTH_LONG).show()
                Toast.makeText(applicationContext, "[Type:1] If the update is cancelled or fails, you can request to start the update again", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun checkForUpdate() {

        // Checks that the platform will allow the specified type of update.
        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->

            val availability = appUpdateInfo.updateAvailability()

            if (availability == UpdateAvailability.UPDATE_AVAILABLE) {
                // Is allow?
                if (appUpdateInfo.isUpdateTypeAllowed(appUpdateType) ) {
                    Toast.makeText(applicationContext, "[Type:$appUpdateType] checkForUpdate, update is available!", Toast.LENGTH_LONG).show()
                    startUpdate(appUpdateInfo)
                } else {
                    Toast.makeText(applicationContext, "[Type:$appUpdateType] checkForUpdate, update is not allowed!", Toast.LENGTH_LONG).show()
                }
            } else if (availability == UpdateAvailability.UPDATE_NOT_AVAILABLE) {
                Toast.makeText(applicationContext, "[Type:$appUpdateType] checkForUpdate, update is not available!", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(applicationContext, "[Type:$appUpdateType] checkForUpdate, unknown status $availability", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun startUpdate(appUpdateInfo: AppUpdateInfo) {

        if (appUpdateType == AppUpdateType.FLEXIBLE) {
            registerListenerForFlexibleUpdate()
        }

        appUpdateManager.startUpdateFlowForResult(
            // Pass the intent that is returned by 'getAppUpdateInfo()'.
            appUpdateInfo,
            // Or 'AppUpdateType.FLEXIBLE' for flexible updates.
            appUpdateType,
            // The current activity making the update request.
            this,
            // Include a request code to later monitor this update request.
            myRequestCode)
    }

    private fun registerListenerForFlexibleUpdate() {

        // Before starting an update, register a listener for updates.
        appUpdateManager.registerListener(listener)
    }

    private fun unregisterListenerForFlexibleUpdate() {

        // When status updates are no longer needed, unregister the listener.
        appUpdateManager.unregisterListener(listener)
    }

    private fun popupDialogDownloadCompleted() {
        // When you call appUpdateManager.completeUpdate() in the foreground, the platform displays a full-screen UI
        // which restart the app in the background. After the platform installs the update, the app restarts into
        // its main activity.
        Toast.makeText(applicationContext, "Download completed", Toast.LENGTH_LONG).show()
        appUpdateManager.completeUpdate()
    }

    private fun popupDialogFailed() {
        Toast.makeText(applicationContext, "Application update failed", Toast.LENGTH_LONG).show()
        unregisterListenerForFlexibleUpdate()
    }

    private fun popupDialogInstalled() {
        Toast.makeText(applicationContext, "Application updated", Toast.LENGTH_LONG).show()
        unregisterListenerForFlexibleUpdate()
    }
}
