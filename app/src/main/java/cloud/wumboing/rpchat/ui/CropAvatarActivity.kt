package cloud.wumboing.rpchat.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import cloud.wumboing.rpchat.databinding.ActivityCropAvatarBinding
import cloud.wumboing.rpchat.util.BitmapUtils
import java.io.File
import java.io.FileOutputStream

class CropAvatarActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_IMAGE_URI = "extra_image_uri"
        const val EXTRA_RESULT_PATH = "extra_result_path"
    }

    private lateinit var binding: ActivityCropAvatarBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCropAvatarBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        val uriString = intent.getStringExtra(EXTRA_IMAGE_URI)
        if (uriString == null) {
            finish()
            return
        }
        val uri = Uri.parse(uriString)

        binding.cropView.post {
            val bmp = BitmapUtils.decodeSampledFromUri(this, uri, 1024)
            if (bmp != null) {
                binding.cropView.setImageBitmap(bmp)
            } else {
                finish()
            }
        }

        binding.btnCancelCrop.setOnClickListener {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }

        binding.btnConfirmCrop.setOnClickListener {
            val cropped = binding.cropView.getCroppedBitmap(512)
            if (cropped != null) {
                val outFile = File(cacheDir, "crop_${System.currentTimeMillis()}.jpg")
                FileOutputStream(outFile).use { out ->
                    cropped.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out)
                }
                val result = Intent().putExtra(EXTRA_RESULT_PATH, outFile.absolutePath)
                setResult(Activity.RESULT_OK, result)
            } else {
                setResult(Activity.RESULT_CANCELED)
            }
            finish()
        }
    }
}
