package cloud.wumboing.rpchat.ui

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import cloud.wumboing.rpchat.R
import cloud.wumboing.rpchat.data.Storage
import cloud.wumboing.rpchat.databinding.FragmentStatisticsBinding
import cloud.wumboing.rpchat.util.ThemeUtils
import java.util.concurrent.TimeUnit

class StatisticsFragment : Fragment() {

    private var _binding: FragmentStatisticsBinding? = null
    private val binding get() = _binding!!

    private lateinit var storage: Storage

    private enum class Range(val labelRes: Int) {
        DAY(R.string.stats_range_day),
        WEEK(R.string.stats_range_week),
        MONTH(R.string.stats_range_month),
        ALL(R.string.stats_range_all)
    }

    private var selectedRange = Range.WEEK

    private val barColors = listOf(
        0xFF4EA4F6, 0xFF7A2B5A, 0xFF1F6F4A, 0xFF7A4B2B,
        0xFF4A2B78, 0xFF1F6F6F, 0xFF7A2B2B, 0xFF264D73
    ).map { it.toInt() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStatisticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        storage = Storage(requireContext())
        setupRangeButtons()
        refreshStats()
    }

    override fun onResume() {
        super.onResume()
        if (_binding == null) return
        refreshStats()
    }

    private fun setupRangeButtons() {
        binding.rangeButtonsRow.removeAllViews()
        Range.values().forEach { range ->
            val btn = Button(requireContext()).apply {
                text = getString(range.labelRes)
                isAllCaps = false
                textSize = 12f
                setTextColor(if (range == selectedRange) Color.WHITE else getColorCompat(R.color.text_secondary))
                background = ThemeUtils.bubbleDrawable(
                    requireContext(),
                    if (range == selectedRange) getColorCompat(R.color.accent) else getColorCompat(R.color.bubble_other),
                    18f
                )
                val params = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                params.marginEnd = 6
                layoutParams = params
                setPadding(4, 18, 4, 18)
                setOnClickListener {
                    selectedRange = range
                    setupRangeButtons()
                    refreshStats()
                }
            }
            binding.rangeButtonsRow.addView(btn)
        }
    }

    private fun getColorCompat(resId: Int): Int = resources.getColor(resId, requireActivity().theme)

    private fun rangeStartMillis(): Long {
        val now = System.currentTimeMillis()
        return when (selectedRange) {
            Range.DAY -> now - TimeUnit.DAYS.toMillis(1)
            Range.WEEK -> now - TimeUnit.DAYS.toMillis(7)
            Range.MONTH -> now - TimeUnit.DAYS.toMillis(30)
            Range.ALL -> 0L
        }
    }

    private fun refreshStats() {
        val rangeStart = rangeStartMillis()
        val characters = storage.loadCharacters().associateBy { it.id }
        val sessions = storage.loadSessions().filter { it.startTime >= rangeStart }

        val durationByCharacter = mutableMapOf<String, Long>()
        sessions.forEach { s ->
            durationByCharacter[s.characterId] = (durationByCharacter[s.characterId] ?: 0L) + s.durationSeconds
        }

        val totalSeconds = durationByCharacter.values.sum()
        binding.txtTotalTime.text = formatDuration(totalSeconds)

        val messages = storage.allMessagesWithCharacterId().filter { it.second.timestamp >= rangeStart }
        val totalWords = messages.sumOf { (_, msg) ->
            msg.text.trim().split(Regex("\\s+")).count { it.isNotBlank() }
        }
        binding.txtTotalWords.text = totalWords.toString()

        val longestEntry = durationByCharacter.entries.maxByOrNull { it.value }
        if (longestEntry != null && longestEntry.value > 0) {
            val name = characters[longestEntry.key]?.name ?: "-"
            binding.txtLongestContact.text = "$name — ${formatDuration(longestEntry.value)}"
        } else {
            binding.txtLongestContact.text = "-"
        }

        renderBarChart(durationByCharacter, characters)
    }

    private fun renderBarChart(
        durationByCharacter: Map<String, Long>,
        characters: Map<String, cloud.wumboing.rpchat.data.Character>
    ) {
        binding.barChartContainer.removeAllViews()
        val sorted = durationByCharacter.entries
            .filter { it.value > 0 }
            .sortedByDescending { it.value }

        if (sorted.isEmpty()) {
            binding.txtNoData.visibility = View.VISIBLE
            return
        }
        binding.txtNoData.visibility = View.GONE

        val maxDuration = sorted.first().value.toFloat().coerceAtLeast(1f)
        val scale = resources.displayMetrics.density

        sorted.forEachIndexed { index, entry ->
            val name = characters[entry.key]?.name ?: "?"
            val color = barColors[index % barColors.size]

            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                params.bottomMargin = (10 * scale).toInt()
                layoutParams = params
            }

            val nameView = TextView(requireContext()).apply {
                text = name
                setTextColor(getColorCompat(R.color.text_primary))
                textSize = 12f
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
                layoutParams = LinearLayout.LayoutParams((78 * scale).toInt(), LinearLayout.LayoutParams.WRAP_CONTENT)
            }
            row.addView(nameView)

            val barTrack = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                val params = LinearLayout.LayoutParams(0, (20 * scale).toInt(), 1f)
                params.marginStart = (8 * scale).toInt()
                params.marginEnd = (8 * scale).toInt()
                layoutParams = params
                background = ThemeUtils.bubbleDrawable(requireContext(), getColorCompat(R.color.bubble_other), 10f)
            }

            val fill = View(requireContext()).apply {
                val fillParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, entry.value.toFloat())
                layoutParams = fillParams
                background = ThemeUtils.bubbleDrawable(requireContext(), color, 10f)
            }
            barTrack.addView(fill)

            val remaining = maxDuration - entry.value
            if (remaining > 0) {
                val spacer = View(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, remaining)
                }
                barTrack.addView(spacer)
            }

            row.addView(barTrack)

            val durationView = TextView(requireContext()).apply {
                text = formatDuration(entry.value)
                setTextColor(getColorCompat(R.color.text_secondary))
                textSize = 11f
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            }
            row.addView(durationView)

            binding.barChartContainer.addView(row)
        }
    }

    private fun formatDuration(totalSeconds: Long): String {
        val h = totalSeconds / 3600
        val m = (totalSeconds % 3600) / 60
        val s = totalSeconds % 60
        return when {
            h > 0 -> "${h}j ${m}m"
            m > 0 -> "${m}m ${s}d"
            else -> "${s}d"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
