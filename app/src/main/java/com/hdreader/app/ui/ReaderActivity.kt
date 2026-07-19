package com.hdreader.app.ui

import android.content.res.Configuration
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.commitNow
import androidx.lifecycle.lifecycleScope
import com.hdreader.app.R
import com.hdreader.app.databinding.ActivityReaderBinding
import com.hdreader.app.readium.PublicationHolder
import com.hdreader.app.readium.ReadingPreferencesStore
import kotlinx.coroutines.launch
import org.readium.r2.navigator.epub.EpubNavigatorFactory
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.navigator.input.InputListener
import org.readium.r2.navigator.input.TapEvent
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Publication

@OptIn(ExperimentalReadiumApi::class)
class ReaderActivity : AppCompatActivity(), EpubNavigatorFragment.Listener {
    private lateinit var binding: ActivityReaderBinding
    private lateinit var prefsStore: ReadingPreferencesStore
    private var navigator: EpubNavigatorFragment? = null
    private var publication: Publication? = null
    private var menuVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReaderBinding.inflate(layoutInflater)
        setContentView(binding.root)
        prefsStore = ReadingPreferencesStore(this)

        val pub = PublicationHolder.current
        if (pub == null) {
            Toast.makeText(this, "没有可打开的书", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        publication = pub

        hideSystemBars()
        binding.bookTitleAuthor.text =
            getString(R.string.book_title_author, PublicationHolder.displayTitle, PublicationHolder.displayAuthor)

        bindMenuChrome()
        setupNavigator(pub, savedInstanceState)
    }

    private fun bindMenuChrome() {
        binding.menuMiddleSpacer.setOnClickListener { setMenuVisible(false) }
        binding.btnBack.setOnClickListener { finish() }
        binding.btnSettings.setOnClickListener { showSettingsDialog() }
        binding.btnToc.setOnClickListener { showToc() }
        binding.btnNotes.setOnClickListener {
            Toast.makeText(this, R.string.placeholder_later, Toast.LENGTH_SHORT).show()
        }
        binding.btnAi.setOnClickListener {
            Toast.makeText(this, R.string.placeholder_later, Toast.LENGTH_SHORT).show()
        }
        binding.btnRelations.setOnClickListener {
            Toast.makeText(this, R.string.placeholder_later, Toast.LENGTH_SHORT).show()
        }
        binding.bookTitleAuthor.setOnClickListener {
            Toast.makeText(this, R.string.placeholder_later, Toast.LENGTH_SHORT).show()
        }
        setMenuVisible(false)
    }

    private fun setupNavigator(publication: Publication, savedInstanceState: Bundle?) {
        binding.loading.visibility = View.VISIBLE
        val factory = EpubNavigatorFactory(publication)
        val isLandscape =
            resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val initialPrefs = prefsStore.buildEpubPreferences(isLandscape)

        supportFragmentManager.fragmentFactory = factory.createFragmentFactory(
            initialLocator = null,
            initialPreferences = initialPrefs,
            listener = this
        )

        if (savedInstanceState == null) {
            supportFragmentManager.commitNow {
                replace(
                    R.id.navigatorContainer,
                    EpubNavigatorFragment::class.java,
                    Bundle(),
                    NAV_TAG
                )
            }
        }

        val nav = supportFragmentManager.findFragmentByTag(NAV_TAG) as? EpubNavigatorFragment
        if (nav == null) {
            binding.loading.visibility = View.GONE
            Toast.makeText(this, "阅读器初始化失败", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        navigator = nav
        binding.loading.visibility = View.GONE

        // Center tap toggles chrome; edge taps follow product default zones.
        nav.addInputListener(object : InputListener {
            override fun onTap(event: TapEvent): Boolean {
                val w = binding.readerRoot.width.coerceAtLeast(1).toFloat()
                val x = event.point.x
                val third = w / 3f
                return when {
                    x < third -> {
                        nav.goBackward(animated = true)
                        true
                    }
                    x > third * 2 -> {
                        nav.goForward(animated = true)
                        true
                    }
                    else -> {
                        setMenuVisible(!menuVisible)
                        true
                    }
                }
            }
        })
    }

    private fun setMenuVisible(visible: Boolean) {
        menuVisible = visible
        binding.menuOverlay.visibility = if (visible) View.VISIBLE else View.GONE
        if (visible) {
            showSystemBars()
        } else {
            hideSystemBars()
        }
    }

    private fun showSettingsDialog() {
        val landscape =
            resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val dual = booleanArrayOf(prefsStore.dualColumnEnabled)
        val volume = booleanArrayOf(prefsStore.volumeKeysEnabled)
        val night = booleanArrayOf(prefsStore.night)
        val items = arrayOf(
            getString(R.string.pref_dual_column),
            getString(R.string.pref_volume_keys),
            getString(R.string.theme_night)
        )
        val checked = booleanArrayOf(dual[0], volume[0], night[0])
        AlertDialog.Builder(this)
            .setTitle(R.string.menu_settings_title)
            .setMultiChoiceItems(items, checked) { _, which, isChecked ->
                when (which) {
                    0 -> dual[0] = isChecked
                    1 -> volume[0] = isChecked
                    2 -> night[0] = isChecked
                }
            }
            .setPositiveButton(android.R.string.ok) { _, _ ->
                prefsStore.dualColumnEnabled = dual[0]
                prefsStore.volumeKeysEnabled = volume[0]
                prefsStore.night = night[0]
                applyPreferences(landscape)
            }
            .setNeutralButton(R.string.pref_font_size) { _, _ ->
                cycleFontSize()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun cycleFontSize() {
        val next = when {
            prefsStore.fontSize < 1.0 -> 1.0
            prefsStore.fontSize < 1.2 -> 1.2
            prefsStore.fontSize < 1.5 -> 1.5
            else -> 0.9
        }
        prefsStore.fontSize = next
        applyPreferences(
            resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        )
        Toast.makeText(this, "字号倍率：${"%.1f".format(next)}", Toast.LENGTH_SHORT).show()
    }

    private fun applyPreferences(isLandscape: Boolean) {
        val nav = navigator ?: return
        nav.submitPreferences(prefsStore.buildEpubPreferences(isLandscape))
    }

    private fun showToc() {
        val pub = publication ?: return
        val links = flattenToc(pub.tableOfContents)
        if (links.isEmpty()) {
            Toast.makeText(this, R.string.no_toc, Toast.LENGTH_SHORT).show()
            return
        }
        val titles = links.map { it.title?.ifBlank { it.href.toString() } ?: it.href.toString() }
            .toTypedArray()
        AlertDialog.Builder(this)
            .setTitle(R.string.action_toc)
            .setItems(titles) { _, index ->
                val link = links[index]
                lifecycleScope.launch {
                    navigator?.go(link)
                    setMenuVisible(false)
                }
            }
            .show()
    }

    private fun flattenToc(links: List<Link>, depth: Int = 0): List<Link> {
        val out = ArrayList<Link>()
        for (link in links) {
            out.add(link)
            if (link.children.isNotEmpty()) {
                out.addAll(flattenToc(link.children, depth + 1))
            }
        }
        return out
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        applyPreferences(newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (!prefsStore.volumeKeysEnabled) {
            return super.onKeyDown(keyCode, event)
        }
        val nav = navigator ?: return super.onKeyDown(keyCode, event)
        return when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> {
                nav.goBackward(animated = true)
                true
            }
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                nav.goForward(animated = true)
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, binding.readerRoot).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private fun showSystemBars() {
        WindowInsetsControllerCompat(window, binding.readerRoot)
            .show(WindowInsetsCompat.Type.systemBars())
    }

    override fun onDestroy() {
        if (isFinishing) {
            // Keep publication until activity fully finishes; holder cleared when reopening another book.
        }
        super.onDestroy()
    }

    companion object {
        private const val NAV_TAG = "epub_navigator"
    }
}
