package sasik.siderking.plantdisease

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.core.BaseOptions
import org.tensorflow.lite.task.vision.classifier.Classifications
import org.tensorflow.lite.task.vision.classifier.ImageClassifier

class TomatoDiseaseClassifier(private val context: Context) {

    private var classifier: ImageClassifier? = null
    private var labels: List<String> = emptyList()

    init {
        loadModel()
        loadLabels()
    }

    fun classify(bitmap: Bitmap): List<Pair<String, Float>> {
        if (classifier == null) {
            return listOf("Модель не загружена" to 0f)
        }

        return try {
            // Конвертируем Bitmap в TensorImage
            val tensorImage = TensorImage.fromBitmap(bitmap)

            // Выполняем классификацию
            val results = classifier!!.classify(tensorImage)

            // Преобразуем результаты в удобный формат
            processResults(results)
        } catch (e: Exception) {
            e.printStackTrace()
            listOf("Ошибка классификации" to 0f)
        }
    }

    fun close() {
        classifier?.close()
        classifier = null
    }

    private fun loadModel() {
        try {
            val baseOptions = BaseOptions.builder()
                .setNumThreads(4) // Количество потоков
                .build()

            val options = ImageClassifier.ImageClassifierOptions.builder()
                .setBaseOptions(baseOptions)
//                .setMaxResults(3) // Показывать топ-3 результата
                .setScoreThreshold(0.01f) // Минимальный порог уверенности
                .build()

            classifier = ImageClassifier.createFromFileAndOptions(
                context,
                "plant_disease_model_generated.tflite",
                options
            )
        } catch (e: Exception) {
            Log.e("Abobus", "${e}")
            e.printStackTrace()
        }
    }

    private fun loadLabels() {
        try {
            context.assets.open("class_labels.txt").use { inputStream ->
                labels = inputStream.bufferedReader().useLines { lines ->
                    lines.toList()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun processResults(results: List<Classifications>): List<Pair<String, Float>> {
        val classifications = mutableListOf<Pair<String, Float>>()

        for (classification in results) {
            for (category in classification.categories) {
                val label = if (category.index < labels.size) {
                    labels[category.index]
                } else {
                    "Класс ${category.index}"
                }
                classifications.add(label to category.score)
            }
        }

        // Сортируем по уверенности (по убыванию)
        return classifications.sortedByDescending { it.second }
    }
}