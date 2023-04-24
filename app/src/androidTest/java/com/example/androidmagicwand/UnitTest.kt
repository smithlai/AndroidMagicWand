package com.example.androidmagicwand
import android.util.Log
import com.example.androidmagicwand.MyConverter
import org.junit.Assert.assertEquals
import org.junit.Test

class UnitTest {
    @Test
    fun addition_isCorrect() {
        val c = MyConverter()
        var a = c.interpolateLine(Pair(0,0), Pair(100,100))
        Log.e("AAA", a.toString())
        var b = c.interpolateLine(Pair(100,100), Pair(0,0))
        Log.e("AAA", b.toString())
        var d = c.interpolateLine(Pair(50,0), Pair(50,100))
        Log.e("AAA", d.toString())
        var e = c.interpolateLine(Pair(0,50), Pair(100,50))
        Log.e("AAA", e.toString())
        var f = c.interpolateLine(Pair(0,50), Pair(100,50))
        Log.e("AAA", f.toString())
        var g = c.interpolateLine(Pair(0,100), Pair(100,0))
        Log.e("AAA", g.toString())
        var h = c.interpolateLine(Pair(100,0), Pair(0,100))
        Log.e("AAA", h.toString())

        assertEquals(4, 2 + 2)
    }
}