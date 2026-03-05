package com.example.ramadancounter

import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private var ramadanSchedule = mutableListOf<RamadanDay>()
    private val handler = Handler(Looper.getMainLooper())
    private var updateRunnable: Runnable? = null

    private lateinit var spinnerDivision: Spinner
    private lateinit var spinnerDistrict: Spinner
    private lateinit var tvStatusLabel: TextView
    private lateinit var tvCountdown: TextView
    private lateinit var tvRamadanDay: TextView
    private lateinit var tvSehriEnd: TextView
    private lateinit var tvIftarTime: TextView

    private val divisionFiles = mapOf(
        "Dhaka Division" to "Dhaka_Divison_times.csv",
        "Chattogram Division" to "Chattogram_Divison_times.csv",
        "Rajshahi Division" to "Rajshahi_Divison_times.csv",
        "Khulna Division" to "Khulna_Divison_times.csv",
        "Barishal Division" to "Barishal _Divison_times.csv",
        "Sylhet Division" to "Sylhet_Divison_times.csv",
        "Rangpur Division" to "Rangpur_Divison_times.csv",
        "Mymensingh Division" to "Mymensingh_Divison_times.csv"
    )

    private var currentDistricts = mutableListOf<String>()
    private var selectedDistrict = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install Splash Screen
        installSplashScreen()
        
        super.onCreate(savedInstanceState)
        
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        
        // Set Navigation Bar color based on theme
        val isNightMode = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        if (isNightMode) {
            window.navigationBarColor = Color.parseColor("#0F1621") // Telegram Dark Blue
        } else {
            window.navigationBarColor = Color.TRANSPARENT
        }

        setContentView(R.layout.activity_main)

        val rootView = findViewById<View>(R.id.main_root)
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(
                left = systemBars.left,
                right = systemBars.right,
                bottom = systemBars.bottom
            )
            findViewById<View>(R.id.titleTextView).updatePadding(top = systemBars.top)
            insets
        }

        spinnerDivision = findViewById(R.id.spinnerDivision)
        spinnerDistrict = findViewById(R.id.spinnerDistrict)
        tvStatusLabel = findViewById(R.id.tvStatusLabel)
        tvCountdown = findViewById(R.id.tvCountdown)
        tvRamadanDay = findViewById(R.id.tvRamadanDay)
        tvSehriEnd = findViewById(R.id.tvSehriEnd)
        tvIftarTime = findViewById(R.id.tvIftarTime)

        setupSpinners()
        startCountdown()
    }

    private fun loadRamadanData(fileName: String) {
        ramadanSchedule.clear()
        currentDistricts.clear()
        try {
            assets.open(fileName).bufferedReader().useLines { lines ->
                lines.drop(1).forEach { line ->
                    val tokens = line.split(",")
                    if (tokens.size >= 6) {
                        val district = tokens[0].trim()
                        if (!currentDistricts.contains(district)) {
                            currentDistricts.add(district)
                        }
                        ramadanSchedule.add(
                            RamadanDay(
                                district,
                                tokens[1].trim().toInt(),
                                tokens[2].trim(),
                                tokens[3].trim(),
                                tokens[4].trim(),
                                tokens[5].trim()
                            )
                        )
                    }
                }
            }
            currentDistricts.sort()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupSpinners() {
        val divisions = divisionFiles.keys.toTypedArray()
        val divisionAdapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, divisions)
        divisionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerDivision.adapter = divisionAdapter

        spinnerDivision.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedDivision = divisions[position]
                val fileName = divisionFiles[selectedDivision] ?: return
                
                loadRamadanData(fileName)
                
                val districtAdapter = ArrayAdapter<String>(this@MainActivity, android.R.layout.simple_spinner_item, currentDistricts)
                districtAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerDistrict.adapter = districtAdapter

                spinnerDistrict.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, i: Long) {
                        selectedDistrict = currentDistricts[pos]
                        updateUI()
                    }
                    override fun onNothingSelected(p: AdapterView<*>?) {}
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun startCountdown() {
        updateRunnable = object : Runnable {
            override fun run() {
                updateUI()
                handler.postDelayed(this, 1000)
            }
        }
        handler.post(updateRunnable!!)
    }

    private fun updateUI() {
        if (ramadanSchedule.isEmpty() || selectedDistrict.isEmpty()) return

        val now = Calendar.getInstance()
        var districtData = findDataForDistrictAndDate(selectedDistrict, now)

        // If after Iftar today, we move to the next day's data (Islamic day starts at Maghrib)
        if (districtData != null) {
            val iftarTime = parseAnyDate(districtData.date, districtData.iftarTime, isPM = true)
            if (now.after(iftarTime)) {
                val tomorrow = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }
                val tomorrowData = findDataForDistrictAndDate(selectedDistrict, tomorrow)
                if (tomorrowData != null) {
                    districtData = tomorrowData
                }
            }
        } else {
            // Check if Ramadan starts tomorrow
            val tomorrow = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }
            val tomorrowData = findDataForDistrictAndDate(selectedDistrict, tomorrow)
            if (tomorrowData != null && tomorrowData.day == 1) {
                districtData = tomorrowData
            }
        }

        if (districtData != null) {
            displaySchedule(districtData, now)
        } else {
            val firstDay = ramadanSchedule.filter { it.district == selectedDistrict }.minByOrNull { it.day }
            if (firstDay != null) {
                val firstDayTime = parseAnyDate(firstDay.date, firstDay.sehriEnd)
                if (now.before(firstDayTime)) {
                    tvStatusLabel.text = "Ramadan starts in"
                    tvCountdown.text = formatCountdown(firstDayTime.timeInMillis - now.timeInMillis)
                    tvRamadanDay.text = "-"
                    tvSehriEnd.text = "-"
                    tvIftarTime.text = "-"
                    return
                }
            }
            tvStatusLabel.text = "No data for today"
            tvCountdown.text = "00:00:00"
        }
    }

    private fun findDataForDistrictAndDate(district: String, now: Calendar): RamadanDay? {
        val formats = listOf("d MMM yyyy", "d-MMM-yy", "dd MMM yyyy", "dd-MMM-yy")
        
        for (format in formats) {
            val sdf = SimpleDateFormat(format, Locale.ENGLISH)
            val nowStr = sdf.format(now.time)
            val match = ramadanSchedule.find { 
                it.district == district && it.date.equals(nowStr, ignoreCase = true) 
            }
            if (match != null) return match
        }
        return null
    }

    private fun displaySchedule(data: RamadanDay, now: Calendar) {
        tvRamadanDay.text = "${data.day} (${data.date})"
        tvSehriEnd.text = "${data.sehriEnd} AM"
        tvIftarTime.text = "${data.iftarTime} PM"

        val sehriTime = parseAnyDate(data.date, data.sehriEnd)
        val iftarTime = parseAnyDate(data.date, data.iftarTime, isPM = true)

        val nextEvent: Calendar
        val label: String

        if (now.before(sehriTime)) {
            nextEvent = sehriTime
            label = "Time Remaining for Sehri"
        } else if (now.before(iftarTime)) {
            nextEvent = iftarTime
            label = "Time Remaining for Iftar"
        } else {
            // Look for tomorrow's data
            val tomorrow = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }
            val tomorrowData = findDataForDistrictAndDate(selectedDistrict, tomorrow)
            
            if (tomorrowData != null) {
                nextEvent = parseAnyDate(tomorrowData.date, tomorrowData.sehriEnd)
                label = "Time Remaining for Sehri"
            } else {
                nextEvent = now
                label = "Eid ul Fitr!"
            }
        }

        tvStatusLabel.text = label
        tvCountdown.text = formatCountdown(nextEvent.timeInMillis - now.timeInMillis)
    }

    private fun parseAnyDate(dateStr: String, timeStr: String, isPM: Boolean = false): Calendar {
        val formats = listOf("d MMM yyyy", "d-MMM-yy", "dd MMM yyyy", "dd-MMM-yy")
        var date: Date? = null
        
        for (format in formats) {
            try {
                date = SimpleDateFormat(format, Locale.ENGLISH).parse(dateStr)
                if (date != null) break
            } catch (e: Exception) {}
        }
        
        val cal = Calendar.getInstance()
        if (date != null) {
            val dateCal = Calendar.getInstance()
            dateCal.time = date
            cal.set(Calendar.YEAR, dateCal.get(Calendar.YEAR))
            cal.set(Calendar.MONTH, dateCal.get(Calendar.MONTH))
            cal.set(Calendar.DAY_OF_MONTH, dateCal.get(Calendar.DAY_OF_MONTH))
        }
        
        val parts = timeStr.split(":")
        var hour = parts[0].toInt()
        val minute = parts[1].toInt()
        
        if (isPM && hour < 12) hour += 12
        
        cal.set(Calendar.HOUR_OF_DAY, hour)
        cal.set(Calendar.MINUTE, minute)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        
        return cal
    }

    private fun formatCountdown(millis: Long): String {
        if (millis <= 0) return "00:00:00"
        val hours = millis / (1000 * 60 * 60)
        val minutes = (millis / (1000 * 60)) % 60
        val seconds = (millis / 1000) % 60
        return String.format(Locale.ENGLISH, "%02d:%02d:%02d", hours, minutes, seconds)
    }

    override fun onDestroy() {
        super.onDestroy()
        updateRunnable?.let { handler.removeCallbacks(it) }
    }
}
