package com.example.androidmagicwand
import android.util.Log
import org.junit.Assert.assertEquals
import org.junit.Test

class UnitTest {
    @Test
    fun interpolateLine_isCorrect() {
//        val aaa = listOf<Int>(2)
//        val mysubList = aaa.subList(1, aaa.size-1)
//        Log.e("AAA", mysubList.toString())
        val c = MyConverter()
        var a1 = c.interpolateLine(Pair(0,0), Pair(100,100))
        var a1_correct = (0..100).map { it to it }
        assertEquals(a1_correct, a1)

        var a2 = c.interpolateLine(Pair(100,100), Pair(0,0))
        var a2_correct = (100 downTo 0).map { it to it }
        assertEquals(a2_correct, a2)

        var b1 = c.interpolateLine(Pair(50,0), Pair(50,100))
        var b1_correct = (0 .. 100).map { 50 to it }
        assertEquals(b1_correct, b1)

        var b2 = c.interpolateLine(Pair(50,100), Pair(50,0))
        var b2_correct = (100 downTo  0).map { 50 to it }
        assertEquals(b2_correct, b2)

        var d1 = c.interpolateLine(Pair(0,50), Pair(100,50))
        var d1_correct = (0 .. 100).map { it to 50 }
        assertEquals(d1_correct, d1)

        var d2 = c.interpolateLine(Pair(100,50), Pair(0,50))
        var d2_correct = (100 downTo  0).map { it to 50 }
        assertEquals(d2_correct, d2)

        var f1 = c.interpolateLine(Pair(0,100), Pair(100,0))
        var f1_correct = (0 .. 100).map { it to 100-it }
        assertEquals(f1_correct, f1)

        var f2 = c.interpolateLine(Pair(100,0), Pair(0,100))
        var f2_correct = (100 downTo 0).map { it to 100-it }
        assertEquals(f2_correct, f2)

    }
}