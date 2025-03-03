package cs501.hw5.p4

import android.content.Context
import android.content.res.Resources
import android.graphics.Rect
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

data class GameState(
    var ballX: Float = 45f,
    var ballY: Float = 45f,
    var ballVelocityX: Float = 0f,
    var ballVelocityY: Float = 0f,
    var lastUpdate: Long = 0
)

class MainActivity : ComponentActivity(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private var gyroscope: Sensor? = null

    private var walls = mutableListOf<Rect>()
    private val ballRadius = 20f

    private var screenWidth = Resources.getSystem().displayMetrics.widthPixels
    private var screenHeight = Resources.getSystem().displayMetrics.heightPixels

    private val gameState = mutableStateOf(GameState())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())


        setContent {
            GameScreen(gameState, walls, ballRadius)
        }
        initializeWalls()
    }

    private fun initializeWalls() {
        val wallThickness = 25
        val width = 100
        walls.add(Rect(0, 0, screenWidth, wallThickness)) // top
        walls.add(Rect(0, 0, wallThickness, screenHeight)) // left
        walls.add(Rect(screenWidth - wallThickness, 0, screenWidth, screenHeight)) // right
        walls.add(Rect(0, screenHeight - wallThickness, screenWidth, screenHeight)) // bottom

        val lines = (screenWidth / width)
        for (i in 0 until lines step 2) {
            walls.add(
                Rect(
                    width * (i + 1),
                    0,
                    width * (i + 1) + wallThickness,
                    screenHeight - width
                )
            )
            if (i + 1 < lines) {
                walls.add(Rect(width * i, width, width * i + wallThickness, screenHeight))
            }
        }
    }

    override fun onResume() {
        super.onResume()
        gyroscope?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_GYROSCOPE) {
            val currentTime = event.timestamp
            val lastUpdate = gameState.value.lastUpdate

            if (lastUpdate != 0L) {
                val deltaTime = (currentTime - lastUpdate) / 1_000_000_000.0f
                val a = 1000f

                val accelerationX = event.values[1] * a
                val accelerationY = event.values[0] * a

                var velocityX = gameState.value.ballVelocityX
                var velocityY = gameState.value.ballVelocityY

                velocityX += accelerationX * deltaTime
                velocityY += accelerationY * deltaTime

                val newX = gameState.value.ballX + velocityX * deltaTime
                val newY = gameState.value.ballY + velocityY * deltaTime

                val ballRect = Rect(
                    (newX - ballRadius).toInt(),
                    (newY - ballRadius).toInt(),
                    (newX + ballRadius).toInt(),
                    (newY + ballRadius).toInt()
                )

                if (walls.any { Rect.intersects(it, ballRect) }) {
                    gameState.value.ballVelocityX = 0f
                    gameState.value.ballVelocityY = 0f
                } else {
                    gameState.value = gameState.value.copy(
                        ballX = newX,
                        ballY = newY,
                        ballVelocityX = velocityX,
                        ballVelocityY = velocityY
                    )
                }
            }

            gameState.value = gameState.value.copy(lastUpdate = currentTime)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}

@Composable
fun GameScreen(gameState: State<GameState>, walls: List<Rect>, ballRadius: Float) {
    Column(Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.windowInsetsPadding(WindowInsets(0, 0, 0, 0))) {
            drawCircle(Color.Red, ballRadius, Offset(gameState.value.ballX, gameState.value.ballY))
            walls.forEach {
                drawRect(
                    Color.Blue,
                    Offset(it.left.toFloat(), it.top.toFloat()),
                    Size(it.width().toFloat(), it.height().toFloat())
                )
            }

        }
    }

}