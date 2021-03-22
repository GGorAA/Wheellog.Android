package com.cooper.wheellog.views

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.graphics.fonts.Font
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.content.ContextCompat.startActivity
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.cooper.wheellog.ElectroClub
import com.cooper.wheellog.R
import com.cooper.wheellog.WheelLog
import com.google.common.io.ByteStreams
import kotlinx.coroutines.*
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

class TripAdapter(var context: Context, private var tripModels: List<TripModel>) : RecyclerView.Adapter<TripAdapter.ViewHolder>() {
    private var inflater: LayoutInflater = LayoutInflater.from(context)
    private var uploadViewVisible: Int = View.VISIBLE
    private var font = WheelLog.ThemeManager.getTypeface(context)

    var uploadVisible: Boolean
        get() = uploadViewVisible == View.VISIBLE
        set(value) { uploadViewVisible = if (value) View.VISIBLE else View.GONE }

    init {
        uploadVisible = WheelLog.AppConfig.autoUploadEc
    }

    fun updateTrips(tripModels: List<TripModel>) {
        this.tripModels = tripModels
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view: View = inflater.inflate(R.layout.list_trip_item, parent, false)
        return ViewHolder(view, font)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val trip = tripModels[position]
        holder.bind(trip, uploadViewVisible)
    }

    override fun getItemCount(): Int {
        return tripModels.size
    }

    class ViewHolder internal constructor(view: View, val font: Typeface) : RecyclerView.ViewHolder(view) {
        private var nameView: TextView = view.findViewById(R.id.name)
        private var descriptionView: TextView = view.findViewById(R.id.description)
        private var uploadButtonLayout: RelativeLayout = view.findViewById(R.id.uploadButtonLayout)
        private var uploadView: ImageView = view.findViewById(R.id.uploadButton)
        private var uploadProgressView: ProgressBar = view.findViewById(R.id.progressBar)
        private var shareView: ImageView = view.findViewById(R.id.shareButton)

        private fun uploadViewEnabled(isEnabled: Boolean) {
            uploadView.isEnabled = isEnabled
            uploadView.imageAlpha = if (isEnabled) 0xFF else 0x20
        }

        
        private fun uploadInProgress(inProgress: Boolean) {
            uploadView.visibility = if (!inProgress) View.VISIBLE else View.GONE
            uploadProgressView.visibility = if (inProgress) View.VISIBLE else View.GONE
        }

        fun bind(tripModel: TripModel, uploadViewVisible: Int) {
            nameView.text = tripModel.title
            nameView.typeface = font
            descriptionView.text = tripModel.description
            descriptionView.typeface = font
            uploadButtonLayout.visibility = uploadViewVisible
            // check upload
            if (uploadViewVisible == View.VISIBLE) {
                GlobalScope.launch {
                    // async find
                    val trip = WheelLog.db.tripDao().getTripByFileName(tripModel.fileName)
                    withContext(Dispatchers.Main) {
                        uploadViewEnabled(trip != null && trip.ecId > 0)
                    }
                }
            }
            uploadInProgress(false)
            uploadView.setOnClickListener {
                uploadInProgress(true)
                val inputStream: InputStream? = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    // Android 9 or less
                    FileInputStream(File(tripModel.mediaId))
                } else {
                    // Android 10+
                    it.context.contentResolver.openInputStream(tripModel.uri)
                }
                if (inputStream == null) {
                    Timber.i("Failed to create inputStream for %s", tripModel.title)
                    uploadInProgress(false)
                    return@setOnClickListener
                }
                val data = ByteStreams.toByteArray(inputStream)
                ElectroClub.instance.uploadTrack(data, tripModel.title, false) {
                    MainScope().launch {
                        uploadInProgress(false)
                    }
                }
                inputStream.close()
                uploadViewEnabled(false)
                GlobalScope.launch {
                    val success = ElectroClub.instance.uploadTrackAsync(data, tripModel.title, false)
                    if (!success) {
                        withContext(Dispatchers.Main) {
                            uploadViewEnabled(true)
                        }
                    }
                }
            }
            shareView.setOnClickListener {
                val sendIntent: Intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_STREAM, tripModel.uri)
                    type = "text/csv"
                }

                val shareIntent = Intent.createChooser(sendIntent, null)
                startActivity(it.context, shareIntent, Bundle.EMPTY)
            }
        }
    }
}