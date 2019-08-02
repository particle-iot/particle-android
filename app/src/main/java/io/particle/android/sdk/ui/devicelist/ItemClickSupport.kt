package io.particle.android.sdk.ui.devicelist

import androidx.recyclerview.widget.RecyclerView
import android.view.View

import io.particle.sdk.app.R

/**
 * This code is taken from the public domain, no copyright notices needed!
 *
 *
 * Taken from http://www.littlerobots.nl/blog/Handle-Android-RecyclerView-Clicks/
 */
internal class ItemClickSupport private constructor(private val recyclerView: RecyclerView) {

    companion object {

        fun addTo(view: RecyclerView): ItemClickSupport {
            var support: ItemClickSupport? =
                view.getTag(R.id.item_click_support) as ItemClickSupport
            if (support == null) {
                support = ItemClickSupport(view)
            }
            return support
        }

        fun removeFrom(view: RecyclerView): ItemClickSupport? {
            val support = view.getTag(R.id.item_click_support) as ItemClickSupport?
            support?.detach(view)
            return support
        }
    }


    internal interface OnItemClickListener {
        fun onItemClicked(recyclerView: RecyclerView, position: Int, v: View)
    }

    internal interface OnItemLongClickListener {
        fun onItemLongClicked(recyclerView: RecyclerView, position: Int, v: View): Boolean
    }


    private var onItemClickListener: OnItemClickListener? = null
    private var onItemLongClickListener: OnItemLongClickListener? = null

    private val onClickListener = View.OnClickListener { v ->
        if (onItemClickListener != null) {
            val holder = recyclerView.getChildViewHolder(v)
            onItemClickListener?.onItemClicked(recyclerView, holder.adapterPosition, v)
        }
    }

    private val onLongClickListener = View.OnLongClickListener { v ->
        if (onItemLongClickListener == null) {
            return@OnLongClickListener false
        }

        val holder = recyclerView.getChildViewHolder(v)
        return@OnLongClickListener onItemLongClickListener?.onItemLongClicked(
            recyclerView,
            holder.adapterPosition,
            v
        ) ?: false
    }

    private val attachListener = object : RecyclerView.OnChildAttachStateChangeListener {
        override fun onChildViewAttachedToWindow(view: View) {
            if (onItemClickListener != null) {
                view.setOnClickListener(onClickListener)
            }
            if (onItemLongClickListener != null) {
                view.setOnLongClickListener(onLongClickListener)
            }
        }

        override fun onChildViewDetachedFromWindow(view: View) {}
    }

    init {
        recyclerView.setTag(R.id.item_click_support, this)
        recyclerView.addOnChildAttachStateChangeListener(attachListener)
    }

    fun setOnItemClickListener(listener: OnItemClickListener): ItemClickSupport {
        onItemClickListener = listener
        return this
    }

    fun setOnItemLongClickListener(listener: OnItemLongClickListener): ItemClickSupport {
        onItemLongClickListener = listener
        return this
    }

    private fun detach(view: RecyclerView) {
        view.removeOnChildAttachStateChangeListener(attachListener)
        view.setTag(R.id.item_click_support, null)
    }

}