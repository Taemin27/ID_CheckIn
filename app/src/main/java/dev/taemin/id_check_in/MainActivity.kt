package dev.taemin.id_check_in

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val spref : SharedPreferences = getSharedPreferences("pref", MODE_PRIVATE);
        val editor : SharedPreferences.Editor = spref.edit();

        val text_returnPassword = findViewById<EditText>(R.id.editText_returnPassword)

        text_returnPassword.setText(spref.getString("returnPassword", ""))

        val btn_launchScanner = findViewById<Button>(R.id.button_launchScanner)
        btn_launchScanner.setOnClickListener {
            if(spref.getString("returnPassword", "") == "") {
                Toast.makeText(this, "Please set the password before launching.", Toast.LENGTH_LONG)
            } else {
                val intent = Intent(this, BarcodeScannerActivity::class.java)
                intent.apply {

                }
                startActivity(intent)
            }
        }

        val btn_setReturnPassword = findViewById<Button>(R.id.button_setReturnPassword)
        btn_setReturnPassword.setOnClickListener {
            editor.putString("returnPassword", text_returnPassword.text.toString())
            editor.apply()
            Toast.makeText(this, "Return password set!", Toast.LENGTH_LONG)
        }
    }
}