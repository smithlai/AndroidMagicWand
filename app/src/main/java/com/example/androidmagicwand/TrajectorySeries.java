package com.example.androidmagicwand;

import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;

import com.androidplot.Plot;
import com.androidplot.util.PixelUtils;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.StepMode;
import com.androidplot.xy.XYGraphWidget;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeries;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

public class TrajectorySeries implements XYSeries {
    // redraws a plot whenever an update is received:
    private class MyPlotUpdater implements Observer {
        Plot plot;

        public MyPlotUpdater(Plot plot) {
            this.plot = plot;
        }

        @Override
        public void update(Observable o, Object arg) {
            plot.redraw();
        }
    }
    class MyObservable extends Observable {
        @Override
        public void notifyObservers() {
            setChanged();
            super.notifyObservers();
        }
    }
    private MyObservable notifier = new MyObservable();
    private MyPlotUpdater plotUpdater;
//    DataYYY.HistoryXYSeries data;

    private XYPlot dynamicPlotTable;
    TrajectorySeries(){}
    void setup(XYPlot dynamicPlotTable){
        // get handles to our View defined in layout.xml:
        this.dynamicPlotTable = dynamicPlotTable;

        plotUpdater = new MyPlotUpdater(dynamicPlotTable);

        // only display whole numbers in domain labels
        dynamicPlotTable.getGraph().getLineLabelStyle(XYGraphWidget.Edge.BOTTOM).
                setFormat(new DecimalFormat("0"));

//        // getInstance and position datasets:
//        data = new DataYYY.HistoryXYSeries();


        LineAndPointFormatter formatter1 = new LineAndPointFormatter(
                Color.rgb(0, 200, 0), null, null, null);
        formatter1.getLinePaint().setStrokeJoin(Paint.Join.ROUND);
        formatter1.getLinePaint().setStrokeWidth(10);
        dynamicPlotTable.addSeries(this, formatter1);

        // hook up the plotUpdater to the data model:
        notifier.addObserver(plotUpdater);

        // thin out domain tick labels so they dont overlap each other:
        dynamicPlotTable.setDomainStepMode(StepMode.INCREMENT_BY_VAL);
        dynamicPlotTable.setDomainStepValue(5);

        dynamicPlotTable.setRangeStepMode(StepMode.INCREMENT_BY_VAL);
        dynamicPlotTable.setRangeStepValue(10);

        dynamicPlotTable.getGraph().getLineLabelStyle(
                XYGraphWidget.Edge.LEFT).setFormat(new DecimalFormat("###.#"));

        // uncomment this line to freeze the range boundaries:
        dynamicPlotTable.setRangeBoundaries(-100, 100, BoundaryMode.FIXED);

        // create a dash effect for domain and range grid lines:
        DashPathEffect dashFx = new DashPathEffect(
                new float[] {PixelUtils.dpToPix(3), PixelUtils.dpToPix(3)}, 0);
        dynamicPlotTable.getGraph().getDomainGridLinePaint().setPathEffect(dashFx);
        dynamicPlotTable.getGraph().getRangeGridLinePaint().setPathEffect(dashFx);
    }
//-----------------------------------------------


    private List<Double> datasourceX = new ArrayList<Double>(0);
    private List<Double> datasourceY = new ArrayList<Double>(0);
    private List<Double> datasourceZ = new ArrayList<Double>(0);;

    private String title="AAAA";

    public void updateDataXYZ(List<Double> x, List<Double> y,List<Double> z){
        this.datasourceX = x;
        this.datasourceY = y;
        this.datasourceZ = z;
    }
    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public int size() {
        return datasourceX.size();
    }

    @Override
    public Number getX(int index) {
        return datasourceX.get(index);
    }

    @Override
    public Number getY(int index) {
        return datasourceZ.get(index);
    }
//    void addObserver(Observer observer) {
//        notifier.addObserver(observer);
//    }
//
//    public void removeObserver(Observer observer) {
//        notifier.deleteObserver(observer);
//    }
    public void notifyObservers(){
        notifier.notifyObservers();
    }

}
