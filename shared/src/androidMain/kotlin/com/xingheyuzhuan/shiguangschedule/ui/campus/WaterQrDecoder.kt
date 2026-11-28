package com.xingheyuzhuan.shiguangschedule.ui.campus

import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.LuminanceSource
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.ReaderException
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader

internal fun decodeWaterQrPixels(
    width: Int,
    height: Int,
    pixels: IntArray,
    matches: (String) -> Boolean
): String? {
    val whole = RGBLuminanceSource(width, height, pixels)
    var otherQr: String? = null
    fun tryDecode(source: LuminanceSource): String? {
        val result = readQr(source) ?: return null
        if (matches(result)) return result
        otherQr = result
        return null
    }

    tryDecode(whole)?.let { return it }
    val shortest = minOf(width, height)
    if (shortest < 480) return otherQr

    for (tileSize in listOf(shortest / 3, shortest / 8)) {
        for (top in scanOffsets(height, tileSize)) {
            for (left in scanOffsets(width, tileSize)) {
                val tile = whole.crop(left, top, tileSize, tileSize)
                tryDecode(tile)?.let { return it }
            }
        }
    }
    return otherQr
}

private fun scanOffsets(length: Int, size: Int): List<Int> {
    val last = length - size
    val step = (size / 6).coerceAtLeast(1)
    return ((0..last step step).toList() + last).distinct()
}

private fun readQr(source: LuminanceSource): String? {
    val hints = mapOf(DecodeHintType.TRY_HARDER to true)
    fun decode(image: LuminanceSource): String? = try {
        QRCodeReader().decode(BinaryBitmap(HybridBinarizer(image)), hints).text
    } catch (_: ReaderException) {
        null
    }

    decode(source)?.let { return it }
    decode(source.invert())?.let { return it }

    val raw = source.matrix
    val low = raw.minOf { it.toInt() and 0xff }
    val high = raw.maxOf { it.toInt() and 0xff }
    if (high <= low) return null
    val adjusted = ByteArray(raw.size) { index ->
        (((raw[index].toInt() and 0xff) - low) * 255 / (high - low)).toByte()
    }
    decode(PlanarYUVLuminanceSource(adjusted, source.width, source.height, 0, 0, source.width, source.height, false))
        ?.let { return it }

    val scale = ((600 + minOf(source.width, source.height) - 1) / minOf(source.width, source.height))
        .coerceIn(1, 4)
    if (scale == 1) return null
    val width = source.width * scale
    val height = source.height * scale
    val normalized = ByteArray(width * height) { index ->
        val y = index / width
        val x = index % width
        val row = y / scale
        val column = x / scale
        val nextRow = minOf(row + 1, source.height - 1)
        val nextColumn = minOf(column + 1, source.width - 1)
        val weightX = x % scale
        val weightY = y % scale
        val top = (raw[row * source.width + column].toInt() and 0xff) * (scale - weightX) +
            (raw[row * source.width + nextColumn].toInt() and 0xff) * weightX
        val bottom = (raw[nextRow * source.width + column].toInt() and 0xff) * (scale - weightX) +
            (raw[nextRow * source.width + nextColumn].toInt() and 0xff) * weightX
        val value = (top * (scale - weightY) + bottom * weightY) / (scale * scale)
        ((value - low) * 255 / (high - low)).toByte()
    }
    val enhanced = PlanarYUVLuminanceSource(normalized, width, height, 0, 0, width, height, false)
    return decode(enhanced)
}
