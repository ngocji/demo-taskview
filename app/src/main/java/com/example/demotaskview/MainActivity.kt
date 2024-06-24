package com.example.demotaskview

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import com.example.demotaskview.taskview.TaskViewComponent


class MainActivity : FragmentActivity() {
    lateinit var taskViewComponent: TaskViewComponent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setUpTaskView(findViewById(R.id.card))
    }

    private fun setUpTaskView(parent: ViewGroup) {
        taskViewComponent = TaskViewComponent(this)
        taskViewComponent.initTask(this)

        taskViewComponent.attachToViewGroup1(parent)
        taskViewComponent.attachToViewGroup2(findViewById(R.id.view_container_2))

        val intent = Intent(Settings.ACTION_SETTINGS)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        taskViewComponent.create1(this, intent)

        val intent2 = Intent().apply {
            setComponent(ComponentName("com.android.car.radio", "com.android.car.radio.RadioActivity"))
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        taskViewComponent.create2(activity = this, intent2)

    }
}