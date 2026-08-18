package cloud.wumboing.rpchat.ui

import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import cloud.wumboing.rpchat.R
import cloud.wumboing.rpchat.data.AppSettings
import cloud.wumboing.rpchat.data.Storage
import cloud.wumboing.rpchat.databinding.ActivitySettingsBinding
import cloud.wumboing.rpchat.util.ThemeUtils
import cloud.wumboing.rpchat.util.clipToCircle
import java.io.File

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var storage: Storage
    private lateinit var settings: AppSettings

    private val bgColors = listOf(
        0xFF0E1621, 0xFF000000, 0xFF121212, 0xFF1A1A2E,
        0xFF1E1633, 0xFF0D1F17, 0xFF1F1712, 0xFF0B1A33
    ).map { it.toInt() }

    private val bubbleColors = listOf(
        0xFF2B5278, 0xFF182533, 0xFF4A2B78, 0xFF1F6F4A,
        0xFF7A2B2B, 0xFF7A4B2B, 0xFF1F6F6F, 0xFF7A2B5A,
        0xFF3A3A3A, 0xFF264D73
    ).map { it.toInt() }

    private var pendingAvatarCroppedPath: String? = null

    private val pickAvatarLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> if (uri != null) launchCrop(uri) }

    private val cropLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val path = result.data?.getStringExtra(CropAvatarActivity.EXTRA_RESULT_PATH)
            if (path != null) {
                pendingAvatarCroppedPath = path
                val bmp = BitmapFactory.decodeFile(path)
                if (bmp != null) binding.imgSettingsAvatar.setImageBitmap(bmp)
                val profile = storage.loadProfile()
                profile.avatarPath = copyCroppedToInternal(path, "self_profile")
                storage.saveProfile(profile)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.imgSettingsAvatar.clipToCircle()

        storage = Storage(this)
        settings = storage.loadSettings()

        setupProfileSection()
        setupFontButtons()
        setupFontSizeSeek()
        setupColorRows()
    }

    // ---------- Profil ----------

    private fun setupProfileSection() {
        val profile = storage.loadProfile()
        binding.editSettingsName.setText(profile.name)
        binding.editSettingsBio.setText(profile.bio ?: "")
        profile.avatarPath?.let { path ->
            val f = File(path)
            if (f.exists()) {
                val bmp = BitmapFactory.decodeFile(path)
                if (bmp != null) binding.imgSettingsAvatar.setImageBitmap(bmp)
            }
        }

        binding.imgSettingsAvatar.setOnClickListener {
            pickAvatarLauncher.launch("image/*")
        }

        binding.editSettingsName.addTextChangedListener(simpleWatcher {
            val p = storage.loadProfile()
            p.name = binding.editSettingsName.text.toString().trim().ifEmpty { p.name }
            storage.saveProfile(p)
        })

        binding.editSettingsBio.addTextChangedListener(simpleWatcher {
            val p = storage.loadProfile()
            p.bio = binding.editSettingsBio.text.toString().trim().ifEmpty { null }
            storage.saveProfile(p)
        })
    }

    private fun simpleWatcher(onChanged: () -> Unit) = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable?) { onChanged() }
    }

    private fun launchCrop(uri: Uri) {
        val intent = Intent(this, CropAvatarActivity::class.java)
        intent.putExtra(CropAvatarActivity.EXTRA_IMAGE_URI, uri.toString())
        cropLauncher.launch(intent)
    }

    private fun copyCroppedToInternal(tempPath: String, key: String): String? {
        return try {
            val outFile = File(storage.avatarsDir, "$key.jpg")
            File(tempPath).copyTo(outFile, overwrite = true)
            outFile.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    // ---------- Font ----------

    private fun setupFontButtons() {
        val options = listOf(
            "default" to getString(R.string.font_default),
            "serif" to getString(R.string.font_serif),
            "monospace" to getString(R.string.font_monospace),
            "condensed" to getString(R.string.font_condensed)
        )
        refreshFontButtons(options)
    }

    private fun refreshFontButtons(options: List<Pair<String, String>>) {
        binding.fontButtonsRow.removeAllViews()
        options.forEach { (key, label) ->
            val btn = Button(this).apply {
                text = label
                isAllCaps = false
                textSize = 13f
                typeface = ThemeUtils.typefaceFor(key)
                setTextColor(if (settings.fontFamily == key) Color.WHITE else resources.getColor(R.color.text_secondary, theme))
                background = ThemeUtils.bubbleDrawable(
                    this@SettingsActivity,
                    if (settings.fontFamily == key) resources.getColor(R.color.accent, theme) else resources.getColor(R.color.bubble_other, theme),
                    20f
                )
                val params = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                params.marginEnd = 8
                layoutParams = params
                setPadding(8, 20, 8, 20)
                setOnClickListener {
                    settings.fontFamily = key
                    storage.saveSettings(settings)
                    refreshFontButtons(options)
                    updateFontSizePreview()
                }
            }
            binding.fontButtonsRow.addView(btn)
        }
    }

    // ---------- Font size ----------

    private fun setupFontSizeSeek() {
        binding.seekFontSize.progress = (settings.fontSizeSp - 12f).toInt().coerceIn(0, 12)
        updateFontSizePreview()
        binding.seekFontSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                settings.fontSizeSp = (12 + progress).toFloat()
                updateFontSizePreview()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                storage.saveSettings(settings)
            }
        })
    }

    private fun updateFontSizePreview() {
        binding.txtFontSizePreview.typeface = ThemeUtils.typefaceFor(settings.fontFamily)
        binding.txtFontSizePreview.setTextSize(TypedValue.COMPLEX_UNIT_SP, settings.fontSizeSp)
        binding.txtFontSizePreview.text = "Aa — ${settings.fontSizeSp.toInt()}sp"
    }

    // ---------- Warna ----------

    private fun setupColorRows() {
        renderSwatchRow(binding.chatBgRow, bgColors, settings.chatBackgroundColor) { color ->
            settings.chatBackgroundColor = color
            storage.saveSettings(settings)
            renderSwatchRow(binding.chatBgRow, bgColors, settings.chatBackgroundColor) { }
        }
        renderSwatchRow(binding.bubbleSelfRow, bubbleColors, settings.bubbleSelfColor) { color ->
            settings.bubbleSelfColor = color
            storage.saveSettings(settings)
            renderSwatchRow(binding.bubbleSelfRow, bubbleColors, settings.bubbleSelfColor) { }
        }
        renderSwatchRow(binding.bubbleOtherRow, bubbleColors, settings.bubbleOtherColor) { color ->
            settings.bubbleOtherColor = color
            storage.saveSettings(settings)
            renderSwatchRow(binding.bubbleOtherRow, bubbleColors, settings.bubbleOtherColor) { }
        }
    }

    private fun renderSwatchRow(
        row: LinearLayout,
        colors: List<Int>,
        selected: Int,
        onPick: (Int) -> Unit
    ) {
        row.removeAllViews()
        val sizeDp = 34
        val scale = resources.displayMetrics.density
        val sizePx = (sizeDp * scale).toInt()
        colors.forEach { color ->
            val swatch = View(this).apply {
                val drawable = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(color)
                    if (color == selected) {
                        setStroke((2 * scale).toInt(), Color.WHITE)
                    }
                }
                background = drawable
                val params = LinearLayout.LayoutParams(sizePx, sizePx)
                params.marginEnd = (8 * scale).toInt()
                layoutParams = params
                setOnClickListener {
                    onPick(color)
                    if (row === binding.chatBgRow) applyPreviewBackground()
                }
            }
            row.addView(swatch)
        }
    }

    private fun applyPreviewBackground() {
        // hanya menyimpan; efek warna langsung terlihat saat kembali ke layar chat
    }
}
