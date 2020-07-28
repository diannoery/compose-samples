/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.samples.crane.ui

import androidx.compose.animation.core.SpringSpec
import androidx.compose.foundation.Box
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.DpConstraints
import androidx.compose.foundation.layout.Stack
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.preferredSizeIn
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.state
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.Modifier
import androidx.compose.ui.WithConstraints
import androidx.compose.ui.gesture.scrollorientationlocking.Orientation
import androidx.compose.ui.onPositioned
import androidx.compose.ui.platform.DensityAmbient
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

enum class FullScreenState {
    MINIMISED,
    COLLAPSED,
    EXPANDED,
}

class DraggableBackdropState(value: FullScreenState = FullScreenState.MINIMISED) {
    var value by mutableStateOf(value)
}

@Composable
fun BackdropFrontLayerDraggable(
    modifier: Modifier = Modifier,
    backdropState: DraggableBackdropState = remember { DraggableBackdropState() },
    staticChildren: @Composable (Modifier) -> Unit,
    backdropChildren: @Composable (Modifier) -> Unit
) {
    var backgroundChildrenSize by state(structuralEqualityPolicy()) { IntSize(0, 0) }

    Box(modifier.fillMaxSize()) {
        WithConstraints {
            val fullHeight = constraints.maxHeight.toFloat()
            val anchors = getAnchors(backgroundChildrenSize, fullHeight)

            var backdropPosition by state { fullHeight }
            Stack(
                Modifier.stateDraggable(
                    state = backdropState.value,
                    onStateChange = { newExploreState -> backdropState.value = newExploreState },
                    anchorsToState = anchors,
                    animationSpec = AnimationSpec,
                    orientation = Orientation.Vertical,
                    minValue = VerticalExplorePadding,
                    maxValue = fullHeight,
                    enabled = true,
                    onNewValue = { backdropPosition = it }
                )
            ) {
                staticChildren(
                    Modifier.onPositioned { coordinates ->
                        if (backgroundChildrenSize.height == 0) {
                            backdropState.value = FullScreenState.COLLAPSED
                        }
                        backgroundChildrenSize = coordinates.size
                    }
                )

                val shadowColor = MaterialTheme.colors.surface.copy(alpha = 0.8f)
                val revealValue = backgroundChildrenSize.height / 2
                if (backdropPosition < revealValue) {
                    Canvas(Modifier.fillMaxSize()) {
                        drawRect(size = size, color = shadowColor)
                    }
                }

                val yOffset = with(DensityAmbient.current) {
                    backdropPosition.toDp()
                }

                backdropChildren(
                    Modifier.offset(0.dp, yOffset)
                        .preferredSizeIn(currentConstraints(constraints))
                )
            }
        }
    }
}

private const val ANCHOR_BOTTOM_OFFSET = 130f

private fun getAnchors(
    searchChildrenSize: IntSize,
    fullHeight: Float
): List<Pair<Float, FullScreenState>> {
    val mediumValue = searchChildrenSize.height + 50.dp.value
    val maxValue = fullHeight - ANCHOR_BOTTOM_OFFSET
    return listOf(
        0f to FullScreenState.EXPANDED,
        mediumValue to FullScreenState.COLLAPSED,
        maxValue to FullScreenState.MINIMISED
    )
}

@Composable
private fun currentConstraints(pxConstraints: Constraints): DpConstraints {
    return with(DensityAmbient.current) {
        DpConstraints(pxConstraints)
    }
}

private const val VerticalExplorePadding = 0f
private const val ExploreStiffness = 1000f
private val AnimationSpec = SpringSpec<Float>(stiffness = ExploreStiffness)
