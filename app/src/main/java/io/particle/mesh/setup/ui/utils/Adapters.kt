package io.particle.mesh.setup.ui.utils

import android.support.annotation.LayoutRes
import android.support.v7.util.DiffUtil
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup


fun <T, I> easyDiffUtilCallback(idFieldGetter: (T) -> I): DiffUtil.ItemCallback<T> {

    return object: DiffUtil.ItemCallback<T>() {

        override fun areItemsTheSame(oldItem: T, newItem: T): Boolean {
            return idFieldGetter(oldItem) == idFieldGetter(newItem)
        }

        override fun areContentsTheSame(oldItem: T?, newItem: T?): Boolean {
            return oldItem == newItem
        }
    }
}


fun inflateRow(parent: ViewGroup, @LayoutRes layoutId: Int): View {
    val inflater = LayoutInflater.from(parent.context)
    return inflater.inflate(layoutId, parent, false)
}