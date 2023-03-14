package com.example.javascripttest

import android.content.Context
import androidx.javascriptengine.JavaScriptIsolate
import androidx.javascriptengine.JavaScriptSandbox
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CompletableDeferred

class JsEngine(private val context: Context) {
    companion object {
        private const val TAG = "JsEngine"
    }

    private lateinit var jsSandboxFuture: ListenableFuture<JavaScriptSandbox>
    private lateinit var jsSandBox: JavaScriptSandbox
    private lateinit var jsIsolate: JavaScriptIsolate
    private lateinit var futureIsolate: ListenableFuture<JavaScriptIsolate>
    val code = "function sum(a, b) { let r = a + b; return r.toString(); }; sum(3, 4)"
    private val setupDone = CompletableDeferred<Unit>()
    private fun setup(): CompletableDeferred<Unit> {
        if (!setupDone.isCompleted) {
            jsSandboxFuture = JavaScriptSandbox.createConnectedInstanceAsync(context)
            futureIsolate = Futures.transform(
                jsSandboxFuture, { input ->
                    jsSandBox = input
                    jsIsolate = input.createIsolate()
                    setupDone.complete(Unit)
                    jsIsolate.evaluateJavaScriptAsync(code)
//                    val resultFuture = jsIsolate.evaluateJavaScriptAsync(code)
//                    val result = resultFuture[1,TimeUnit.NANOSECONDS] // 1ns is too fast for timeout, error should go to onFailure
                    jsIsolate
                }
            ) { it.run() }
        }
        return setupDone
    }

    suspend fun run(script: String): Result<String> {
        setup().await()
        val resultFun = CompletableDeferred<Result<String>>()
        val js: ListenableFuture<String> = Futures.transformAsync(
            futureIsolate,
            { isolate ->
                val resultFuture = isolate.evaluateJavaScriptAsync(script)
                resultFuture
            }) { it.run() }
        Futures.addCallback(
            js,
            object : FutureCallback<String> {
                override fun onSuccess(result: String) {
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