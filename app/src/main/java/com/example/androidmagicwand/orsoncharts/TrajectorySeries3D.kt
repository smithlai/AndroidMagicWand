package com.example.androidmagicwand.orsoncharts

import android.view.View
import com.orsoncharts.android.Chart3DFactory
import com.orsoncharts.android.ChartSurfaceView
import com.orsoncharts.android.data.xyz.XYZSeries
import com.orsoncharts.android.data.xyz.XYZSeriesCollection
import com.orsoncharts.android.graphics3d.Point3D
import com.orsoncharts.android.graphics3d.ViewPoint3D

class TrajectorySeries3D{
    constructor() {}
    private lateinit var orson_chartview:ChartSurfaceView
    fun setup(orson_chartview: ChartSurfaceView) {
        this.orson_chartview = orson_chartview
        updateDataXYZ(listOf<Double>(),listOf<Double>(),listOf<Double>())
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
    fun updateDataXYZ(x: List<Double>, y: List<Double>, z: List<Double>) {

        val series = XYZSeries("Trajectory")
        for (i in 0 until x.size){
            series.add(x[i],y[i],z[i])
        }
        val dataset = XYZSeriesCollection()
        dataset.add(series)
        val chart = Chart3DFactory.createScatterChart(
            "Scatter Chart", "3D Trajectory",
            dataset, "X", "Y", "Z"
        )

//        if (prev_point != null) {
//            chart.viewPoint = prev_point
//        }
        val position = Point3D(0.0, 100.0, 0.0)
        val orientation: Double = Math.PI
        val viewpoint = ViewPoint3D(position, orientation)
        chart.viewPoint = viewpoint

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