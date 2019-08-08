package io.particle.mesh.ui

import android.content.Context
import android.os.Bundle
import androidx.annotation.IdRes
import androidx.navigation.NavController
import io.particle.mesh.setup.flow.NavigationTool
import mu.KotlinLogging


class BaseNavigationToolImpl(
    private val nav: NavController,
    private val ctx: Context
) : NavigationTool {

    private val log = KotlinLogging.logger {}

    override fun navigate(@IdRes target: Int) {
        val name = ctx.resources.getResourceName(target)
        log.info { "Navigating to new target: $name" }
        nav.navigate(target)
    }

    override fun navigate(@IdRes target: Int, args: Bundle) {
        val name = ctx.resources.getResourceName(target)
        log.info { "Navigating to new target: $name" }
        nav.navigate(target, args)
    }

    override fun popBackStack(@IdRes destinationId: Int?): Boolean {
        log.info { "Popping back stack" }
        if (destinationId != null) {
            return nav.popBackStack(destinationId, false)
        }
        return nav.popBackStack()
    }

}