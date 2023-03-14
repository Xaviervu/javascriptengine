package com.example.javascripttest

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MainViewModel(val app: Application):AndroidViewModel(app) {
    private val jsEngine: JsEngine = JsEngine(app)
    private var jsEngineJob: Job? = null
    private val calcResultMutableLiveData = MutableLiveData<Result<String>>()
    val calcResultLiveData: LiveData<Result<String>> = calcResultMutableLiveData
    init {
        calcResultMutableLiveData.postValue(Result.success(jsEngine.code))
    }
    fun calcResult(script: String){
        jsEngineJob?.cancel()
        jsEngineJob = viewModelScope.launch {
            val calcResult = jsEngine.run(script)
            calcResultMutableLiveData.postValue(calcResult)
        }
    }

    override fun onCleared() {
        super.onCleared()
        jsEngine.clear()
    }
}