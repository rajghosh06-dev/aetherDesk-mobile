package com.example.aetherdesk

import android.app.ActivityManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Environment
import android.os.StatFs
import android.webkit.JavascriptInterface
import android.app.AlertDialog
import org.json.JSONObject
import java.io.File
import java.util.concurrent.CountDownLatch
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.security.MessageDigest
import java.util.ArrayList
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import org.json.JSONArray

class AndroidBridge(private val activity: MainActivity) {

    @JavascriptInterface
    fun checkWifiStatus(): Boolean {
        val cm = activity.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    @JavascriptInterface
    fun setStatusBarTheme(isDark: Boolean) {
        activity.runOnUiThread {
            activity.setStatusBarTheme(isDark)
        }
    }

    @JavascriptInterface
    fun checkMobileData(): Boolean {
        val cm = activity.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
    }

    @JavascriptInterface
    fun requestDownloadPermission(sizeMb: Double): Boolean {
        val latch = CountDownLatch(1)
        var allowed = false

        activity.runOnUiThread {
            AlertDialog.Builder(activity)
                .setTitle("Cellular Download Request")
                .setMessage("AetherDesk needs to download model files (approx. ${sizeMb.toInt()} MB). You are currently connected to Mobile Data. Do you want to download over mobile network?")
                .setPositiveButton("Download anyway") { _, _ ->
                    allowed = true
                    latch.countDown()
                }
                .setNegativeButton("Wait for WiFi") { _, _ ->
                    allowed = false
                    latch.countDown()
                }
                .setCancelable(false)
                .show()
        }

        try {
            latch.await()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        return allowed
    }

    @JavascriptInterface
    fun getDeviceTelemetry(): String {
        val telemetry = JSONObject()
        try {
            // Get RAM Stats
            val actManager = activity.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            actManager.getMemoryInfo(memInfo)
            
            val rawRamGb = memInfo.totalMem.toDouble() / (1024 * 1024 * 1024)
            val rawAvailGb = memInfo.availMem.toDouble() / (1024 * 1024 * 1024)
            
            // Round RAM to standard capacities (1, 2, 3, 4, 6, 8, 12, 16, 24, 32, 64)
            val standardRamSizes = doubleArrayOf(1.0, 2.0, 3.0, 4.0, 6.0, 8.0, 12.0, 16.0, 24.0, 32.0, 64.0)
            var totalRamGb = rawRamGb
            var rMinDiff = Double.MAX_VALUE
            for (size in standardRamSizes) {
                val diff = Math.abs(rawRamGb - size)
                if (diff < rMinDiff && rawRamGb <= size * 1.15) {
                    rMinDiff = diff
                    totalRamGb = size
                }
            }
            if (rMinDiff >= 1.5) {
                totalRamGb = Math.ceil(rawRamGb)
            }
            
            val rScale = totalRamGb / rawRamGb
            val availRamGb = rawAvailGb * rScale
            val usedRamGb = totalRamGb - availRamGb
            val ramPercentage = (usedRamGb / totalRamGb) * 100

            // Get Storage Stats
            val path = Environment.getDataDirectory()
            val stat = StatFs(path.path)
            val blockSize = stat.blockSizeLong
            val totalBlocks = stat.blockCountLong
            val availableBlocks = stat.availableBlocksLong

            val rawStorageGb = (totalBlocks * blockSize).toDouble() / (1024 * 1024 * 1024)
            val rawFreeGb = (availableBlocks * blockSize).toDouble() / (1024 * 1024 * 1024)
            
            // Round storage to standard capacities (16, 32, 64, 128, 256, 512, 1024)
            val standardStorageSizes = doubleArrayOf(16.0, 32.0, 64.0, 128.0, 256.0, 512.0, 1024.0)
            var totalStorageGb = rawStorageGb
            var sMinDiff = Double.MAX_VALUE
            for (size in standardStorageSizes) {
                val diff = Math.abs(rawStorageGb - size)
                if (diff < sMinDiff && rawStorageGb <= size * 1.25) {
                    sMinDiff = diff
                    totalStorageGb = size
                }
            }
            if (sMinDiff >= 20.0) {
                totalStorageGb = Math.ceil(rawStorageGb)
            }
            
            val sScale = totalStorageGb / rawStorageGb
            val freeStorageGb = rawFreeGb * sScale
            val usedStorageGb = totalStorageGb - freeStorageGb

            telemetry.put("cpu_usage", (5 + (Math.random() * 10)).toInt()) // Simulated mobile CPU usage
            telemetry.put("ram_percentage", ramPercentage.toInt())
            telemetry.put("ram_total", String.format("%.2f GB", totalRamGb))
            telemetry.put("ram_avail", String.format("%.2f GB", availRamGb))
            telemetry.put("ram_used", String.format("%.2f GB", usedRamGb))
            telemetry.put("storage_total", String.format("%.2f GB", totalStorageGb))
            telemetry.put("storage_free", String.format("%.2f GB", freeStorageGb))
            telemetry.put("storage_used", String.format("%.2f GB", usedStorageGb))
            telemetry.put("vram_free", String.format("%.2f GB", freeStorageGb * 0.1)) // Simulated Virtual RAM
            
            // Check physical SD card storage
            val dirs = activity.getExternalFilesDirs(null)
            if (dirs != null && dirs.size > 1) {
                val sdDir = dirs[1]
                if (sdDir != null) {
                    try {
                        val sdStat = StatFs(sdDir.path)
                        val sdRawTotal = (sdStat.blockCountLong * sdStat.blockSizeLong).toDouble() / (1024 * 1024 * 1024)
                        val sdRawFree = (sdStat.availableBlocksLong * sdStat.blockSizeLong).toDouble() / (1024 * 1024 * 1024)
                        
                        var sdTotalGb = sdRawTotal
                        var sdMinDiff = Double.MAX_VALUE
                        for (size in standardStorageSizes) {
                            val diff = Math.abs(sdRawTotal - size)
                            if (diff < sdMinDiff && sdRawTotal <= size * 1.25) {
                                sdMinDiff = diff
                                sdTotalGb = size
                            }
                        }
                        if (sdMinDiff >= 20.0) {
                            sdTotalGb = Math.ceil(sdRawTotal)
                        }
                        val sdScale = sdTotalGb / sdRawTotal
                        val sdFreeGb = sdRawFree * sdScale
                        val sdUsedGb = sdTotalGb - sdFreeGb
                        
                        telemetry.put("sd_total", String.format("%.2f GB", sdTotalGb))
                        telemetry.put("sd_free", String.format("%.2f GB", sdFreeGb))
                        telemetry.put("sd_used", String.format("%.2f GB", sdUsedGb))
                        telemetry.put("sd_present", true)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            
            telemetry.put("device_model", android.os.Build.MODEL)
            telemetry.put("device_manufacturer", android.os.Build.MANUFACTURER)
            telemetry.put("device_brand", android.os.Build.BRAND)
            telemetry.put("device_hardware", android.os.Build.HARDWARE)
            telemetry.put("device_board", android.os.Build.BOARD)
            telemetry.put("device_cores", Runtime.getRuntime().availableProcessors())
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return telemetry.toString()
    }

    @JavascriptInterface
    fun saveNoteFile(filename: String, content: String): String {
        return try {
            val directory = activity.getExternalFilesDir(null) ?: activity.filesDir
            val ocrDir = File(directory, "ocr_notes")
            if (!ocrDir.exists()) {
                ocrDir.mkdirs()
            }
            val cleanName = if (filename.endsWith(".txt")) filename else "$filename.txt"
            val file = File(ocrDir, cleanName)
            file.writeText(content, Charsets.UTF_8)
            file.absolutePath
        } catch (e: Exception) {
            "Error: " + e.message
        }
    }

    @JavascriptInterface
    fun setStatusBarTheme(theme: String) {
        activity.runOnUiThread {
            activity.setStatusBarTheme(theme == "dark")
        }
    }

    @JavascriptInterface
    fun saveImageToGallery(filename: String, base64Data: String): String {
        return try {
            val directory = try {
                val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val aetherDir = File(dir, "AetherDesk")
                if (!aetherDir.exists()) {
                    aetherDir.mkdirs()
                }
                aetherDir
            } catch (e: Exception) {
                val dir = activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: activity.filesDir
                val aetherDir = File(dir, "AetherDesk")
                if (!aetherDir.exists()) {
                    aetherDir.mkdirs()
                }
                aetherDir
            }
            val cleanName = if (filename.endsWith(".png")) filename else "$filename.png"
            val file = File(directory, cleanName)
            
            val pureBase64 = if (base64Data.contains(",")) {
                base64Data.substringAfter(",")
            } else {
                base64Data
            }
            val bytes = android.util.Base64.decode(pureBase64, android.util.Base64.DEFAULT)
            file.writeBytes(bytes)
            
            // Register image with system media scanner so it is visible in the Gallery
            val mediaScanIntent = android.content.Intent(android.content.Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
            val contentUri = android.net.Uri.fromFile(file)
            mediaScanIntent.data = contentUri
            activity.sendBroadcast(mediaScanIntent)
            
            file.absolutePath
        } catch (e: Exception) {
            "Error: " + e.message
        }
    }

    @JavascriptInterface
    fun saveFileToDownloads(filename: String, base64Data: String): String {
        return try {
            val directory = try {
                val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!dir.exists()) {
                    dir.mkdirs()
                }
                dir
            } catch (e: Exception) {
                activity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: activity.filesDir
            }
            val file = File(directory, filename)
            
            val pureBase64 = if (base64Data.contains(",")) {
                base64Data.substringAfter(",")
            } else {
                base64Data
            }
            val bytes = android.util.Base64.decode(pureBase64, android.util.Base64.DEFAULT)
            file.writeBytes(bytes)
            
            val mediaScanIntent = android.content.Intent(android.content.Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
            val contentUri = android.net.Uri.fromFile(file)
            mediaScanIntent.data = contentUri
            activity.sendBroadcast(mediaScanIntent)
            
            file.absolutePath
        } catch (e: Exception) {
            "Error: " + e.message
        }
    }

    @JavascriptInterface
    fun extractTextFromBase64(base64Data: String): String {
        return try {
            val pureBase64 = if (base64Data.contains(",")) {
                base64Data.substringAfter(",")
            } else {
                base64Data
            }
            val bytes = android.util.Base64.decode(pureBase64, android.util.Base64.DEFAULT)
            val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                ?: return "Error: Failed to decode image bytes."
            
            val image = com.google.mlkit.vision.common.InputImage.fromBitmap(bitmap, 0)
            val recognizer = com.google.mlkit.vision.text.TextRecognition.getClient(
                com.google.mlkit.vision.text.latin.TextRecognizerOptions.DEFAULT_OPTIONS
            )
            
            val task = recognizer.process(image)
            val resultText = com.google.android.gms.tasks.Tasks.await(task)
            resultText.text
        } catch (e: Exception) {
            "Offline OCR failed: " + e.message
        }
    }

    @JavascriptInterface
    fun extractTextFromPdf(base64Pdf: String): String {
        return try {
            val pureBase64 = if (base64Pdf.contains(",")) base64Pdf.substringAfter(",") else base64Pdf
            val bytes = android.util.Base64.decode(pureBase64, android.util.Base64.DEFAULT)
            
            val tempFile = File.createTempFile("temp_pdf_ocr", ".pdf", activity.cacheDir)
            tempFile.writeBytes(bytes)
            
            val pfd = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            val sb = StringBuilder()
            
            val recognizer = com.google.mlkit.vision.text.TextRecognition.getClient(
                com.google.mlkit.vision.text.latin.TextRecognizerOptions.DEFAULT_OPTIONS
            )
            
            for (i in 0 until renderer.pageCount) {
                val page = renderer.openPage(i)
                val scale = 2.0f
                val width = (page.width * scale).toInt()
                val height = (page.height * scale).toInt()
                
                val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
                val canvas = android.graphics.Canvas(bitmap)
                canvas.drawColor(android.graphics.Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                
                val image = com.google.mlkit.vision.common.InputImage.fromBitmap(bitmap, 0)
                val task = recognizer.process(image)
                val resultText = com.google.android.gms.tasks.Tasks.await(task)
                sb.append("--- Page ").append(i + 1).append(" ---\n")
                sb.append(resultText.text).append("\n")
                
                bitmap.recycle()
            }
            
            renderer.close()
            pfd.close()
            tempFile.delete()
            
            sb.toString()
        } catch (e: Exception) {
            "Error extracting text from PDF: " + e.message
        }
    }

    @JavascriptInterface
    fun encryptFile(base64Data: String, filename: String, passwordKey: String): String {
        return try {
            val pureBase64 = if (base64Data.contains(",")) base64Data.substringAfter(",") else base64Data
            val rawBytes = android.util.Base64.decode(pureBase64, android.util.Base64.DEFAULT)
            
            val digest = MessageDigest.getInstance("SHA-256")
            val keyBytes = digest.digest(passwordKey.toByteArray(Charsets.UTF_8))
            val secretKey = SecretKeySpec(keyBytes, "AES")
            
            val ivBytes = ByteArray(16)
            val ivSpec = IvParameterSpec(ivBytes)
            
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec)
            val encryptedBytes = cipher.doFinal(rawBytes)
            
            val directory = activity.getExternalFilesDir(null) ?: activity.filesDir
            val secureDir = File(directory, "secure_vault")
            if (!secureDir.exists()) secureDir.mkdirs()
            
            val cleanName = if (filename.contains(".")) {
                val ext = filename.substringAfterLast(".")
                val base = filename.substringBeforeLast(".")
                "${base}_locked.$ext"
            } else {
                "${filename}_locked"
            }
            val file = File(secureDir, cleanName)
            file.writeBytes(encryptedBytes)
            
            file.absolutePath
        } catch (e: Exception) {
            "Error: " + e.message
        }
    }

    @JavascriptInterface
    fun decryptFile(base64Data: String, filename: String, passwordKey: String): String {
        return try {
            val pureBase64 = if (base64Data.contains(",")) base64Data.substringAfter(",") else base64Data
            val rawBytes = android.util.Base64.decode(pureBase64, android.util.Base64.DEFAULT)
            
            val digest = MessageDigest.getInstance("SHA-256")
            val keyBytes = digest.digest(passwordKey.toByteArray(Charsets.UTF_8))
            val secretKey = SecretKeySpec(keyBytes, "AES")
            
            val ivBytes = ByteArray(16)
            val ivSpec = IvParameterSpec(ivBytes)
            
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)
            val decryptedBytes = cipher.doFinal(rawBytes)
            
            val directory = activity.getExternalFilesDir(null) ?: activity.filesDir
            val secureDir = File(directory, "secure_vault")
            if (!secureDir.exists()) secureDir.mkdirs()
            
            val cleanName = if (filename.contains(".")) {
                val ext = filename.substringAfterLast(".")
                val base = filename.substringBeforeLast(".")
                val finalBase = if (base.endsWith("_locked")) base.substringBeforeLast("_locked") else base
                "${finalBase}_unlocked.$ext"
            } else {
                "${filename}_unlocked"
            }
            val file = File(secureDir, cleanName)
            file.writeBytes(decryptedBytes)
            
            file.absolutePath
        } catch (e: Exception) {
            "Error: " + e.message
        }
    }

    @JavascriptInterface
    fun recoverPassword(base64Data: String, mode: String, candidatesJson: String): String {
        return try {
            val pureBase64 = if (base64Data.contains(",")) base64Data.substringAfter(",") else base64Data
            val rawBytes = android.util.Base64.decode(pureBase64, android.util.Base64.DEFAULT)
            
            val json = org.json.JSONArray(candidatesJson)
            val candidates = ArrayList<String>()
            for (i in 0 until json.length()) {
                candidates.add(json.getString(i))
            }
            
            val digest = MessageDigest.getInstance("SHA-256")
            val ivBytes = ByteArray(16)
            val ivSpec = IvParameterSpec(ivBytes)
            
            for (pwd in candidates) {
                try {
                    val keyBytes = digest.digest(pwd.toByteArray(Charsets.UTF_8))
                    val secretKey = SecretKeySpec(keyBytes, "AES")
                    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
                    cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)
                    cipher.doFinal(rawBytes)
                    return pwd
                } catch (e: Exception) {
                    // Ignore and try next
                }
            }
            ""
        } catch (e: Exception) {
            ""
        }
    }

    @JavascriptInterface
    fun combineImagesToPdf(base64ImagesJson: String, outputFilename: String): String {
        return try {
            val json = JSONArray(base64ImagesJson)
            val pdfDoc = PdfDocument()
            val directory = try {
                val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!dir.exists()) {
                    dir.mkdirs()
                }
                dir
            } catch (e: Exception) {
                activity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: activity.filesDir
            }
            
            val cleanName = if (outputFilename.endsWith(".pdf")) outputFilename else "$outputFilename.pdf"
            val outputFile = File(directory, cleanName)
            
            for (i in 0 until json.length()) {
                val base64Str = json.getString(i)
                val pureBase64 = if (base64Str.contains(",")) base64Str.substringAfter(",") else base64Str
                val bytes = android.util.Base64.decode(pureBase64, android.util.Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: continue
                
                val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, i + 1).create()
                val page = pdfDoc.startPage(pageInfo)
                val canvas = page.canvas
                canvas.drawBitmap(bitmap, 0f, 0f, null)
                pdfDoc.finishPage(page)
            }
            
            val outStream = java.io.FileOutputStream(outputFile)
            pdfDoc.writeTo(outStream)
            outStream.close()
            pdfDoc.close()
            
            // Register with system media scanner
            val mediaScanIntent = android.content.Intent(android.content.Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
            val contentUri = android.net.Uri.fromFile(outputFile)
            mediaScanIntent.data = contentUri
            activity.sendBroadcast(mediaScanIntent)
            
            outputFile.absolutePath
        } catch (e: Exception) {
            "Error: " + e.message
        }
    }

    @JavascriptInterface
    fun rasterizePdf(base64Pdf: String, pdfName: String): String {
        return try {
            val pureBase64 = if (base64Pdf.contains(",")) base64Pdf.substringAfter(",") else base64Pdf
            val bytes = android.util.Base64.decode(pureBase64, android.util.Base64.DEFAULT)
            
            val tempFile = File.createTempFile("temp_pdf", ".pdf", activity.cacheDir)
            tempFile.writeBytes(bytes)
            
            val pfd = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            val resultJson = org.json.JSONArray()
            
            for (i in 0 until renderer.pageCount) {
                val page = renderer.openPage(i)
                val bitmap = android.graphics.Bitmap.createBitmap(page.width, page.height, android.graphics.Bitmap.Config.ARGB_8888)
                val canvas = android.graphics.Canvas(bitmap)
                canvas.drawColor(android.graphics.Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                
                val stream = java.io.ByteArrayOutputStream()
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, stream)
                val imgBytes = stream.toByteArray()
                val imgBase64 = android.util.Base64.encodeToString(imgBytes, android.util.Base64.NO_WRAP)
                resultJson.put("data:image/jpeg;base64,$imgBase64")
            }
            renderer.close()
            pfd.close()
            tempFile.delete()
            
            resultJson.toString()
        } catch (e: Exception) {
            "Error: " + e.message
        }
    }

    @JavascriptInterface
    fun compressPdf(base64Pdf: String, pdfName: String, quality: Int): String {
        return try {
            val pureBase64 = if (base64Pdf.contains(",")) base64Pdf.substringAfter(",") else base64Pdf
            val bytes = android.util.Base64.decode(pureBase64, android.util.Base64.DEFAULT)
            
            val tempFile = File.createTempFile("temp_pdf_comp", ".pdf", activity.cacheDir)
            tempFile.writeBytes(bytes)
            
            val pfd = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            val pdfDoc = PdfDocument()
            
            for (i in 0 until renderer.pageCount) {
                val page = renderer.openPage(i)
                val origWidth = page.width
                val origHeight = page.height
                
                val scale = when {
                    quality >= 85 -> 2.778   // 200 DPI
                    quality >= 70 -> 2.083   // 150 DPI
                    quality >= 50 -> 1.389   // 100 DPI
                    else -> 1.0              // 72 DPI
                }
                val width = (origWidth * scale).toInt()
                val height = (origHeight * scale).toInt()
                
                val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
                val canvas = android.graphics.Canvas(bitmap)
                canvas.drawColor(android.graphics.Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                
                val stream = java.io.ByteArrayOutputStream()
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, quality, stream)
                val compressedBytes = stream.toByteArray()
                val compressedBitmap = BitmapFactory.decodeByteArray(compressedBytes, 0, compressedBytes.size) ?: continue
                
                val pageInfo = PdfDocument.PageInfo.Builder(origWidth, origHeight, i + 1).create()
                val pdfPage = pdfDoc.startPage(pageInfo)
                val pageCanvas = pdfPage.canvas
                val destRect = android.graphics.Rect(0, 0, origWidth, origHeight)
                pageCanvas.drawBitmap(compressedBitmap, null, destRect, null)
                pdfDoc.finishPage(pdfPage)
            }
            
            renderer.close()
            pfd.close()
            tempFile.delete()
            
            val outFile = File.createTempFile("compressed_pdf", ".pdf", activity.cacheDir)
            val outStream = java.io.FileOutputStream(outFile)
            pdfDoc.writeTo(outStream)
            outStream.close()
            pdfDoc.close()
            
            val compressedPdfBytes = outFile.readBytes()
            outFile.delete()
            
            val compressedBase64 = android.util.Base64.encodeToString(compressedPdfBytes, android.util.Base64.DEFAULT)
            "data:application/pdf;base64,$compressedBase64"
        } catch (e: Exception) {
            "Error: " + e.message
        }
    }

    @JavascriptInterface
    fun warpPerspective(
        base64Data: String,
        x0: Float, y0: Float,
        x1: Float, y1: Float,
        x2: Float, y2: Float,
        x3: Float, y3: Float,
        destWidth: Int, destHeight: Int
    ): String {
        return try {
            val pureBase64 = if (base64Data.contains(",")) base64Data.substringAfter(",") else base64Data
            val bytes = android.util.Base64.decode(pureBase64, android.util.Base64.DEFAULT)
            val srcBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                ?: return "Error: Failed to decode image."
            
            val destBitmap = android.graphics.Bitmap.createBitmap(destWidth, destHeight, android.graphics.Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(destBitmap)
            
            val matrix = android.graphics.Matrix()
            val srcPts = floatArrayOf(
                x0, y0,
                x1, y1,
                x2, y2,
                x3, y3
            )
            val dstPts = floatArrayOf(
                0f, 0f,
                destWidth.toFloat(), 0f,
                destWidth.toFloat(), destHeight.toFloat(),
                0f, destHeight.toFloat()
            )
            
            val success = matrix.setPolyToPoly(srcPts, 0, dstPts, 0, 4)
            if (success) {
                val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
                paint.isFilterBitmap = true
                canvas.drawBitmap(srcBitmap, matrix, paint)
            } else {
                canvas.drawBitmap(srcBitmap, 0f, 0f, null)
            }
            
            val stream = java.io.ByteArrayOutputStream()
            destBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, stream)
            val imgBytes = stream.toByteArray()
            
            srcBitmap.recycle()
            destBitmap.recycle()
            
            val imgBase64 = android.util.Base64.encodeToString(imgBytes, android.util.Base64.NO_WRAP)
            "data:image/jpeg;base64,$imgBase64"
        } catch (e: Exception) {
            "Error: " + e.message
        }
    }
}
