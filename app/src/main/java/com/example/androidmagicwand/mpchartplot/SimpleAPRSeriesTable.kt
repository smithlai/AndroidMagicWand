package com.example.androidmagicwand.mpchartplot

import android.graphics.Color
import android.hardware.Sensor
import com.androidplot.util.PlotStatistics
import com.androidplot.xy.BoundaryMode
import com.androidplot.xy.LineAndPointFormatter
import com.androidplot.xy.SimpleXYSeries
import com.androidplot.xy.StepMode
import com.androidplot.xy.XYGraphWidget
import com.androidplot.xy.XYPlot
import com.example.androidmagicwand.SensorCheckActivity
import java.text.DecimalFormat

class SimpleAPRSeriesTable{
    var dataseries = mutableMapOf<String, SimpleXYSeries>()
    private set
    val histStats = PlotStatistics(1000, false)

    lateinit var dynamicPlotTable: XYPlot
    private set

    var colors= arrayListOf(
        Color.rgb(0, 0, 200),
        Color.rgb(0, 200, 0),
        Color.rgb(200, 0, 0),
        Color.rgb(0, 0, 100),
        Color.rgb(0, 100, 0),
        Color.rgb(100, 0, 0),
        Color.rgb(50, 50, 100),
        Color.rgb(50, 100, 50),
        Color.rgb(100, 50, 50)
    )
    private constructor(){}
    public var sensor: Sensor? = null
//    constructor(dynamicPlotTable: XYPlot): this(dynamicPlotTable)
    constructor(xyplot: XYPlot, labeles:List<String> = listOf("X","Y","Z"), boundry:Pair<Int,Int> = Pair(-30,30), unitlabel:String="m/s^2", stepValue:Double=1.0){
        for (label in labeles){
            val s = SimpleXYSeries(label)
            dataseries.put(label, s)
        }

        this.dataseries.forEach { label, series ->
            series.useImplicitXVals()
            xyplot.addSeries(series,
                LineAndPointFormatter(
                    colors.removeAt(0), null, null, null
                )
            )
        }

        xyplot.apply {
            setRangeBoundaries(boundry.first, boundry.second, BoundaryMode.FIXED)
//            centerOnRangeOrigin(0)
            setDomainBoundaries(0, HISTORY_SIZE, BoundaryMode.FIXED)
            rangeStepMode = StepMode.INCREMENT_BY_VAL
            rangeStepValue = stepValue
            domainStepMode = StepMode.INCREMENT_BY_VAL
            domainStepValue = (HISTORY_SIZE / 10).toDouble()
            linesPerRangeLabel = 10
//            setDomainLabel("Sample Index")
//            domainTitle.pack()
            setRangeLabel(unitlabel)
//            rangeTitle.pack()
            this.graph.getLineLabelStyle(XYGraphWidget.Edge.LEFT).format =
                 DecimalFormat("#")
            this.graph.getLineLabelStyle(XYGraphWidget.Edge.BOTTOM).format =
                 DecimalFormat("#")

        }

        this.dynamicPlotTable= xyplot
        this.dynamicPlotTable.addListener(histStats)
    }

    fun appendData(data: List<Number>){
        // get rid the oldest sample in history:
        while (dataseries.values.toList()[0].size() > HISTORY_SIZE) {
            dataseries.map { (k,v) ->  v.removeFirst()}
        }
        dataseries.onEachIndexed { index, entry ->  entry.value.addLast(null, data[index])}

    }
    companion object{
        val HISTORY_SIZE = 500
    }
}