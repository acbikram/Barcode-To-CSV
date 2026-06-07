package com.example.barcodescanner

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
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
    private var isScanning = true
    private lateinit var recentAdapter: RecentScansAdapter
    private val tagTypes = listOf("A4","4PCS","VEG","4PCS_DATE","4PCS_SAME")
    private val unitTypes = listOf("PCS","CTN","PKT","KGS")
    private val cameraExecutor = Executors.newSingleThreadExecutor()

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
        observeRecentScans()  // ✅ Fixed: using Flow collection
        binding.buttonHistory.setOnClickListener { HistoryActivity.start(this) }
        binding.buttonExport.setOnClickListener { exportCSV() }

        if (hasCameraPermission()) startCamera() else requestPermissionLauncher.launch(Manifest.permission.CAMERA)
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
        // ✅ Collect Flow instead of observing LiveData
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
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also { it.setSurfaceProvider(binding.viewFinder.surfaceProvider) }
            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            imageAnalysis.setAnalyzer(cameraExecutor, BarcodeAnalyzer { barcode ->
                if (isScanning) {
                    isScanning = false
                    runOnUiThread {
                        CopiesDialogFragment.newInstance { copies -> saveScannedRecord(barcode, copies) }
                            .show(supportFragmentManager, "copies_dialog")
                    }
                }
            })
            cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis)
        }, ContextCompat.getMainExecutor(this))
    }

    private fun saveScannedRecord(barcode: String, copies: Int) {
        val record = BarcodeRecord(
            barcode = barcode,
            price = null,
            tagType = binding.spinnerTagType.selectedItem.toString(),
            unitType = binding.spinnerUnitType.selectedItem.toString(),
            copies = copies,
            customEng = null,
            customAra = null
        )
        lifecycleScope.launch {
            viewModel.saveRecord(record)
            val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                vibrator.vibrate(100)
            }
            delay(500)
            isScanning = true
        }
    }

    private fun exportCSV() {
        lifecycleScope.launch {
            // ✅ first() is a suspend function from kotlinx.coroutines.flow
            val records = repository.getAllRecords().first()
            CsvExporter.exportToCSV(this@MainActivity, records)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
