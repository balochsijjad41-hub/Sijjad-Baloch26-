package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.theme.OrbitronFontFamily
import com.example.ui.theme.RajdhaniFontFamily

@Composable
fun CameraScannerOverlay(
    onCaptureImage: (Bitmap, String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val accent = MaterialTheme.colorScheme.primary

    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var selectedPromptType by remember { mutableStateOf("dekho yeh kya hai") }

    val infiniteTransition = rememberInfiniteTransition(label = "scanner_beam")
    val scanProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scan_beam"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // CameraX Live Preview View
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }
                    val capture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build()
                    imageCapture = capture

                    val cameraSelector = CameraSelector.Builder()
                        .requireLensFacing(lensFacing)
                        .build()

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            capture
                        )
                    } catch (_: Exception) {}
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            }
        )

        // Futuristic Sci-Fi HUD Scanline & Crosshairs Overlay
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Scanning horizontal laser beam
            val beamY = h * scanProgress
            drawLine(
                color = accent.copy(alpha = 0.8f),
                start = Offset(0f, beamY),
                end = Offset(w, beamY),
                strokeWidth = 3f
            )

            // Corner brackets
            val bracketLen = 40f
            val stroke = 3.5f
            val inset = 30f

            // Top Left
            drawLine(accent, Offset(inset, inset), Offset(inset + bracketLen, inset), stroke)
            drawLine(accent, Offset(inset, inset), Offset(inset, inset + bracketLen), stroke)

            // Top Right
            drawLine(accent, Offset(w - inset, inset), Offset(w - inset - bracketLen, inset), stroke)
            drawLine(accent, Offset(w - inset, inset), Offset(w - inset, inset + bracketLen), stroke)

            // Bottom Left
            drawLine(accent, Offset(inset, h - inset), Offset(inset + bracketLen, h - inset), stroke)
            drawLine(accent, Offset(inset, h - inset), Offset(inset, h - inset - bracketLen), stroke)

            // Bottom Right
            drawLine(accent, Offset(w - inset, h - inset), Offset(w - inset - bracketLen, h - inset), stroke)
            drawLine(accent, Offset(w - inset, h - inset), Offset(w - inset, h - inset - bracketLen), stroke)
        }

        // Top HUD Bar: Close & Camera Switch
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 40.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0x99000000))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "JARVIS // OPTICAL SENSOR",
                    fontFamily = OrbitronFontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = accent
                )
            }

            Row {
                IconButton(
                    onClick = {
                        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                            CameraSelector.LENS_FACING_FRONT
                        } else {
                            CameraSelector.LENS_FACING_BACK
                        }
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0x99000000))
                        .testTag("switch_camera_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Cameraswitch,
                        contentDescription = "Switch Camera",
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0x99000000))
                        .testTag("close_camera_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Camera",
                        tint = Color.White
                    )
                }
            }
        }

        // Bottom HUD Bar: Prompt Type Buttons & Big Shutter Button
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 36.dp, start = 16.dp, end = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Quick preset pills: "Identify", "Read Text", "Translate"
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                QuickPromptPill(
                    label = "Identify Object",
                    sub = "dekho yeh kya hai",
                    isSelected = selectedPromptType == "dekho yeh kya hai",
                    onClick = { selectedPromptType = "dekho yeh kya hai" }
                )
                QuickPromptPill(
                    label = "Read Text",
                    sub = "ye text padho",
                    isSelected = selectedPromptType == "Read and summarize all text visible in this view.",
                    onClick = { selectedPromptType = "Read and summarize all text visible in this view." }
                )
                QuickPromptPill(
                    label = "Translate",
                    sub = "tarjuma karo",
                    isSelected = selectedPromptType == "Translate any text visible here into Urdu and English.",
                    onClick = { selectedPromptType = "Translate any text visible here into Urdu and English." }
                )
            }

            // Big Tactical Shutter Button
            Button(
                onClick = {
                    val capture = imageCapture ?: return@Button
                    val executor = ContextCompat.getMainExecutor(context)
                    capture.takePicture(executor, object : ImageCapture.OnImageCapturedCallback() {
                        override fun onCaptureSuccess(image: ImageProxy) {
                            val bitmap = imageProxyToBitmap(image)
                            image.close()
                            if (bitmap != null) {
                                onCaptureImage(bitmap, selectedPromptType)
                            }
                        }

                        override fun onError(exception: ImageCaptureException) {}
                    })
                },
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .border(3.dp, accent, CircleShape)
                    .testTag("shutter_capture_button"),
                colors = ButtonDefaults.buttonColors(containerColor = accent.copy(alpha = 0.35f))
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(accent)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "TAP SHUTTER TO ANALYZE TARGET",
                fontFamily = OrbitronFontFamily,
                fontSize = 10.sp,
                letterSpacing = 1.sp,
                color = accent
            )
        }
    }
}

@Composable
private fun QuickPromptPill(
    label: String,
    sub: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val accent = MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) accent.copy(alpha = 0.3f) else Color(0x99000000))
            .border(1.dp, if (isSelected) accent else Color.Gray.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag("prompt_pill_$label")
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                fontFamily = OrbitronFontFamily,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) accent else Color.White
            )
            Text(
                text = sub,
                fontFamily = RajdhaniFontFamily,
                fontSize = 10.sp,
                color = Color.LightGray
            )
        }
    }
}

private fun imageProxyToBitmap(image: ImageProxy): Bitmap? {
    val plane = image.planes[0]
    val buffer = plane.buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null

    val rotation = image.imageInfo.rotationDegrees
    return if (rotation != 0) {
        val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
        Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    } else {
        bitmap
    }
}
