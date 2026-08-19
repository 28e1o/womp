package cloud.wumboing.rpchat.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object ChatDateUtils {

    private val dayNames = arrayOf("Min", "Sen", "Sel", "Rab", "Kam", "Jum", "Sab") // index = Calendar.DAY_OF_WEEK - 1
    private val monthNames = arrayOf("Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt", "Nov", "Des")
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    /**
     * Hari ini -> "HH:mm"
     * Kemarin s/d 7 hari lalu -> nama hari singkat ("Sen", "Sel", dst)
     * Lebih dari 7 hari -> "d MMM" ("1 Agu")
     */
    fun formatChatTime(timestamp: Long): String {
        val now = Calendar.getInstance()
        val msgCal = Calendar.getInstance().apply { timeInMillis = timestamp }

        val dayDiff = daysBetween(msgCal, now)

        return when {
            dayDiff <= 0 -> timeFormat.format(Date(timestamp))
            dayDiff in 1..7 -> dayNames[msgCal.get(Calendar.DAY_OF_WEEK) - 1]
            else -> "${msgCal.get(Calendar.DAY_OF_MONTH)} ${monthNames[msgCal.get(Calendar.MONTH)]}"
        }
    }

    private fun daysBetween(from: Calendar, to: Calendar): Int {
        val a = (from.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val b = (to.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val diffMs = b.timeInMillis - a.timeInMillis
        return (diffMs / (1000 * 60 * 60 * 24)).toInt()
    }
}
