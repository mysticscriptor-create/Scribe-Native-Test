package com.primaloptima.scribe.engine

import android.graphics.Canvas
import io.github.rosemoe.sora.graphics.InlayHintRenderParams
import io.github.rosemoe.sora.graphics.Paint
import io.github.rosemoe.sora.graphics.inlayHint.InlayHintRenderer
import io.github.rosemoe.sora.lang.styling.inlayHint.CharacterSide
import io.github.rosemoe.sora.lang.styling.inlayHint.InlayHint
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme

/**
 * Exact-metrics visual first-line indent inlay hint.
 *
 * Unlike TextInlayHint (which uses 0.75x font size, padding margins, and a rounded background pill),
 * this indent hint renderer measures exactly:
 *     paint.measureText(" ") * spaces
 * using the editor's primary font and 1.0x typeface metrics with zero padding or margin.
 *
 * When the user releases the slider and the visual hint is replaced with real space characters
 * (' ' * spaces), the advance is 100% mathematically and visually identical, completely eliminating
 * word-wrap reflow jumps or text warping during transitions.
 */
class IndentInlayHint(
    line: Int,
    column: Int = 0,
    val spaces: Int
) : InlayHint(line, column, TYPE_NAME, CharacterSide.LEFT) {
    companion object {
        const val TYPE_NAME = "scribe_first_line_indent"
    }
}

/**
 * Renderer for IndentInlayHint.
 * Produces transparent spacing with zero border/background and exact standard space glyph width.
 */
class IndentInlayHintRenderer : InlayHintRenderer() {
    companion object {
        val Instance = IndentInlayHintRenderer()
    }

    override val typeName: String
        get() = IndentInlayHint.TYPE_NAME

    override fun onMeasure(
        inlayHint: InlayHint,
        paint: Paint,
        params: InlayHintRenderParams
    ): Float {
        val spaces = (inlayHint as? IndentInlayHint)?.spaces ?: return 0f
        if (spaces <= 0) return 0f
        val spaceAdvance = paint.measureText(" ")
        return spaceAdvance * spaces
    }

    override fun onRender(
        inlayHint: InlayHint,
        canvas: Canvas,
        paint: Paint,
        params: InlayHintRenderParams,
        colorScheme: EditorColorScheme,
        measuredWidth: Float
    ) {
        // Transparent blank indent space: no rect, no border, no glyph drawing needed.
    }
}
