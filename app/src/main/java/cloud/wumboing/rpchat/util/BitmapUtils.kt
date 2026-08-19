package cloud.wumboing.rpchat.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri

object BitmapUtils {

    fun videoThumbnail(path: String): Bitmap? {
        return try {
            val retriever = android.media.MediaMetadataRetriever()
            retriever.setDataSource(path)
            val frame = retriever.getFrameAtTime(1_000_000)
            retriever.release()
            frame
        } catch (e: Exception) {
            null
        }
    }

    fun decodeSampledFromFile(path: String, reqSize: Int = 1600): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, options)
            options.inSampleSize = calculateInSampleSize(options.outWidth, options.outHeight, reqSize)
            options.inJustDecodeBounds = false
            BitmapFactory.decodeFile(path, options)
        } catch (e: Exception) {
            null
        }
    }

    fun decodeSampledFromUri(context: Context, uri: Uri, reqSize: Int = 1024): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input, null, options)
            }
            options.inSampleSize = calculateInSampleSize(options.outWidth, options.outHeight, reqSize)
            options.inJustDecodeBounds = false
            context.contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input, null, options)
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun calculateInSampleSize(width: Int, height: Int, reqSize: Int): Int {
        var inSampleSize = 1
        var w = width
        var h = height
        while (w / 2 >= reqSize || h / 2 >= reqSize) {
            w /= 2
            h /= 2
            inSampleSize *= 2
        }
        return inSampleSize
    }
}
