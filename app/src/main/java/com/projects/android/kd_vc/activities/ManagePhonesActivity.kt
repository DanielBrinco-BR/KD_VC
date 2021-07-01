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
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.lifecycle.Observer
import com.projects.android.kd_vc.*
import com.projects.android.kd_vc.room.Phone
import com.projects.android.kd_vc.room.PhoneViewModel
import com.projects.android.kd_vc.room.PhoneViewModelFactory
import com.projects.android.kd_vc.utils.Encryption
import com.projects.android.kd_vc.utils.PhoneNumberFormatType
import com.projects.android.kd_vc.utils.PhoneNumberFormatter
import com.projects.android.kd_vc.utils.removeSymbolsFromString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import java.lang.ref.WeakReference

class ManagePhonesActivity : AppCompatActivity() {
    private var TAG = "KadeVc"
    private lateinit var oldPhoneNumber: String
    private lateinit var oldImageUri: String
    private lateinit var editName: EditText
    private lateinit var editNumber: EditText
    private lateinit var pictureImageView: ImageView
    private lateinit var phoneNumber: String
    private lateinit var phone: Phone

    // No need to cancel this scope as it'll be torn down with the process
    val applicationScope = CoroutineScope(SupervisorJob())

    private val phoneViewModel: PhoneViewModel by viewModels {
        PhoneViewModelFactory((application as PhoneApplication).repository)
    }

    private val pickImage = 100
    private var imageUri: Uri? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_phones)

        // Inserindo botão return na barra de menu:
        val actionBar: ActionBar = supportActionBar!!
        actionBar.setDisplayHomeAsUpEnabled(true)
        actionBar.setHomeButtonEnabled(true)

        //val database = PhoneRoomDatabase.getDatabase(this, applicationScope)

        phoneNumber = intent.getStringExtra("phone_number").toString()
        oldPhoneNumber = phoneNumber

        Log.i(TAG, "ManagePhonesActivity.onCreate() - intent phone_number: $phoneNumber")

        editName = findViewById(R.id.edit_name)

        editNumber = findViewById(R.id.edit_number)
        val country = PhoneNumberFormatType.PT_BR
        val phoneFormatter = PhoneNumberFormatter(WeakReference(editNumber), country)
        editNumber.addTextChangedListener(phoneFormatter)

        pictureImageView = findViewById(R.id.pictureImageView)

        phoneViewModel.findByPhoneNumber(phoneNumber).observe(this, Observer { phoneLiveData ->
            try {
                phone = phoneLiveData
                oldImageUri = phone.imageUri
                val uri = phone.imageUri.toUri()
                pictureImageView.setImageURI(uri)
                editNumber.setText(phone.phoneNumber)
                editName.setText(phone.alias)

                Log.i(TAG, "ManagePhonesActivity.onCreate() - Observer - oldImageUri: $oldImageUri, oldPhoneNumber: $oldPhoneNumber, alias: ${phone.alias}")
            } catch(e: Exception) {
                Log.e(TAG, "ManagePhonesActivity.onCreate() - Observer - Exception: ${e.message} \n ${e.stackTraceToString()}")
            }
        })

        pictureImageView.setOnClickListener {
            val gallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI)
            startActivityForResult(gallery, pickImage)
        }

        val buttonSave = findViewById<Button>(R.id.button_save)
        buttonSave.setOnClickListener {
            val replyIntent = Intent()
            if (TextUtils.isEmpty(editName.text) || TextUtils.isEmpty(editNumber.text)) {
                setResult(Activity.RESULT_CANCELED, replyIntent)
            } else {
                var image = oldImageUri

                if(imageUri != null) {
                    Log.i(TAG, "ManagePhonesActivity.onCreate() - imageUri != null: ${imageUri.toString()}")
                    image = imageUri.toString()
                }

                val name = editName.text.toString().trimStart()
                val number = editNumber.text.toString().trimStart()

                //val list = "$number,$name,$image"

                val formattedNumber = "55" + number.substring(1, 3) + number.substring(5, 10) + number.substring(11)

                val encryptedPhoneNumber = Encryption.AESEncyption.encrypt(formattedNumber)
                val filteredEncryptedPhoneNumber = removeSymbolsFromString(encryptedPhoneNumber)

                Log.i(TAG, "ManagePhonesActivity.onCreate() - updatedPhone: $formattedNumber, $name, $image, $filteredEncryptedPhoneNumber")

                val updatedPhone = Phone(formattedNumber, name, image, "", "", filteredEncryptedPhoneNumber)

                phoneViewModel.updatePhone(oldPhoneNumber, updatedPhone)
            }
            finish()
        }

        val buttonDelete = findViewById<Button>(R.id.button_delete)
        buttonDelete.setOnClickListener {
            val builder = AlertDialog.Builder(this@ManagePhonesActivity)
            builder.setMessage("Tem certeza que deseja apagar esse contato?")
                .setCancelable(false)
                .setPositiveButton("Sim") { _, _ ->
                    // Delete selected phone from database
                    GlobalScope.async {
                        phoneViewModel.deletePhone(phone)
                        finish()
                    }
                }
                .setNegativeButton("Não") { dialog, _ ->
                    // Dismiss the dialog
                    dialog.dismiss()
                }

            val alert = builder.create()
            alert.show()
        }

        /*
        val buttonReturn = findViewById<ImageButton>(R.id.button_return)
        buttonReturn.setOnClickListener {
            finish()
        } */
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle item selection
        return when (item.itemId) {
            android.R.id.home -> {
                Log.i(TAG, "ManagePhonesActivity.onOptionsItemSelected() - this.finish()")
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
            pictureImageView.setImageURI(imageUri)
            //phoneViewModel.updatePhoneImage(imageUri.toString(), editNumber.text.toString())
            Log.i(TAG, "ManagePhonesActivity.onActivityResult() - imageUri: ${imageUri.toString()}")
        }
    }

    companion object {
        const val EXTRA_REPLY = "com.example.android.phonelistsql.REPLY"
    }
}