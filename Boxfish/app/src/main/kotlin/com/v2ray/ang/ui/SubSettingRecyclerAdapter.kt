package com.v2ray.ang.ui

import android.content.Intent
import android.graphics.Color
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.v2ray.ang.R
import com.v2ray.ang.databinding.ItemQrcodeBinding
import com.v2ray.ang.databinding.ItemRecyclerSubSettingBinding
import com.v2ray.ang.extension.toast
import com.v2ray.ang.helper.ItemTouchHelperAdapter
import com.v2ray.ang.helper.ItemTouchHelperViewHolder
import com.v2ray.ang.util.QRCodeDecoder
import com.v2ray.ang.util.SettingsManager
import com.v2ray.ang.util.Utils

class SubSettingRecyclerAdapter(val activity: SubSettingActivity) :
    RecyclerView.Adapter<SubSettingRecyclerAdapter.MainViewHolder>(), ItemTouchHelperAdapter {

    private var mActivity: SubSettingActivity = activity

    private val shareMethod: Array<out String> by lazy {
        mActivity.resources.getStringArray(R.array.share_sub_method)
    }

    override fun getItemCount() = mActivity.subscriptions.size

    override fun onBindViewHolder(holder: MainViewHolder, position: Int) {
        val subId = mActivity.subscriptions[position].first
        val subItem = mActivity.subscriptions[position].second
        holder.itemSubSettingBinding.tvName.text = subItem.remarks
        holder.itemSubSettingBinding.tvUrl.text = subItem.url

        // Show or hide the image based on subItem.enabled
        if (subItem.enabled) {
            holder.itemSubSettingBinding.chkEnable.visibility = View.VISIBLE
        } else {
            holder.itemSubSettingBinding.chkEnable.visibility = View.GONE
        }

        holder.itemView.setBackgroundColor(Color.TRANSPARENT)

        holder.itemSubSettingBinding.layoutEdit.setOnClickListener {
            mActivity.startActivity(
                Intent(mActivity, SubEditActivity::class.java).putExtra("subId", subId)
            )
        }

        if (TextUtils.isEmpty(subItem.url)) {
            holder.itemSubSettingBinding.layoutShare.visibility = View.INVISIBLE
        } else {
            holder.itemSubSettingBinding.layoutShare.setOnClickListener {
                MaterialAlertDialogBuilder(mActivity).setItems(
                    shareMethod.asList().toTypedArray()
                ) { _, i ->
                    try {
                        when (i) {
                            0 -> {
                                val ivBinding =
                                    ItemQrcodeBinding.inflate(LayoutInflater.from(mActivity))
                                ivBinding.ivQRCode.setImageBitmap(
                                    QRCodeDecoder.createQRCode(
                                        subItem.url
                                    )
                                )
                                MaterialAlertDialogBuilder(mActivity).setView(ivBinding.root)
                                    .setTitle(R.string.scanner_qr_code)
                                    .setNegativeButton(android.R.string.cancel) { _, _ ->
                                        // Respond to negative button press
                                    }
                                    .show()
                            }

                            1 -> {
                                Utils.setClipboard(mActivity, subItem.url)
                            }

                            else -> mActivity.toast("else")
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                    .setTitle(R.string.title_share)
                    .setNegativeButton(android.R.string.cancel) { _, _ ->
                        // Respond to negative button press
                    }
                    .show()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MainViewHolder {
        return MainViewHolder(
            ItemRecyclerSubSettingBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )
    }

    class MainViewHolder(val itemSubSettingBinding: ItemRecyclerSubSettingBinding) :
        BaseViewHolder(itemSubSettingBinding.root), ItemTouchHelperViewHolder

    open class BaseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun onItemSelected() {
            itemView.setBackgroundColor(Color.LTGRAY)
        }

        fun onItemClear() {
            itemView.setBackgroundColor(0)
        }
    }

    override fun onItemMove(fromPosition: Int, toPosition: Int): Boolean {
        SettingsManager.swapSubscriptions(fromPosition, toPosition)
        notifyItemMoved(fromPosition, toPosition)
        return true
    }

    override fun onItemMoveCompleted() {
        mActivity.refreshData()
    }

    override fun onItemDismiss(position: Int) {
    }
}
