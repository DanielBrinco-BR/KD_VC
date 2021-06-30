package com.projects.android.kd_vc.room

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.projects.android.kd_vc.R
import com.projects.android.kd_vc.activities.ManagePhonesActivity
import com.projects.android.kd_vc.activities.MapsActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch


class PhoneListAdapter(private val context: Context) : ListAdapter<Phone, PhoneListAdapter.PhoneViewHolder>(
    PhoneViewHolder.PhoneComparator()
) {
    private val TAG = "KadeVc"

    // No need to cancel this scope as it'll be torn down with the process
    val applicationScope = CoroutineScope(SupervisorJob())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhoneViewHolder {
        return PhoneViewHolder.create(parent)
    }

    override fun onBindViewHolder(holder: PhoneViewHolder, position: Int) {
        val current = getItem(position)

        val database = PhoneRoomDatabase.getDatabase(context, applicationScope)
        val textConstraintLayout = holder.itemView.findViewById<ConstraintLayout>(R.id.constraintLayout)

        textConstraintLayout.setOnClickListener {
            Log.i(TAG,"PhoneListAdapter.onBindViewHolder() position: $position, textConstraintLayout: ${R.id.constraintLayout}")

            val t = textConstraintLayout.findViewById<TextView>(R.id.textView)
            val smallT = textConstraintLayout.findViewById<TextView>(R.id.smallTextView)

            Log.i(TAG,"PhoneListAdapter.onBindViewHolder(), text: ${t.text}, smallText: ${smallT.text}")

            applicationScope.launch {
                val number = smallT.text.toString()
                val phoneNumber = number.replace('(', ' ').replace(')', ' ').replace('-', ' ')
                val filteredPhoneNumber = phoneNumber.replace("\\s".toRegex(), "")
                val finalPhoneNumber = "%" + filteredPhoneNumber

                Log.i(TAG,"PhoneListAdapter.onBindViewHolder(), finalPhoneNumber: $finalPhoneNumber")

                if(database.phoneDao().countPhoneDataByNumber(finalPhoneNumber) > 0) {
                    val intent = Intent(context, MapsActivity::class.java)
                    intent.putExtra("phone_number", finalPhoneNumber)
                    ContextCompat.startActivity(context, intent, null)
                }
            }
        }

        holder.itemView.findViewById<ImageView>(R.id.imageView).setOnClickListener {
            Log.i(TAG,"PhoneListAdapter.onBindViewHolder() position: $position, imageView: ${R.id.imageView}")

            val smallT = textConstraintLayout.findViewById<TextView>(R.id.smallTextView)

            val number = smallT.text.toString()
            val phoneNumber = number.replace('(', ' ').replace(')', ' ').replace('-', ' ')
            val filteredPhoneNumber = phoneNumber.replace("\\s".toRegex(), "")
            val finalPhoneNumber = "55" + filteredPhoneNumber

            //val formattedNumber = "55" + number.substring(1, 3) + number.substring(5, 10) + number.substring(11)

            Log.i(TAG,"PhoneListAdapter.onBindViewHolder() - number: $number, finalPhoneNumber: $finalPhoneNumber")

            val intent = Intent(context, ManagePhonesActivity::class.java)
            intent.putExtra("phone_number", finalPhoneNumber)
            ContextCompat.startActivity(context, intent, null)
        }

        val text = current.alias + "," + current.phoneNumber + "," + current.imageUri + "," + current.date  + "," + current.time
                holder.bind(text)
    }

    open class PhoneViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val TAG = "KadeVC"
        private val phoneItemView: TextView = itemView.findViewById(R.id.textView)
        private val smallPhoneItemView: TextView = itemView.findViewById(R.id.smallTextView)
        private val day: TextView = itemView.findViewById(R.id.day)
        private val time: TextView = itemView.findViewById(R.id.time)
        open val imageContact: ImageView = itemView.findViewById(R.id.imageView)

        fun bind(text: String?) {
            val data = text?.split(",")

            Log.i(TAG,"PhoneListAdapter.PhoneViewHolder.bind data.0: ${data?.get(0)}, " +
                    "data.1: ${data?.get(1)}, data.2: ${data?.get(2)}, data.3: ${data?.get(3)}, data.4: ${data?.get(4)}")

            val formattedPhoneNumber = "(${data?.get(1)?.substring(2, 4)}) ${data?.get(1)?.substring(4, 9)}-${data?.get(1)?.substring(9)}"

            Log.i(TAG,"PhoneListAdapter.PhoneViewHolder.bind formattedPhoneNumber: $formattedPhoneNumber")

            phoneItemView.text = data?.get(0)
            smallPhoneItemView.text = formattedPhoneNumber // data?.get(1)
            imageContact.setImageURI(data?.get(2)?.toUri())
            day.text = data?.get(3)
            time.text = data?.get(4)
        }

        companion object {
            fun create(parent: ViewGroup): PhoneViewHolder {
                val view: View = LayoutInflater.from(parent.context)
                    .inflate(R.layout.recyclerview_item, parent, false)

                return PhoneViewHolder(view)
            }
        }

        class PhoneComparator : DiffUtil.ItemCallback<Phone>() {
            override fun areItemsTheSame(oldItem: Phone, newItem: Phone): Boolean {
                return oldItem === newItem
            }

            override fun areContentsTheSame(oldItem: Phone, newItem: Phone): Boolean {
                return oldItem.phoneNumber == newItem.phoneNumber
            }
        }
    }
}