package com.example.androidmagicwand

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.androidplot.xy.XYPlot
import com.example.androidmagicwand.mpchartplot.TrajectorySeries
import com.example.androidmagicwand.opengl.GLesAgent
import com.example.androidmagicwand.orsoncharts.TrajectorySeries3D
import com.orsoncharts.android.ChartSurfaceView
import org.jetbrains.kotlinx.multik.api.mk
import org.jetbrains.kotlinx.multik.api.ndarray
import org.jetbrains.kotlinx.multik.ndarray.data.get


class MainActivity : AppCompatActivity(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
//    private var gyroSensor: Sensor? = null
    private var imuSensor: Sensor? = null
    private var gravSensor: Sensor? = null
    private var linearSensor: Sensor? = null
    private var rotationSensor: Sensor? = null

//    private var gyroTextView: TextView? = null
//    private var imuTextView: TextView? = null
//    private var gravTextView: TextView? = null
    private var linearTextView: TextView? = null
//    private var orientationTextView: TextView? = null

    private lateinit var myplot: XYPlot
    private lateinit var trajectorySeries2D: TrajectorySeries

    private val converter = MyConverter()
    private var previous_stroke:Triple<List<Double>,List<Double>,List<Double>>? = null
    private var prev_timestamp:Long = 0
    private var triggered:Long = 0
    val glagent = GLesAgent()

    private lateinit var trajectorySeries3D: TrajectorySeries3D

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
//        gyroTextView = findViewById<TextView>(R.id.gyroTextView)
//        imuTextView = findViewById<TextView>(R.id.imuTextView)
//        gravTextView = findViewById<TextView>(R.id.gravTextView)
        linearTextView = findViewById<TextView>(R.id.linearTextView)
//        orientationTextView = findViewById<TextView>(R.id.orientationTextView)
        // 取得 SensorManager 實例
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager

        // 取得陀螺儀 Sensor 實例
//        gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        // 取得 IMU Sensor 實例
//        imuSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        // 取得 Gravity Sensor 實例
//        gravSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)

        // 取得 Linear Sensor 實例
        linearSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        // 取得 rotationsensor Sensor 實例
        rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

        myplot = findViewById(R.id.dynamicXYPlot) as XYPlot;
        trajectorySeries2D = TrajectorySeries();
        trajectorySeries2D.setup(myplot)


//        val mGLTextureView = findViewById<myGLTextureView>(R.id.glSurfaceView) as myGLTextureView?
//        if (!glagent.setup(this,mGLTextureView)){
//            finish()
//        }
        val orson_chartview = findViewById(R.id.chartView) as ChartSurfaceView
        trajectorySeries3D = TrajectorySeries3D().also { it.setup(orson_chartview!!)}


    }

    public override fun onResume() {
        super.onResume()

        // 註冊陀螺儀和 IMU 監聽器
//        sensorManager.registerListener(this, gyroSensor, SensorManager.SENSOR_DELAY_NORMAL)
//        sensorManager.registerListener(this, imuSensor, SensorManager.SENSOR_DELAY_NORMAL)
//        sensorManager.registerListener(this, gravSensor, SensorManager.SENSOR_DELAY_NORMAL)
        // Normal: 130 ms
        // UI: 60 ms
        // Game: 15 ms
        // Fast: ?? ms
        sensorManager.registerListener(this, linearSensor, SensorManager.SENSOR_DELAY_FASTEST)
        sensorManager.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_FASTEST)


        glagent?.onResume()
    }

    public override fun onPause() {
        // 取消註冊陀螺儀和 IMU 監聽器
        sensorManager.unregisterListener(this)

        glagent.onPause()

        super.onPause()



    }

    override fun onSensorChanged(event: SensorEvent) {


        if (event.sensor.type == Sensor.TYPE_LINEAR_ACCELERATION) {
//            converter.setLinear(event.values)
            var linear_acc = mk.ndarray(event.values).asType<Double>()


            var w_linear = linear_acc
//            w_linear = converter.toEatrhFrame(w_linear)
            converter.add_earth_linear(w_linear, System.nanoTime())

            linearTextView!!.text = "Time:\n "+
                    "X: ${(w_linear[0]*1_000).toInt()}\n"+
                    "Y: ${(w_linear[1]*1_000).toInt()}\n"+
                    "Z: ${(w_linear[2]*1_000).toInt()}"
//            orientationTextView!!.text = "Linear Acc\n: ${w_linear.get(0)}\n${w_linear.get(1)}\n${w_linear.get(2)}\n"

            if (System.nanoTime() - triggered > 10_000_000L) {
                // 10 second interval
                val stroke = converter.stroke
                stroke?.apply {
                    if (previous_stroke !== stroke) {
                        trajectorySeries2D.updateDataXYZ(stroke.first, stroke.second, stroke.third)
                        trajectorySeries2D.notifyObservers()
                        triggered = System.nanoTime()
                        trajectorySeries3D.updateDataXYZ(stroke.first, stroke.second, stroke.third)
//                    zzz.myrand
                        previous_stroke = stroke
                    }
                }
            }



        }

        if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
            val current1=System.nanoTime()
            val dt = current1 - prev_timestamp
            prev_timestamp=current1
            val matrix = FloatArray(9)  // 3*3
            SensorManager.getRotationMatrixFromVector(matrix,event.values)

            val orientation = FloatArray(3)/*azimuth, pitch, roll*/
            SensorManager.getOrientation(matrix, orientation)

            converter.setupRotationMatrix(orientation)
        }

    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // 當精度改變時，不做任何操作
    }
}