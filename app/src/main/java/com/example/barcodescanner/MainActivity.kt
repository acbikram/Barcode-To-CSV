package com.example.barcodescanner

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.barcodescanner.data.BarcodeRecord
import com.example.barcodescanner.data.BarcodeDatabase
import com.example.barcodescanner.data.BarcodeRepository
import com.example.barcodescanner.databinding.ActivityMainBinding
import com.example.barcodescanner.scanning.BarcodeAnalyzer
import com.example.barcodescanner.ui.adapters.RecentScansAdapter
import com.example.barcodescanner.ui.dialogs.CopiesDialogFragment
import com.example.barcodescanner.utils.CsvExporter
import com.example.barcodescanner.utils.PreferencesManager
import com.example.barcodescanner.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var repository: BarcodeRepository
    private lateinit var viewModel: MainViewModel
    private var isScanningActive = false
    private lateinit var recentAdapter: RecentScansAdapter
    private val tagTypes = listOf("A4", "4PCS", "VEG", "4PCS_DATE", "4PCS_SAME")
    private val unitTypes = listOf("PCS", "CTN", "PKT", "KGS")
    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private var cameraProvider: ProcessCameraProvider? = null
    private var imageAnalysis: ImageAnalysis? = null
    private var inactivityTimer: CountDownTimer? = null
    private var mediaPlayer: MediaPlayer? = null

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startCamera() else Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        preferencesManager = PreferencesManager(this)
        repository = BarcodeRepository(BarcodeDatabase.getDatabase(this).barcodeDao())
        viewModel = MainViewModel(repository)

        setupSpinners()
        setupRecyclerView()
        observeRecentScans()
        setupUI()

        try {
            mediaPlayer = MediaPlayer.create(this, R.raw.beep)
        } catch (e: Exception) { }

        binding.buttonHistory.setOnClickListener { HistoryActivity.start(this) }
        binding.buttonExport.setOnClickListener { exportCSV() }
        binding.buttonStartScan.setOnClickListener {
            if (hasCameraPermission()) {
                startCamera()
                binding.buttonStartScan.visibility = View.GONE
                binding.viewFinder.visibility = View.VISIBLE
                binding.scanOverlay.visibility = View.VISIBLE
            } else {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }

        binding.scanOverlay.setOnClickListener {
            if (!isScanningActive && hasCameraPermission()) {
                startCamera()
                binding.scanOverlay.alpha = 1f
                binding.scanOverlay.visibility = View.VISIBLE
            }
        }
    }

    private fun setupUI() {
        binding.viewFinder.visibility = View.GONE
        binding.scanOverlay.visibility = View.GONE
        binding.scanOverlay.alpha = 1f
    }

    private fun setupSpinners() {
        val tagAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, tagTypes).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.spinnerTagType.adapter = tagAdapter
        val lastTag = preferencesManager.lastTagType
        binding.spinnerTagType.setSelection(tagTypes.indexOf(lastTag).takeIf { it >= 0 } ?: 0)
        binding.spinnerTagType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                preferencesManager.lastTagType = tagTypes[position]
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        val unitAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, unitTypes).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.spinnerUnitType.adapter = unitAdapter
        val lastUnit = preferencesManager.lastUnitType
        binding.spinnerUnitType.setSelection(unitTypes.indexOf(lastUnit).takeIf { it >= 0 } ?: 0)
        binding.spinnerUnitType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                preferencesManager.lastUnitType = unitTypes[position]
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupRecyclerView() {
        recentAdapter = RecentScansAdapter()
        binding.recyclerViewRecentScans.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewRecentScans.adapter = recentAdapter
    }

    private fun observeRecentScans() {
        lifecycleScope.launch {
            viewModel.recentScans.collect { records ->
                recentAdapter.submitList(records)
            }
        }
    }

    private fun hasCameraPermission() = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
            }
            imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            imageAnalysis?.setAnalyzer(cameraExecutor, BarcodeAnalyzer { barcode ->
                if (isScanningActive) {
                    isScanningActive = false
                    runOnUiThread {
                        playBeepAndVibrate()
                        handleScannedBarcode(barcode)
                        resetInactivityTimer()
                    }
                }
            })
            cameraProvider?.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis)
            isScanningActive = true
            startInactivityTimer()
        }, ContextCompat.getMainExecutor(this))
    }

    private fun stopCamera() {
        cameraProvider?.unbindAll()
        isScanningActive = false
        inactivityTimer?.cancel()
        binding.viewFinder.visibility = View.GONE
        binding.scanOverlay.visibility = View.VISIBLE
        binding.scanOverlay.alpha = 0.7f
        binding.buttonStartScan.visibility = View.VISIBLE
    }

    private fun startInactivityTimer() {
        inactivityTimer?.cancel()
        inactivityTimer = object : CountDownTimer(10000, 1000) {
            override fun onTick(millisUntilFinished: Long) {}
            override fun onFinish() {
                if (isScanningActive) {
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "No scan for 10 seconds. Camera closed.", Toast.LENGTH_SHORT).show()
                        stopCamera()
                    }
                }
            }
        }.start()
    }

    private fun resetInactivityTimer() {
        if (isScanningActive) startInactivityTimer()
    }

    private fun playBeepAndVibrate() {
        val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(100)
        }
        try {
            mediaPlayer?.start()
        } catch (e: Exception) { }
    }

    private fun handleScannedBarcode(barcode: String) {
        val selectedTag = binding.spinnerTagType.selectedItem.toString()
        val selectedUnit = binding.spinnerUnitType.selectedItem.toString()

        lifecycleScope.launch {
            val existing = repository.findDuplicate(barcode, selectedTag, selectedUnit)
            if (existing != null) {
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("Duplicate Barcode")
                    .setMessage("This barcode already exists with same Tag and Unit.\nCurrent copies: ${existing.copies}\nAdd one more copy?")
                    .setPositiveButton("Yes") { _, _ ->
                        lifecycleScope.launch {
                            existing.copies += 1
                            repository.updateRecord(existing)
                            Toast.makeText(this@MainActivity, "Copies increased to ${existing.copies}", Toast.LENGTH_SHORT).show()
                            delay(500)
                            isScanningActive = true
                        }
                    }
                    .setNegativeButton("No") { _, _ ->
                        lifecycleScope.launch {
                            saveNewRecord(barcode, selectedTag, selectedUnit)
                        }
                    }
                    .setOnDismissListener { isScanningActive = true }
                    .show()
            } else {
                saveNewRecord(barcode, selectedTag, selectedUnit)
            }
        }
    }

    private suspend fun saveNewRecord(barcode: String, tag: String, unit: String) {
        CopiesDialogFragment.newInstance { copies ->
            val record = BarcodeRecord(
                barcode = barcode,
                price = null,
                tagType = tag,
                unitType = unit,
                copies = copies,
                customEng = null,
                customAra = null
            )
            lifecycleScope.launch {
                viewModel.saveRecord(record)
                delay(500)
                isScanningActive = true
            }
        }.show(supportFragmentManager, "copies_dialog")
    }

    private fun exportCSV() {
        lifecycleScope.launch {
            val records = repository.getAllRecords().first()
            CsvExporter.exportToCSV(this@MainActivity, records)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        mediaPlayer?.release()
        inactivityTimer?.cancel()
    }
}
