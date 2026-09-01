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

package com.galaxyjoy.cpuinfo.feat.infor.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.galaxyjoy.cpuinfo.R
import dagger.hilt.android.AndroidEntryPoint

/**
 * Simple base for all info fragments which displays data on [RecyclerView]
 *
 */
@AndroidEntryPoint
abstract class BaseRvFragment : Fragment(), AdtInfoItems.OnClickListener {

    protected lateinit var mainContainer: View
    protected lateinit var recyclerView: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.frm_rv, container, false)
        mainContainer = view.findViewById(R.id.mainContainer)
        recyclerView = view.findViewById(R.id.rv)
        setupRecyclerView()
        setupRecyclerViewAdapter()
        return view
    }

    override fun onDestroyView() {
        recyclerView.adapter = null
        super.onDestroyView()
    }

    /**
     * Basic [RecyclerView] configuration with [LinearLayoutManager] and disabled animations
     */
    private fun setupRecyclerView() {
        val layoutManager = LinearLayoutManager(requireContext())
        recyclerView.layoutManager = layoutManager

        // Remove change animation
        (recyclerView.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false
    }

    /**
     * Setup adapter for [RecyclerView]. Use this method to set adapter and refreshing logic.
     */
    abstract fun setupRecyclerViewAdapter()

    override fun onItemLongPressed(item: Pair<String, String>) {
        copyToClipboardAndNotify(mainContainer, item.second)
    }
}
