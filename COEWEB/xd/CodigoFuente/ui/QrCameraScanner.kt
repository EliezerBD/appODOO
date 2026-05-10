package com.example.odooapp.ui

import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage

/**
 * Componente de bajo nivel para el escaneo de QR.
 * Utiliza CameraX para la vista previa y ML Kit para procesar la imagen y detectar códigos.
 */
@OptIn(ExperimentalGetImage::class)
@Composable
fun QrCameraScanner(
    modifier: Modifier = Modifier,
    onScan: (String) -> Unit // Se llama cada vez que detecta un texto en un código QR
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    // Usamos AndroidView para integrar una vista clásica de Android (PreviewView) en Compose
    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val executor = ContextCompat.getMainExecutor(ctx)
            
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                
                // 1. Configuración de la vista previa
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                // 2. Configuración del análisis de imagen (Escaneo de QR)
                val scanner = BarcodeScanning.getClient()
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                imageAnalysis.setAnalyzer(executor) { imageProxy ->
                    val mediaImage = imageProxy.image
                    if (mediaImage != null) {
                        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                        scanner.process(image)
                            .addOnSuccessListener { barcodes ->
                                for (barcode in barcodes) {
                                    // Si detecta un código, envía el valor al callback
                                    barcode.rawValue?.let { value ->
                                        onScan(value)
                                    }
                                }
                            }
                            .addOnFailureListener {
                                Log.e("QrCameraScanner", "Error al escanear QR", it)
                            }
                            .addOnCompleteListener {
                                // Es vital cerrar el frame para procesar el siguiente
                                imageProxy.close()
                            }
                    } else {
                        imageProxy.close()
                    }
                }

                // 3. Selección de cámara trasera
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    // Desvincular cualquier uso previo y conectar al ciclo de vida de la pantalla
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )
                } catch (e: Exception) {
                    Log.e("QrCameraScanner", "Fallo al iniciar cámara", e)
                }
            }, executor)
            previewView
        },
        modifier = modifier
    )
}
