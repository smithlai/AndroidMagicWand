package com.example.androidmagicwand
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Environment
import android.util.Log
import org.jetbrains.kotlinx.multik.api.linalg.dot
import org.jetbrains.kotlinx.multik.api.mk
import org.jetbrains.kotlinx.multik.api.ndarray
import org.jetbrains.kotlinx.multik.api.zeros
import org.jetbrains.kotlinx.multik.ndarray.data.D1Array
import org.jetbrains.kotlinx.multik.ndarray.data.D2Array
import org.jetbrains.kotlinx.multik.ndarray.data.get
import org.jetbrains.kotlinx.multik.ndarray.operations.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*
import kotlin.math.*

class MyConverter {
    private infix fun Int.toward(to: Int): IntProgression {
        val step = if (this > to) -1 else 1
        return IntProgression.fromClosedRange(this, to, step)
    }
    companion object{
        final val Null_XYZ:D1Array<Double> = mk.zeros(3)
        final val Null_RotationMatrix:D2Array<Double> = mk.zeros(3,3)
        final val HISTORY_START_BUFFER = 20
        final val HISTORY_END_BUFFER = 50
        final val ACC_TRIGGER_START_XZ = 2
        final val ACC_TRIGGER_START_Y = 2
        final val ACC_TRIGGER_END = 1
        final val TRIM_TAIL = 100
    }

//    var linear:D1Array<Double> = Null_XYZ
//        private set;
    private var isRecording = false
    private var historyItems = mutableListOf<HistoryItems>()

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
            return device_xyz
        val _xyz = device_xyz.reshape(3,1)
        var earthxyz = rotationMatrix.dot(_xyz).reshape(3)
        return earthxyz
    }

    fun add_earth_linear(linear_xyz:D1Array<Double>, nano_timestamp:Long){
        val last = historyItems.lastOrNull()
        val new_history = trapz(last, linear_xyz, nano_timestamp)
        historyItems.add(new_history)
        if (!isRecording) {
            if (historyItems.size >= HISTORY_START_BUFFER) {
                val istart = max(historyItems.size - HISTORY_START_BUFFER, 0)
                historyItems = historyItems.subList(istart, historyItems.size)
//                var isTrigger = historyItems.all { it -> it.acc.any { acc -> Math.abs(acc) > ACC_TRIGGER_START } }
                var isTrigger = historyItems.all { it -> listOf(it.acc[0], it.acc[2]).any{ it2 -> it2 > ACC_TRIGGER_START_XZ} || (it.acc[2] > ACC_TRIGGER_START_Y) }
                if (isTrigger){
                    isRecording = true
                    Log.e("AAA", "Drawing Start......")
                }
            }
        }else{

            if (historyItems.size >= HISTORY_END_BUFFER+HISTORY_START_BUFFER) {
                val iend = max(historyItems.size - HISTORY_END_BUFFER, 0)
                val sublist1 = historyItems.subList(iend, historyItems.size)
                val sublist2 = historyItems.subList(0, iend)
                var isTrigger = sublist1.all { it -> it.acc.all { acc -> Math.abs(acc) < ACC_TRIGGER_END } }

                if (isTrigger){
                    Log.e("AAA", "Drawing Stopped......")
                    isRecording = false
                    historyItems = mutableListOf<HistoryItems>()

                    stroke = normalize(trim_last(sublist2, TRIM_TAIL),100)
                    val interpolateLines = interpolateLines(stroke!!)
                    val rasterize_stroke = rasterize_stroke(interpolateLines,100)
                    savetoBitmap(rasterize_stroke,100)

                }else{
                    stroke = normalize(sublist2,100)
                }
            }

        }
    }
    fun trim_last(list:MutableList<HistoryItems>, trim_ms:Int):MutableList<HistoryItems>{
        val trim_nano = trim_ms * 1_000_000
        val last = list.last().nano_timestamp

        var trim_idx = -1
        for (i in list.size-2 downTo  0){
            val t = list[i].nano_timestamp
            if ((last - t) >= trim_nano){
                trim_idx = i
                break
            }
        }
        if (trim_idx>0)
            return list.subList(0, trim_idx)
        else
            return list

    }
    fun normalize(items:List<HistoryItems>, size:Int = 100, center: Double=0.0):Triple<List<Double>,List<Double>,List<Double>>{
        var xList = items.map { it.distance[0] }
        var yList = items.map { it.distance[1] }
        var zList = items.map { it.distance[2] }

        xList = centrolize(xList, center)
        yList = centrolize(yList, center)
        zList = centrolize(zList, center)


        val xmax = xList.max()
        val xmin = xList.min()
        val xdis = xmax - xmin

        val ymax = yList.max()
        val ymin = yList.min()
        val ydis = ymax - ymin

        val zmax = zList.max()
        val zmin = zList.min()
        val zdis = zmax - zmin

        var min = 0.0
        var max = 0.0

        if (xdis>zdis) {
            max = xmax
            min = xmin
        }else if (ydis>zdis){
            max = ymax
            min = ymin
        }else{
            max = zmax
            min = zmin
        }
        val dis = max - min
        val res = (size-1)/dis  // 0 ..100 -> 0 ..99

        xList = xList.map { it ->  it*res}
        yList = yList.map { it ->  it*res}
        zList = zList.map { it ->  it*res}


        return Triple(xList, yList, zList)
    }
    fun centrolize(values:List<Double>, center:Double=0.0) : List<Double>{
        val max = values.max()
        val min = values.min()
//        val dis = max - min
        val mean = (max + min)/2.0
        val center_shift = center - mean
        return values.map { it+center_shift }
    }

    fun interpolateLines(xyzs:Triple<List<Double>,List<Double>,List<Double>>): List<Pair<Int,Int>> {
        val interpolate_points = mutableListOf<Pair<Int,Int>>()
        val xs = xyzs.first
        val ys = xyzs.second
        val zs = xyzs.third

        for (i in 0 until xs.size-1) {

            val start = Pair(Math.round(xs[i]).toInt(),Math.round(zs[i]).toInt())
            val end = Pair(Math.round(xs[i+1]).toInt(),Math.round(zs[i+1]).toInt())
            val tmpinterpolatepoints = interpolateLine(start, end)
            if (i == 0){
                interpolate_points.add(start)
            }
            interpolate_points.addAll(tmpinterpolatepoints)
            interpolate_points.add(end)
        }
        return interpolate_points
    }
    fun interpolateLine(start: Pair<Int,Int>, end:Pair<Int,Int>): List<Pair<Int,Int>> {
        val interpolate_points = mutableListOf<Pair<Int,Int>>()
        val deltaX = end.first - start.first
        val deltaY = end.second - start.second

        var startPoint = Pair(start.first, start.second)
        var endPoint = Pair(end.first, end.second)

        if ( deltaX == 0 && deltaY == 0) {
            return interpolate_points
        }
        //follow X
        else if (Math.abs(deltaX) >= Math.abs(deltaY)) {
            val slope = deltaY.toDouble() / deltaX.toDouble() //deltaX always > 0
            for (x in startPoint.first.toward(endPoint.first) ){
                val y_ = (x - startPoint.first) * slope + startPoint.second
                val y = Math.round(y_).toInt()
                val p = Pair(x,y)
                interpolate_points.add(p)
            }
        //follow Y to avoid infiniate slope (X=0)
        }else{ // if (Math.abs(deltaX) < Math.abs(deltaY)){
            val slope = deltaX.toDouble() / deltaY.toDouble() //deltaY always > 0
            for (y in startPoint.second.toward(endPoint.second) ){
                val x_ = (y - startPoint.second) * slope + startPoint.first
                val x = Math.round(x_).toInt()
                val p = Pair(x,y)
                interpolate_points.add(p)
            }
        }
        interpolate_points.subList(0, interpolate_points.size-1)  // remove first and last (keep new points only)
        return interpolate_points
    }
    fun savetoBitmap(rasterize_strokes: List<Triple<Int,Int,Int>>, size:Int){
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)

        for (point_i in 0 until rasterize_strokes.size) {
            val point = rasterize_strokes[point_i]
            var x = point.first
            var z = point.second
            var color = point.third
            x = min(max(x, 0), size-1)
            z = min(max(size-1-z, 0), size-1)  //reverse y in bitmap
            bmp.setPixel(x, z, color) // 設定像素的色彩值
        }
        val downloadsDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

        val uuidname = "${UUID.randomUUID()}.PNG"

        val file = File(downloadsDirectory, uuidname)

        try {
            FileOutputStream(file).use { fos ->
                bmp.compress(Bitmap.CompressFormat.PNG, 100, fos)
            }
            Log.d("MainActivity", "寫入成功")
        } catch (e: IOException) {
            Log.e("MainActivity", "寫入失敗：${e.localizedMessage}")
        }
    }
    fun rasterize_stroke(stroke_points:List<Pair<Int,Int>>, size:Int) : List<Triple<Int,Int,Int>>{
        val colors = generateGradient(stroke_points.size)
        val rasterize_strokes = mutableListOf<Triple<Int,Int,Int>>()
        var xList = centrolize(stroke_points.map { it.first.toDouble() }, size.toDouble()/2)
        var zList = centrolize(stroke_points.map { it.second.toDouble() }, size.toDouble()/2)

        for (point_index in 0 until xList.size) {
            var x = Math.round(xList[point_index]).toInt()
            var z = Math.round(zList[point_index]).toInt()
            var color = colors[point_index]
            x = min(max(x, 0), size)
            z = min(max(z, 0), size)
            rasterize_strokes.add(Triple(x,z,color))
        }
        return rasterize_strokes
    }
    fun generateGradient(n: Int): List<Int> {
        val colors = mutableListOf<Int>()
        val hueStep = 180f / n
        var hue = 360f
        for(i in 0 until n) {
            colors.add(Color.HSVToColor(floatArrayOf(hue, 1f, 1f)))
            hue -= hueStep
        }
        return colors
    }
    fun trapz(prev: HistoryItems?, linear_acc:D1Array<Double>, nano_timestamp: Long): HistoryItems {
        if (prev != null) {
            var dt = (nano_timestamp - prev.nano_timestamp).toDouble()
            dt = dt * 1E-9
            val vel = ((prev.acc + linear_acc) / 2.0) * dt


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
