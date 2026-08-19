package cloud.wumboing.rpchat.util

import android.content.Context
import android.net.Uri
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Backup/restore seluruh data aplikasi (chat, kontak, avatar, media, pengaturan)
 * dalam satu file .zip, supaya bisa dipindah ke instalasi lain.
 */
object BackupManager {

    fun export(context: Context, targetUri: Uri): Boolean {
        return try {
            context.contentResolver.openOutputStream(targetUri)?.use { out ->
                ZipOutputStream(out).use { zip ->
                    val filesDir = context.filesDir
                    filesDir.walkTopDown().filter { it.isFile }.forEach { file ->
                        val relativePath = file.relativeTo(filesDir).path.replace(File.separatorChar, '/')
                        zip.putNextEntry(ZipEntry(relativePath))
                        file.inputStream().use { input -> input.copyTo(zip) }
                        zip.closeEntry()
                    }
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun import(context: Context, sourceUri: Uri): Boolean {
        return try {
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                ZipInputStream(input).use { zip ->
                    var entry = zip.nextEntry
                    while (entry != null) {
                        val outFile = File(context.filesDir, entry.name)
                        outFile.parentFile?.mkdirs()
                        if (!entry.isDirectory) {
                            outFile.outputStream().use { output -> zip.copyTo(output) }
                        }
                        zip.closeEntry()
                        entry = zip.nextEntry
                    }
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}
