package com.android.example.cameraxbasic.utils

import android.graphics.Rect
import android.util.Log
import android.util.Size
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import kotlin.collections.ArrayList

/**
 * Our custom image analysis class.
 *
 * <p>All we need to do is override the function `analyze` with our desired operations. Here,
 * we compute the average luminosity of the image by looking at the Y plane of the YUV frame.
 */

@kotlin.ExperimentalUnsignedTypes
abstract class AnalysisListener {
	open fun fps(fps:Float) = Unit
	open fun image(image:ImageBand, par:HashMap<String,Comparable<*>>) = Unit
	open fun ball(rect:Rect) = Unit
	open fun size(s:Size, rot:Int) = Unit
	open fun enter() = Unit
	open fun leave(image:ImageBand, rot:Int) = Unit
}

@kotlin.ExperimentalUnsignedTypes
class ImageAnalyzer: ImageAnalysis.Analyzer {

	private val listeners = ArrayList<AnalysisListener>()
	private var countImages = 0L
	var framesPerSecond: Float = 0f

	private var stateBall = 0L // positive frame count when in view, negative when not
	private var timePrevious: Long = 0 // timestamp of previous image frame
	private var imPrevious: ImageBand? = null // previous image analyzed

	fun onFrameAnalyzed(listener: AnalysisListener) = listeners.add(listener)

	private fun findBall(im1: ImageBand, im0: ImageBand, maxBlue:UByte, maxBlueExtend:UByte): Rect? {
		val rangeYellow = 20..500 // yellowish pixel appearing count range for ball detect
		val rangeArea = 25..9000 // acceptable ball extent range
		val aspectMax = 4.0f // max bb aspect ratio (also inverse)
		var yellowNew = 0
		val w = im1.width; val h = im1.height
		// bounding-box edges
		var ilo = h; var ihi = 0
		var jlo = w; var jhi = 0
		// find (inner) bounding-box for newly appearing yellow pixels
		for (i in 0 until h) {
			for (j in 0 until w) {
				val b = (im1[i, j] < maxBlue) && (im0[i,j]>=maxBlue)
				if (b) {
					yellowNew++
					if (i < ilo) ilo = i; if (i > ihi) ihi = i
					if (j < jlo) jlo = j; if (j > jhi) jhi = j
				}
			}
		}
		if (yellowNew in rangeYellow) {
			// Log.d("CameraXBasic","findBall: initial-bb  [$ilo,$jlo][$ihi,$jhi]")
			// extend bounding-box to also include yellow pixels overlapping prev image
			fun countYellow(i1:Int, i2:Int, j1:Int, j2:Int):Int {
				var count = 0
				for (i in i1..i2) {
					for (j in j1..j2) {
						val b = (im1[i, j] < maxBlueExtend)
						if (b) count++
					}
				}
				return count
			}
			do { // extend (outer) bb iteratively
				var changes = 0
				val cilo = countYellow(ilo,ilo,jlo,jhi)
				if (cilo>0 && ilo>0) { ilo--; changes++ }
				val cihi = countYellow(ihi,ihi,jlo,jhi)
				if (cihi>0 && ihi<h-1) { ihi++; changes++ }
				val cjlo = countYellow(ilo,ihi,jlo,jlo)
				if (cjlo>0 && jlo>0) { jlo--; changes++ }
				val cjhi = countYellow(ilo,ihi,jhi,jhi)
				if (cjhi>0 && jhi<w-1) { jhi++; changes++ }

			} while (changes>0)
			// Log.d("CameraXBasic","findBall: extended-bb [$ilo,$jlo][$ihi,$jhi]")
			val bb = Rect(jlo,ilo,jhi,ihi)
			val area = bb.width()*bb.height()
			val aspect = bb.width()/bb.height().toFloat()
			Log.d("CameraXBasic","findBall: yellowNew=$yellowNew area=$area apsect=$aspect")
			if (area in rangeArea && aspect in (1/aspectMax)..aspectMax) return bb
		}
		return null // not found
	}


	override fun analyze(image: ImageProxy) {

		countImages++
		// Log.d("CameraXBasic","analyze ${image.imageInfo.rotationDegrees} ")

		// Compute FPS with exponetial smoothing
		val timeCurrent = System.currentTimeMillis()
		if (timeCurrent-timePrevious in 1..100) {
			framesPerSecond =  { s: Float ->
				(1-s)*framesPerSecond + s*1000f/(timeCurrent-timePrevious) }(1f/countImages)
			listeners.forEach { it.fps(framesPerSecond) }
		}
		// Analysis parameters
		val maxBlue: UByte = 90u
		val maxBlueExtend: UByte = 100u
		val countIdle = 10L // frames without ball detection before leave assumed
		val rotCurr = image.imageInfo.rotationDegrees

		// after ball sequence, provide a gray-level image for court analysis
		if (stateBall == -countIdle) {
			val imGray = ImageBand(image, 0) // Y from YUV
			listeners.forEach { it.leave(imGray, rotCurr) }
		}
		// image for ball tracking analysis
		val imCurr = ImageBand(image, 1) // U (Cb) from YUV
		image.close() // Since camerax-alpha08, we must close image in analyze()!

		// report image size at camera startup
		if (imPrevious==null) {
			val s = Size(imCurr.width,imCurr.height)
			listeners.forEach { it.size(s,rotCurr) }
		}
		// detect and report ball in image
		val rectBall = if (imPrevious!=null)
			findBall(imCurr,imPrevious!!,maxBlue, maxBlueExtend) else null
		if (rectBall!=null) {
			if (stateBall < -countIdle) {
				stateBall = 1 // first in a potential sequence
				listeners.forEach { it.enter() }
			}
			else stateBall++
		}
		else { // no ball in current image
			if (stateBall>=0) stateBall = -1 // first after a sequence
			else stateBall--
		}
		// update image listeners only every few images
		if (stateBall==1L || (stateBall>1L && countImages.rem(5)==0L)) {
			val par = hashMapOf("rotation" to rotCurr, "maxBlue" to maxBlue, "maxBlueExtend" to maxBlueExtend)
			listeners.forEach { it.image(imCurr, par) }
		}
		// update ball tracker listeners
		rectBall?.let {
			Log.d("CameraXBasic","Ball: ${rectBall.toShortString()}")
			listeners.forEach { it.ball(rectBall) }
		}
		// Prepare for next analysis
		imPrevious = imCurr
		timePrevious = timeCurrent
	}
}
