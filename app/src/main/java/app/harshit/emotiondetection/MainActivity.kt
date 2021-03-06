/*
 * Copyright 2016, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package app.harshit.emotiondetection

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.KeyEvent
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    private val cameraFragment by lazy {
        CameraFragment.newInstance()
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // We need permission to access the camera

        container.setOnClickListener {
            cameraFragment.onKeyUp(KeyEvent.KEYCODE_ENTER)
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.container, cameraFragment)
            .commit()
    }

//    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
//        cameraFragment.onKeyUp(keyCode)
//        return super.onKeyUp(keyCode, event)
//    }

}
