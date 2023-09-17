package dev.taemin.id_check_in

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraX
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.BasicNetwork
import com.android.volley.toolbox.DiskBasedCache
import com.android.volley.toolbox.HurlStack
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import dev.taemin.id_check_in.databinding.ActivityBarcodeScannerBinding
import org.json.JSONArray
import java.util.concurrent.Executors


class BarcodeScannerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBarcodeScannerBinding
    private lateinit var cameraProviderFuture : ListenableFuture<ProcessCameraProvider>

    val scriptURL = "https://script.google.com/macros/s/AKfycbxlv2jKISndLDW4JnPOwWOudd_6m4IKM1BJ6ayclSzq_IPe_y2uca5U-hKf7kFAjaEo/exec"
    lateinit var dataJSONArray : JSONArray
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBarcodeScannerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Toast.makeText(this, "Fetching data...", Toast.LENGTH_SHORT).show()
        getData()
        requestPermission()

        // Back button listener
        val btn_back = binding.buttonBack
        btn_back.setOnClickListener {
            passwordDialog()
        }
        val btn_typeID = binding.buttonTypeID
        btn_typeID.setOnClickListener{
            typeIDDialog()
        }
    }

    private fun getData() {
        val queue = Volley.newRequestQueue(this);
        val jsonObjectRequest = JsonObjectRequest(Request.Method.GET, scriptURL, null,
            Response.Listener { response ->
                dataJSONArray = response.getJSONArray("data")
                Toast.makeText(this, "Data refreshed", Toast.LENGTH_SHORT).show()
            },
            Response.ErrorListener { error ->
                Toast.makeText(this, "Error while requesting data JSON", Toast.LENGTH_LONG).show()
            }
        )
        queue.add(jsonObjectRequest)
    }

    private fun backToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.apply {

        }
        startActivity(intent)
    }

    @SuppressLint("RestrictedApi")
    private fun typeIDDialog() {
        val dialogView: View = layoutInflater.inflate(R.layout.activity_typeinid, null)
        val builder: AlertDialog.Builder = AlertDialog.Builder(dialogView.context)

        builder.setView(dialogView)
        builder.setCancelable(false)
        val dialog = builder.create()

        CameraX.unbindAll()

        dialog.show()

        val editText_ID = dialogView.findViewById<EditText>(R.id.editText_idNumber)

        val btn_enter = dialogView.findViewById<Button>(R.id.button_enterID)
        btn_enter.setOnClickListener {
            val text = editText_ID.text.toString()
            if(text == "") {
                Toast.makeText(this, "Enter your ID first!", Toast.LENGTH_SHORT).show()
            } else {
                dialog.dismiss()
                checkInDialog(text)
            }
        }

        val btn_cancel = dialogView.findViewById<Button>(R.id.button_cancelID)
        btn_cancel.setOnClickListener {
            startCamera()
            dialog.dismiss()
        }
    }

    @SuppressLint("RestrictedApi")
    private fun passwordDialog() {
        val dialogView: View = layoutInflater.inflate(R.layout.activity_returnpassworddialog, null)
        val builder: AlertDialog.Builder = AlertDialog.Builder(dialogView.context)

        builder.setView(dialogView)
        builder.setCancelable(false)
        val dialog = builder.create()

        CameraX.unbindAll()

        dialog.show()

        val spref : SharedPreferences = getSharedPreferences("pref", MODE_PRIVATE);

        val editTxt_returnPassword = dialogView.findViewById<EditText>(R.id.editText_returnPassword)

        val btn_enter = dialogView.findViewById<Button>(R.id.button_enterPassword)
        btn_enter.setOnClickListener {
            if(editTxt_returnPassword.text.toString() == spref.getString("returnPassword", "")) {
                backToMain()
                dialog.dismiss()
            } else {
                Toast.makeText(this, "Wrong Password!", Toast.LENGTH_LONG).show()
                editTxt_returnPassword.setText("")
            }
        }

        val btn_cancel = dialogView.findViewById<Button>(R.id.button_cancelPassword)
        btn_cancel.setOnClickListener {
            startCamera()
            dialog.dismiss()
        }
    }

    @SuppressLint("RestrictedApi")
    private fun checkInDialog(ID: String) {
        CameraX.unbindAll()
        var name  = ""
        var email = ""
        for(i in 0 until dataJSONArray.length()) {
            val jsonOBJ = dataJSONArray.getJSONObject(i)
            if(jsonOBJ.getString("id") == ID) {
                name = jsonOBJ.getString("name")
                email = jsonOBJ.getString("email")
            }
        }
        if(name == "" && email == "") {
            Toast.makeText(this, ID + ": No match found", Toast.LENGTH_LONG).show()
            startCamera()
        } else {
            val builder: AlertDialog.Builder = AlertDialog.Builder(this)
            builder.setCancelable(false)

            builder.setTitle("Is this you?")
            builder.setMessage("$name\n" + ID.toString() + "\n$email")

            builder.setPositiveButton("Check-In", DialogInterface.OnClickListener() { dialog, id ->
                writeToSheets(ID, name, email)
                startCamera()
            })

            builder.setNegativeButton("Rescan", DialogInterface.OnClickListener() { dialog, id ->
                startCamera()
            })

            builder.show()
        }
    }



    private fun writeToSheets(id: String, name: String, email: String) {
        val cache = DiskBasedCache(cacheDir, 1024 * 1024)

        val network = BasicNetwork(HurlStack())

        val requestQueue = RequestQueue(cache, network).apply {
            start()
        }
        val stringRequest = object : StringRequest(Request.Method.POST, scriptURL, Response.Listener { response ->
            Toast.makeText(this, "Checked-In!", Toast.LENGTH_SHORT).show()
        },
        Response.ErrorListener {error ->  
            Toast.makeText(this, "Error while writing to Spreadsheet.", Toast.LENGTH_LONG).show()
        }) {
            override fun getParams(): MutableMap<String, String> {
                var params = HashMap<String, String>()

                params.put("action", "addItem")
                params.put("id", id)
                params.put("name", name)
                params.put("email", email)

                return params
            }
        }
        requestQueue.add(stringRequest)
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
                        if (rawValue != null) {
                            try {
                                checkInDialog(rawValue)

                            } catch(e: NumberFormatException) {

                            }
                        }
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
