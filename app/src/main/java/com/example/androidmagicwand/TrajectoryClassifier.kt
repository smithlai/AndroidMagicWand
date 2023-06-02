/* Copyright 2021 The TensorFlow Authors. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
==============================================================================
*/

package com.example.androidmagicwand

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter

import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp

class TrajectoryClassifier(
    private val interpreter: Interpreter,
    private val labels: List<String>
) {
    private val input = interpreter.getInputTensor(0).shape()   // type: float32[1,128,128,3]


    // type: float32[1,128,128,3]
    private val inputWidth = input[1]
    private val inputHeight = input[2]

//    private var cropHeight = 0f
//    private var cropWidth = 0f
//    private var cropSize = 0
    fun FloatArray.indexOfMax(): Int {
        var maxIndex = 0
        var maxValue = this[maxIndex]

        for (i in 1 until size) {
            if (this[i] > maxValue) {
                maxIndex = i
                maxValue = this[i]
            }
        }

        return maxIndex
    }

    companion object {
        private const val MODEL_FILENAME = "float_model.tflite"
        private const val LABELS_FILENAME = "labels.txt"
        private const val CPU_NUM_THREADS = 4

        fun create(context: Context): TrajectoryClassifier {
            val options = Interpreter.Options().apply {
                setNumThreads(CPU_NUM_THREADS)
            }
            return TrajectoryClassifier(
                Interpreter(
                    FileUtil.loadMappedFile(
                        context, MODEL_FILENAME
                    ), options
                ),
                FileUtil.loadLabels(context, LABELS_FILENAME)
            )
        }
    }
    private fun processInputImage(bitmap: Bitmap): TensorImage {
        // reset crop width and height
//        cropWidth = 0f
//        cropHeight = 0f
//        cropSize = if (bitmap.width > bitmap.height) {
//            cropHeight = (bitmap.width - bitmap.height).toFloat()
//            bitmap.width
//        } else {
//            cropWidth = (bitmap.height - bitmap.width).toFloat()
//            bitmap.height
//        }
//        Log.e("aaaaa", inputWidth.toString() + " " + inputHeight.toString())
        val imageProcessor = ImageProcessor.Builder().apply {
//            add(ResizeWithCropOrPadOp(cropSize, cropSize))
            add(ResizeOp(inputWidth, inputHeight, ResizeOp.ResizeMethod.BILINEAR))
//            add(NormalizeOp(MEAN, STD))
        }.build()
        val tensorImage = TensorImage(DataType.FLOAT32)
        tensorImage.load(bitmap)
        return imageProcessor.process(tensorImage)
    }

    /**
     * Initializes an outputMap of 1 * x * y  FloatArrays for the model processing to populate.
     */
    private fun initOutputMap(interpreter: Interpreter): HashMap<Int, Any> {
        val outputMap = HashMap<Int, Any>()


        val output0Shape = interpreter.getOutputTensor(0).shape() //type: float32[1,6]
        outputMap[0] = Array(output0Shape[0]) {
                FloatArray(output0Shape[1])
        }
        return outputMap
    }

    fun inferenceImage(bitmap: Bitmap): Int{

        val inputArray = arrayOf(processInputImage(bitmap).tensorBuffer.buffer)

        val outputMap = initOutputMap(interpreter)
        interpreter.runForMultipleInputsOutputs(inputArray, outputMap)
        var output0 = outputMap.get(0) as Array<FloatArray>
        var possibility = output0.get(0)
        return possibility.indexOfMax()
    }
    fun mapToLabel(index:Int): String{
        return labels[index]
    }
//    fun classify(): List<Pair<String, Float>> {
//        // Preprocess the pose estimation result to a flat array
//        val inputVector = FloatArray(input[1])
//        person?.keyPoints?.forEachIndexed { index, keyPoint ->
//            inputVector[index * 3] = keyPoint.coordinate.y
//            inputVector[index * 3 + 1] = keyPoint.coordinate.x
//            inputVector[index * 3 + 2] = keyPoint.score
//        }
//
//        // Postprocess the model output to human readable class names
//        val outputTensor = FloatArray(output[1])
//        interpreter.run(arrayOf(inputVector), arrayOf(outputTensor))
//        val output = mutableListOf<Pair<String, Float>>()
//        outputTensor.forEachIndexed { index, score ->
//            output.add(Pair(labels[index], score))
//        }
//        return output
//    }

    fun close() {
        interpreter.close()
    }
}
