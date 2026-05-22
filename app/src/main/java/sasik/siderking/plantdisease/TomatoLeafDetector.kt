import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.task.core.BaseOptions
import org.tensorflow.lite.task.vision.detector.ObjectDetector
import java.util.Collections

class TomatoLeafDetector(private val context: Context) {

    private var objectDetector: ObjectDetector? = null

    init {
        setupDetector()
    }

    private fun setupDetector() {
        try {
            val baseOptions = BaseOptions.builder()
                .setNumThreads(4)
                .build()

            val options = ObjectDetector.ObjectDetectorOptions.builder()
                .setBaseOptions(baseOptions)
                .setScoreThreshold(0.4f) // Уверенность детектора > 40%
                .setMaxResults(1) // Нам нужен только один (главный) лист в кадре
                .build()

            objectDetector = ObjectDetector.createFromFileAndOptions(
                context,
                "detector_float16.tflite", // Имя вашего файла в assets
                options
            )
        } catch (e: Exception) {
            Log.e("LeafDetector", "Ошибка инициализации детектора: ${e.message}")
        }
    }

    /**
     * Находит лист на исходном изображении и вырезает его с безопасным отступом.
     * Если лист не найден, возвращает исходный bitmap.
     */
    fun detectAndCropLeaf(srcBitmap: Bitmap): Bitmap {
        if (objectDetector == null) return srcBitmap

        return try {
            val tensorImage = org.tensorflow.lite.support.image.TensorImage.fromBitmap(srcBitmap)
            val results = objectDetector!!.detect(tensorImage)

            if (results.isEmpty()) {
                Log.d("LeafDetector", "Лист не обнаружен, возвращаем исходное фото")
                return srcBitmap
            }

            // Берем рамку первого (лучшего) найденного объекта
            val boundingBox = results[0].boundingBox

            // Координаты бокса
            val left = boundingBox.left.toInt()
            val top = boundingBox.top.toInt()
            val width = boundingBox.width().toInt()
            val height = boundingBox.height().toInt()

            // Считаем безопасный отступ (padding) 10%, чтобы края листа не обрезались
            val paddingW = (width * 0.10f).toInt()
            val paddingH = (height * 0.10f).toInt()

            // Корректируем координаты с учетом границ оригинальной картинки
            val cropLeft = maxOf(0, left - paddingW)
            val cropTop = maxOf(0, top - paddingH)
            val cropWidth = minOf(srcBitmap.width - cropLeft, width + (paddingW * 2))
            val cropHeight = minOf(srcBitmap.height - cropTop, height + (paddingH * 2))

            // Проверка на валидность размеров кропа
            if (cropWidth > 0 && cropHeight > 0) {
                Log.d("LeafDetector", "Лист успешно вырезан. Размер: ${cropWidth}x${cropHeight}")
                Bitmap.createBitmap(srcBitmap, cropLeft, cropTop, cropWidth, cropHeight)
            } else {
                srcBitmap
            }
        } catch (e: Exception) {
            Log.e("LeafDetector", "Ошибка при детекции/кропе: ${e.message}")
            srcBitmap
        }
    }

    fun close() {
        objectDetector?.close()
        objectDetector = null
    }
}
