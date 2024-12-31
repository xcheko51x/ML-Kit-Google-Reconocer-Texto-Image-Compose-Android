package com.example.ml_kit_google_compose

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.activity.result.contract.ActivityResultContracts.TakePicture
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.ml_kit_google_compose.ui.theme.ML_KIT_Google_ComposeTheme
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    private var fotoPath: String? = null

    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var takePictureLauncher: ActivityResultLauncher<Uri>

    private var recognizerText by mutableStateOf("Texto leido")

    private var imageBitmap by mutableStateOf<ImageBitmap?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestPermissionLauncher = registerForActivityResult(RequestPermission()) { isGranted ->
            if (isGranted) {
                captureImage()
            } else {
                Toast.makeText(this, "Permisos camara negados", Toast.LENGTH_SHORT).show()
            }
        }

        takePictureLauncher = registerForActivityResult(TakePicture()) { success ->
            if (success) {
                fotoPath?.let { path ->
                    val bitmap = BitmapFactory.decodeFile(path)
                    recognizeText(bitmap)
                    imageBitmap = bitmap.asImageBitmap()
                }
            }
        }

        setContent {
            ML_KIT_Google_ComposeTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MLKitTextRecognitionScreen()
                }
            }
        }
    }

    private fun recognizeText(bitmap: Bitmap) {
        val image = InputImage.fromBitmap(bitmap, 0)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        recognizer.process(image)
            .addOnSuccessListener { ocrText ->
                recognizerText = ocrText.text ?: "No se encontro texto"
            }
            .addOnFailureListener {
                Toast.makeText(this, "Fallo al reconocer el texto", Toast.LENGTH_SHORT).show()
            }
    }

    private fun crearArchivoImagen(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("JPEG_${timeStamp}_", "jpg", storageDir).apply {
            fotoPath = absolutePath
        }
    }

    private fun captureImage() {
        val fotoFile: File? = try {
            crearArchivoImagen()
        } catch (ex: IOException) {
            Toast.makeText(this, "Ocurrio un error al crear el archivo", Toast.LENGTH_SHORT).show()
            null
        }

        fotoFile?.also {
            val fotoUri: Uri = FileProvider.getUriForFile(this, "${applicationContext.packageName}.provider", it)
            takePictureLauncher.launch(fotoUri)
        }
    }

    @Composable
    fun MLKitTextRecognitionScreen() {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (imageBitmap == null) {
                Image(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    painter = painterResource(R.drawable.image_24),
                    contentDescription = ""
                )
            } else {
                imageBitmap?.let {
                    Image(
                        modifier = Modifier
                            .height(200.dp),
                        painter = BitmapPainter(it),
                        contentDescription = ""
                    )
                }
            }

            Button(
                onClick =  {
                    requestPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                }
            ) {
                Text(text = "TOMAR FOTO")
            }

            Text(
                modifier = Modifier
                    .padding(top = 16.dp),
                text = "Texto leido",
                style = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.Bold)
            )

            SelectionContainer(
                modifier = Modifier
                    .padding(top = 16.dp)
            ) {
                Text(
                    text = recognizerText,
                    style = TextStyle(fontSize = 20.sp)
                )
            }
        }
    }
}