package com.example.androidmagicwand.orsoncharts

import android.graphics.Color
import android.view.View
import com.orsoncharts.android.Chart3DFactory
import com.orsoncharts.android.ChartSurfaceView
import com.orsoncharts.android.Range
import com.orsoncharts.android.axis.CategoryAxis3D
import com.orsoncharts.android.axis.NumberAxis3D
import com.orsoncharts.android.axis.StandardCategoryAxis3D
import com.orsoncharts.android.data.xyz.XYZSeries
import com.orsoncharts.android.data.xyz.XYZSeriesCollection
import com.orsoncharts.android.graphics3d.Point3D
import com.orsoncharts.android.graphics3d.ViewPoint3D
import com.orsoncharts.android.plot.XYZPlot

class TrajectorySeries3D{
    constructor() {}
    private lateinit var orson_chartview:ChartSurfaceView
    private var min:Double=-50.0
    private var max:Double=50.0
    fun setup(orson_chartview: ChartSurfaceView) {
        this.orson_chartview = orson_chartview
        updateDataXYZ(listOf<Double>(),listOf<Double>(),listOf<Double>(),listOf<Double>(),listOf<Double>(),listOf<Double>())
    }
    fun setRange(min:Double=-50.0, max:Double=50.0){
        if (min == max){
            return
        }
        this.min = min
        this.max = max
    }
    fun myrand(){

        val series1 = XYZSeries("Series 1")
        val series2 = XYZSeries("Series 2")
        for (i in 0..99) {
            series1.add(
                Math.random() * 5, Math.random() * 10,
                Math.random() * 20
            )
            series2.add(
                Math.random() * 10, Math.random() * 5,
                Math.random() * 20
            )
        }
        val dataset = XYZSeriesCollection()
        dataset.add(series1)
        dataset.add(series2)
        val chart = Chart3DFactory.createScatterChart(
            "Scatter Chart", "Subtitle...",
            dataset, "X", "Y", "Z"
        )
        orson_chartview.setChart(chart)
        // here we add a listener that will zoom-to-fit the new chart when
        // the layout changes...
        // here we add a listener that will zoom-to-fit the new chart when
        // the layout changes...
        orson_chartview.addOnLayoutChangeListener(View.OnLayoutChangeListener { v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
            orson_chartview.zoomToFit(
                (right - left).toDouble(),
                (bottom - top).toDouble()
            )
        })
    }
    fun updateDataXYZ(x: List<Double>, y: List<Double>, z: List<Double>, ax: List<Double>?, ay: List<Double>?, az: List<Double>?) {
        val dataset = XYZSeriesCollection()
        val series = XYZSeries("Trajectory")
        for (i in 0 until x.size){
            series.add(x[i],y[i],z[i])
        }
        dataset.add(series)
        if (null != ax && null != ay && null != az){
            val series2 = XYZSeries("Acc")
            for (i in 0 until ax.size){
                series2.add(ax[i],ay[i],az[i])
            }
            dataset.add(series2)
        }

        val chart = Chart3DFactory.createScatterChart(
            "Scatter Chart", "3D Trajectory",
            dataset, "X", "Y", "Z"
        )

//        if (prev_point != null) {
//            chart.viewPoint = prev_point
//        }
        val viewpoint = chart.viewPoint
        viewpoint.moveUpDown(Math.PI/180 * -75)
        chart.setChartBoxColor(Color.WHITE)
        val plot = chart.plot as XYZPlot
        val xAxis = NumberAxis3D("X Axis")
        val yAxis = NumberAxis3D("Y Axis")
        val zAxis = NumberAxis3D("Z Axis")

        // 設定座標軸的範圍
        xAxis.range = Range(min,max)
        yAxis.range = Range(min,max)
        zAxis.range = Range(min,max)
        plot.xAxis = xAxis
        plot.yAxis = yAxis
        plot.zAxis = zAxis
        orson_chartview.setChart(chart)
        // here we add a listener that will zoom-to-fit the new chart when
        // the layout changes...
        // here we add a listener that will zoom-to-fit the new chart when
        // the layout changes...
        orson_chartview.addOnLayoutChangeListener(View.OnLayoutChangeListener { v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
            orson_chartview.zoomToFit(
                (right - left).toDouble(),
                (bottom - top).toDouble()
            )
        })
    }
}