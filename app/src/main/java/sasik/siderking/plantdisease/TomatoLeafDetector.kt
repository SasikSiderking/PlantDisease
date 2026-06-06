import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Log
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import java.io.FileInputStream
import java.nio.channels.FileChannel

class TomatoLeafDetector(private val context: Context) {

    private var interpreter: Interpreter? = null

    private val imageProcessor by lazy {
        ImageProcessor.Builder()
            .add(ResizeOp(320, 320, ResizeOp.ResizeMethod.BILINEAR))
            .add(NormalizeOp(0f, 255f))
            .build()
    }

    init {
        loadModel()
    }

    private fun loadModel() {
        try {
            val fileDescriptor = context.assets.openFd("detector_float16.tflite")
            val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
            val fileChannel = inputStream.channel
            val startOffset = fileDescriptor.startOffset
            val declaredLength = fileDescriptor.declaredLength
            val modelBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)

            val options = Interpreter.Options().apply {
                setNumThreads(4)
            }
            interpreter = Interpreter(modelBuffer, options)
            Log.i("LeafDetector", "Interpreter детектора успешно инициализирован.")
        } catch (e: Exception) {
            Log.e("LeafDetector", "Ошибка загрузки детектора через Interpreter: ${e.message}")
            e.printStackTrace()
        }
    }


    fun detectLeaves(srcBitmap: Bitmap): List<RectF> {
        val tflite = interpreter ?: return emptyList()

        return try {
            var tensorImage = TensorImage.fromBitmap(srcBitmap)
            tensorImage = imageProcessor.process(tensorImage)

            val outputBuffer = Array(1) { Array(300) { FloatArray(6) } }

            val outputs = mapOf(0 to outputBuffer)

            tflite.runForMultipleInputsOutputs(arrayOf(tensorImage.buffer), outputs)

            val validBoxes = mutableListOf<RectF>()
            val predictions = outputBuffer[0]

            for (i in 0 until 300) {
                val prediction = predictions[i]
                val score = prediction[4]

                if (score > 0.516f) {
                    val left = prediction[0] * srcBitmap.width
                    val top = prediction[1] * srcBitmap.height
                    val right = prediction[2] * srcBitmap.width
                    val bottom = prediction[3] * srcBitmap.height

                    validBoxes.add(RectF(left, top, right, bottom))
                }
            }

            Log.d("LeafDetector", "Успешная детекция. Найдено листьев: ${validBoxes.size}")
            validBoxes
        } catch (e: Exception) {
            Log.e("LeafDetector", "Ошибка во время детекции: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }

    fun close() {
        interpreter?.close()
        interpreter = null
    }
}
