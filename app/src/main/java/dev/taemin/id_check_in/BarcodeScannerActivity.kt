package dev.taemin.id_check_in

import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.core.content.ContextCompat
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.lifecycle.LifecycleOwner
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import dev.taemin.id_check_in.databinding.ActivityBarcodeScannerBinding
import java.util.concurrent.Executors


class BarcodeScannerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBarcodeScannerBinding
    private lateinit var cameraProviderFuture : ListenableFuture<ProcessCameraProvider>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBarcodeScannerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Back button listener
        val btn_back = binding.buttonBack
        btn_back.setOnClickListener {
            backToMain()
        }

        requestPermission()
    }

    private fun startCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener(Runnable {
            val cameraProvider = cameraProviderFuture.get()
            bindCameraUseCases(cameraProvider)
        }, ContextCompat.getMainExecutor(this))
    }
    private fun bindCameraUseCases(cameraProvider : ProcessCameraProvider) {

        // Setting up the barcode scanner
        val options = BarcodeScannerOptions.Builder().setBarcodeFormats(
            Barcode.FORMAT_CODE_128,
            Barcode.FORMAT_CODE_39,
            Barcode.FORMAT_CODE_93,
            Barcode.FORMAT_EAN_8,
            Barcode.FORMAT_EAN_13,
            Barcode.FORMAT_QR_CODE,
            Barcode.FORMAT_UPC_A,
            Barcode.FORMAT_UPC_E,
            Barcode.FORMAT_PDF417
        ).build()

        val scanner = BarcodeScanning.getClient(options)

        // Analysis use case
        val analysisUseCase = ImageAnalysis.Builder().build()
        analysisUseCase.setAnalyzer(
            Executors.newSingleThreadExecutor(),
            { imageProxy ->
                analyze(imageProxy, scanner)
            }
        )

        // Preview use case
        var preview : Preview = Preview.Builder().build()
        preview.setSurfaceProvider(binding.previewView.createSurfaceProvider())

        var cameraSelector : CameraSelector = CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_FRONT).build()

        // Bind to life cycle
        var camera = cameraProvider.bindToLifecycle(this as LifecycleOwner, cameraSelector, analysisUseCase, preview)
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun analyze(imageProxy: ImageProxy, scanner: BarcodeScanner) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            // Pass image to an ML Kit Vision API
            val result = scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    // Task completed successfully
                    for (barcode in barcodes) {
                        //val bounds = barcode.boundingBox
                        //val corners = barcode.cornerPoints

                        val rawValue = barcode.rawValue
                        binding.textViewRawValue.text = rawValue
                    }
                }
                .addOnFailureListener {
                    // Task failed with an exception

                }
                .addOnCompleteListener {
                    imageProxy.image?.close()
                    imageProxy.close()
                }
        }
    }

    private fun backToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.apply {

        }
        startActivity(intent)
    }
    private fun requestPermission() {
        requestCameraPermissionIfMissing { granted ->
            if(granted) {
                startCamera()
            } else {
                Toast.makeText(this, "Please Allow the Permission", Toast.LENGTH_LONG).show()
                backToMain()
            }
        }
    }
    private fun requestCameraPermissionIfMissing(onResult: ((Boolean) -> Unit)) {
        if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == PERMISSION_GRANTED) {
            onResult(true)
        } else {
            registerForActivityResult(ActivityResultContracts.RequestPermission()) {
                onResult(it)
            }.launch(android.Manifest.permission.CAMERA)
        }
    }
}