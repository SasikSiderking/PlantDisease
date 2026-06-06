package sasik.siderking.plantdisease

import TomatoDiseaseClassifier
import TomatoLeafDetector
import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import androidx.compose.foundation.layout.size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream

class MainActivityViewModel(application: Application) : AndroidViewModel(application) {
    private val applicationContext = application.applicationContext
    private val plantDiseaseClassifier = TomatoDiseaseClassifier(applicationContext)
    private val leafDetector = TomatoLeafDetector(applicationContext)

    private val _uiState = MutableStateFlow(ImagePickerUiState())
    private val colors: List<Color> = listOf(
        Color(0xFFE53935),
        Color(0xFF00FA9A),
                Color(0xFF1E88E5),
        Color(0xFFFB8C00),
        Color(0xFF8E24AA),
        Color(0xFF00ACC1),
        Color(0xFFFDD835),
        Color(0xFF6D4C41)
    )

        val uiState: StateFlow<ImagePickerUiState> = _uiState.asStateFlow()

    fun setImageUri(uri: Uri?) {
        if (uri == null) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, leafResults = emptyList())

            val result = withContext(Dispatchers.IO) {
                try {
                    val originalBitmap = loadBitmapFromUri(uri)
                        ?: return@withContext Result.failure(Throwable("Не удалось загрузить изображение"))

                    val leafBoxes = leafDetector.detectLeaves(originalBitmap)

                    if (leafBoxes.isEmpty()) {
                        return@withContext Result.failure(Throwable("Листья на фото не обнаружены"))
                    }

                    val mutableBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true)
                    val canvas = Canvas(mutableBitmap)

                    val resultsList = mutableListOf<LeafResult>()

                    val screenWidthPx = applicationContext.resources.displayMetrics.widthPixels.toFloat()
                    val scaleFactor = (originalBitmap.width / screenWidthPx)
                    val density = applicationContext.resources.displayMetrics.density
                    val strokePx = 5f * density * scaleFactor
                    val textPx = 24f * density * scaleFactor

                    leafBoxes.forEachIndexed { index, box ->

                        val itemColor =  colors.getOrElse(index % colors.size){ Color.Blue }

                        val boxPaint = Paint().apply {
                            color = itemColor.toArgb()
                            style = Paint.Style.STROKE
                            strokeWidth = strokePx
                        }

                        val textPaint = Paint().apply {
                            color = itemColor.toArgb()
                            textSize = textPx
                            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        }

                        val leafId = index + 1

                        canvas.drawRect(box, boxPaint)
                        canvas.drawText("#$leafId", box.left, (box.top - 10f).coerceAtLeast(box.top), textPaint)

                        val width = box.width().toInt()
                        val height = box.height().toInt()
                        val paddingW = (width * 0.10f).toInt()
                        val paddingH = (height * 0.10f).toInt()

                        val cropLeft = maxOf(0, box.left.toInt() - paddingW)
                        val cropTop = maxOf(0, box.top.toInt() - paddingH)
                        val cropWidth = minOf(originalBitmap.width - cropLeft, width + (paddingW * 2))
                        val cropHeight = minOf(originalBitmap.height - cropTop, height + (paddingH * 2))

                        if (cropWidth > 0 && cropHeight > 0) {
                            val croppedLeaf = Bitmap.createBitmap(originalBitmap, cropLeft, cropTop, cropWidth, cropHeight)

                            val classifications = plantDiseaseClassifier.classify(croppedLeaf)

                            val topPrediction = classifications.firstOrNull()
                            if (topPrediction != null) {
                                resultsList.add(
                                    LeafResult(
                                        id = leafId,
                                        diseaseName = topPrediction.first,
                                        itemColor = itemColor.copy(alpha = 0.5f)
                                    )
                                )
                            }
                        }
                    }

                    Result.success(Pair(mutableBitmap, resultsList))
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }

            result.onSuccess { (bitmapWithBoxes, results) ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    processedBitmap = bitmapWithBoxes,
                    leafResults = results
                )
            }

            result.onFailure {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = it.message ?: "Неизвестная ошибка"
                )
            }
        }
    }

    fun setError(errorMessage: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(error = errorMessage)
        }
    }

    fun setErrorWasShown() { _uiState.value = _uiState.value.copy(error = null) }

    override fun onCleared() {
        plantDiseaseClassifier.close()
        leafDetector.close()
        super.onCleared()
    }

    private fun loadBitmapFromUri(uri: Uri): Bitmap? {
        return try {
            val inputStream: InputStream? = applicationContext.contentResolver.openInputStream(uri)
            inputStream?.use { BitmapFactory.decodeStream(it) }
        } catch (e: Exception) {
            null
        }
    }
}

data class ImagePickerUiState(
    val processedBitmap: Bitmap? = null,
    val isLoading: Boolean = false,
    val leafResults: List<LeafResult> = emptyList(),
    val error: String? = null,
)

data class LeafResult(
    val id: Int,
    val diseaseName: String,
    val itemColor: Color = Color.Blue.copy(alpha = 0.5f)
)