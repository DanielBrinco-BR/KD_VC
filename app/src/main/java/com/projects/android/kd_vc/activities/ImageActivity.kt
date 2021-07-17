package com.projects.android.kd_vc.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.projects.android.kd_vc.PhoneApplication
import com.projects.android.kd_vc.R
import com.projects.android.kd_vc.room.PhoneViewModel
import com.projects.android.kd_vc.room.PhoneViewModelFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class ImageActivity : AppCompatActivity() {
    private var TAG = "KadeVc"
    private lateinit var contactImageView: ImageView
    private lateinit var buttonGallery: Button
    private lateinit var buttonSave: Button
    private lateinit var phoneNumber: String

    val applicationScope = CoroutineScope(SupervisorJob())

    private val phoneViewModel: PhoneViewModel by viewModels {
        PhoneViewModelFactory((application as PhoneApplication).repository)
    }

    private val pickImage = 100
    private var imageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image)

        phoneNumber = intent.getStringExtra("phone_number").toString()

        contactImageView = findViewById(R.id.contactImageView)

        buttonGallery = findViewById(R.id.button_gallery)

        buttonSave = findViewById(R.id.button_update)

        buttonGallery.setOnClickListener {
            val gallery = Intent(Intent.ACTION_OPEN_DOCUMENT, MediaStore.Images.Media.INTERNAL_CONTENT_URI)
            gallery.flags = (Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            startActivityForResult(gallery, pickImage)
        }

        buttonSave.setOnClickListener {
            if(imageUri != null) {
                imageUri?.let {
                    phoneViewModel.updatePhoneImage(it.toString(), phoneNumber) }
            }
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == pickImage) {
            imageUri = data?.data

            try {
                this.contentResolver?.takePersistableUriPermission(imageUri!!, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch(e: Exception) {
                Log.e(TAG, "ImageActivity.onActivityResult() - Exception: ${e.message}")
                e.printStackTrace()
            }

            contactImageView.setImageURI(imageUri)
            Log.i(TAG, "ImageActivity.onActivityResult() - imageUri: ${imageUri.toString()}")
        }
    }
}