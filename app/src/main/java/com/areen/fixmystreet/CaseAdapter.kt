package com.areen.fixmystreet

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import android.util.Base64
import android.graphics.BitmapFactory
import com.areen.fixmystreet.models.Case

class CaseAdapter(private val cases: List<Case>) : RecyclerView.Adapter<CaseAdapter.CaseViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CaseViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_case, parent, false)
        return CaseViewHolder(view)
    }

    override fun onBindViewHolder(holder: CaseViewHolder, position: Int) {
        holder.bind(cases[position])
    }

    override fun getItemCount(): Int = cases.size

    class CaseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val address: TextView = itemView.findViewById(R.id.itemCaseAddressTextView)
        private val submittedBy: TextView = itemView.findViewById(R.id.itemCaseSubmittedByTextView)
        private val resolved: TextView = itemView.findViewById(R.id.itemCaseResolvedTextView)
        private val image: ImageView = itemView.findViewById(R.id.itemCaseImageView)

        fun bind(case: Case) {
            address.text = case.address
            submittedBy.text = "Submitted by: ${case.submittedBy}"
            resolved.text = if (case.resolved) "Resolved" else "Unresolved"

            // Decode base64 image and set to ImageView
            val decodedString = Base64.decode(case.image.split(",")[1], Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
            image.setImageBitmap(bitmap)
        }
    }
}
