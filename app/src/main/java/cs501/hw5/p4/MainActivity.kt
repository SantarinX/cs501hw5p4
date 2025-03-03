package cs501.hw5.p4

import android.content.Context
import android.content.res.Resources
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import cs501.hw5.p4.ui.theme.P4Theme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var velocityX = 0f
    private var velocityY = 0f
    private val walls = mutableListOf<Rect>()
    private val ballRadius = 20f

    private var screenWidth = Resources.getSystem().displayMetrics.widthPixels.toFloat()
    private var screenHeight = Resources.getSystem().displayMetrics.heightPixels.toFloat()
    
    private val gameState = mutableStateOf(GameState())
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        enableEdgeToEdge()

        setContent {
            P4Theme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MazeGame(gameState, Modifier
                        .fillMaxSize()
                        .padding(innerPadding), ballRadius, walls)
                }
            }
        }

        initializeWalls()
    }

    private fun initializeWalls() {
        val wallThickness = 20f
        val width = 100f
        walls.add(Rect(0f, 0f, screenWidth, wallThickness))//top
        walls.add(Rect(0f, 0f, wallThickness, screenHeight))//left
        walls.add(Rect(screenWidth - wallThickness, 0f, screenWidth, screenHeight))//right
        walls.add(Rect(0f, screenHeight - wallThickness, screenWidth, screenHeight))//bottom

        val lines = (screenWidth / width).toInt()
        for (i in 0 until lines step 2) {
            walls.add(Rect(width * (i + 1), 0f, width * (i + 1) + wallThickness, screenHeight - width))


            if (i + 1 < lines) {
                walls.add(Rect(width * i, width, width * i + wallThickness, screenHeight))
            }
        }

    }

    override fun onResume() {
        super.onResume()
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            velocityX -= event.values[0] * 0.1f
            velocityY += event.values[1] * 0.1f

            var newX = gameState.value.ballX + velocityX
            var newY = gameState.value.ballY + velocityY

            val ballRect = Rect(
                newX - ballRadius,
                newY - ballRadius,
                newX + ballRadius,
                newY + ballRadius
            )

            for (wall in walls) {
                if (intersects(ballRect, wall)) {
                    val overlapX = minOf(ballRect.right - wall.left, wall.right - ballRect.left)
                    val overlapY = minOf(ballRect.bottom - wall.top, wall.bottom - ballRect.top)

                    if (overlapX < overlapY) {
                        val overlapLeft = ballRect.right - wall.left
                        val overlapRight = wall.right - ballRect.left

                        newX = if (overlapLeft < overlapRight) {
                            wall.left - ballRadius
                        } else {
                            wall.right + ballRadius
                        }
                        velocityX = -velocityX * 0.15f
                    } else {
                        newY = if (newY < wall.top) {
                            wall.top - ballRadius
                        } else {
                            wall.bottom + ballRadius
                        }
                        velocityY = -velocityY * 0.15f
                    }
                    break
                }
            }

            // 添加精确的边界约束
            newX = newX.coerceIn(ballRadius, screenWidth - ballRadius)
            newY = newY.coerceIn(ballRadius, screenHeight - ballRadius)

            gameState.value = gameState.value.copy(
                ballX = newX,
                ballY = newY
            )
        }
    }
    private fun intersects(ball: Rect, wall: Rect): Boolean {
        return ball.left <= wall.right &&
                ball.right >= wall.left &&
                ball.top <= wall.bottom &&
                ball.bottom >= wall.top
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}

@Composable
fun MazeGame(gameState: State<GameState>, modifier: Modifier = Modifier, ballRadius: Float, walls: List<Rect>) {
    LaunchedEffect(Unit) {
        while (true) {
            delay(10)
        }
    }

    Box(modifier = modifier.background(Color.Black)) {
        Canvas(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets(0, 0, 0, 0))) {
            drawCircle(Color.Red, ballRadius, Offset(gameState.value.ballX, gameState.value.ballY))

            walls.forEach { wall ->
                drawRect(Color.White, Offset(wall.left, wall.top), Size(wall.width, wall.height))
            }
        }
    }
}

data class GameState(
    val ballX: Float = 80f,
    val ballY: Float = 50f
)