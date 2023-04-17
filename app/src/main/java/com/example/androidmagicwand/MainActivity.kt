package com.example.androidmagicwand

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.androidplot.xy.*
import org.jetbrains.kotlinx.multik.ndarray.data.get


class MainActivity : AppCompatActivity(), SensorEventListener {
    private var sensorManager: SensorManager? = null
//    private var gyroSensor: Sensor? = null
    private var imuSensor: Sensor? = null
    private var gravSensor: Sensor? = null
    private var linearSensor: Sensor? = null
    private var rotationSensor: Sensor? = null

//    private var gyroTextView: TextView? = null
    private var imuTextView: TextView? = null
    private var gravTextView: TextView? = null
    private var linearTextView: TextView? = null
    private var orientationTextView: TextView? = null

    private var myplot: XYPlot? = null
    var yyy:TrajectorySeries? = null
    private var myThread: Thread? = null

    private val converter = MyConverter()
    private var prev_timestamp:Long = 0
    private var triggered:Long = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
//        gyroTextView = findViewById<TextView>(R.id.gyroTextView)
        imuTextView = findViewById<TextView>(R.id.imuTextView)
        gravTextView = findViewById<TextView>(R.id.gravTextView)
        linearTextView = findViewById<TextView>(R.id.linearTextView)
        orientationTextView = findViewById<TextView>(R.id.orientationTextView)
        // 取得 SensorManager 實例
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager

        // 取得陀螺儀 Sensor 實例
//        gyroSensor = sensorManager!!.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        // 取得 IMU Sensor 實例
        imuSensor = sensorManager!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        // 取得 Gravity Sensor 實例
        gravSensor = sensorManager!!.getDefaultSensor(Sensor.TYPE_GRAVITY)

        // 取得 Linear Sensor 實例
        linearSensor = sensorManager!!.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        // 取得 rotationsensor Sensor 實例
        rotationSensor = sensorManager!!.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

        myplot = findViewById(R.id.dynamicXYPlot) as XYPlot;
        yyy = TrajectorySeries();
        yyy?.setup(myplot)
    }

    public override fun onResume() {
        super.onResume()

        // 註冊陀螺儀和 IMU 監聽器
//        sensorManager!!.registerListener(this, gyroSensor, SensorManager.SENSOR_DELAY_NORMAL)
//        sensorManager!!.registerListener(this, imuSensor, SensorManager.SENSOR_DELAY_NORMAL)
//        sensorManager!!.registerListener(this, gravSensor, SensorManager.SENSOR_DELAY_NORMAL)
        // Normal: 130 ms
        // UI: 60 ms
        // Game: 15 ms
        // Fast: ?? ms
        sensorManager!!.registerListener(this, linearSensor, SensorManager.SENSOR_DELAY_GAME)
        sensorManager!!.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_GAME)

        // kick off the data generating thread:
//        myThread = Thread(yyy?.data)
//        myThread?.start()
    }

    public override fun onPause() {
        sensorManager!!.unregisterListener(this)
//        yyy?.data?.stopThread()
        super.onPause()

        // 取消註冊陀螺儀和 IMU 監聽器

    }

    override fun onSensorChanged(event: SensorEvent) {


        if (event.sensor.type == Sensor.TYPE_LINEAR_ACCELERATION) {
            converter.setLinear(event.values)
        }
        if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
            val current1=System.nanoTime()
            val dt = current1 - prev_timestamp
            prev_timestamp=current1
            val matrix = FloatArray(9)
            SensorManager.getRotationMatrixFromVector(matrix,event.values)

            val orientation = FloatArray(3)/*azimuth, pitch, roll*/
            SensorManager.getOrientation(matrix, orientation)

            converter.setupRotationMatrix(orientation)
//            val w_acc = converter.toEatrhFrame(converter.accel)
//            val w_grav = converter.toEatrhFrame(converter.grav)
            val current2=System.nanoTime()
            val w_linear = converter.toEatrhFrame(converter.linear)
            val current3=System.nanoTime()
            converter.add_earth_linear(w_linear, System.nanoTime())
            val current4=System.nanoTime()
            linearTextView!!.text = "LINEAR:\nX: ${dt / 1_000_000}\nY: ${(current2-current1) / 1_000_000}\nZ: ${(current3 - current2) / 1_000_000}\n" +
                    "W: ${(current4 - current3) / 1_000_000}"
            orientationTextView!!.text = "Accel: Linear: ${w_linear.get(0)}\n${w_linear.get(1)}\n${w_linear.get(2)}\n"
//            "${w_acc.get(0)}\n${w_acc.get(1)}\n${w_acc.get(2)}\n\n" +
//            "Grav: ${w_grav.get(0)}\n${w_grav.get(1)}\n${w_grav.get(2)}\n\n"
            if (System.nanoTime() - triggered > 10L*1_000_000_000L) {
                // 10 second interval
                val stroke = converter.stroke
                stroke?.apply {
                    yyy?.updateDataXYZ(stroke.first, stroke.second, stroke.third)
                    yyy?.notifyObservers()
                    triggered = System.nanoTime()
                }
            }
        }

    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // 當精度改變時，不做任何操作
    }
}