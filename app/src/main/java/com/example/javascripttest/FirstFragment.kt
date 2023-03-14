package com.example.javascripttest

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.InputEvent
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.activity.viewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.javascripttest.databinding.FragmentFirstBinding

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment() {
    companion object {
        private const val TAG = "FirstFragment"
    }

    private lateinit var binding: FragmentFirstBinding
    private val viewModel by viewModels<MainViewModel>()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root

    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding) {
            editTextFirst.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    viewModel.calcResult(editTextFirst.text.toString())
                    true
                } else false
            }
            buttonFirst.setOnClickListener {
                viewModel.calcResult(editTextFirst.text.toString())
            }
            viewModel.calcResultLiveData.observe(viewLifecycleOwner) {
                if (it.isSuccess) {
                    editTextFirst.setText(it.getOrNull())
                    editTextFirst.error = null
                } else {
                    editTextFirst.setText("error: ${it.exceptionOrNull()}")
                    editTextFirst.error = it.exceptionOrNull()?.message
                }
            }
        }
    }
}