package com.vandenbreemen.secretcamera.fragment

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.vandenbreemen.secretcamera.R
import kotlinx.android.synthetic.main.layout_delete_confirm.view.*
import kotlinx.android.synthetic.main.layout_kds_info_item.view.*

class ConfirmationViewHolder(item: ViewGroup) : RecyclerView.ViewHolder(item)

class ConfirmationAdapter(private val items: List<String>, private val parent: ConfirmDeleteDialogFragment) : RecyclerView.Adapter<ConfirmationViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConfirmationViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val info = inflater.inflate(R.layout.layout_kds_info_item, parent, false) as ViewGroup
        return ConfirmationViewHolder(info)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: ConfirmationViewHolder, position: Int) {
        val group = holder.itemView as ViewGroup
        group.textContent.text = items[position]
    }
}

/**
 *
 * @author kevin
 */
class ConfirmDeleteDialogFragment(private val fileNames: List<String>, private val onConfirmationCallback: () -> Unit) : DialogFragment() {

    private lateinit var adapter: ConfirmationAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        this.adapter = ConfirmationAdapter(fileNames, this)
    }

    override fun onStart() {
        super.onStart()

        dialog?.apply {
            window?.apply {
                setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            }
        }

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val dialogContent = inflater.inflate(R.layout.layout_delete_confirm, container, false)
        dialogContent.itemList.apply {
            adapter = this@ConfirmDeleteDialogFragment.adapter
            layoutManager = LinearLayoutManager(this@ConfirmDeleteDialogFragment.context)
        }

        dialogContent.ok_button.setOnClickListener {
            onConfirmationCallback()
            dismiss()
        }

        dialogContent.cancel_button.setOnClickListener {
            dismiss()
        }

        return dialogContent

    }

}