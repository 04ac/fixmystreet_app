package com.areen.fixmystreet

import androidx.lifecycle.ViewModel

class ClickPicViewModel : ViewModel() {
    var currentPhotoPath: String? = null
    var currLat: Double? = null
    var currLng: Double? = null
    var currAddress: String? = null
}
