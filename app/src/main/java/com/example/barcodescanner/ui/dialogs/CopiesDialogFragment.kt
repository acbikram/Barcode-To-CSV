package com.example.barcodescanner.ui.dialogs

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.example.barcodescanner.databinding.DialogCopiesBinding

class CopiesDialogFragment : DialogFragment() {
    private var _binding: DialogCopiesBinding? = null
    private val binding get() = _binding!!
    private var selectionCallback: ((Int) -> Unit)? = null
    private val timeoutHandler = Handler(Looper.getMainLooper())
    private var timeoutRunnable: Runnable? = null

    companion object {
        fun newInstance(callback: (Int) -> Unit): CopiesDialogFragment {
            return CopiesDialogFragment().apply { selectionCallback = callback }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogCopiesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        timeoutRunnable = Runnable {
            if (isAdded) {
                selectionCallback?.invoke(1)
                dismiss()
            }
        }
        timeoutHandler.postDelayed(timeoutRunnable!!, 3000)

        binding.buttonCopy1.setOnClickListener { selectCopies(1) }
        binding.buttonCopy2.setOnClickListener { selectCopies(2) }
        binding.buttonCopy3.setOnClickListener { selectCopies(3) }
        binding.buttonCopy4.setOnClickListener { selectCopies(4) }
        binding.buttonCopy5.setOnClickListener { selectCopies(5) }
    }

    private fun selectCopies(copies: Int) {
        timeoutHandler.removeCallbacks(timeoutRunnable!!)
        selectionCallback?.invoke(copies)
        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        timeoutHandler.removeCallbacks(timeoutRunnable!!)
        _binding = null
    }
}
