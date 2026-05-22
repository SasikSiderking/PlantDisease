package sasik.siderking.plantdisease

import TomatoDiseaseClassifier
import TomatoLeafDetector
import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
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
    val uiState: StateFlow<ImagePickerUiState> = _uiState.asStateFlow()

    fun setImageUri(uri: Uri?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(imageUri = uri)

            val result = withContext(Dispatchers.IO) {
                try {
                    uri?.let { loadBitmapFromUri(it) }?.let {
                        val croppedLeafBitmap = leafDetector.detectAndCropLeaf(it)
                        val classification = plantDiseaseClassifier.classify(croppedLeafBitmap)
                        Result.success(classification)
                    } ?: Result.failure(Throwable(message = "Ошибка при получепнии изображения"))
                } catch (e: Exception) {
                    Result.failure(exception = e)
                }
            }

            result.onSuccess {
                Log.i("Abobus Success", "${it}")
                _uiState.value = _uiState.value.copy(predictions = it.take(3).map { it.first + "-" + "%.2f%%".format(it.second * 100) })
            }

            result.onFailure {
                Log.i("Abobus Failure", "${it}")
                _uiState.value = _uiState.value.copy(error = it.message)
            }
        }
    }

    fun setError(errorMessage: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(error = errorMessage)
        }
    }

    fun setErrorWasShown() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(error = null)
        }
    }

    fun onDestroy() {
        plantDiseaseClassifier.close()
    }

    override fun onCleared() {
        plantDiseaseClassifier.close()
        super.onCleared()
    }

    private fun loadBitmapFromUri(uri: Uri): Bitmap? {
        var bitmap: Bitmap? = null
        val inputStream: InputStream? = applicationContext.contentResolver.openInputStream(uri)
        inputStream?.use { stream ->
            bitmap = BitmapFactory.decodeStream(stream)
        }
        return bitmap
    }
}

data class ImagePickerUiState(
    val imageUri: Uri? = null,
    val isLoading: Boolean = false,
    val predictions: List<String>? = null,
    val error: String? = null
)