package com.example.androidmagicwand
import android.graphics.Bitmap
import android.os.Environment
import android.util.Log
import com.google.gson.GsonBuilder
import org.json.JSONArray
import org.json.JSONObject
import java.io.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class TrajectoryJson {
    public val mJson = JSONObject()
    private lateinit var mStrokes: JSONArray
    public lateinit var filename:String
    private set
    private var pointsArr: JSONArray? = null
    public var isValidPointsArr = {
        pointsArr != null
    }
    init{
        mJson.put("strokes", JSONArray())
        filename = generateFileName()
        mStrokes = mJson.getJSONArray("strokes")
    }
    private fun generateFileName(): String {
        val now = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
        val formatted = now.format(formatter)
        return "traj_$formatted.json"
    }
    fun setTemp(strokePoints: List<Pair<Double, Double>>) {
        val pointsArr_ = JSONArray()
        // 加入第一個 stroke point
        strokePoints.forEach { (x, y) ->
            pointsArr_.put(JSONObject().apply {
                put("x", x)
                put("y", y)
            })
        }
        pointsArr = pointsArr_
    }
    fun confirmStroke(label: String): Boolean {
        if (!isValidPointsArr())
            return false
        // 1. 計算當前索引
        val currentIndex = mStrokes.length()

        // 2. 將索引、標籤和筆劃點組成對象，並添加到strokes[]中
        mStrokes.put(JSONObject().apply {
            put("index", currentIndex)
            put("label", label)
            put("strokePoints", pointsArr)
        })

        pointsArr = null
        return true
    }

    fun saveToFile(filename: String): Boolean {
        val downloadsDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file = File(downloadsDirectory, filename)
        //>adb shell  "ls /sdcard/Download"
        //>adb pull "/sdcard/Download/xxxxxx.json"
        // 使用 PrintWriter 寫入檔案
        PrintWriter(FileWriter(file)).use { out ->
            out.print(mJson.toString(2))
        }
        return true
    }
}