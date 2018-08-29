package io.particle.mesh.setup.ui

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import androidx.navigation.findNavController
import io.particle.sdk.app.R
import mu.KotlinLogging


class MeshSetupActivity : AppCompatActivity() {

    private val log = KotlinLogging.logger {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onSupportNavigateUp(): Boolean {
        return findNavController(R.id.main_nav_host_fragment).navigateUp()
    }

    override fun onStart() {
        super.onStart()

//        @SuppressLint("StaticFieldLeak")
//        val exercute = object : AsyncTask<Unit, Unit, Unit>() {
//            override fun doInBackground(vararg params: Unit?) {
//                val demo = EllipticCurveJPAKEDemoOrig()
//                demo.run()
//            }
//
//        }.execute()

    }

}


