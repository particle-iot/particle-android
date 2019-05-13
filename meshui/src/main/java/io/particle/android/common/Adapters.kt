package io.particle.android.common

import android.annotation.SuppressLint
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.DiffUtil
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup


fun <T, I> easyDiffUtilCallback(idFieldGetter: (T) -> I): DiffUtil.ItemCallback<T> {

    return object: DiffUtil.ItemCallback<T>() {

        @SuppressLint("DiffUtilEquals")
        override fun areContentsTheSame(oldItem: T, newItem: T): Boolean {
            return oldItem == newItem
        }

        override fun areItemsTheSame(oldItem: T, newItem: T): Boolean {
            return idFieldGetter(oldItem) == idFieldGetter(newItem)
        }

    }
}
