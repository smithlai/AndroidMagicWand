package com.example.androidmagicwand
import android.util.Log
import org.jetbrains.kotlinx.multik.api.empty
import org.jetbrains.kotlinx.multik.api.linalg.dot
import org.jetbrains.kotlinx.multik.api.mk
import org.jetbrains.kotlinx.multik.api.ndarray
import org.jetbrains.kotlinx.multik.api.zeros
import org.jetbrains.kotlinx.multik.ndarray.data.D1Array
import org.jetbrains.kotlinx.multik.ndarray.data.D2Array
import org.jetbrains.kotlinx.multik.ndarray.data.get
import org.jetbrains.kotlinx.multik.ndarray.data.set
import org.jetbrains.kotlinx.multik.ndarray.operations.*
import kotlin.math.*

class MyConverter {
    companion object{
        final val Null_XYZ:D1Array<Double> = mk.zeros(3)
        final val Null_RotationMatrix:D2Array<Double> = mk.zeros(3,3)
        final val HISTORY_START_BUFFER = 25
        final val HISTORY_END_BUFFER = 50
        final val ACC_TRIGGER_START = 1
        final val ACC_TRIGGER_END = 1.5
    }

//    var linear:D1Array<Double> = Null_XYZ
//        private set;
    private var isRecording = false
    public var historyItems = mutableListOf<HistoryItems>()

    var stroke:Triple<List<Double>,List<Double>,List<Double>>? = null
    private set
    private var rotationMatrix:D2Array<Double> = Null_RotationMatrix
    constructor(){

    }

//    fun setLinear(new_grav:FloatArray){
//        linear = mk.ndarray(new_grav).asType<Double>()
//    }

    // Transform body frame accelerations into the inertial (Earth) frame
    // Set up rotation matrices
    private fun _getRx(x: Double): D2Array<Double> {
        // body frame rotation about x axis
        return mk.ndarray(
            mk[
                    mk[1.0, 0.0,    0.0],
                    mk[0.0, cos(-x), -sin(-x)],
                    mk[0.0, sin(-x), cos(-x)]
            ]
        )
    }

    private fun _getRy(y: Double): D2Array<Double> {
        // body frame rotation about y axis
        return mk.ndarray(
            mk[
                    mk[cos(-y), 0.0, -sin(-y)],
                    mk[0.0, 1.0, 0.0],
                    mk[sin(-y), 0.0, cos(-y)]
            ]
        )
    }

    private fun _getRz(z: Double): D2Array<Double> {
        // body frame rotation about z axis
        return mk.ndarray(
            mk[
                    mk[cos(-z), -sin(-z), 0.0],
                    mk[sin(-z), cos(-z), 0.0],
                    mk[0.0, 0.0, 1.0]
            ]
        )
    }

    fun setupRotationMatrix(orientation:FloatArray /*azimuth, pitch, roll*/){
        val pitch = orientation[1].toDouble()
        val roll = orientation[2].toDouble()
        val azimuth = orientation[0].toDouble()
        rotationMatrix = _getRz(azimuth).dot(_getRy(roll)).dot(_getRx(pitch))
    }
    /**
     * ACCELEROMETER (m/s²)
     * GRAVITY (m/s²)
     * LINEAR ACCELERATION (m/s²)
     */
    fun toEatrhFrame(device_xyz:D1Array<Double>): D1Array<Double> {
        if (device_xyz === Null_XYZ)
            return Null_XYZ
        val _xyz = device_xyz.reshape(3,1)
        var earthxyz = rotationMatrix.dot(_xyz).reshape(3)
        return earthxyz
    }

    fun add_earth_linear(linear_xyz:D1Array<Double>, nano_timestamp:Long){
        val last = historyItems.lastOrNull()
        val new_history = trapz(last, linear_xyz, nano_timestamp)
        historyItems.add(new_history)
        if (!isRecording) {
//            Log.e("AAA", "0:" + historyItems.size)
            if (historyItems.size >= HISTORY_START_BUFFER) {
                val istart = max(historyItems.size - HISTORY_START_BUFFER, 0)
                historyItems = historyItems.subList(istart, historyItems.size)
//                Log.e("AAA", historyItems.joinToString(","))
                var isTrigger = historyItems.all { it -> it.acc.any { acc -> Math.abs(acc) > ACC_TRIGGER_START } }
                if (isTrigger){
                    isRecording = true
                    Log.e("AAA", "Drawing Start......")
                }
            }
        }else{
//            Log.e("AAA", "-0: " + historyItems.size)
            if (historyItems.size >= HISTORY_END_BUFFER+HISTORY_START_BUFFER) {
                val iend = max(historyItems.size - HISTORY_END_BUFFER, 0)
                val sublist = historyItems.subList(iend, historyItems.size)
                stroke = normalize(historyItems.subList(0, iend),100)
                var isTrigger = sublist.all { it -> it.acc.all { acc -> Math.abs(acc) < ACC_TRIGGER_END } }
                if (isTrigger){
                    Log.e("AAA", "Drawing Stopped......")
                    isRecording = false

                    historyItems.clear()

                }
            }

        }
    }
    fun normalize(items:List<HistoryItems>, size:Int = 100):Triple<List<Double>,List<Double>,List<Double>>?{
        var xList = items.map { it.distance[0] }
        var yList = items.map { it.distance[1] }
        var zList = items.map { it.distance[2] }

        xList = centrolize(xList)
        yList = centrolize(yList)
        zList = centrolize(zList)


        val xmax = xList.max()
        val xmin = xList.min()
        val xdis = xmax - xmin

        val ymax = xList.max()
        val ymin = xList.min()
        val ydis = xmax - xmin

        val zmax = zList.max()
        val zmin = zList.min()
        val zdis = zmax - zmin

        var min = 0.0
        var max = 0.0

        if (xdis>zdis){
            max = xmax
            min = xmin
        }else{
            max = zmax
            min = zmin
        }
        val dis = max - min
        val res = size/dis

        xList = xList.map { it ->  it*res}
        yList = yList.map { it ->  it*res}
        zList = zList.map { it ->  it*res}
//        val stroke3d = mutableListOf<Triple<Double,Double,Double>>()
//        for (i in 0 until xList.size){
////            Log.e("AAA", "${i+1}/${xList.size}: ${xList[i]}, ${zList[i]}")
//            val v = Triple(xList[i],yList[i],zList[i])
//            stroke3d.add(v)
//        }
        return Triple(xList, yList, zList)
    }
    fun centrolize(values:List<Double>) : List<Double>{
        val max = values.max()
        val min = values.min()
//        val dis = max - min
        val center = (max + min)/2.0

        return values.map { it-center }
    }

    fun trapz(prev: HistoryItems?, linear_acc:D1Array<Double>, nano_timestamp: Long): HistoryItems {
        if (prev != null) {
            var dt = (nano_timestamp - prev.nano_timestamp).toDouble()
            dt = dt * 1E-9
            val delta_speed = ((prev.acc + linear_acc) / 2.0) * dt
            val vel = delta_speed

            val delta_distance = vel * dt
            val distance = prev.distance + delta_distance
            return HistoryItems(
                acc = linear_acc,
                vel = vel,
                distance = distance,
                nano_timestamp = nano_timestamp
            )
        } else {
            return HistoryItems(
                acc = linear_acc,
                vel = Null_XYZ,
                distance = Null_XYZ,
                nano_timestamp = nano_timestamp
            )
        }
    }
}
