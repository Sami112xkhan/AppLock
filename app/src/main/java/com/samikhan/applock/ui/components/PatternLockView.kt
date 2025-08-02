package com.samikhan.applock.ui.components

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlin.math.abs
import kotlin.math.sqrt

data class PatternPoint(val row: Int, val col: Int) {
    val index: Int get() = row * 3 + col
}

class PatternLockView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    
    private val points = mutableListOf<PatternPoint>()
    private val selectedPoints = mutableListOf<PatternPoint>()
    private var currentDragPoint: android.graphics.PointF? = null
    private var isDrawing = false
    
    private var onPatternComplete: ((String) -> Unit)? = null
    private var isEnabled = true
    
    // Colors
    private var primaryColor = Color.BLUE
    private var surfaceColor = Color.WHITE
    private var outlineColor = Color.GRAY
    private var onPrimaryColor = Color.WHITE
    
    // Dynamic dimensions
    private var gridSize = 0f
    private var cellSize = 0f
    private var pointRadius = 0f
    private var lineWidth = 0f
    private var touchRadius = 0f
    
    init {
        // Initialize the 9 points
        for (row in 0..2) {
            for (col in 0..2) {
                points.add(PatternPoint(row, col))
            }
        }
    }
    
    fun setOnPatternCompleteListener(listener: (String) -> Unit) {
        onPatternComplete = listener
    }
    
    fun setPatternEnabled(enabled: Boolean) {
        isEnabled = enabled
        invalidate()
    }
    
    fun setColors(
        primary: Int,
        surface: Int,
        outline: Int,
        onPrimary: Int
    ) {
        primaryColor = primary
        surfaceColor = surface
        outlineColor = outline
        onPrimaryColor = onPrimary
        invalidate()
    }
    
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        
        // Calculate dynamic dimensions based on available space
        gridSize = minOf(w, h).toFloat()
        cellSize = gridSize / 3f
        pointRadius = cellSize * 0.15f // 15% of cell size
        lineWidth = cellSize * 0.08f   // 8% of cell size
        touchRadius = cellSize * 0.4f  // 40% of cell size for touch detection
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled) return false
        
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val point = getPointAtLocation(event.x, event.y)
                if (point != null) {
                    selectedPoints.clear()
                    selectedPoints.add(point)
                    isDrawing = true
                    currentDragPoint = android.graphics.PointF(event.x, event.y)
                    invalidate()
                }
                return true
            }
            
            MotionEvent.ACTION_MOVE -> {
                if (isDrawing) {
                    currentDragPoint = android.graphics.PointF(event.x, event.y)
                    val point = getPointAtLocation(event.x, event.y)
                    if (point != null && !selectedPoints.contains(point)) {
                        selectedPoints.add(point)
                    }
                    invalidate()
                }
                return true
            }
            
            MotionEvent.ACTION_UP -> {
                if (isDrawing && selectedPoints.isNotEmpty()) {
                    if (selectedPoints.size >= 4) {
                        val patternString = selectedPoints.joinToString(",") { it.index.toString() }
                        android.util.Log.d("PatternLockView", "Pattern completed: '$patternString' with ${selectedPoints.size} points")
                        onPatternComplete?.invoke(patternString)
                    } else {
                        android.util.Log.d("PatternLockView", "Pattern too short: ${selectedPoints.size} points")
                    }
                    selectedPoints.clear()
                    currentDragPoint = null
                    isDrawing = false
                    invalidate()
                }
                return true
            }
            
            MotionEvent.ACTION_CANCEL -> {
                selectedPoints.clear()
                currentDragPoint = null
                isDrawing = false
                invalidate()
                return true
            }
        }
        
        return super.onTouchEvent(event)
    }
    
    private fun getPointAtLocation(x: Float, y: Float): PatternPoint? {
        // Calculate grid offset to center the pattern
        val offsetX = (width - gridSize) / 2f
        val offsetY = (height - gridSize) / 2f
        
        // Adjust coordinates for grid offset
        val adjustedX = x - offsetX
        val adjustedY = y - offsetY
        
        // Check if touch is within grid bounds
        if (adjustedX < 0 || adjustedX > gridSize || adjustedY < 0 || adjustedY > gridSize) {
            return null
        }
        
        for (point in points) {
            val centerX = (point.col * cellSize) + (cellSize / 2) + offsetX
            val centerY = (point.row * cellSize) + (cellSize / 2) + offsetY
            val distance = sqrt((x - centerX) * (x - centerX) + (y - centerY) * (y - centerY))
            
            if (distance <= touchRadius) {
                return point
            }
        }
        return null
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // Calculate grid offset to center the pattern
        val offsetX = (width - gridSize) / 2f
        val offsetY = (height - gridSize) / 2f
        
        // Draw connecting lines
        if (selectedPoints.size > 1) {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = lineWidth
            paint.color = primaryColor
            paint.strokeCap = Paint.Cap.ROUND
            
            path.reset()
            val firstPoint = selectedPoints.first()
            val startX = (firstPoint.col * cellSize) + (cellSize / 2) + offsetX
            val startY = (firstPoint.row * cellSize) + (cellSize / 2) + offsetY
            path.moveTo(startX, startY)
            
            for (i in 1 until selectedPoints.size) {
                val point = selectedPoints[i]
                val x = (point.col * cellSize) + (cellSize / 2) + offsetX
                val y = (point.row * cellSize) + (cellSize / 2) + offsetY
                path.lineTo(x, y)
            }
            
            // Draw line to current drag point
            if (currentDragPoint != null && selectedPoints.isNotEmpty()) {
                path.lineTo(currentDragPoint!!.x, currentDragPoint!!.y)
            }
            
            canvas.drawPath(path, paint)
        }
        
        // Draw points
        for (point in points) {
            val centerX = (point.col * cellSize) + (cellSize / 2) + offsetX
            val centerY = (point.row * cellSize) + (cellSize / 2) + offsetY
            
            val isSelected = selectedPoints.contains(point)
            val isHighlighted = selectedPoints.lastOrNull() == point
            
            // Draw point background
            paint.style = Paint.Style.FILL
            paint.color = when {
                isHighlighted -> primaryColor
                isSelected -> primaryColor
                else -> surfaceColor
            }
            canvas.drawCircle(centerX, centerY, pointRadius, paint)
            
            // Draw point border
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 2f
            paint.color = when {
                isHighlighted -> onPrimaryColor
                isSelected -> primaryColor
                else -> outlineColor
            }
            canvas.drawCircle(centerX, centerY, pointRadius, paint)
            
            // Draw center dot for highlighted points
            if (isHighlighted) {
                paint.style = Paint.Style.FILL
                paint.color = onPrimaryColor
                canvas.drawCircle(centerX, centerY, pointRadius * 0.3f, paint)
            }
        }
    }
    
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // Calculate desired size based on screen density and available space
        val density = resources.displayMetrics.density
        val desiredSize = (340 * density).toInt() // 340dp base size
        
        val width = resolveSize(desiredSize, widthMeasureSpec)
        val height = resolveSize(desiredSize, heightMeasureSpec)
        setMeasuredDimension(width, height)
    }
}

@Composable
fun PatternLockView(
    modifier: Modifier = Modifier,
    pattern: String = "",
    onPatternComplete: (String) -> Unit,
    isEnabled: Boolean = true,
    showPattern: Boolean = false
) {
    val context = LocalContext.current
    val primaryColor = MaterialTheme.colorScheme.primary.toArgb()
    val surfaceColor = MaterialTheme.colorScheme.surface.toArgb()
    val outlineColor = MaterialTheme.colorScheme.outline.toArgb()
    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary.toArgb()
    
    val animatedAlpha by animateFloatAsState(
        targetValue = if (isEnabled) 1f else 0.5f,
        animationSpec = tween(300),
        label = "pattern_alpha"
    )
    
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AndroidView(
            factory = { context ->
                PatternLockView(context).apply {
                    setOnPatternCompleteListener(onPatternComplete)
                    setPatternEnabled(isEnabled)
                    setColors(primaryColor, surfaceColor, outlineColor, onPrimaryColor)
                }
            },
            modifier = Modifier
                .size(340.dp)
                .alpha(animatedAlpha)
                .padding(16.dp),
            update = { view ->
                view.setPatternEnabled(isEnabled)
                view.setColors(primaryColor, surfaceColor, outlineColor, onPrimaryColor)
            }
        )
        
        if (showPattern && pattern.isNotEmpty()) {
            Text(
                text = "Pattern: ${pattern.split(",").size} points",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
fun PatternPreview(
    pattern: String,
    modifier: Modifier = Modifier
) {
    if (pattern.isEmpty()) return
    
    val context = LocalContext.current
    val primaryColor = MaterialTheme.colorScheme.primary.toArgb()
    val surfaceColor = MaterialTheme.colorScheme.surface.toArgb()
    val outlineColor = MaterialTheme.colorScheme.outline.toArgb()
    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary.toArgb()
    
    AndroidView(
        factory = { context ->
            PatternLockView(context).apply {
                setColors(primaryColor, surfaceColor, outlineColor, onPrimaryColor)
                // For preview, we'll show the pattern but not allow interaction
                setPatternEnabled(false)
            }
        },
        modifier = modifier.size(120.dp),
        update = { view ->
            view.setColors(primaryColor, surfaceColor, outlineColor, onPrimaryColor)
        }
    )
} 