package com.example.androidschedulednotification

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.androidschedulednotification.databinding.MainActivityBinding
import com.example.notification.Notification
import com.example.notification.channelID
import com.example.notification.messageExtra
import com.example.notification.notificationID
import com.example.notification.titleExtra
import java.util.Calendar
import java.util.Date

class MainActivity : AppCompatActivity() {

    private lateinit var binding: MainActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = MainActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }

        // Set onClickListener for the submit button
        binding.submitButton.setOnClickListener {
            // Check if notification permissions are granted
            if (checkNotificationPermissions(this)) {
                // Schedule a notification
                scheduleNotification()
            }
        }
    }

    @SuppressLint("ScheduleExactAlarm")
    private fun scheduleNotification() {
        // Create an intent for the Notification BroadcastReceiver
        val intent = Intent(applicationContext, Notification::class.java)

        // Extract title and message from user input
        val title = binding.title.text.toString()
        val message = binding.message.text.toString()

        // Add title and message as extras to the intent
        intent.putExtra(titleExtra, title)
        intent.putExtra(messageExtra, message)

        // Create a PendingIntent for the broadcast
        val pendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            notificationID,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Get the AlarmManager service
        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager

        // On Android 12+ (API 31+), check if exact alarms are allowed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Toast.makeText(
                    this,
                    "Please allow exact alarms for this app to schedule notifications.",
                    Toast.LENGTH_LONG
                ).show()
                val intentSettings = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                startActivity(intentSettings)
                return
            }
        }

        // Get the selected time and schedule the notification
        val time = getTime()

        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                time,
                pendingIntent
            )
            // Show an alert dialog with information about the scheduled notification
            showAlert(time, title, message)
        } catch (e: SecurityException) {
            Log.e("MainActivity", "Exact alarm permission missing", e)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val intentSettings = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                startActivity(intentSettings)
            }
        }
    }

    private fun showAlert(time: Long, title: String, message: String) {
        // Format the time for display
        val date = Date(time)
        val dateFormat = android.text.format.DateFormat.getLongDateFormat(applicationContext)
        val timeFormat = android.text.format.DateFormat.getTimeFormat(applicationContext)

        // Create and show an alert dialog with notification details
        AlertDialog.Builder(this)
            .setTitle("Notification Scheduled")
            .setMessage(
                "Title: $title\nMessage: $message\nAt: ${dateFormat.format(date)} ${timeFormat.format(date)}"
            )
            .setPositiveButton("Okay") { _, _ -> }
            .show()
    }

    private fun getTime(): Long {
        // Get selected time from TimePicker and DatePicker
        val minute = binding.timePicker.minute
        val hour = binding.timePicker.hour
        val day = binding.datePicker.dayOfMonth
        val month = binding.datePicker.month
        val year = binding.datePicker.year

        // Create a Calendar instance and set the selected date and time
        val calendar = Calendar.getInstance()
        calendar.set(year, month, day, hour, minute)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        return calendar.timeInMillis
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        // Create a notification channel for devices running Android Oreo (API level 26) and above
        val name = "Notify Channel"
        val desc = "A Description of the Channel"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(channelID, name, importance)
        channel.description = desc

        // Get the NotificationManager service and create the channel
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    fun checkNotificationPermissions(context: Context): Boolean {
        // Check POST_NOTIFICATIONS runtime permission for Android 13+ (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    101
                )
                return false
            }
        }

        // Check if notifications are enabled
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager

            val isEnabled = notificationManager.areNotificationsEnabled()

            if (!isEnabled) {
                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                }
                context.startActivity(intent)
                return false
            }
        } else {
            val areEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()

            if (!areEnabled) {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = android.net.Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
                return false
            }
        }

        return true
    }
}
