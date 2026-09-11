package com.google.android.gms.mlkit.vision.codescanner

import android.content.Context

class GmsBarcodeScannerOptions private constructor(
    internal val delegate: com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
) {
    class Builder {
        private val delegate = com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions.Builder()

        fun setBarcodeFormats(format: Int, vararg moreFormats: Int): Builder = apply {
            delegate.setBarcodeFormats(format, *moreFormats)
        }

        fun enableAutoZoom(): Builder = apply {
            delegate.enableAutoZoom()
        }

        fun build(): GmsBarcodeScannerOptions = GmsBarcodeScannerOptions(delegate.build())
    }
}

object GmsBarcodeScanning {
    fun getClient(
        context: Context,
        options: GmsBarcodeScannerOptions
    ): com.google.mlkit.vision.codescanner.GmsBarcodeScanner =
        com.google.mlkit.vision.codescanner.GmsBarcodeScanning.getClient(context, options.delegate)
}
