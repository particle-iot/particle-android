package io.particle.mesh.ui

import android.os.Bundle
import androidx.annotation.IdRes
import androidx.navigation.NavController
import io.particle.mesh.setup.flow.NavigationTool


class NavigationToolImpl(private val nav: NavController) : NavigationTool {

    override fun navigate(@IdRes target: Int) {
        nav.navigate(target)
    }

    override fun navigate(@IdRes target: Int, args: Bundle) {
        nav.navigate(target, args)
    }

    override fun popBackStack() {
        nav.popBackStack()
    }

}