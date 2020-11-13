package com.android.example.cameraxbasic.utils

import androidx.camera.core.ImageProxy

@kotlin.ExperimentalUnsignedTypes
class ImageBand(im: ImageProxy, plane: Int) {
	val height = im.height.div(if (plane==0) 1 else 2)
	val width = im.width.div(if (plane==0) 1 else 2)
	private val strideRow = im.planes[plane].rowStride
	private val stridePixel = im.planes[plane].pixelStride
	private val bytes = im.planes[plane].buffer.let {
		it.rewind()
		val data = ByteArray(it.remaining())
		it.get(data)
		data
	}
	operator fun get(i: Int, j: Int): UByte =
		bytes[i.times(strideRow)+j.times(stridePixel)].toUByte()
	fun grayscale() =
		bytes.filterIndexed { i,_ -> i.rem(stridePixel)==0 }.map {
			val v = it.toUInt(); (255u.shl(24)+v.shl(16)+v.shl(8)+v).toInt()
		}.toIntArray()
	fun grayscale(pixmap:(UByte)->UByte) =
		bytes.filterIndexed { i, _ -> i.rem(stridePixel)==0 }.map {
			val v = pixmap(it.toUByte()).toUInt()
			(255u.shl(24)+v.shl(16)+v.shl(8)+v).toInt()
		}.toIntArray()
	fun binary(thres:UByte) =
		bytes.filterIndexed { i, _ -> i.rem(stridePixel)==0 }.map {
			val v = if (it.toUInt()<thres) 255u else 0u
			(255u.shl(24)+v.shl(16)+v.shl(8)+v).toInt()
		}.toIntArray()
}
