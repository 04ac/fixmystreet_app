package com.areen.fixmystreet

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ClickPicActivity : AppCompatActivity() {

    private lateinit var clickedImgIV: ImageView
    private lateinit var addressTV: TextView
    private var currLat: Double? = null
    private var currLng: Double? = null
    private var currAddress: String? = null
    private var currentPhotoPath: String? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_click_pic)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        clickedImgIV = findViewById(R.id.clickedImageIV)
        addressTV = findViewById(R.id.addressTV)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        currentPhotoPath = intent.getStringExtra("currentPhotoPath")
        currLat = intent.getDoubleExtra("currLat", 90000.0)
        currLng = intent.getDoubleExtra("currLng", 90000.0)
        currAddress = intent.getStringExtra("currAddress")

        if (currLat == 90000.0) currLat = null
        if (currLng == 90000.0) currLng = null

        currentPhotoPath?.let {
            setPic(it, clickedImgIV)
        }

        currLat?.let {
            displayLocation()
        }

        val fab = findViewById<FloatingActionButton>(R.id.fab)
        fab.setOnClickListener {
            checkLocationPermission()
        }
    }

    private fun checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION))
        } else {
            // Permission already granted from before
            dispatchTakePictureIntent()
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        if (permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true || permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            // Permission granted
            dispatchTakePictureIntent()
        } else {
            // Permission denied
            Toast.makeText(this, "Location Perms Denied :(", Toast.LENGTH_LONG).show()
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

    private fun displayLocation() {
        addressTV.text = currAddress
        Toast.makeText(this, "Latitude: ${currLat}\nLongitude: ${currLng}", Toast.LENGTH_SHORT).show()
    }

    private fun getAddress(latitude: Double, longitude: Double) {
        val geocoder = Geocoder(this, Locale.getDefault())
        val addresses = geocoder.getFromLocation(latitude, longitude, 1)
        val address = addresses!![0].getAddressLine(0)
        currAddress = address
    }

    // Camera Stuff -----------------------------------------------------------

    private val startCameraActivityForResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            getLocation()
            displayLocation()
            currentPhotoPath?.let { setPic(it, clickedImgIV) }
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

    private fun setPic(photoPath: String, imageView: ImageView) {
        // Get the dimensions of the View
        val targetW: Int = imageView.width
        val targetH: Int = imageView.height

        val bmOptions = BitmapFactory.Options().apply {
            // Get the dimensions of the bitmap
            inJustDecodeBounds = true

            BitmapFactory.decodeFile(photoPath)

            val photoW: Int = outWidth
            val photoH: Int = outHeight

            // Determine how much to scale down the image
//            val scaleFactor: Int = Math.max(1, Math.min(photoW / targetW, photoH / targetH))

            // Decode the image file into a Bitmap sized to fill the View
            inJustDecodeBounds = false
//            inSampleSize = scaleFactor
            inPurgeable = true
        }

        BitmapFactory.decodeFile(photoPath, bmOptions)?.also { bitmap ->
            imageView.setImageBitmap(bitmap)
        }
    }
}