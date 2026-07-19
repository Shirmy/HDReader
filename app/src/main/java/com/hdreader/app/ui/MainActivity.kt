package com.hdreader.app.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.hdreader.app.HdReaderApp
import com.hdreader.app.databinding.ActivityMainBinding
import com.hdreader.app.readium.PublicationHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private val openEpub = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@registerForActivityResult
        try {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: SecurityException) {
            // Some providers do not support persistable permission; still try open.
        }
        openUri(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnOpenEpub.setOnClickListener {
            openEpub.launch(arrayOf("application/epub+zip", "application/octet-stream", "*/*"))
        }

        // Open from VIEW intent (file manager / share).
        if (savedInstanceState == null) {
            intent?.data?.let { openUri(it) }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.data?.let { openUri(it) }
    }

    private fun openUri(uri: Uri) {
        binding.status.text = getString(com.hdreader.app.R.string.reading_loading)
        binding.btnOpenEpub.isEnabled = false
        val app = application as HdReaderApp
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                app.readium.openFromUri(uri)
            }
            binding.btnOpenEpub.isEnabled = true
            result
                .onSuccess { publication ->
                    PublicationHolder.set(publication, uri.toString())
                    binding.status.text = ""
                    startActivity(Intent(this@MainActivity, ReaderActivity::class.java))
                }
                .onFailure { error ->
                    val msg = error.message ?: error.toString()
                    binding.status.text = getString(com.hdreader.app.R.string.open_failed, msg)
                    Toast.makeText(this@MainActivity, msg, Toast.LENGTH_LONG).show()
                }
        }
    }
}
