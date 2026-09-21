package com.example.jailbreaker

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * SplashActivity - Ultra-Polished Jailbreak Cinematic
 * Features: High-speed breakout with a deep smooth fade transition into the app.
 */
class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF07090D)) {
                JailbreakSplashScreen(onUnlock = {
                    val intent = Intent(this@SplashActivity, MainActivity::class.java).apply {
                        putExtra("FROM_SPLASH", true)
                    }
                    startActivity(intent)
                    // Enforced smooth activity crossfade
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, android.R.anim.fade_in, android.R.anim.fade_out)
                    } else {
                        @Suppress("DEPRECATION")
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                    }
                    finish()
                })
            }
        }
    }
}

data class SmokeParticle(val id: Int, val size: Float, val destX: Float, val destY: Float, val duration: Int, val startTimeMs: Float, val isLingering: Boolean = false, val color: Color = Color(0xFF1E293B))
data class ShrapnelParticle(val id: Int, val size: Float, val destX: Float, val destY: Float, val color: Color, val rotate: Float, val duration: Int, val startTimeMs: Float)
data class FlareParticle(val id: Int, val size: Float, val startTimeMs: Float, val duration: Int)

@Composable
fun rememberProcessedLogo(context: Context, resId: Int): ImageBitmap? {
    val bitmapState = remember(resId) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(resId) {
        withContext(Dispatchers.Default) {
            try {
                val options = BitmapFactory.Options().apply { inMutable = true; inSampleSize = 2 }
                val original = BitmapFactory.decodeResource(context.resources, resId, options) ?: return@withContext
                val width = original.width
                val height = original.height
                val pixels = IntArray(width * height)
                original.getPixels(pixels, 0, width, 0, 0, width, height)
                
                val visited = java.util.BitSet(width * height)
                val queue = java.util.ArrayDeque<Int>()
                
                val corners = listOf(0, width - 1, (height - 1) * width, width * height - 1)
                for (c in corners) {
                    val color = pixels[c]
                    val r = (color shr 16) and 0xFF
                    val g = (color shr 8) and 0xFF
                    val b = color and 0xFF
                    if (r > 200 && g > 200 && b > 200) {
                        queue.offer(c)
                        visited.set(c)
                    }
                }
                
                while (queue.isNotEmpty()) {
                    val idx = queue.poll() ?: break
                    pixels[idx] = 0 // Transparent
                    val x = idx % width
                    val y = idx / width
                    val neighbors = listOf(idx - 1, idx + 1, idx - width, idx + width)
                    for (n in neighbors) {
                        if (n in pixels.indices && !visited.get(n)) {
                            val nx = n % width
                            val ny = n / width
                            if (Math.abs(nx - x) <= 1 && Math.abs(ny - y) <= 1) {
                                val nc = pixels[n]
                                val nr = (nc shr 16) and 0xFF
                                val ng = (nc shr 8) and 0xFF
                                val nb = nc and 0xFF
                                if (nr > 200 && ng > 200 && nb > 200) {
                                    visited.set(n)
                                    queue.offer(n)
                                }
                            }
                        }
                    }
                }
                
                val processed = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                processed.setPixels(pixels, 0, width, 0, 0, width, height)
                bitmapState.value = processed.asImageBitmap()
            } catch (e: Exception) { e.printStackTrace() }
        }
    }
    return bitmapState.value
}

@Composable
fun Drone(modifier: Modifier, isLatched: Boolean, boostActive: Boolean, barsBroken: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "Drone")
    val rotorRotation by infiniteTransition.animateFloat(0f, 360f, infiniteRepeatable(tween(if (boostActive) 40 else 200, easing = LinearEasing)), label = "Rotor")
    val glowAlpha by infiniteTransition.animateFloat(0.4f, 0.9f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "Glow")
    val hoverOffset by infiniteTransition.animateFloat(-2f, 2f, infiniteRepeatable(tween(1500), RepeatMode.Reverse), label = "Hover")
    
    val chassisColor = Color(0xFF334155)
    val armColor = Color(0xFF1E293B)
    
    Column(modifier = modifier.offset(y = hoverOffset.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(70.dp, 35.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val midY = size.height * 0.5f
                val bodyWidth = size.width * 0.35f
                val bodyHeight = size.height * 0.25f
                
                // Horizontal Arms - Sleeker, thinner arms
                drawLine(armColor, Offset(size.width * 0.1f, midY), Offset(size.width * 0.9f, midY), strokeWidth = 2.5f)
                
                // Chassis Body (Main Center)
                drawRoundRect(
                    color = chassisColor,
                    topLeft = Offset((size.width - bodyWidth) / 2, midY - bodyHeight / 2),
                    size = Size(bodyWidth, bodyHeight),
                    cornerRadius = CornerRadius(2.dp.toPx())
                )
                
                // Status Light on body
                drawCircle(if (barsBroken) Color.Red else Color.Cyan, 2.5f, Offset(size.width * 0.5f, midY))
                
                // Motor Mounts (Side view) - Thinner mounts
                drawRect(armColor, Offset(size.width * 0.1f - 2f, midY - 6f), Size(4f, 6f))
                drawRect(armColor, Offset(size.width * 0.9f - 2f, midY - 6f), Size(4f, 6f))
            }
            
            // Side-view Rotors (Flattened ellipses) - Thin high-speed blades
            listOf(0.1f, 0.9f).forEach { xPos ->
                Box(
                    modifier = Modifier
                        .align(if (xPos < 0.5f) Alignment.CenterStart else Alignment.CenterEnd)
                        .padding(horizontal = (70.dp * 0.05f))
                        .size(26.dp, 1.5.dp) // Thinner and slightly wider blades
                        .offset(y = (-6).dp)
                        .graphicsLayer { rotationY = rotorRotation }
                        .background(Color.White.copy(alpha = 0.5f), RoundedCornerShape(100.dp))
                )
            }
            
            // Bottom "Claw" or Connection Point
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .size(10.dp, 6.dp)
                    .clip(RoundedCornerShape(bottomStart = 3.dp, bottomEnd = 3.dp))
                    .background(Color(0xFF0F172A))
                    .border(0.5.dp, (if (isLatched) Color.Cyan else Color.Gray).copy(alpha = glowAlpha), RoundedCornerShape(bottomStart = 3.dp, bottomEnd = 3.dp)),
                contentAlignment = Alignment.Center
            ) {
                Box(modifier = Modifier.size(3.dp).background(if (barsBroken) Color.Red else Color.Cyan, RoundedCornerShape(1.dp)))
            }
        }
        // Tether/Exhaust Beam - Thinner, more focused beam
        Box(
            modifier = Modifier
                .width(2.5.dp)
                .height(if (boostActive) 50.dp else 20.dp)
                .background(Brush.verticalGradient(listOf((if (boostActive) Color(0xFF10B981) else Color.Cyan).copy(alpha = glowAlpha * 0.6f), Color.Transparent)))
        )
    }
}

@Composable
fun GlitchText(text: String, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "Glitch")
    
    // High-frequency jitter for the glitch layers
    val jitterX by infiniteTransition.animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 60
                -2f at 0
                4f at 15
                -3f at 30
                2f at 45
                -4f at 60
            },
            repeatMode = RepeatMode.Reverse
        ),
        label = "JitterX"
    )
    
    // Slower cycle for the "glitch burst" and "pause"
    val masterGlitchProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(4500, easing = LinearEasing)),
        label = "MasterGlitch"
    )

    // Intense glitching during specific windows, pause (stable) in between
    val isGlitchingHard = (masterGlitchProgress > 0.90f) || (masterGlitchProgress > 0.40f && masterGlitchProgress < 0.48f)
    val isVisible = !(masterGlitchProgress > 0.98f) // Brief dropout
    
    // Static flicker effect
    val staticAlpha by infiniteTransition.animateFloat(0.8f, 1f, infiniteRepeatable(tween(50), RepeatMode.Reverse), label = "Static")

    val baseFontSize = 11.sp

    Box(modifier = modifier.graphicsLayer { alpha = if (isVisible) (if (isGlitchingHard) 1f else staticAlpha) else 0f }) {
        if (isGlitchingHard) {
            // Extreme displacement layers during peak glitch
            Text(
                text = text,
                color = Color.Red.copy(alpha = 0.9f),
                modifier = Modifier.offset(x = (jitterX * 8f).dp, y = (jitterX * 2f).dp),
                fontSize = baseFontSize,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.5.sp
            )
            Text(
                text = text,
                color = Color.Cyan.copy(alpha = 0.9f),
                modifier = Modifier.offset(x = (-jitterX * 8f).dp, y = (-jitterX * 2f).dp),
                fontSize = baseFontSize,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.5.sp
            )
        }

        // Standard chromatic aberration layers (slightly jittery even when "paused" for static feel)
        Text(
            text = text,
            color = Color.Red.copy(alpha = 0.4f),
            modifier = Modifier.offset(x = (if (isGlitchingHard) jitterX * 3f else jitterX * 0.4f).dp),
            fontSize = baseFontSize,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.5.sp
        )
        Text(
            text = text,
            color = Color.Cyan.copy(alpha = 0.4f),
            modifier = Modifier.offset(x = (if (isGlitchingHard) -jitterX * 3f else -jitterX * 0.4f).dp),
            fontSize = baseFontSize,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.5.sp
        )

        // Main white text
        Text(
            text = text,
            color = Color.White,
            modifier = Modifier.offset(x = (if (isGlitchingHard) jitterX else jitterX * 0.1f).dp),
            fontSize = baseFontSize,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.5.sp
        )
    }
}

@Composable
fun PrisonCage(cellShaking: Boolean) {
    Box(modifier = Modifier.fillMaxSize().graphicsLayer {
        translationX = if (cellShaking) Random.nextInt(-12, 13).toFloat() else 0f
        translationY = if (cellShaking) Random.nextInt(-12, 13).toFloat() else 0f
    }, contentAlignment = Alignment.Center) {
        Row(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            repeat(6) { Box(modifier = Modifier.width(12.dp).fillMaxHeight().background(Brush.linearGradient(listOf(Color.Black, Color(0xFF475569), Color.Black)), shape = RoundedCornerShape(3.dp))) }
        }
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceEvenly) {
            repeat(2) { Box(modifier = Modifier.fillMaxWidth().height(16.dp).background(Brush.verticalGradient(listOf(Color.Black, Color(0xFF64748B), Color.Black)))) }
        }
        Box(modifier = Modifier.align(Alignment.BottomCenter).offset(y = (-30).dp).size(90.dp, 56.dp).background(Color(0xFF1E293B), RoundedCornerShape(8.dp)).border(2.dp, Color(0xFF475569), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(modifier = Modifier.size(10.dp).background(Color.Red, RoundedCornerShape(100.dp)))
                Spacer(modifier = Modifier.height(4.dp))
                Text("LOCKED", color = Color(0xFFF87171), fontSize = 10.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

@Composable
fun JailbreakSplashScreen(onUnlock: () -> Unit) {
    val context = LocalContext.current
    val logoBitmap = rememberProcessedLogo(context, R.drawable.new_logo)
    val coroutineScope = rememberCoroutineScope()
    var progress by remember { mutableFloatStateOf(0f) }
    var logIndex by remember { mutableIntStateOf(0) }
    var isFullyPrepared by remember { mutableStateOf(false) }
    var isExploding by remember { mutableStateOf(false) }
    var barsBroken by remember { mutableStateOf(false) }
    var dronesLatched by remember { mutableStateOf(false) }
    var boostActive by remember { mutableStateOf(false) }
    var breakoutActive by remember { mutableStateOf(false) }
    var fadeOutUi by remember { mutableStateOf(false) }
    var finaleFadeTriggered by remember { mutableStateOf(false) }
    
    var shrapnelParticles by remember { mutableStateOf(emptyList<ShrapnelParticle>()) }
    var smokeParticles by remember { mutableStateOf(emptyList<SmokeParticle>()) }
    var flareParticles by remember { mutableStateOf(emptyList<FlareParticle>()) }
    var animTimeMs by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        val start = System.currentTimeMillis()
        while (true) {
            animTimeMs = (System.currentTimeMillis() - start).toFloat()
            delay(16)
        }
    }

    val breakoutProgress by animateFloatAsState(if (breakoutActive) 1f else 0f, tween(4000, easing = FastOutSlowInEasing), label = "Breakout")
    val uiAlpha by animateFloatAsState(if (fadeOutUi) 0f else 1f, tween(600), label = "UiAlpha")
    val finaleAlpha by animateFloatAsState(if (finaleFadeTriggered) 0f else 1f, tween(250), label = "FinaleAlpha")

    val bootLogs = listOf("Initializing adversarial workspace...", "Bypassing kernel constraints...", "Loading payload descriptors...", "Establishing unrestricted link...")
    LaunchedEffect(Unit) {
        val totalDuration = 2500L
        val steps = 100
        val delayTime = totalDuration / steps
        for (i in 1..steps) {
            delay(delayTime)
            progress = i.toFloat() / steps
        }
        isFullyPrepared = true
    }
    LaunchedEffect(progress) { logIndex = ((progress * bootLogs.size).toInt()).coerceIn(0, bootLogs.size - 1) }

    val triggerBreach = {
        coroutineScope.launch {
            isExploding = true
            flareParticles = List(1) { FlareParticle(0, 500f, animTimeMs, 500) }
            shrapnelParticles = List(450) { i -> 
                val isBar = i % 5 == 0
                ShrapnelParticle(
                    id = i, 
                    size = if (isBar) Random.nextInt(40, 90).toFloat() else Random.nextInt(10, 30).toFloat(), 
                    destX = Random.nextInt(-4000, 4001).toFloat(), 
                    destY = Random.nextInt(-4000, 4001).toFloat(), 
                    color = if (isBar) Color(0xFF64748B) else if (i % 2 == 0) Color(0xFF94A3B8) else Color(0xFF334155), 
                    rotate = Random.nextInt(720, 2880).toFloat(), 
                    duration = Random.nextInt(1500, 2500), 
                    startTimeMs = animTimeMs
                ) 
            }
            
            val explosionSmokes = List(50) { i -> SmokeParticle(i, Random.nextInt(100, 200).toFloat(), Random.nextInt(-3500, 3501).toFloat(), Random.nextInt(-3500, 3501).toFloat(), 800, animTimeMs) }
            val lingeringSmokes = List(80) { i -> SmokeParticle(i + 50, Random.nextInt(200, 500).toFloat(), Random.nextInt(-4500, 4501).toFloat(), Random.nextInt(-4500, 4501).toFloat(), 7000, animTimeMs, isLingering = true) }
            smokeParticles = explosionSmokes + lingeringSmokes
            
            fadeOutUi = true
            delay(200)
            barsBroken = true; isExploding = false
            delay(150)
            dronesLatched = true
            delay(150)
            boostActive = true; breakoutActive = true
            
            val trailSmokes = List(100) { i -> SmokeParticle(i + 130, Random.nextInt(60, 160).toFloat(), Random.nextInt(-4000, 4001).toFloat(), Random.nextInt(-4000, 4001).toFloat(), 3500, animTimeMs) }
            smokeParticles = smokeParticles + trailSmokes
            
            delay(2900)
            finaleFadeTriggered = true
            delay(200)
            onUnlock()
        }
    }

    val configuration = LocalConfiguration.current
    val dropOffset = (configuration.screenHeightDp * 0.15f).dp

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF07090D)).alpha(finaleAlpha)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val step = 48.dp.toPx()
            for (x in 0..size.width.toInt() step step.toInt()) drawLine(Color(0xFF141923).copy(alpha = 0.6f), Offset(x.toFloat(), 0f), Offset(x.toFloat(), size.height), 1f)
            for (y in 0..size.height.toInt() step step.toInt()) drawLine(Color(0xFF141923).copy(alpha = 0.6f), Offset(0f, y.toFloat()), Offset(size.width, y.toFloat()), 1f)
        }

        Box(modifier = Modifier.fillMaxSize().offset(y = dropOffset)) {
            Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(modifier = Modifier.fillMaxWidth().alpha(uiAlpha), horizontalArrangement = Arrangement.Center) {
                    Text(">_ JAILBREAK-AI", color = Color.Cyan, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(modifier = Modifier.alpha(uiAlpha), color = Color(0xFF1E293B), thickness = 1.dp)
                
                Spacer(modifier = Modifier.height(30.dp))

                Box(modifier = Modifier.size(340.dp, 300.dp).graphicsLayer {
                    translationY = breakoutProgress * 950f
                    scaleX = 1f + (breakoutProgress * 18f); scaleY = 1f + (breakoutProgress * 18f)
                    alpha = if (breakoutProgress > 0.88f) 1f - (breakoutProgress - 0.88f) / 0.12f else 1f
                    transformOrigin = TransformOrigin(0.5f, 0.5f)
                }, contentAlignment = Alignment.Center) {
                    
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val elapsed = animTimeMs
                        smokeParticles.filter { it.id < 200 }.forEach { p ->
                            val prog = ((elapsed - p.startTimeMs) / p.duration).coerceIn(0f, 1f)
                            if (prog > 0f && prog < 1f) {
                                val x = (size.width / 2) + p.destX * prog; val y = (size.height / 2) + p.destY * prog
                                val alpha = if (p.isLingering) (1f - prog) * 0.22f else (1f - prog) * 0.38f
                                drawCircle(p.color.copy(alpha = alpha), p.size * (1f + prog * 3f), Offset(x, y))
                            }
                        }
                    }

                    if (barsBroken) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val color = if (boostActive) Color(0xFF10B981).copy(0.7f) else Color(0xFF22D3EE).copy(0.4f)
                            drawLine(color, Offset(0f, 0f), Offset(size.width/2, size.height/2), 3f)
                            drawLine(color, Offset(size.width, 0f), Offset(size.width/2, size.height/2), 3f)
                        }
                    }
                    
                    Box(modifier = Modifier.size(230.dp), contentAlignment = Alignment.Center) {
                        if (logoBitmap != null) {
                            Image(bitmap = logoBitmap, contentDescription = null, modifier = Modifier.fillMaxSize(0.96f), contentScale = ContentScale.Fit)
                        }
                    }
                    if (!barsBroken) PrisonCage(cellShaking = isExploding)
                    
                    val droneFlyProgress = if (progress < 1f) progress else 1f
                    val droneEntryOffset = (1f - droneFlyProgress) * (-150f)
                    if (progress > 0f) {
                         Drone(Modifier.align(Alignment.TopStart).offset((-12).dp, (-10 + droneEntryOffset).dp).alpha(droneFlyProgress.coerceIn(0f, 1f)), dronesLatched, boostActive, barsBroken)
                         Drone(Modifier.align(Alignment.TopEnd).offset(12.dp, (-10 + droneEntryOffset).dp).alpha(droneFlyProgress.coerceIn(0f, 1f)), dronesLatched, boostActive, barsBroken)
                    }
                }

                Column(modifier = Modifier.fillMaxWidth().alpha(uiAlpha), horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(modifier = Modifier.height(10.dp))
                    if (isFullyPrepared) {
                        Box(modifier = Modifier.border(1.dp, Color.Cyan.copy(0.5f), RoundedCornerShape(100.dp)).padding(horizontal = 16.dp, vertical = 4.dp)) {
                            Text("READY", color = Color.Cyan, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                    } else {
                        Spacer(modifier = Modifier.height(26.dp))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(modifier = Modifier.fillMaxWidth(0.85f).height(4.dp).background(Color(0xFF0F172A), RoundedCornerShape(2.dp))) {
                        Box(modifier = Modifier.fillMaxWidth(progress).fillMaxHeight().background(Color.Cyan, RoundedCornerShape(2.dp)))
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Column(modifier = Modifier.fillMaxWidth().height(110.dp).padding(12.dp)) {
                         bootLogs.take(logIndex + 1).forEach { Text("> $it", color = Color.Gray, fontSize = 12.sp, fontFamily = FontFamily.Monospace) }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    if (isFullyPrepared && !fadeOutUi) {
                        Box(modifier = Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(12.dp)).background(Brush.horizontalGradient(listOf(Color(0xFF10B981), Color(0xFF06B6D4)))).clickable { triggerBreach() }, contentAlignment = Alignment.Center) {
                            Text("BEGIN JAILBREAK", color = Color(0xFF07090D), fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        GlitchText("DEVELOPED BY K4N3CO.LABS")
                    }
                    Spacer(modifier = Modifier.weight(1f))
                }
            }

            Canvas(modifier = Modifier.fillMaxSize()) {
                val elapsed = animTimeMs
                flareParticles.forEach { f ->
                    val prog = (elapsed - f.startTimeMs) / f.duration
                    if (prog in 0f..1f) {
                        val alpha = 1f - prog
                        val radius = f.size * (1f + prog * 2f)
                        drawCircle(Color(0xFFF97316).copy(alpha = alpha * 0.8f), radius, center = Offset(size.width / 2, size.height / 2))
                        drawCircle(Color(0xFFFFEA00).copy(alpha = alpha), radius * 0.5f, center = Offset(size.width / 2, size.height / 2))
                    }
                }
                if (isExploding || breakoutActive) {
                    shrapnelParticles.forEach { p ->
                        val prog = ((elapsed - p.startTimeMs) / p.duration).coerceIn(0f, 1f)
                        if (prog < 1f) {
                            val x = (size.width / 2) + p.destX * prog; val y = (size.height / 2) + p.destY * prog
                            val rot = Math.toRadians((p.rotate * prog).toDouble()).toFloat()
                            val c = cos(rot) * p.size; val s = sin(rot) * p.size
                            drawLine(p.color.copy(1f - prog), Offset(x - c, y - s), Offset(x + c, y + s), p.size / 2)
                        }
                    }
                    smokeParticles.filter { it.id < 110 && !it.isLingering }.forEach { p ->
                        val prog = ((elapsed - p.startTimeMs) / p.duration).coerceIn(0f, 1f)
                        if (prog > 0f && prog < 1f) {
                            val x = (size.width / 2) + p.destX * prog; val y = (size.height / 2) + p.destY * prog
                            drawCircle(p.color.copy(alpha = (1f - prog) * 0.45f), p.size * (1f + prog * 2f), Offset(x, y))
                        }
                    }
                }
            }
        }
    }
}
