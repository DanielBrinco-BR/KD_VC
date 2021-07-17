package com.projects.android.kd_vc.activities

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Log
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import com.projects.android.kd_vc.PhoneApplication
import com.projects.android.kd_vc.R
import com.projects.android.kd_vc.room.PhoneViewModel
import com.projects.android.kd_vc.room.PhoneViewModelFactory
import com.projects.android.kd_vc.utils.PhoneNumberFormatType
import com.projects.android.kd_vc.utils.PhoneNumberFormatter
import java.lang.ref.WeakReference

class NewPhoneActivity : AppCompatActivity() {
    private var TAG = "KadeVc"
    private lateinit var editName: EditText
    private lateinit var editNumber: EditText
    private lateinit var pictureImageView: ImageView

    private val phoneViewModel: PhoneViewModel by viewModels {
        PhoneViewModelFactory((application as PhoneApplication).repository)
    }

    private val pickImage = 100
    private var imageUri: Uri? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_phone)
        editName = findViewById(R.id.edit_name)

        editNumber = findViewById(R.id.edit_number)
        val country = PhoneNumberFormatType.PT_BR
        val phoneFormatter = PhoneNumberFormatter(WeakReference(editNumber), country)
        editNumber.addTextChangedListener(phoneFormatter)

        pictureImageView = findViewById(R.id.pictureImageView)

        // Inserindo botão return na barra de menu:
        val actionBar: ActionBar = supportActionBar!!
        actionBar.setDisplayHomeAsUpEnabled(true)
        actionBar.setHomeButtonEnabled(true)

        val buttonSave = findViewById<Button>(R.id.button_update)
        buttonSave.setOnClickListener {
            val replyIntent = Intent()
            if (TextUtils.isEmpty(editName.text) || TextUtils.isEmpty(editNumber.text)) {
                setResult(Activity.RESULT_CANCELED, replyIntent)
            } else {
                var image = R.mipmap.default_image_round.toString()

                if(imageUri != null) {
                    Log.i(TAG, "NewPhoneActivity.onCreate() - imageUri != null: ${imageUri.toString()}")
                    image = imageUri.toString()
                }

                val name = editName.text.toString().trimStart()
                val number = editNumber.text.toString().trimStart()

                val formattedNumber = "55" + number.substring(1, 3) + number.substring(5, 10) + number.substring(11)

                val list = "$formattedNumber,$name,$image"

                Log.i(TAG, "NewPhoneActivity.onCreate() - name: $name, formattedNumber: $formattedNumber, $image, $list")

                phoneViewModel.updatePhoneImage(image, formattedNumber)

                replyIntent.putExtra(EXTRA_REPLY, list)
                setResult(Activity.RESULT_OK, replyIntent)
            }
            finish()
        }

        val buttonImage = findViewById<Button>(R.id.button_image)
        buttonImage.setOnClickListener {
            val gallery = Intent(Intent.ACTION_OPEN_DOCUMENT, MediaStore.Images.Media.INTERNAL_CONTENT_URI)
            gallery.flags = (Intent.FLAG_GRANT_READ_URI_PERMISSION or
                             Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                             Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            startActivityForResult(gallery, pickImage)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle item selection
        return when (item.itemId) {
            android.R.id.home -> {
                Log.i(TAG, "NewPhoneActivity.onOptionsItemSelected() - this.finish()")
                this.finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == pickImage) {
            imageUri = data?.data

            try {
                this.contentResolver?.takePersistableUriPermission(imageUri!!, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch(e: Exception) {
                Log.e(TAG, "NewPhoneActivity.onActivityResult() - Exception: ${e.message}")
                e.printStackTrace()
            }

            pictureImageView.setImageURI(imageUri)
            Log.i(TAG, "NewPhoneActivity.onActivityResult() - imageUri: ${imageUri.toString()}")
        }
    }

    companion object {
        const val EXTRA_REPLY = "com.example.android.phonelistsql.REPLY"
    }
}
