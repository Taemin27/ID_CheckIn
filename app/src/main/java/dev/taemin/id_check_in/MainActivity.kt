package dev.taemin.id_check_in

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        val requestMultiplePermissions = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            permissions.entries.forEach {
                Log.d("DEBUG", "${it.key} = ${it.value}")
            }
        }

        requestMultiplePermissions.launch(
            arrayOf(
                android.Manifest.permission.CAMERA,
                android.hardware.camera.any
            )
        )
    }



}