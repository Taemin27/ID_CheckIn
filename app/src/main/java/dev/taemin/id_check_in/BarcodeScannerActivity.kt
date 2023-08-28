package dev.taemin.id_check_in

import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.core.content.ContextCompat
import androidx.camera.lifecycle.ProcessCameraProvider
import dev.taemin.id_check_in.databinding.ActivityBarcodeScannerBinding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class BarcodeScannerActivity : AppCompatActivity() {
    private lateinit var binding : ActivityBarcodeScannerBinding
    private lateinit var cameraExecutor : ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBarcodeScannerBinding.inflate(layoutInflater, null, false)
        setContentView(binding.root)
        cameraExecutor = Executors.newSingleThreadExecutor()

        requestPermission()

        val btn_back = findViewById<Button>(R.id.button_back)
        btn_back.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.apply {

            }
            startActivity(intent)
        }
    }
    private fun requestPermission() {
        requestCameraPermissionIfMissing { granted ->
            if(granted) {
                startCamera()
            } else {
                Toast.makeText(this, "Please Allow the Permission", Toast.LENGTH_LONG).show()
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
    private fun startCamera() {
        val processCameraProvider = ProcessCameraProvider.getInstance(this)
        processCameraProvider.addListener({
            try{
                val cameraProvider = processCameraProvider.get()
                val previewUseCase = buildPreviewUseCase()

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_FRONT_CAMERA, previewUseCase)
            } catch (e: Exception){
                Log.d("ERROR", e.message.toString())
                //Toast.makeText(this, "Error starting the camera", Toast.LENGTH_LONG).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    fun buildPreviewUseCase(): Preview {
        return Preview.Builder().build().also { it.setSurfaceProvider(binding.previewView.createSurfaceProvider())}
    }
}