package com.ariful.mobile.timeline

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.TypedArray
import android.graphics.*
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.AsyncTask
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import kotlin.math.abs

var density: Float = 1f
fun dpFromPx(px: Float): Float {
    return px / (density)
}

fun pxFromDp(dp: Float): Float {
    return dp * (density)
}

class TimelineView : View {
    enum class CutMode {
        TRIM,
        CUT
    }

    interface Callback {
        fun onSeek(position: Float, seekMillis: Long)
        fun onSeekStart(position: Float, seekMillis: Long)
        fun onStopSeek(position: Float, seekMillis: Long)
        fun onLeftProgress(leftPos: Float, seekMillis: Long)
        fun onRightProgress(rightPos: Float, seekMillis: Long)
    }

    var callback: Callback? = null
    private lateinit var leftThumbPaint: Paint
    private lateinit var progressPaint: Paint
    private lateinit var rightThumbPaint: Paint
    private lateinit var arrowPaint: Paint
    private lateinit var disabledAreaPaint: Paint
    private lateinit var selectedBoxPaint: Paint

    private val frames = mutableListOf<Bitmap>()
    private var retriever: MediaMetadataRetriever? = null
    private var progressLeft = 0f
    private var progressRight = 1f
    private var currentProgress = 0f;
    private var thumbWidth = 20f
    private var frameDimentionRatio = 1f
    private var videoLength = 0L
    private var frameOffset = 0
    private val bottomPadding = 30
    private val leftPadding = 0
    private val rightPadding: Int = 0
    private val topPadding = 0
    private var seeking = false
    private var totalDuration: Long = 0
    private var cutMode: CutMode = CutMode.TRIM

    var leftPosition: Long
        get() = ((progressLeft * totalDuration).toLong())
        set(value) {}

    var rightPosition: Long
        get() = ((progressRight * totalDuration).toLong())
        set(value) {}



    fun setTotalDuration(duration: Long) {
        this.totalDuration = duration
        invalidate()
    }
    fun updateProgress(leftPos: Float,rightPos: Float){
        progressLeft = leftPos.coerceAtLeast(0f)
        progressRight = rightPos.coerceAtMost(1f)
    }

    fun setCutMode(cutMode: CutMode) {
        this.cutMode = cutMode
        invalidate()
    }

    constructor(context: Context) : super(context) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(context)
        initAttr(attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(context)
        initAttr(attrs)
    }

    constructor(
        context: Context,
        attrs: AttributeSet,
        defStyleAttr: Int,
        defStyleRes: Int,
    ) : super(context, attrs, defStyleAttr, defStyleRes) {
        init(context)
        initAttr(attrs)
    }

    fun setCurrentProgressValue(float: Float) {
        if (seeking) return
        currentProgress = float
        invalidate()
    }

    fun init(context: Context) {
        density = context.resources.displayMetrics.density
        thumbWidth = pxFromDp(4f)

        leftThumbPaint = Paint()
        leftThumbPaint.strokeWidth = thumbWidth
        leftThumbPaint.style = Paint.Style.FILL_AND_STROKE
        leftThumbPaint.color = Color.RED

        rightThumbPaint = Paint()
        rightThumbPaint.strokeWidth = thumbWidth
        rightThumbPaint.style = Paint.Style.FILL_AND_STROKE
        rightThumbPaint.color = Color.RED


        disabledAreaPaint = Paint()
        disabledAreaPaint.style = Paint.Style.FILL_AND_STROKE
        disabledAreaPaint.color = Color.BLACK
        disabledAreaPaint.alpha = 90

        progressPaint = Paint()
        progressPaint.strokeWidth = pxFromDp(2f)
        progressPaint.style = Paint.Style.FILL_AND_STROKE
        progressPaint.color = Color.WHITE



        selectedBoxPaint = Paint()
        selectedBoxPaint.strokeWidth = pxFromDp(2f)
        selectedBoxPaint.style = Paint.Style.STROKE
        selectedBoxPaint.color = Color.RED

        arrowPaint = Paint()
        arrowPaint.strokeWidth = pxFromDp(2f)
        arrowPaint.style = Paint.Style.FILL_AND_STROKE
        arrowPaint.color = Color.WHITE
    }

    private fun initAttr(attrs: AttributeSet) {
        val ta: TypedArray =
            context.obtainStyledAttributes(
                attrs,
                R.styleable.TimelineView,
                0,
                0
            )
        try {
            leftThumbPaint.color = ta.getColor(
                R.styleable.TimelineView_left_thumb_color,
                context.resources.getColor(R.color.purple_500)
            )
            progressPaint.color = ta.getColor(
                R.styleable.TimelineView_progress_thumb_color,
                context.resources.getColor(R.color.purple_500)
            )
            rightThumbPaint.color = ta.getColor(
                R.styleable.TimelineView_right_thumb_color,
                context.resources.getColor(R.color.purple_500)
            )
            leftThumbPaint.strokeWidth =
                pxFromDp(ta.getDimension(R.styleable.TimelineView_thumb_width, 4f))
            rightThumbPaint.strokeWidth =
                pxFromDp(ta.getDimension(R.styleable.TimelineView_thumb_width, 4f))

            frameDimentionRatio =
                ta.getFloat(R.styleable.TimelineView_thumb_dimension, 1f)
        } finally {
            ta.recycle()
        }
    }

    var pressedBtn = -1

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val width = measuredWidth
        val x = event.x
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                when {
                    abs(x - width * progressLeft) < thumbWidth * 2 -> {
                        pressedBtn = 0
                    }
                    abs(x - (width * currentProgress)) < thumbWidth * 2 -> {
                        seeking = true
                        callback?.onSeekStart(
                            currentProgress,
                            (currentProgress * totalDuration).toLong()
                        )
                        pressedBtn = 1
                    }
                    abs(x - width * progressRight) < thumbWidth * 2 -> {
                        pressedBtn = 2
                        return true
                    }

                    else -> {
                        pressedBtn = -1
                    }
                }
                return pressedBtn >= 0
            }
            MotionEvent.ACTION_MOVE -> if (pressedBtn >= 0) {
                val newProgress = x / measuredWidth
                if (pressedBtn == 0 && newProgress < progressRight) {
                    progressLeft = newProgress
                    callback?.onLeftProgress(progressLeft, (progressLeft * totalDuration).toLong())

                } else if (pressedBtn == 2 && newProgress > progressLeft) {
                    progressRight = newProgress
                    callback?.onRightProgress(
                        progressRight,
                        (progressRight * totalDuration).toLong()
                    )
                } else if (pressedBtn == 1) {
                    currentProgress = newProgress
                    if (seeking) {
                        callback?.onSeek(
                            currentProgress,
                            (currentProgress * totalDuration).toLong()
                        )
                    }
                }
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_POINTER_UP -> {
                pressedBtn = -1
                if (seeking) {
                    seeking = false
                    callback?.onStopSeek(
                        currentProgress,
                        (currentProgress * totalDuration).toLong()
                    )
                }
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawFrames(canvas)
        drawThumbs(canvas)
    }

    var calcWidth: Int
        get() = (measuredWidth - leftPadding - rightPadding)
        set(value) {}

    var calcHeight: Int
        get() = (measuredHeight - topPadding - bottomPadding)
        set(value) {}


    private fun drawFrames(canvas: Canvas) {
        var offset = 0
        frames.forEach { bitmap ->
            val x: Int = offset * frameWidth
            canvas.drawBitmap(bitmap, x.toFloat(), 0f, null)
            offset++;
        }
    }

    private fun drawThumbs(canvas: Canvas) {
        val leftPoint = calcWidth * progressLeft
        val rightPoint = calcWidth * progressRight
        val progressPoint = calcWidth * currentProgress

        drawSelectionArea(canvas, leftPoint, rightPoint, progressPoint)
        drawArrow(canvas)
        drawProgress(canvas, progressPoint)
    }

    private fun drawProgress(canvas: Canvas, progressPoint: Float) {
        canvas.drawLine(
            progressPoint,
            topPadding.toFloat(),
            progressPoint,
            measuredHeight.toFloat(),
            progressPaint
        )
        canvas.drawCircle(progressPoint, measuredHeight - 15f, 12f, progressPaint)
    }

    private fun drawSelectionArea(
        canvas: Canvas,
        leftPoint: Float,
        rightPoint: Float,
        progressPoint: Float,
    ) {

        drawVerticalLine(canvas, leftPoint, selectedBoxPaint)
        drawVerticalLine(canvas, rightPoint, selectedBoxPaint)

        if (cutMode == CutMode.TRIM) {
            drawHorizontalLine(canvas, leftPoint, rightPoint, selectedBoxPaint, 2)
            drawHorizontalLine(canvas, leftPoint, rightPoint, selectedBoxPaint, calcHeight)

            //draw black overlay
            canvas.drawRect(leftPadding.toFloat(),
                topPadding.toFloat(),
                leftPoint,
                calcHeight.toFloat(),
                disabledAreaPaint)
            canvas.drawRect(rightPoint,
                topPadding.toFloat(),
                calcWidth.toFloat(),
                calcHeight.toFloat(),
                disabledAreaPaint)

        } else {

            drawHorizontalLine(canvas, leftPadding.toFloat(), leftPoint, selectedBoxPaint, 2)
            drawHorizontalLine(canvas,
                leftPadding.toFloat(),
                leftPoint,
                selectedBoxPaint,
                calcHeight)
            drawHorizontalLine(canvas, rightPoint, measuredWidth.toFloat(), selectedBoxPaint, 2)
            drawHorizontalLine(canvas,
                rightPoint,
                measuredWidth.toFloat(),
                selectedBoxPaint,
                calcHeight)

            //draw black overlay
            canvas.drawRect(leftPoint,
                topPadding.toFloat(),
                rightPoint,
                calcHeight.toFloat(),
                disabledAreaPaint)
        }
    }

    private fun drawHorizontalLine(
        canvas: Canvas,
        start: Float,
        end: Float,
        paint: Paint,
        topOffset: Int = 0,
    ) {
        canvas.drawLine(
            start,
            topPadding.toFloat() + topOffset,
            end,
            topPadding.toFloat() + topOffset,
            paint
        )
    }

    private fun drawVerticalLine(canvas: Canvas, xPoint: Float, paint: Paint) {
        canvas.drawLine(
            xPoint,
            topPadding.toFloat(),
            xPoint,
            calcHeight.toFloat(),
            paint
        )
    }

    val path = Path()
    private fun drawArrow(canvas: Canvas) {
        path.reset()
        path.fillType = Path.FillType.EVEN_ODD
        path.moveTo(
            (calcWidth * progressLeft) - thumbWidth / 2,
            (calcHeight / 2f) - thumbWidth
        )
        path.lineTo(
            (calcWidth * progressLeft) - thumbWidth / 2,
            (calcHeight / 2f) + thumbWidth
        )
        path.lineTo((calcWidth * progressLeft) + thumbWidth / 2, (calcHeight / 2f))
        path.close()
        canvas.drawPath(path, arrowPaint)
        path.reset()
        path.moveTo(
            (calcWidth * progressRight) + thumbWidth / 2,
            (calcHeight / 2f) - thumbWidth
        )
        path.lineTo(
            (calcWidth * progressRight) + thumbWidth / 2,
            (calcHeight / 2f) + thumbWidth
        )
        path.lineTo((calcWidth * progressRight) - thumbWidth / 2, (calcHeight / 2f))
        path.close()
        canvas.drawPath(path, arrowPaint)

    }

    private var frameWidth = 100
    private var frameHeight = 100
    fun processBitmap(frameNum: Int): Bitmap? {
        try {
            Log.d(TAG, "processBitmap: $frameOffset ${frameOffset * frameNum * 1000}")
            var bitmap = retriever?.getFrameAtTime(
                (frameOffset * frameNum * 1000).toLong(),
                MediaMetadataRetriever.OPTION_CLOSEST_SYNC
            )
            if (bitmap != null) {
                val result = Bitmap.createBitmap(frameWidth, frameHeight, bitmap.config)
                val canvas = Canvas(result)
                val scaleX = frameWidth.toFloat() / bitmap.width.toFloat()
                val scaleY = frameHeight.toFloat() / bitmap.height.toFloat()
                val w = (bitmap.width * scaleX).toInt()
                val h = (bitmap.height * scaleY).toInt()
                val srcRect = Rect(0, 0, bitmap.width, bitmap.height)
                val destRect = Rect(
                    0,
                    0, w, h
                )
                canvas.drawBitmap(bitmap, srcRect, destRect, null)
                bitmap.recycle()
                bitmap = result
            }
            return bitmap
        } catch (e: Exception) {
        }
        return null
    }

    var frameToLoad = 10;
    private fun loadFrame(framePosition: Int) {
        if (framePosition == 0) {
            if (calcHeight <= 0) {
                loadFrame(-1);
                return
            }
            frameHeight = calcHeight
            Log.d(TAG, "loadFrame: height $calcHeight")
            frameWidth = (frameHeight * frameDimentionRatio).toInt()
            frameToLoad = (calcWidth / frameWidth) + 1
            Log.d(TAG, "loadFrame: frame to load $frameToLoad")
        }

        val task = @SuppressLint("StaticFieldLeak")
        object : AsyncTask<Int, Void, Bitmap?>() {
            var frameNum: Int = 2
            override fun doInBackground(vararg p0: Int?): Bitmap? {
                frameNum = p0[0] ?: -2
                if (frameNum == -1) {
                    Thread.sleep(50)
                }
                if (frameNum >= 0)
                    return processBitmap(frameNum)
                return null
            }

            override fun onPostExecute(result: Bitmap?) {
                super.onPostExecute(result)
                Log.d(TAG, "onPostExecute: $frameNum")
                if (result != null)
                    frames.add(result)
                if (frameNum < frameToLoad) {
                    loadFrame(framePosition + 1)
                }
                invalidate()
            }
        }
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, framePosition, null, null);
    }

    fun setVideoUri(path: Uri) {
        try {
            release()
            retriever = retriever ?: MediaMetadataRetriever()
            retriever?.setDataSource(context, path)
            loadFrame(0)
            val duration: String? =
                retriever?.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            videoLength = duration?.toLong() ?: 0
            frameOffset = ((videoLength / frameToLoad).toInt())
            invalidate()
        } catch (ex: Exception) {
            ex.printStackTrace()
            Toast.makeText(context, "Invalid Video", Toast.LENGTH_SHORT).show()
        }
    }

    private fun release() {
        retriever?.release()
        retriever = null
        frames.forEach {
            it.recycle()
        }
        frames.clear()
    }

    companion object {
        private const val TAG = "TimelineView"
    }
}