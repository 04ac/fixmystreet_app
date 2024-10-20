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
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.activityViewModels
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ClickPicFragment : Fragment() {

    private lateinit var clickImgBtn: ImageButton
    private lateinit var clickedImgIV: ImageView
    private lateinit var addressTV: TextView
    private var currLat: Double? = null
    private var currLng: Double? = null
    private var currAddress: String? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val viewModel: ClickPicViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_click_pic, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        clickImgBtn = view.findViewById(R.id.clickImgBtn)
        clickedImgIV = view.findViewById(R.id.clickedImageIV)
        addressTV = view.findViewById(R.id.addressTV)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        // Restore state from ViewModel
        viewModel.currentPhotoPath?.let {
            setPic(it, clickedImgIV)
            currentPhotoPath = it
        }
        viewModel.currAddress?.let {
            addressTV.text = it
            currAddress = it
        }
        viewModel.currLat?.let {
            currLat = it
        }
        viewModel.currLng?.let {
            currLng = it
        }

        clickImgBtn.setOnClickListener {
            checkLocationPermission()
        }
    }

    override fun onPause() {
        super.onPause()
        // Save state to ViewModel
        viewModel.currentPhotoPath = currentPhotoPath
        viewModel.currLat = currLat
        viewModel.currLng = currLng
        viewModel.currAddress = currAddress
    }


    //  HELPER METHODS -------------------------------------------------------------------------

    // LOCATION STUFF
    private fun getLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
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
        Toast.makeText(requireContext(), "Latitude: ${currLat}\nLongitude: ${currLng}", Toast.LENGTH_SHORT).show()
    }

    private fun getAddress(latitude: Double, longitude: Double) {
        val geocoder = Geocoder(requireContext(), Locale.getDefault())
        val addresses = geocoder.getFromLocation(latitude, longitude, 1)
        val address = addresses!![0].getAddressLine(0)
        currAddress = address
    }

    // LOCATION PERMISSIONS STUFF
    private fun checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION))
        } else {
            getLocation()
            dispatchTakePictureIntent()
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        if (permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true || permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            getLocation()
            dispatchTakePictureIntent()
        } else {
            // Permission denied
            Toast.makeText(requireContext(), "Location Perms Denied :(", Toast.LENGTH_LONG).show()
        }
    }

    // PICTURE STUFF
    private val startCameraActivityForResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            setPic(currentPhotoPath!!, clickedImgIV)
            displayLocation()
        }
    }

    private fun dispatchTakePictureIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            // Ensure that there's a camera activity to handle the intent
            // We need requireContext().packageManager since this is a fragment and we don't have a reference to "this"
            takePictureIntent.resolveActivity(requireContext().packageManager)?.also {
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
                        requireContext(),
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
        val storageDir: File = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!

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
