package com.galaxyjoy.cpuinfo.feat.gpubench

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import com.galaxyjoy.cpuinfo.feat.temp.TemperatureProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.inject.Inject
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * Drives the U15 GPU workload. Unlike every other benchmark in this app (pure coroutine loops on
 * [kotlinx.coroutines.Dispatchers.Default]/IO), a GPU stress test needs a real GL context bound to
 * an attached `GLSurfaceView`, so the workload runs inside [GLSurfaceView.Renderer.onDrawFrame] on
 * the GL thread, not a coroutine. [state] is a [MutableStateFlow] (safe to write from any thread)
 * instead of the suspend-callback shape [com.galaxyjoy.cpuinfo.feat.rambench.RamBenchmarkRunner]
 * uses, since GL-thread code can't easily hop back into a caller's coroutine.
 */
class GpuBenchmarkRunner @Inject constructor(
    private val temperatureProvider: TemperatureProvider,
) {

    sealed interface State {
        data object Idle : State
        data class Running(val warmingUp: Boolean) : State
        data class Finished(val result: GpuBenchmark.Result) : State
        data class Aborted(val reason: GpuBenchmark.AbortReason) : State
    }

    private val _state = MutableStateFlow<State>(State.Idle)
    val state = _state.asStateFlow()

    @Volatile private var running = false

    fun start() {
        running = true
        _state.value = State.Running(warmingUp = true)
    }

    /** Called both by the user's Stop button and by the screen's lifecycle observer when the app
     * backgrounds mid-run — `GLSurfaceView.onPause()` stops [Renderer.onDrawFrame] from firing at
     * all, so a paused run can never reach [GpuBenchmark.MEASURE_DURATION_MS] on its own; walking
     * away leaves it stuck in [State.Running] forever without this direct abort. */
    fun requestStop() {
        if (!running) return
        running = false
        _state.value = State.Aborted(GpuBenchmark.AbortReason.INTERRUPTED)
    }

    fun reset() {
        running = false
        _state.value = State.Idle
    }

    fun newRenderer(): GLSurfaceView.Renderer = Renderer()

    private inner class Renderer : GLSurfaceView.Renderer {
        private var program = 0
        private var positionHandle = 0
        private var resolutionHandle = 0
        private lateinit var vertexBuffer: FloatBuffer

        private var width = 1
        private var height = 1
        private var startNanos = 0L
        private var warmupDone = false
        private var measureStartNanos = 0L
        private var lastTempCheckNanos = 0L
        private var frameCount = 0L

        override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
            GLES20.glClearColor(0f, 0f, 0f, 1f)
            program = buildProgram()
            positionHandle = GLES20.glGetAttribLocation(program, "aPosition")
            resolutionHandle = GLES20.glGetUniformLocation(program, "uResolution")
            vertexBuffer = ByteBuffer.allocateDirect(FULLSCREEN_QUAD.size * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .apply { put(FULLSCREEN_QUAD); position(0) }

            startNanos = System.nanoTime()
            warmupDone = false
            frameCount = 0
        }

        override fun onSurfaceChanged(gl: GL10?, w: Int, h: Int) {
            width = w
            height = h
            GLES20.glViewport(0, 0, w, h)
        }

        override fun onDrawFrame(gl: GL10?) {
            if (!running) return

            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
            GLES20.glUseProgram(program)
            GLES20.glUniform2f(resolutionHandle, width.toFloat(), height.toFloat())
            GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer)
            GLES20.glEnableVertexAttribArray(positionHandle)
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

            val now = System.nanoTime()
            if (!warmupDone) {
                if (now - startNanos >= GpuBenchmark.WARMUP_DURATION_MS * 1_000_000L) {
                    warmupDone = true
                    measureStartNanos = now
                    lastTempCheckNanos = now
                    _state.value = State.Running(warmingUp = false)
                }
                return
            }

            frameCount++
            if (now - lastTempCheckNanos >= TEMP_CHECK_INTERVAL_NANOS) {
                lastTempCheckNanos = now
                if (GpuBenchmark.shouldAbortForSafety(temperatureProvider.getBatteryTemperature())) {
                    running = false
                    _state.value = State.Aborted(GpuBenchmark.AbortReason.OVERHEAT)
                    return
                }
            }

            val elapsed = now - measureStartNanos
            if (elapsed >= GpuBenchmark.MEASURE_DURATION_MS * 1_000_000L) {
                running = false
                _state.value = State.Finished(
                    GpuBenchmark.Result(
                        avgFps = GpuBenchmark.fps(frameCount, elapsed),
                        frameCount = frameCount,
                        durationMs = elapsed / 1_000_000L,
                    ),
                )
            }
        }

        private fun buildProgram(): Int {
            val vertexShader = compileShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER)
            val fragmentShader = compileShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER)
            return GLES20.glCreateProgram().also { p ->
                GLES20.glAttachShader(p, vertexShader)
                GLES20.glAttachShader(p, fragmentShader)
                GLES20.glLinkProgram(p)
            }
        }

        private fun compileShader(type: Int, source: String): Int =
            GLES20.glCreateShader(type).also { shader ->
                GLES20.glShaderSource(shader, source)
                GLES20.glCompileShader(shader)
            }
    }

    private companion object {
        const val TEMP_CHECK_INTERVAL_NANOS = 500_000_000L

        val FULLSCREEN_QUAD = floatArrayOf(
            -1f, -1f,
            1f, -1f,
            -1f, 1f,
            1f, 1f,
        )

        const val VERTEX_SHADER = """
            attribute vec2 aPosition;
            void main() {
                gl_Position = vec4(aPosition, 0.0, 1.0);
            }
        """

        /** Fixed 64-iteration trig loop per pixel — a real, GPU-vendor-agnostic ALU workload
         * (Mali/Adreno/PowerVR/Xclipse all execute the same instructions), heavy enough to pull
         * average FPS below the display's refresh rate on typical mobile GPUs so it actually
         * differentiates devices, unlike a workload light enough to just vsync-cap everywhere. */
        const val FRAGMENT_SHADER = """
            precision mediump float;
            uniform vec2 uResolution;
            void main() {
                vec2 uv = gl_FragCoord.xy / uResolution;
                float v = 0.0;
                for (int i = 0; i < 64; i++) {
                    float fi = float(i);
                    v += sin(uv.x * fi + uv.y) * cos(uv.y * fi + uv.x);
                }
                gl_FragColor = vec4(vec3(0.5 + 0.5 * sin(v)), 1.0);
            }
        """
    }
}
