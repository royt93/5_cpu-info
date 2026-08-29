/*
 * Copyright 2017 KG Soft
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.galaxyjoy.cpuinfo.feat.infor.gpu

import android.opengl.GLSurfaceView
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import com.galaxyjoy.cpuinfo.R
import com.galaxyjoy.cpuinfo.databinding.FrmGpuInfoBinding
import com.galaxyjoy.cpuinfo.domain.model.GpuData
import com.galaxyjoy.cpuinfo.feat.infor.base.BaseFrm
import com.galaxyjoy.cpuinfo.ui.theme.CpuInfoTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * Fragment which provides OpenGL info from custom [GLSurfaceView]
 *
 */
@AndroidEntryPoint
class FrmGpuInfo : BaseFrm<FrmGpuInfoBinding>(R.layout.frm_gpu_info) {

    private val viewModel: VMGpuInfo by viewModels()

    @Inject
    lateinit var graphicsDetailProvider: GraphicsDetailProvider

    private var glSurfaceView: GLSurfaceView? = null
    private val handler = Handler(Looper.getMainLooper())

    private val glRenderer = object : GLSurfaceView.Renderer {
        override fun onSurfaceCreated(gl: GL10, config: EGLConfig) {
            viewModel.onGlInfoReceived(
                gl.glGetString(GL10.GL_VENDOR),
                gl.glGetString(GL10.GL_RENDERER),
                gl.glGetString(GL10.GL_EXTENSIONS)
            )
            handler.post { glSurfaceView?.visibility = View.GONE }
        }

        override fun onSurfaceChanged(gl: GL10, width: Int, height: Int) {
        }

        override fun onDrawFrame(gl: GL10) {
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = super.onCreateView(inflater, container, savedInstanceState)
        glSurfaceView = GLSurfaceView(requireActivity()).apply {
            setEGLConfigChooser(8, 8, 8, 8, 16, 0)
            setRenderer(glRenderer)
        }
        binding.mainContainer.addView(glSurfaceView)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val controller = GpuInfoEpoxyController(requireContext())
        binding.rv.adapter = controller.adapter

        // F08 — the Compose summary bar/detail sheet needs the same GpuData the Epoxy list
        // shows, including glExtensions which only arrives once GLSurfaceView's
        // onSurfaceCreated() callback fires (asynchronous, after first render).
        val gpuDataState = mutableStateOf<GpuData?>(null)
        viewModel.viewState.observe(viewLifecycleOwner) { state ->
            controller.setData(state)
            gpuDataState.value = state.gpuData
        }

        val vulkanCapability = graphicsDetailProvider.vulkanCapability()
        binding.graphicsDetailCompose.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                CpuInfoTheme {
                    var showDetail by remember { mutableStateOf(false) }
                    val gpuData = gpuDataState.value
                    if (gpuData != null) {
                        val extensions = GraphicsExtensionParser.parse(gpuData.glExtensions)
                        GraphicsDetailBar(
                            gpuData = gpuData,
                            extensionCount = extensions.size,
                            onClick = { showDetail = true },
                        )
                        if (showDetail) {
                            GraphicsDetailBottomSheet(
                                gpuData = gpuData,
                                vulkanCapability = vulkanCapability,
                                extensions = extensions,
                                onDismiss = { showDetail = false },
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        glSurfaceView?.onResume()
    }

    override fun onPause() {
        glSurfaceView?.onPause()
        super.onPause()
    }
}
