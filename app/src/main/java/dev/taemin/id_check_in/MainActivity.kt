package dev.taemin.id_check_in

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btn_launchScanner = findViewById<Button>(R.id.button_launchScanner)

        btn_launchScanner.setOnClickListener {
            val intent = Intent(this, BarcodeScannerActivity::class.java)
            intent.apply {

            }
            startActivity(intent)
        }

    }
}