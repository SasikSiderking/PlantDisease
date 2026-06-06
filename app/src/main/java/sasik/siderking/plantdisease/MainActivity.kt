package sasik.siderking.plantdisease

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import sasik.siderking.plantdisease.ui.theme.PlantDiseaseTheme
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PlantDiseaseTheme {
                val viewModel: MainActivityViewModel = viewModel()
                val uiState by viewModel.uiState.collectAsState()

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = Color(0xFFF5F5F5)
                ) { innerPadding ->
                    ImagePickerScreen(
                        modifier = Modifier.padding(innerPadding),
                        uiState = uiState,
                        setImageUri = { viewModel.setImageUri(it) },
                        setErrorWasShown = { viewModel.setErrorWasShown() },
                        setError = {viewModel.setError(it)}
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImagePickerScreen(
    modifier: Modifier = Modifier,
    uiState: ImagePickerUiState,
    setImageUri: (uri: Uri) -> Unit,
    setErrorWasShown: () -> Unit,
    setError: (errorMsg: String) -> Unit
) {
    Log.e("Abobus", "sus")
    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current

    val tempPhotoFile = remember {
        if (!isPreview) createImageFile(context) else null
    }

    val tempPhotoUri = remember {
        if (!isPreview && tempPhotoFile != null) {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                tempPhotoFile
            )
        } else Uri.EMPTY
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data ?: tempPhotoUri
            setImageUri(uri)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }

        if (allGranted) {
            showImagePicker(imagePickerLauncher, tempPhotoUri)
        } else {
            val deniedPermissions = permissions.filter { !it.value }.keys
            if (deniedPermissions.isNotEmpty()) {
                setError("Для работы приложению нужны разрешения: ${deniedPermissions}")
            }
        }
    }

    fun requestPermissions() {
        val permissions = getRequiredPermissions()

        Log.e("Abobus", "request permissions $permissions")
        permissionLauncher.launch(permissions.toTypedArray())
    }

    LaunchedEffect(null) {
        if (hasPermissions(context).not()) {
            requestPermissions()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFF5F5F5), Color(0xFFE8EAF6))
                )
            )
    ) {
        if (!isPreview) {
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = "🌱 Определитель болезней томатов",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2E7D32),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 20.dp, bottom = 16.dp)
            )

            Text(
                text = stringResource(R.string.title_description),
                fontSize = 14.sp,
                color = Color(0xFF546E7A),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (uiState.processedBitmap != null) 280.dp else 200.dp),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White,
                    contentColor = Color(0xFF2E7D32)
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (uiState.processedBitmap != null) {
                            Image(
                                bitmap = uiState.processedBitmap.asImageBitmap(),
                                contentDescription = "Selected image",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(24.dp)),
                                contentScale = ContentScale.Fit
                            )

                        IconButton(
                            onClick = {
                                if (hasPermissions(context)) {
                                    showImagePicker(imagePickerLauncher, tempPhotoUri)
                                } else requestPermissions()
                            },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(16.dp)
                                .size(40.dp)
                                .background(
                                    Color.White.copy(alpha = 0.9f),
                                    CircleShape
                                )
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Change image",
                                tint = Color(0xFF2E7D32)
                            )
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "No image",
                                modifier = Modifier.size(64.dp),
                                tint = Color(0xFFA5D6A7)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.image_not_chosen),
                                color = Color(0xFF9E9E9E),
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (uiState.isLoading) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFF2E7D32)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp),
                            color = Color(0xFF4CAF50),
                            strokeWidth = 3.dp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.processing),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF2E7D32)
                        )
                        Text(
                            text = stringResource(R.string.take_your_time),
                            fontSize = 12.sp,
                            color = Color(0xFF9E9E9E),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            if (uiState.leafResults.isNotEmpty()) {
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + slideInVertically()
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White,
                            contentColor = Color(0xFF2E7D32)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF4CAF50),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.results),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2E7D32)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            LazyColumn(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                itemsIndexed(uiState.leafResults) { index, result ->
                                    PredictionCard(index, result)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    Log.e("Abobus", "click!")
                    if (hasPermissions(context)) {
                        Log.e("Abobus", "click!2")
                        showImagePicker(imagePickerLauncher, tempPhotoUri)
                    } else requestPermissions()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50),
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 4.dp,
                    pressedElevation = 8.dp
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.choose_image),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        uiState.error?.let {
            CustomErrorDialog(
                error = it,
                onDismiss = setErrorWasShown
            )
        }
    }
}

@Composable
fun PredictionCard(index: Int, prediction: LeafResult) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = prediction.itemColor
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = prediction.id.toString(),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2E7D32),
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.White, CircleShape)
                    .wrapContentSize(Alignment.Center)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = prediction.diseaseName,
                fontSize = 15.sp,
                color = Color(0xFF37474F),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun CustomErrorDialog(
    error: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = null,
                    tint = Color(0xFFF44336),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.understood),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFD32F2F)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = error,
                    fontSize = 14.sp,
                    color = Color(0xFF757575),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    )
                ) {
                    Text("Понятно", color = Color.White)
                }
            }
        }
    }
}

private fun hasPermissions(context: Context): Boolean {
    val permissions = getRequiredPermissions()
    Log.e("Abobus", "permissions ${permissions}")
    return permissions.all { permission ->
        ContextCompat.checkSelfPermission(context, permission).also {
            Log.e(
                "Abobus",
                "permission ${permission} is ${it}"
            )
        } == PackageManager.PERMISSION_GRANTED
    }
}

private fun getRequiredPermissions(): List<String> {
    return when {
        Build.VERSION.SDK_INT >= 33 -> {
            listOf(
                Manifest.permission.CAMERA,
                Manifest.permission.READ_MEDIA_IMAGES
            )
        }

        Build.VERSION.SDK_INT >= 29 -> {
            listOf(
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        }

        else -> {
            listOf(
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }
    }
}

private fun showImagePicker(
    launcher: ActivityResultLauncher<Intent>,
    tempPhotoUri: Uri
) {
    val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
        putExtra(MediaStore.EXTRA_OUTPUT, tempPhotoUri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    val galleryIntent = Intent(Intent.ACTION_PICK).apply {
        type = "image/*"
    }

    val chooserIntent = Intent.createChooser(galleryIntent, "Выберите изображение").apply {
        putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(cameraIntent))
    }
    Log.e("Abobus", "launch")
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
        ImagePickerScreen(
            modifier = Modifier,
            uiState = ImagePickerUiState(
                processedBitmap = null,
                isLoading = false,
                leafResults = listOf(
                    LeafResult(
                        id = 1,
                        diseaseName =  "Ржавчина листьев",
                    ),
                    LeafResult(
                        id = 1,
                        diseaseName =  "Мучнистая роса",
                    ),
                    LeafResult(
                        id = 1,
                        diseaseName =  "Пятнистость листьев",
                    ),
                    LeafResult(
                        id = 1,
                        diseaseName =  "Корневая гниль",
                    ),
                ),
                error = null
            ),
            setImageUri = {},
            setErrorWasShown = {},
            setError = {}
        )
    }
}