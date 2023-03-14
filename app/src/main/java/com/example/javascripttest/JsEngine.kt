package com.example.javascripttest

import android.content.Context
import android.util.Log
import androidx.javascriptengine.JavaScriptIsolate
import androidx.javascriptengine.JavaScriptSandbox
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CompletableDeferred
import java.io.FileNotFoundException
import java.nio.charset.Charset

class JsEngine(private val context: Context) {
    companion object {
        private const val TAG = "JsEngine"
    }

    private lateinit var jsSandboxFuture: ListenableFuture<JavaScriptSandbox>
    private lateinit var jsSandBox: JavaScriptSandbox
    private lateinit var jsIsolate: JavaScriptIsolate
    private lateinit var futureIsolate: ListenableFuture<JavaScriptIsolate>
//    val code = "function sum(a, b) { let r = a + b; return r.toString(); }; sum(3, 4)"
    val code = "sum(3, 4)"
    private val setupDone = CompletableDeferred<Unit>()

    private var mainFunctions: String  = getScript("jsengine/main.js")
        ?:throw(FileNotFoundException("main not found!"))


    private fun setup(): CompletableDeferred<Unit> {
        if (!setupDone.isCompleted) {
            jsSandboxFuture = JavaScriptSandbox.createConnectedInstanceAsync(context)
            futureIsolate = Futures.transform(
                jsSandboxFuture, { input ->
                    jsSandBox = input
                    jsIsolate = input.createIsolate()
                    setupDone.complete(Unit)
                    jsIsolate.evaluateJavaScriptAsync(code)
                    val resultFuture = jsIsolate.evaluateJavaScriptAsync(mainFunctions)
                    val res = resultFuture.get()
                    Log.d(TAG, "setup: mainFunctions load = $res")
                    jsIsolate
                }
            ) { it.run() }
        }
        return setupDone
    }

    private fun getScript(assetName: String): String? {
        val inputStream =
            context.assets?.open(assetName)
        val buffer = ByteArray(inputStream?.available() ?: return null)
        inputStream.read(buffer)
        return String(buffer, Charset.forName("UTF-8"))
    }
    suspend fun run(script: String): Result<String> {
        setup().await()
        val resultFun = CompletableDeferred<Result<String>>()
        val js: ListenableFuture<String> = Futures.transformAsync(
            futureIsolate,
            { isolate ->
                Log.d(TAG, "run: script $script")
                val resultFuture = isolate.evaluateJavaScriptAsync(script)
                resultFuture
            }) { it.run() }
        Futures.addCallback(
            js,
            object : FutureCallback<String> {
                override fun onSuccess(result: String) {
                    Log.d(TAG, "onSuccess: $result")
                    resultFun.complete(Result.success(result))
                }

                override fun onFailure(e: Throwable) {

                    resultFun.complete(Result.failure(e))
                }
            }
        ) { it.run() }
        return resultFun.await()
    }

    fun clear() {
        try {
            jsSandboxFuture.cancel(true)
            jsSandBox.close()
            jsIsolate.close()
        } catch (_: Throwable) {
        }
    }
}