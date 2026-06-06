import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.task.core.BaseOptions
import org.tensorflow.lite.task.vision.classifier.Classifications
import org.tensorflow.lite.task.vision.classifier.ImageClassifier
import kotlin.math.exp

class TomatoDiseaseClassifier(private val context: Context) {

    private var classifier: ImageClassifier? = null
    private var labels: List<String> = emptyList()

    private val imageProcessor by lazy {
        ImageProcessor.Builder()
            .add(ResizeOp(224, 224, ResizeOp.ResizeMethod.BILINEAR))
            .build()
    }

    init {
        loadModel()
        loadLabels()
    }

    fun classify(bitmap: Bitmap): List<Pair<String, Float>> {
        if (classifier == null) {
            throw Exception("Модель не загружена")
        }


        var tensorImage = TensorImage.fromBitmap(bitmap)

        tensorImage = imageProcessor.process(tensorImage)

        val results = classifier!!.classify(tensorImage)

        return processResults(results)

    }

    fun close() {
        classifier?.close()
        classifier = null
    }

    private fun loadModel() {
        try {
            val baseOptions = BaseOptions.builder()
                .setNumThreads(4)
                .build()

            val options = ImageClassifier.ImageClassifierOptions.builder()
                .setBaseOptions(baseOptions)
                .setScoreThreshold(0.01f)
                .build()

            classifier = ImageClassifier.createFromFileAndOptions(
                context,
                "plant_disease_model_with_metadata.tflite",
                options
            )
        } catch (e: Exception) {
            Log.e("TomatoClassifier", "Ошибка загрузки модели: $e")
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
            Log.e("TomatoClassifier", "Ошибка загрузки лейблов: $e")
        }
    }

    private fun processResults(results: List<Classifications>): List<Pair<String, Float>> {
        val classifications = mutableListOf<Pair<String, Float>>()

        for (classification in results) {
            val categories = classification.categories
            if (categories.isEmpty()) continue

            val logits = categories.map { it.score }

            val maxLogit = logits.maxOrNull() ?: 0f
            val exps = logits.map { exp(it - maxLogit) }
            val sumExps = exps.sum()
            val probabilities = exps.map { it / sumExps }

            categories.forEachIndexed { index, category ->
                val label = if (category.index < labels.size) {
                    labels[category.index]
                } else {
                    category.label.ifEmpty { "Класс ${category.index}" }
                }
                classifications.add(label to probabilities[index])
            }
        }

        return classifications.sortedByDescending { it.second }
    }
}
