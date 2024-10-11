package com.v2ray.ang.util

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.EncodeHintType
import com.google.zxing.NotFoundException
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.GlobalHistogramBinarizer
import com.google.zxing.qrcode.QRCodeReader
import com.google.zxing.qrcode.QRCodeWriter
import java.util.EnumMap

/**
 * Description: Parse QR code image
 */
object QRCodeDecoder {
    private val HINTS: MutableMap<DecodeHintType, Any?> = EnumMap(DecodeHintType::class.java)

    /**
     * create QR Code using zxing
     */
    fun createQRCode(text: String, size: Int = 600): Bitmap? {
        try {
            val hints = HashMap<EncodeHintType, String>()
            hints[EncodeHintType.CHARACTER_SET] = "utf-8"
            val bitMatrix = QRCodeWriter().encode(
                text,
                BarcodeFormat.QR_CODE, size, size, hints
            )
            val pixels = IntArray(size * size)
            for (y in 0 until size) {
                for (x in 0 until size) {
                    if (bitMatrix.get(x, y)) {
                        pixels[y * size + x] = Color.BLACK
                    } else {
                        pixels[y * size + x] = Color.WHITE
                    }

                }
            }
            val bitmap = Bitmap.createBitmap(
                size, size,
                Bitmap.Config.ARGB_8888
            )
            bitmap.setPixels(pixels, 0, size, 0, 0, size, size)
            return bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    /**
     * Parse bitmap QR code synchronously. This method is a time-consuming operation, please call it in a sub-thread.
     *
     * @param bitmap The QR code image to parse.
     * @return Returns the content of the QR code image or null.
     */
    fun syncDecodeQRCode(bitmap: Bitmap?): String? {
        if (bitmap == null) {
            return null
        }
        val source: RGBLuminanceSource?
        try {
            val width = bitmap.width
            val height = bitmap.height
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
            source = RGBLuminanceSource(width, height, pixels)
            val qrReader = QRCodeReader()
            try {
                val result = try {
                    qrReader.decode(
                        BinaryBitmap(GlobalHistogramBinarizer(source)),
                        mapOf(DecodeHintType.TRY_HARDER to true)
                    )
                } catch (e: NotFoundException) {
                    qrReader.decode(
                        BinaryBitmap(GlobalHistogramBinarizer(source.invert())),
                        mapOf(DecodeHintType.TRY_HARDER to true)
                    )
                }
                return result.text
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return null
    }

    init {
        val allFormats: List<BarcodeFormat> = arrayListOf(
            BarcodeFormat.AZTEC,
            BarcodeFormat.CODABAR,
            BarcodeFormat.CODE_39,
            BarcodeFormat.CODE_93,
            BarcodeFormat.CODE_128,
            BarcodeFormat.DATA_MATRIX,
            BarcodeFormat.EAN_8,
            BarcodeFormat.EAN_13,
            BarcodeFormat.ITF,
            BarcodeFormat.MAXICODE,
            BarcodeFormat.PDF_417,
            BarcodeFormat.QR_CODE,
            BarcodeFormat.RSS_14,
            BarcodeFormat.RSS_EXPANDED,
            BarcodeFormat.UPC_A,
            BarcodeFormat.UPC_E,
            BarcodeFormat.UPC_EAN_EXTENSION
        )
        HINTS[DecodeHintType.TRY_HARDER] = BarcodeFormat.QR_CODE
        HINTS[DecodeHintType.POSSIBLE_FORMATS] = allFormats
        HINTS[DecodeHintType.CHARACTER_SET] = "utf-8"
    }
}
