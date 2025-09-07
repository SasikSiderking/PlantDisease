package sasik.siderking.plantdisease

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.rememberAsyncImagePainter
import sasik.siderking.plantdisease.ui.theme.PlantDiseaseTheme
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PlantDiseaseTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ImagePickerScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun ImagePickerScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var imageUri by remember { mutableStateOf<Uri?>(null) }

    // Создаем временный файл для фото
    val tempPhotoFile = remember {
        createImageFile(context)
    }

    val tempPhotoUri = remember {
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            tempPhotoFile
        )
    }

    // Лаунчер для выбора изображения
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val uri = result.data?.data ?: tempPhotoUri
            imageUri = uri
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Отображение выбранного изображения
        imageUri?.let { uri ->
            Image(
                painter = rememberAsyncImagePainter(uri),
                contentDescription = "Selected image",
                modifier = Modifier
                    .size(200.dp)
                    .padding(16.dp),
                contentScale = ContentScale.Crop
            )
        }

        // Кнопка для выбора изображения
        Button(
            onClick = {
                showImagePicker(context, imagePickerLauncher, tempPhotoUri)
            }
        ) {
            Text("Выбрать изображение")
        }
    }
}

private fun showImagePicker(
    context: Context,
    launcher: ActivityResultLauncher<Intent>,
    tempPhotoUri: Uri
) {
    // Intent для камеры
    val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
        putExtra(MediaStore.EXTRA_OUTPUT, tempPhotoUri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    // Intent для галереи
    val galleryIntent = Intent(Intent.ACTION_PICK).apply {
        type = "image/*"
    }

    // Создаем chooser intent с обоими вариантами
    val chooserIntent = Intent.createChooser(galleryIntent, "Выберите изображение").apply {
        putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(cameraIntent))
    }

    launcher.launch(chooserIntent)
}

private fun createImageFile(context: Context): File {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
    return File.createTempFile(
        "JPEG_${timeStamp}_",
        ".jpg",
        storageDir
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    PlantDiseaseTheme {
    }
}