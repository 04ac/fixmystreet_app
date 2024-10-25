package com.areen.fixmystreet

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.areen.fixmystreet.models.Case
import com.areen.fixmystreet.services.ApiClient
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.floatingactionbutton.FloatingActionButton
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private var currLat: Double? = null
    private var currLng: Double? = null
    private var currAddress: String? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private lateinit var noDataTextView: TextView
    private lateinit var fab: FloatingActionButton
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        recyclerView = findViewById(R.id.recyclerView)
        fab = findViewById(R.id.fab)
        noDataTextView = findViewById(R.id.textView2)
        progressBar = findViewById(R.id.progressBar)

        recyclerView.layoutManager = LinearLayoutManager(this)
        fetchData()

        fab.setOnClickListener {
            checkLocationPermission()
        }
    }

    private fun fetchData() {
        progressBar.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
        noDataTextView.visibility = View.GONE

        val call = ApiClient.apiService.getAllCases()
        call.enqueue(object : Callback<List<Case>> {
            override fun onResponse(call: Call<List<Case>>, response: Response<List<Case>>) {
                // Hide the progress spinner when data is loaded
                progressBar.visibility = View.GONE

                if (response.isSuccessful) {
                    val caseList = response.body()

                    if (caseList.isNullOrEmpty()) {
                        // Show FAB and TextView, hide RecyclerView
                        fab.visibility = View.VISIBLE
                        noDataTextView.visibility = View.VISIBLE
                        recyclerView.visibility = View.GONE
                    } else {
                        // Show RecyclerView
                        noDataTextView.visibility = View.GONE
                        recyclerView.visibility = View.VISIBLE
                        recyclerView.adapter = CaseAdapter(caseList)
                    }
                } else {
                    Log.e("MainActivity", "Error: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<List<Case>>, t: Throwable) {
                Log.e("MainActivity", "Failure: ${t.message}")
            }
        })
    }

    private fun checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION))
        } else {
            // Permission already granted from before
            getLocation()
            dispatchTakePictureIntent()
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        if (permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true || permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            // Permission granted
            getLocation()
            dispatchTakePictureIntent()
        } else {
            // Permission denied
            Toast.makeText(this, "Location Perms Denied :(", Toast.LENGTH_LONG).show()
        }
    }

    private val startCameraActivityForResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            val intent = Intent(this, ClickPicActivity::class.java).apply {
                putExtra("currentPhotoPath", currentPhotoPath)
                putExtra("currLat", currLat)
                putExtra("currLng", currLng)
                putExtra("currAddress", currAddress)
            }
            startActivity(intent)
        }
    }

    private fun dispatchTakePictureIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            // Ensure that there's a camera activity to handle the intent
            // We need requireContext().packageManager since this is a fragment and we don't have a reference to "this"
            takePictureIntent.resolveActivity(packageManager)?.also {
                // Create the File where the photo should go
                val photoFile: File? = try {
                    createImageFile()
                } catch (ex: IOException) {
                    // Error occurred while creating the File
                    null
                }
                // Continue only if the File was successfully created
                photoFile?.also {
                    val photoURI: Uri = FileProvider.getUriForFile(
                        this,
                        "com.example.android.fileprovider",
                        it
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    startCameraActivityForResult.launch(takePictureIntent)
                }
            }
        }
    }

    private var currentPhotoPath: String? = null

    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name

        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())

        // We need requireContext() since this is a fragment, not activity and we don't have a reference to "this"
        val storageDir: File = getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!

        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
            currentPhotoPath = absolutePath
        }
    }

    // LOCATION STUFF
    private fun getLocation() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                location?.let {
                    currLat = it.latitude
                    currLng = it.longitude
                    getAddress(it.latitude, it.longitude)
                }
            }
        }
    }

    private fun getAddress(latitude: Double, longitude: Double) {
        val geocoder = Geocoder(this, Locale.getDefault())
        val addresses = geocoder.getFromLocation(latitude, longitude, 1)
        val address = addresses!![0].getAddressLine(0)
        currAddress = address
    }

}
