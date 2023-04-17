package com.example.androidmagicwand
import org.jetbrains.kotlinx.multik.ndarray.data.D1Array

data class HistoryItems(
    var acc:D1Array<Double>,
    var vel:D1Array<Double>,
    var distance:D1Array<Double>,
    var nano_timestamp:Long,
)