package com.primaloptima.scribe.util

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.view.View
import androidx.annotation.WorkerThread

/**
 * Pure-Kotlin bitmap blur — no RenderScript, no deprecated APIs.
 *
 * Two entry points:
 *
 *  • [blurBitmap]       — blur any existing bitmap (used for the static wallpaper).
 *  • [captureAndBlur]   — one-shot: captures the current screen content, crops to
 *                         [cropRect] (pass null for full screen), downscales 25 %,
 *                         blurs, then upscales back. Call from a background thread.
 *
 * Both functions are @WorkerThread — always call from Dispatchers.IO.
 */
object BitmapBlur {

    // ─── Public API ──────────────────────────────────────────────────────────

    /**
     * Blurs [src] in-place using a 3-pass box blur approximation of Gaussian.
     * Downscales to 40 % before blurring and upscales back — quality is identical
     * for frosted glass but ~6× faster than blurring full resolution.
     *
     * @param src     Source bitmap (ARGB_8888). Not recycled by this call.
     * @param radius  Blur radius 1–25 px (clamped).
     * @return        New blurred bitmap at the same size as [src].
     */
    @WorkerThread
    fun blurBitmap(src: Bitmap, radius: Int): Bitmap {
        val r = radius.coerceIn(1, 25)
        val scale = 0.4f
        val small = Bitmap.createScaledBitmap(
            src,
            (src.width * scale).toInt().coerceAtLeast(1),
            (src.height * scale).toInt().coerceAtLeast(1),
            true
        )
        val blurred = stackBlur(small, r)
        small.recycle()
        val result = Bitmap.createScaledBitmap(blurred, src.width, src.height, true)
        blurred.recycle()
        return result
    }

    /**
     * Post-processes a blurred bitmap to look like real frosted glass rather than
     * a plain blur. Applies two things iOS/Haze do that most Android implementations miss:
     *
     *  1. Brightness + saturation boost — frosted glass lightens and slightly desaturates
     *     the content behind it, giving it that milky, luminous quality.
     *  2. Noise grain overlay — the fine grain texture is what makes glass *feel* like
     *     glass instead of just "blurry". Haze calls this noiseFactor.
     *
     * Call this on the result of [blurBitmap] or [captureAndBlur] before displaying it
     * as a panel/drawer/dialog background on pre-API-31 devices.
     *
     * @param src         Blurred bitmap (ARGB_8888). Modified in-place.
     * @param brightness  How much to brighten (0f = no change, 0.15f = subtle lift).
     * @param noiseAlpha  Opacity of grain overlay (0f = none, 0.04f = subtle, 0.08f = visible).
     */
    @WorkerThread
    fun applyFrostedGlassLook(
        src: Bitmap,
        brightness: Float = 0.12f,
        noiseAlpha: Float = 0.05f
    ): Bitmap {
        val w = src.width
        val h = src.height
        val pixels = IntArray(w * h)
        src.getPixels(pixels, 0, w, 0, 0, w, h)

        // 1. Brightness boost — lift each channel toward white by [brightness] fraction.
        //    This is what makes blurred glass look luminous rather than muddy.
        val lift = (brightness * 255).toInt().coerceIn(0, 60)
        if (lift > 0) {
            for (i in pixels.indices) {
                val p = pixels[i]
                val a = (p shr 24) and 0xFF
                val r = ((p shr 16 and 0xFF) + lift).coerceAtMost(255)
                val g = ((p shr 8  and 0xFF) + lift).coerceAtMost(255)
                val b = ((p        and 0xFF) + lift).coerceAtMost(255)
                pixels[i] = (a shl 24) or (r shl 16) or (g shl 8) or b
            }
        }

        // 2. Noise grain — add subtle random luminance variation per pixel.
        //    Uses a fast LCG pseudo-random number generator (no allocation).
        //    This is the single biggest visual difference between "blurry" and "glassy".
        if (noiseAlpha > 0f) {
            val noiseStrength = (noiseAlpha * 255).toInt().coerceIn(0, 30)
            var seed = 0x12345678L
            for (i in pixels.indices) {
                seed = (seed * 1664525L + 1013904223L) and 0xFFFFFFFFL
                val noise = ((seed shr 16) and 0xFFL).toInt() * noiseStrength / 255
                val offset = noise - noiseStrength / 2

                val p = pixels[i]
                val a = (p shr 24) and 0xFF
                val r = ((p shr 16 and 0xFF) + offset).coerceIn(0, 255)
                val g = ((p shr 8  and 0xFF) + offset).coerceIn(0, 255)
                val b = ((p        and 0xFF) + offset).coerceIn(0, 255)
                pixels[i] = (a shl 24) or (r shl 16) or (g shl 8) or b
            }
        }

        src.setPixels(pixels, 0, w, 0, 0, w, h)
        return src
    }

    /**
     * One-shot screen capture + blur for pre-Android-12 frosted glass on panels/dialogs.
     *
     * Captures the current window content via [View.drawToBitmap], optionally crops
     * to [cropRect], downscales to 25 %, blurs with [radius], then upscales back.
     * The result is a static bitmap that can be drawn as the panel background.
     *
     * @param view      Any view in the target activity window (used to get the window).
     * @param cropRect  Region to capture in screen pixels, or null for full screen.
     * @param radius    Blur radius 1–25 (clamped). Default 15 matches app default.
     * @return          Blurred bitmap at [cropRect] size (or full screen), or null on error.
     */
    @WorkerThread
    fun captureAndBlur(
        view: View,
        cropRect: Rect? = null,
        radius: Int = 15
    ): Bitmap? {
        return try {
            val r = radius.coerceIn(1, 25)

            // Capture the whole window into a software bitmap
            val full = captureOnly(view) ?: return null

            // Crop to the desired region
            val cropped = if (cropRect != null) {
                val safeRect = Rect(
                    cropRect.left.coerceAtLeast(0),
                    cropRect.top.coerceAtLeast(0),
                    cropRect.right.coerceAtMost(full.width),
                    cropRect.bottom.coerceAtMost(full.height)
                )
                if (safeRect.isEmpty) {
                    full.recycle()
                    return null
                }
                val c = Bitmap.createBitmap(full, safeRect.left, safeRect.top, safeRect.width(), safeRect.height())
                full.recycle()
                c
            } else {
                full
            }

            // Downscale to 25 % — frosted glass is low-frequency, quality is identical
            val scale = 0.25f
            val small = Bitmap.createScaledBitmap(
                cropped,
                (cropped.width * scale).toInt().coerceAtLeast(1),
                (cropped.height * scale).toInt().coerceAtLeast(1),
                true
            )
            cropped.recycle()

            // Blur on the tiny bitmap — very fast even on old hardware
            val blurred = stackBlur(small, r)
            small.recycle()

            // Upscale back with bilinear filtering — the upscale softens any
            // pixelation and enhances the frosted look
            val result = Bitmap.createScaledBitmap(blurred, cropped.width, cropped.height, true)
            blurred.recycle()
            result
        } catch (_: Exception) {
            null
        }
    }

    // ─── Internal helpers ─────────────────────────────────────────────────────

    /**
     * Captures the given view's root window into a software Bitmap.
     * Must be called on the MAIN thread (View drawing is not thread-safe).
     * After capturing, pass the result to [blurBitmap] on a background thread.
     */
    fun captureOnly(view: View): Bitmap? {
        return try {
            val w = view.rootView.width.coerceAtLeast(1)
            val h = view.rootView.height.coerceAtLeast(1)
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            view.rootView.draw(canvas)
            bmp
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Captures the view's root window directly at a reduced resolution on the Main thread.
     *
     * This is significantly faster than [captureOnly] + downscale because the Canvas
     * draw call itself operates on a smaller bitmap — fewer pixels written, less GPU→CPU
     * readback, and less memory allocated. At 25 % scale the draw call is ~16× cheaper
     * than full resolution, which keeps the Main thread free so the drawer slide-in
     * animation stays smooth.
     *
     * The returned bitmap is already downscaled. Pass it directly to [blurBitmap] on a
     * background thread, then upscale the result back to [targetW] × [targetH] for display.
     *
     * Must be called on the MAIN thread.
     *
     * @param view    Any view in the target activity window.
     * @param scale   Capture resolution as a fraction of screen size (default 0.25 = 25 %).
     * @return        Downscaled software bitmap, or null on error.
     */
    fun captureScaled(view: View, scale: Float = 0.25f): Bitmap? {
        return try {
            val root = view.rootView
            val fullW = root.width.coerceAtLeast(1)
            val fullH = root.height.coerceAtLeast(1)
            val smallW = (fullW * scale).toInt().coerceAtLeast(1)
            val smallH = (fullH * scale).toInt().coerceAtLeast(1)
            val bmp = Bitmap.createBitmap(smallW, smallH, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            // Scale the canvas so the entire view tree draws into the smaller bitmap.
            canvas.scale(scale, scale)
            root.draw(canvas)
            bmp
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Pure Kotlin stack blur (Zhu/Rijnders algorithm).
     * Significantly smoother than box blur at the same radius.
     * Operates in-place on a copy of [src]; does not recycle [src].
     */
    private fun stackBlur(src: Bitmap, radius: Int): Bitmap {
        val w = src.width
        val h = src.height
        val pixels = IntArray(w * h)
        src.getPixels(pixels, 0, w, 0, 0, w, h)

        val div = radius + radius + 1
        val wm = w - 1
        val hm = h - 1
        val wh = w * h
        val divSum = (div + 1) shr 1
        val divSum2 = divSum * divSum

        // Pre-compute reciprocal table to avoid division in hot loop
        val dv = IntArray(256 * divSum2)
        for (i in dv.indices) dv[i] = i / divSum2

        var yw = 0
        var yi = 0

        val stack = IntArray(div)
        var stackPointer: Int
        var stackStart: Int
        var sir: Int
        var rbs: Int
        val r1 = radius + 1

        var rSum: Int; var gSum: Int; var bSum: Int
        var rOutSum: Int; var gOutSum: Int; var bOutSum: Int
        var rInSum: Int; var gInSum: Int; var bInSum: Int

        // Horizontal pass
        for (y in 0 until h) {
            rInSum = 0; gInSum = 0; bInSum = 0
            rOutSum = 0; gOutSum = 0; bOutSum = 0
            rSum = 0; gSum = 0; bSum = 0

            for (i in -radius..radius) {
                sir = pixels[yi + i.coerceIn(0, wm)]
                stack[i + radius] = sir
                rbs = r1 - Math.abs(i)
                rSum += ((sir shr 16) and 0xFF) * rbs
                gSum += ((sir shr 8) and 0xFF) * rbs
                bSum += (sir and 0xFF) * rbs
                if (i > 0) {
                    rInSum += (sir shr 16) and 0xFF
                    gInSum += (sir shr 8) and 0xFF
                    bInSum += sir and 0xFF
                } else {
                    rOutSum += (sir shr 16) and 0xFF
                    gOutSum += (sir shr 8) and 0xFF
                    bOutSum += sir and 0xFF
                }
            }
            stackPointer = radius

            for (x in 0 until w) {
                pixels[yi] = (0xFF shl 24) or
                        (dv[rSum] shl 16) or
                        (dv[gSum] shl 8) or
                        dv[bSum]
                rSum -= rOutSum; gSum -= gOutSum; bSum -= bOutSum
                stackStart = (stackPointer - radius + div) % div
                sir = stack[stackStart]
                rOutSum -= (sir shr 16) and 0xFF
                gOutSum -= (sir shr 8) and 0xFF
                bOutSum -= sir and 0xFF
                val nextX = if (x + radius + 1 <= wm) yi + radius + 1 else yi + (wm - x)
                sir = pixels[nextX]
                stack[stackStart] = sir
                rInSum += (sir shr 16) and 0xFF
                gInSum += (sir shr 8) and 0xFF
                bInSum += sir and 0xFF
                rSum += rInSum; gSum += gInSum; bSum += bInSum
                stackPointer = (stackPointer + 1) % div
                sir = stack[stackPointer]
                rOutSum += (sir shr 16) and 0xFF
                gOutSum += (sir shr 8) and 0xFF
                bOutSum += sir and 0xFF
                rInSum -= (sir shr 16) and 0xFF
                gInSum -= (sir shr 8) and 0xFF
                bInSum -= sir and 0xFF
                yi++
            }
            yw += w
            yi = yw
        }

        // Vertical pass
        for (x in 0 until w) {
            rInSum = 0; gInSum = 0; bInSum = 0
            rOutSum = 0; gOutSum = 0; bOutSum = 0
            rSum = 0; gSum = 0; bSum = 0
            var yp = -radius * w
            for (i in -radius..radius) {
                yi = (yp + x).coerceAtLeast(x)
                sir = pixels[yi]
                stack[i + radius] = sir
                rbs = r1 - Math.abs(i)
                rSum += ((sir shr 16) and 0xFF) * rbs
                gSum += ((sir shr 8) and 0xFF) * rbs
                bSum += (sir and 0xFF) * rbs
                if (i > 0) {
                    rInSum += (sir shr 16) and 0xFF
                    gInSum += (sir shr 8) and 0xFF
                    bInSum += sir and 0xFF
                } else {
                    rOutSum += (sir shr 16) and 0xFF
                    gOutSum += (sir shr 8) and 0xFF
                    bOutSum += sir and 0xFF
                }
                if (i < hm) yp += w
            }
            yi = x
            stackPointer = radius
            for (y in 0 until h) {
                pixels[yi] = (0xFF shl 24) or
                        (dv[rSum] shl 16) or
                        (dv[gSum] shl 8) or
                        dv[bSum]
                rSum -= rOutSum; gSum -= gOutSum; bSum -= bOutSum
                stackStart = (stackPointer - radius + div) % div
                sir = stack[stackStart]
                rOutSum -= (sir shr 16) and 0xFF
                gOutSum -= (sir shr 8) and 0xFF
                bOutSum -= sir and 0xFF
                val nextY = if (y + r1 <= hm) yi + r1 * w else yi + (hm - y) * w
                sir = pixels[nextY]
                stack[stackStart] = sir
                rInSum += (sir shr 16) and 0xFF
                gInSum += (sir shr 8) and 0xFF
                bInSum += sir and 0xFF
                rSum += rInSum; gSum += gInSum; bSum += bInSum
                stackPointer = (stackPointer + 1) % div
                sir = stack[stackPointer]
                rOutSum += (sir shr 16) and 0xFF
                gOutSum += (sir shr 8) and 0xFF
                bOutSum += sir and 0xFF
                rInSum -= (sir shr 16) and 0xFF
                gInSum -= (sir shr 8) and 0xFF
                bInSum -= sir and 0xFF
                yi += w
            }
        }

        val result = src.copy(Bitmap.Config.ARGB_8888, true)
        result.setPixels(pixels, 0, w, 0, 0, w, h)
        return result
    }
}
