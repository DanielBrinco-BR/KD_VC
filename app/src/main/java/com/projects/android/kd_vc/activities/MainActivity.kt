package com.projects.android.kd_vc.activities

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.auth.api.credentials.*
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.projects.android.kd_vc.*
import com.projects.android.kd_vc.broadcastreceivers.BroadcastAlarmManger
import com.projects.android.kd_vc.retrofit.PhoneDataInfo
import com.projects.android.kd_vc.retrofit.RestApiManager
import com.projects.android.kd_vc.room.*
import com.projects.android.kd_vc.services.EndlessService
import com.projects.android.kd_vc.utils.*
import com.projects.android.kd_vc.utils.Encryption.AESEncyption.encrypt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {
    private val TAG = "KadeVc"
    private val newPhoneActivityRequestCode = 1
    private val PERMISSION_ID = 42

    private lateinit var menu: Menu
    private lateinit var adapter: PhoneListAdapter

    private val phoneViewModel: PhoneViewModel by viewModels {
        PhoneViewModelFactory((application as PhoneApplication).repository)
    }

    companion object {
        // Variables related to device phone number:
        var CREDENTIAL_PICKER_REQUEST = 1

        // AlarmManager to keep EndlessService always running and to send/receive data to cloud
        fun registerAlarm(context: Context) {
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale("pt", "BR"))
            val currentDate = sdf.format(Date())
            val FIVE_MINUTES_IN_MILLI = 300000
            val TEN_MINUTES_IN_MILLI = 600000
            val FIFTEEN_MINUTES_IN_MILLI = 900000
            val THIRTY_SECOND_IN_MILLI = 30000
            val launchTime = System.currentTimeMillis() + FIVE_MINUTES_IN_MILLI
            val am = context.getSystemService(ALARM_SERVICE) as AlarmManager
            val i = Intent(context, BroadcastAlarmManger::class.java)
            val pi = PendingIntent.getBroadcast(context, 0, i, 0)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, launchTime, pi)
            } else {
                am.setExact(AlarmManager.RTC_WAKEUP, launchTime, pi)
            }
            appendLog("$currentDate - companion object ALARM", context)
            Log.i("KadeVc","MainActivity.onCreate() - $currentDate - companion object ALARM")

            // Send data to server (Heroku):
            sendUpdatesToServer(context)

            // Check updates from server (Heroku):
            checkUpdatesFromServer(context)
        }

        private fun sendUpdatesToServer(context: Context) {
            //val applicationScope = CoroutineScope(SupervisorJob())
            val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
            val database = PhoneRoomDatabase.getDatabase(context, applicationScope)

            applicationScope.launch {
                try {
                    val listMyPhoneData = database.phoneDao().getMyPhoneDataList()

                    for (myPhoneData in listMyPhoneData) {
                        val phoneDataInfo = PhoneDataInfo(
                            id = null,
                            number = myPhoneData.number,
                            data = myPhoneData.data
                        )
                        postData(myPhoneData, phoneDataInfo, context)
                    }

                } catch(e: Exception) {
                    Log.e("KadeVc", "MainActivity.sendUpdatesFromServer() - POST to Heroku - Exception: ${e.message}")
                    appendLog("KadeVc - MainActivity.sendUpdatesFromServer() - POST to Heroku - Exception: ${e.message}", context)
                }
            }
        }

        fun checkUpdatesFromServer(context: Context) {
            //val applicationScope = CoroutineScope(SupervisorJob())
            val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

            applicationScope.launch {
                try {
                    val database = PhoneRoomDatabase.getDatabase(context, applicationScope)
                    val listPhones = database.phoneDao().getAllActivePhones()
                    val apiManager = RestApiManager()

                    for (phone in listPhones) {
                        Log.i("KadeVc", "MainActivity.checkUpdatesFromServer() - GET from Heroku - phoneNumber: ${phone.phoneNumber}")
                        Log.i("KadeVc", "MainActivity.checkUpdatesFromServer() - GET from Heroku - encryptedPhoneNumber: ${phone.encryptedPhoneNumber}")
                        apiManager.getPhoneData(phone.encryptedPhoneNumber, context)
                    }
                } catch(e: Exception) {
                    Log.e("KadeVc", "MainActivity.checkUpdatesFromServer() - GET from Heroku - Exception: ${e.message}")
                    appendLog("KadeVc - MainActivity.checkUpdatesFromServer() - GET from Heroku - Exception: ${e.message}", context)
                }
            }
        }

        private fun postData(myPhoneData: MyPhoneData, phoneDataInfo: PhoneDataInfo, context: Context) {
            val apiManager = RestApiManager()
            val applicationScope = CoroutineScope(SupervisorJob())

            apiManager.addPhoneData(phoneDataInfo) {
                if (it?.id != null) {
                    // it = newly added user parsed as response
                    // it?.dataId = newly added phoneData ID
                    val id = it.id
                    val number = it.number
                    val data = Encryption.AESEncyption.decrypt(it.data)

                    val response = "$id \n$number \n$data"

                    // Delete myPhoneData after send to server:
                    applicationScope.launch {
                        try {
                            val database = PhoneRoomDatabase.getDatabase(context, applicationScope)
                            database.phoneDao().deleteMyPhoneData(myPhoneData)
                        } catch(e: Exception) {
                            Log.e("KadeVc", "MainActivity.postData() - Exception: ${e.message}")
                            appendLog("KadeVc - MainActivity.postData() - Exception: ${e.message}", context)
                        }
                    }

                    Log.i("KadeVc", "*************************************************************************************************")
                    Log.i("KadeVc", "MainActivity.postData() - POST Response: \n $response")
                    appendLog("KdVc - MainActivity.postData - POST Response: \n $response", context)
                } else {
                    Log.e("KadeVc", "MainActivity.postData() - Error on POST method;")
                    appendLog("KdVc - MainActivity.postData - POST Response: Error on POST method", context)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Log.i(TAG,"MainActivity.onCreate()")

        if(!checkPermissions()) {
            requestPermissions()
        }

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerview)
        adapter = PhoneListAdapter(this@MainActivity)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.setDivider(R.drawable.recycler_view_divider)

        // Try to get Device Phone Number
        try {
            val devicePhoneNumber = getPhoneNumber(this)

            if(devicePhoneNumber == null || devicePhoneNumber.isEmpty()) {
                Log.i(TAG,"MainActivity.onCreate() - devicePhoneNumber is null, not on SharedPreferences yet!!")
                phoneSelection()
            } else {
                Log.i(TAG,"MainActivity.onCreate() - devicePhoneNumber not null: $devicePhoneNumber")
            }
        } catch(e: Exception) {
            Log.e(TAG, "$e.message")
        }

        // Add an observer on the LiveData returned by getAlphabetizedPhones.
        // The onChanged() method fires when the observed data changes and the activity is
        // in the foreground.
        phoneViewModel.allPhones.observe(this, Observer { phones ->

            for(phone in phones) {
                Log.i(TAG, "MainActivity.onCreate() observe: ${phone.alias}, ${phone.phoneNumber}, ${phone.imageUri}")
            }

            // Update the cached copy of the phones in the adapter.
            phones?.let {
                adapter.submitList(it)
                adapter.notifyDataSetChanged() // Test
            }
        })

        phoneViewModel.lastPhoneData.observe(this, Observer { phoneData ->
            try {
                Log.i(TAG, "MainActivity.onCreate() - Observer - phoneData.phoneNumber: ${phoneData.phoneNumber}")
                var formattedPhoneNumber = phoneData.phoneNumber.substring(2)
                formattedPhoneNumber = "(" + formattedPhoneNumber.substring(0, 2) + ") " + formattedPhoneNumber.substring(2, 7) + "-" + formattedPhoneNumber.substring(7)

                val title = formattedPhoneNumber + " - " + phoneData.date + " - " + phoneData.time
                Log.i(TAG, "MainActivity.onCreate() - Observer - formattedPhoneNumber: $formattedPhoneNumber, title: $title")
                Log.i(TAG, "MainActivity.onCreate() - Observer: ${phoneData.phoneNumber}, ${phoneData.latitude}, ${phoneData.longitude}")

                // Update phone - Verificar se é necessário incluir o "%" na frente do número!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
                phoneViewModel.updateLastPhoneInfo(phoneData.phoneNumber, phoneData.date, phoneData.time)

            } catch(e: Exception) {
                Log.e(TAG, "MapsActivity.onCreate() - Observer: ${e.message}, \n ${e.stackTraceToString()}")
            }
        })

        val fab = findViewById<FloatingActionButton>(R.id.fab)
        fab.setOnClickListener {
            val intent = Intent(this@MainActivity, NewPhoneActivity::class.java)
            startActivityForResult(intent, newPhoneActivityRequestCode)
        }

        try {
            registerAlarm(this)
        } catch(e: Exception) {
            Log.e(TAG, "MainActivity.onCreate() - Exception: $e.message \n ${e.stackTraceToString()}")
        }
    }

    override fun onResume() {
        super.onResume()
        Log.i("KadeVc","MainActivity.onResume()")
        try {
            checkUpdatesFromServer(this)
            adapter.notifyDataSetChanged()
        } catch(e: Exception) {
            Log.e(TAG, "MainActivity.onResume() - Exception: ${e.message} \n ${e.stackTraceToString()}")
        }
    }

    private fun phoneSelection() {
        // To retrieve the Phone Number hints, first, configure
        // the hint selector dialog by creating a HintRequest object.
        val hintRequest = HintRequest.Builder()
            .setPhoneNumberIdentifierSupported(true)
            .build()

        val options = CredentialsOptions.Builder()
            .forceEnableSaveDialog()
            .build()

        // Then, pass the HintRequest object to
        // credentialsClient.getHintPickerIntent()
        // to get an intent to prompt the user to
        // choose a phone number.
        val credentialsClient = Credentials.getClient(applicationContext, options)
        val intent = credentialsClient.getHintPickerIntent(hintRequest)

        try {
            startIntentSenderForResult(
                intent.intentSender,
                CREDENTIAL_PICKER_REQUEST, null, 0, 0, 0, Bundle()
            )
        } catch (e: IntentSender.SendIntentException) {
            e.printStackTrace()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == newPhoneActivityRequestCode && resultCode == Activity.RESULT_OK) {
            data?.getStringExtra(NewPhoneActivity.EXTRA_REPLY)?.let {
                val list = it.split(",")

                val number = list.get(0)
                val name = list.get(1)
                val imageUri = list.get(2)

                val encryptedPhoneNumber = encrypt(number)
                val filteredEncryptedPhoneNumber = removeSymbolsFromString(encryptedPhoneNumber)

                Log.i(TAG, "MainActivity.onActivityResult() - Phone: $number, $name, $imageUri, $encryptedPhoneNumber")

                val phone = Phone(number, name, imageUri, "", "", filteredEncryptedPhoneNumber)

                phoneViewModel.insert(phone)
                phoneViewModel.updatePhoneImage(imageUri, number)
                adapter.notifyDataSetChanged()
            }
        }

        if (requestCode == CREDENTIAL_PICKER_REQUEST && resultCode == RESULT_OK) {
            val credential: Credential? = data?.getParcelableExtra(Credential.EXTRA_KEY)

            credential?.apply {
                val phoneNumber = credential.id

                // Remove plus sign ("+") from phone number and encrypt:
                val encryptedPhoneNumber = encrypt(phoneNumber.substring(1))

                // Remove symbols from encrypted number to avoid problems with GET requests:
                val filteredEncryptedPhoneNumber = removeSymbolsFromString(encryptedPhoneNumber)

                // Using substring to remove plus sign from phone number:
                setPhoneNumber(this@MainActivity, filteredEncryptedPhoneNumber)

                Log.i(TAG, "MainActivity.onActivityResult() - Device Phone Number: $phoneNumber ************************************************************")
                Log.i(TAG, "MainActivity.onActivityResult() - getPhoneNumber: ${getPhoneNumber(this@MainActivity)} ************************************************************")
            }
        } else if (requestCode == CREDENTIAL_PICKER_REQUEST && resultCode == CredentialsApi.ACTIVITY_RESULT_NO_HINTS_AVAILABLE) {
            Toast.makeText(this, "Nenhum número de telefone encontrado no aparelho", Toast.LENGTH_LONG).show();
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu_options, menu)
        this.menu = menu as Menu

        // Configura o ícone do botão de ativar/desativar o envio da localização por SMS:
        if(getSmsState(applicationContext) == SmsState.DEACTIVATED) {
            this.menu.getItem(0).setIcon(ContextCompat.getDrawable(this,
                R.drawable.sms_off_foreground
            ));
        } else {
            this.menu.getItem(0).setIcon(ContextCompat.getDrawable(this,
                R.drawable.sms_on_foreground
            ));
        }

        // Configura o ícone do botão de ativar/desativar o rastreador:
        if(getServiceState(applicationContext) == ServiceState.STOPPED) {
            this.menu.getItem(1).setIcon(ContextCompat.getDrawable(this,
                R.drawable.ic_baseline_location_off_24
            ));
        } else {
            this.menu.getItem(1).setIcon(ContextCompat.getDrawable(this,
                R.drawable.ic_baseline_location_on_24
            ));
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle item selection
        return when (item.itemId) {
            R.id.sms -> {
                if(getSmsState(applicationContext) == SmsState.DEACTIVATED) {
                    val title = "Rastreamento por SMS"
                    val message = "Deseja ativar o rastreamento por SMS?"
                    showAlertDialog(title, message, Actions.ACTIVATE_SMS)
                    menu.getItem(0).setIcon(ContextCompat.getDrawable(this,
                        R.drawable.sms_on_foreground
                    ));
                } else {
                    val title = "Rastreamento por SMS"
                    val message = "Deseja desativar o rastreamento por SMS?"
                    showAlertDialog(title, message, Actions.DEACTIVATE_SMS)
                    menu.getItem(0).setIcon(ContextCompat.getDrawable(this,
                        R.drawable.sms_off_foreground
                    ));
                }
                true
            }
            R.id.stop_start -> {
                if(getServiceState(applicationContext) == ServiceState.STOPPED) {
                    //actionOnService(Actions.START)
                    val title = "Rastreador GPS"
                    val message = "Deseja ativar o rastreador?"
                    showAlertDialog(title, message, Actions.START)
                    menu.getItem(1).setIcon(ContextCompat.getDrawable(this,
                        R.drawable.ic_baseline_location_on_24
                    ));
                } else {
                    //actionOnService(Actions.STOP)
                    val title = "Rastreador GPS"
                    val message = "Deseja desativar o rastreador?"
                    showAlertDialog(title, message, Actions.STOP)
                    menu.getItem(1).setIcon(ContextCompat.getDrawable(this,
                        R.drawable.ic_baseline_location_off_24
                    ));
                }
                true
            }
            R.id.manage_phones -> {
                Toast.makeText(
                    applicationContext,
                    "Gerenciar Telefones",
                    Toast.LENGTH_LONG).show()
                true
            }
            R.id.start_stop_service -> {
                Toast.makeText(
                    applicationContext,
                    "Ativar/Desativar Rastreador",
                    Toast.LENGTH_LONG).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setDatePicker(dateEditText: EditText) {
        val myCalendar = Calendar.getInstance()

        val datePickerOnDataSetListener = DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
            myCalendar.set(Calendar.YEAR, year)
            myCalendar.set(Calendar.MONTH, monthOfYear)
            myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            updateLabel(myCalendar, dateEditText)
        }

        dateEditText.setOnClickListener {
            DatePickerDialog(this, datePickerOnDataSetListener, myCalendar
                .get(Calendar.YEAR), myCalendar.get(Calendar.MONTH),
                myCalendar.get(Calendar.DAY_OF_MONTH)).show()
        }
    }

    private fun updateLabel(myCalendar: Calendar, dateEditText: EditText) {
        val myFormat: String = "dd-MMM-yyyy"
        val sdf = SimpleDateFormat(myFormat, Locale.UK)
        dateEditText.setText(sdf.format(myCalendar.time))
    }

    private fun actionOnService(action: Actions) {
        if (getServiceState(this) == ServiceState.STOPPED && action == Actions.STOP) return
        Intent(this, EndlessService::class.java).also {
            it.action = action.name
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Log.d(TAG,"MainActivity.actionOnService() - Starting the service in >=26 Mode")
                startForegroundService(it)
                return
            }
            Log.d(TAG,"MainActivity.actionOnService() - Starting the service in < 26 Mode")
            startService(it)
        }
    }

    private fun showAlertDialog(title: String, message: String, action: Actions) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(title)
        builder.setMessage(message)

        builder.setPositiveButton("Sim") { _, _ ->
            Log.i(TAG, "MainActivity.showDialog() - Start Button: Sim")

            if(action == Actions.ACTIVATE_SMS) {
                setSmsState(this, SmsState.ACTIVATED)
            } else if(action == Actions.DEACTIVATE_SMS) {
                setSmsState(this, SmsState.DEACTIVATED)
            } else {
                actionOnService(action)
            }
        }

        builder.setNeutralButton("Sair") { _, _ ->
            Log.i(TAG, "MainActivity.showDialog() - Neutral Button: Sair")
        }
        builder.show()
    }

    private fun showCustomDialog(title: String, message: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(title)
        builder.setMessage(message)

        builder.setNeutralButton("Sair") { _, _ ->
            Log.i(TAG, "MapsActivity.showDialog() - Neutral Button: Sair")
        }
        builder.show()
    }

    private fun checkPermissions(): Boolean {
        //Log.d("WhereAreYou", "MainActivity.checkPermissions()")
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.FOREGROUND_SERVICE) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.WAKE_LOCK) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.BROADCAST_SMS) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_BOOT_COMPLETED) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.INTERNET) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_WIFI_STATE) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.CHANGE_WIFI_STATE) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_NETWORK_STATE) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            return true
        }
        return false
    }

    private fun isLocationEnabled(): Boolean {
        //Log.d(TAG, "MainActivity.isLocationEnabled()")
        val locationManager: LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        // return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            // LocationManager.NETWORK_PROVIDER)
    }

    private fun requestPermissions() {
        //Log.d("WhereAreYou", "MainActivity.requestPermissions()")
        ActivityCompat.requestPermissions(this,
            arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                Manifest.permission.SEND_SMS,
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.FOREGROUND_SERVICE,
                Manifest.permission.WAKE_LOCK,
                Manifest.permission.BROADCAST_SMS,
                Manifest.permission.RECEIVE_BOOT_COMPLETED,
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.INTERNET,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.CHANGE_WIFI_STATE,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE),
            PERMISSION_ID
        )
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        //Log.d("WhereAreYou", "MapsActivity.onRequestPermissionsResult()")
        if (requestCode == PERMISSION_ID) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                // Inicia o Rastreador de imediato:
                //actionOnService(Actions.START)
            } else {
                val sb: StringBuilder = java.lang.StringBuilder()
                sb.appendLine("O App KD_VC precisa das permissões de Localização e SMS para funcionar!\n")
                sb.appendLine("Feche e abra o aplicativo novamente para aprovar as permissões.")

                //showCustomDialog("Rastreador Desabilitado", sb.toString())
            }
        }
    }
}