/*
 * Copyright 2015 AndroidPlot.com
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.example.androidmagicwand

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.CheckBox
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.androidplot.util.Redrawer
import com.androidplot.xy.XYPlot
import com.example.androidmagicwand.mpchartplot.SimpleAPRSeriesTable

// Monitor the phone's orientation sensor and plot the resulting azimuth pitch and roll values.
// See: http://developer.android.com/reference/android/hardware/SensorEvent.html
class SensorCheckActivity : Activity(), SensorEventListener {
    private var sensorMgr: SensorManager? = null
//    private var sensorMap = mutableMapOf<String,Sensor>()
    private var dataseriesMap= mutableMapOf<Int, SimpleAPRSeriesTable>()
    private lateinit var showStepText:TextView
    private var hwAcceleratedCb: CheckBox? = null
    private var showFpsCb: CheckBox? = null

    private var redrawer: Redrawer? = null
//    private val sensorList= listOf(Sensor.TYPE_ACCELEROMETER)
    /** Called when the activity is first created.  */
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check if the permission is already granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION)
            != PackageManager.PERMISSION_GRANTED) {

            // Permission is not granted, request it
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACTIVITY_RECOGNITION),
                REQUEST_ACTIVITY_RECOGNITION_PERMISSION);
        }


        setContentView(R.layout.sensor_check)
        showStepText = findViewById<View>(R.id.showStepText) as TextView
        dataseriesMap.put(Sensor.TYPE_ACCELEROMETER, SimpleAPRSeriesTable("ACC", findViewById<View>(R.id.dynamic_acc_Plot) as XYPlot,
            stepValue = 5.0))
        dataseriesMap.put(Sensor.TYPE_ACCELEROMETER_UNCALIBRATED, SimpleAPRSeriesTable("ACC2", findViewById<View>(R.id.dynamic_acc2_Plot) as XYPlot,
            labeles = listOf("X","Y","Z","x","y","z"),
            stepValue = 5.0))
        dataseriesMap.put(Sensor.TYPE_GRAVITY, SimpleAPRSeriesTable("GRAVITY", findViewById<View>(R.id.dynamic_grav_Plot) as XYPlot,
            stepValue = 5.0))
        dataseriesMap.put(Sensor.TYPE_GYROSCOPE, SimpleAPRSeriesTable("GYRO", findViewById<View>(R.id.dynamic_gyro_Plot) as XYPlot,
            boundry = Pair(-Math.ceil(Math.PI*5).toInt(),Math.ceil(Math.PI*5).toInt()),
            unitlabel = "Rad",
            stepValue = 50.0
        ))
        dataseriesMap.put(Sensor.TYPE_GYROSCOPE_UNCALIBRATED, SimpleAPRSeriesTable("GYRO", findViewById<View>(R.id.dynamic_gyro2_Plot) as XYPlot,
            labeles = listOf("X","Y","Z","x","y","z"),
            boundry = Pair(-Math.ceil(Math.PI*5).toInt(),Math.ceil(Math.PI*5).toInt()),
            unitlabel = "Rad",
            stepValue = 50.0
        ))
        dataseriesMap.put(Sensor.TYPE_MAGNETIC_FIELD, SimpleAPRSeriesTable("MAGNET", findViewById<View>(R.id.dynamic_mag_Plot) as XYPlot,
//            labeles = listOf("X","Y","Z"),
            boundry = Pair(-300,300),
            unitlabel = "uT",
            stepValue = 50.0
        ))


        // setup checkboxes:
        hwAcceleratedCb = findViewById<View>(R.id.hwAccelerationCb) as CheckBox
        hwAcceleratedCb!!.setOnCheckedChangeListener { compoundButton, b ->
            dataseriesMap.forEach{ k, v ->
                if (b) {
//                aprLevelsPlot!!.setLayerType(View.LAYER_TYPE_NONE, null)
                    v?.dynamicPlotTable?.setLayerType(
                        View.LAYER_TYPE_NONE,
                        null
                    )
                } else {
//                aprLevelsPlot!!.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
                    v?.dynamicPlotTable?.setLayerType(
                        View.LAYER_TYPE_SOFTWARE,
                        null
                    )
                }
            }
        }
        showFpsCb = findViewById<View>(R.id.showFpsCb) as CheckBox
        showFpsCb!!.setOnCheckedChangeListener { compoundButton, b ->
            dataseriesMap.forEach{ k, v ->
                v.histStats.setAnnotatePlotEnabled(b)
            }

        }
        // register for orientation sensor events:
        sensorMgr = applicationContext.getSystemService(SENSOR_SERVICE) as SensorManager
        for (sensorkey in  dataseriesMap.keys){
            val sensor = sensorMgr!!.getDefaultSensor(sensorkey)
            dataseriesMap.get(sensorkey)?.sensor = sensor
            sensorMgr!!.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
        }
        sensorMgr!!.registerListener(this, sensorMgr!!.getDefaultSensor(Sensor.TYPE_STEP_COUNTER), SensorManager.SENSOR_DELAY_UI)
        redrawer = Redrawer(
            dataseriesMap.map { it.value.dynamicPlotTable },100f, false )
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_ACTIVITY_RECOGNITION_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission is granted, proceed with using the Step Counter sensor
                // Call the method or perform the actions that require the permission
            } else {
                // Permission is denied, handle accordingly (e.g., show an explanation, disable functionality)
            }
        }
    }
    public override fun onResume() {
        super.onResume()
        redrawer!!.start()
    }

    public override fun onPause() {
        redrawer!!.pause()
        super.onPause()
    }

    public override fun onDestroy() {
        redrawer!!.finish()
        super.onDestroy()
    }

    private fun cleanup() {
        // aunregister with the orientation sensor before exiting:
        sensorMgr!!.unregisterListener(this)
        finish()
    }

    // Called whenever a new orSensor reading is taken.
    @Synchronized
    override fun onSensorChanged(sensorEvent: SensorEvent) {

        dataseriesMap.get(sensorEvent.sensor.type)?.appendData(sensorEvent.values.toList())
        if (sensorEvent.sensor.type == Sensor.TYPE_STEP_COUNTER) {
            showStepText.text="Step:" + sensorEvent.values.joinToString(",")
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, i: Int) {
        // Not interested in this event
    }

    companion object{
        const val REQUEST_ACTIVITY_RECOGNITION_PERMISSION = 123
    }

}