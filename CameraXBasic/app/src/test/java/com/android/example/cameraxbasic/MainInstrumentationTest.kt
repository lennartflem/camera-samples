/*
 * Copyright 2019 Google LLC
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

package com.android.example.cameraxbasic

import android.Manifest
import android.view.KeyEvent
import android.view.View
import android.widget.FrameLayout
import androidx.core.view.isVisible
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule

import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainInstrumentationTest {

    @get:Rule
    val permissionRule = GrantPermissionRule.grant(Manifest.permission.CAMERA)

    @Test
    fun MainActivityUi() {

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->

            scenario.onActivity { activity: MainActivity ->

                val container = activity.findViewById<View>(R.id.fragment_container)
                assert(container is FrameLayout)
                assert(container.isVisible)

                activity.onKeyDown(KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent(0, KeyEvent.KEYCODE_VOLUME_DOWN))
                assert(! activity.isFinishing)
            }
        }
    }
}