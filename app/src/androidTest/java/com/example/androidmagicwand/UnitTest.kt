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
    @Test
    fun trajectory_isCorrect() {
        val c = MyConverter()
        val json = c.trajectoryJson
        json.setTemp(listOf(Pair(0.0,0.0),Pair(1.0,1.0),Pair(2.0,2.0)))
        json.confirmStroke("aaa")
        json.setTemp(listOf(Pair(3.0,3.0),Pair(4.0,4.0),Pair(5.0,5.0)))
        json.confirmStroke("aaa")
        val correct_ans = "{\"strokes\":[{\"index\":0,\"label\":\"aaa\",\"strokePoints\":[{\"x\":0,\"y\":0},{\"x\":1,\"y\":1},{\"x\":2,\"y\":2}]},{\"index\":1,\"label\":\"aaa\",\"strokePoints\":[{\"x\":3,\"y\":3},{\"x\":4,\"y\":4},{\"x\":5,\"y\":5}]}]}"
        assertEquals(json.mJson.toString(), correct_ans)
    }

}