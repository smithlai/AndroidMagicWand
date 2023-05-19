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

import android.app.Activity
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.View
import android.widget.CheckBox
import com.androidplot.Plot
import com.androidplot.util.PlotStatistics
import com.androidplot.util.Redrawer
import com.androidplot.xy.BoundaryMode
import com.androidplot.xy.LineAndPointFormatter
import com.androidplot.xy.StepMode
import com.androidplot.xy.XYGraphWidget
import com.androidplot.xy.XYPlot
import com.example.androidmagicwand.mpchartplot.SimpleAPRSeriesTable
import java.text.DecimalFormat
import java.util.Arrays

// Monitor the phone's orientation sensor and plot the resulting azimuth pitch and roll values.
// See: http://developer.android.com/reference/android/hardware/SensorEvent.html
class SensorCheckActivity : Activity(), SensorEventListener {
    private var sensorMgr: SensorManager? = null
//    private var sensorMap = mutableMapOf<String,Sensor>()
    private var dataseriesMap= mutableMapOf<Int, SimpleAPRSeriesTable>()

    private var hwAcceleratedCb: CheckBox? = null
    private var showFpsCb: CheckBox? = null

    private var redrawer: Redrawer? = null
//    private val sensorList= listOf(Sensor.TYPE_ACCELEROMETER)
    /** Called when the activity is first created.  */
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.sensor_check)

        dataseriesMap.put(Sensor.TYPE_ACCELEROMETER, SimpleAPRSeriesTable(findViewById<View>(R.id.dynamic_acc_Plot) as XYPlot,
            stepValue = 5.0))
        dataseriesMap.put(Sensor.TYPE_GRAVITY, SimpleAPRSeriesTable(findViewById<View>(R.id.dynamic_grav_Plot) as XYPlot,
            stepValue = 5.0))
        dataseriesMap.put(Sensor.TYPE_GYROSCOPE, SimpleAPRSeriesTable(findViewById<View>(R.id.dynamic_gyro_Plot) as XYPlot,
            boundry = Pair(-180,359),
            unitlabel = "Rad",
            stepValue = 50.0
        ))

        // setup checkboxes:
        hwAcceleratedCb = findViewById<View>(R.id.hwAccelerationCb) as CheckBox
        hwAcceleratedCb!!.setOnCheckedChangeListener { compoundButton, b ->
            for (sensortype in dataseriesMap.keys) {
                if (b) {
//                aprLevelsPlot!!.setLayerType(View.LAYER_TYPE_NONE, null)
                    dataseriesMap.get(sensortype)?.dynamicPlotTable?.setLayerType(
                        View.LAYER_TYPE_NONE,
                        null
                    )
                } else {
//                aprLevelsPlot!!.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
                    dataseriesMap.get(sensortype)?.dynamicPlotTable?.setLayerType(
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

        redrawer = Redrawer(
            dataseriesMap.map { it.value.dynamicPlotTable },100f, false )
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
    }

    override fun onAccuracyChanged(sensor: Sensor, i: Int) {
        // Not interested in this event
    }

}