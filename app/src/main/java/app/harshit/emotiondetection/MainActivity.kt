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

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.media.Image
import android.media.ImageReader
import android.net.ConnectivityManager
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.support.design.widget.BottomSheetBehavior
import android.support.v7.app.AppCompatActivity
import android.util.Base64
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.View.GONE
import android.widget.Toast
import com.google.android.things.contrib.driver.button.ButtonInputDriver
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.bottom_sheet.*
import okhttp3.*
import okhttp3.Request
import java.io.IOException


class MainActivity : AppCompatActivity() {

    private val API_KEY = "AIzaSyB6ViM9lAH5s772Gep0e1VJ26e7hhDMa9A"

    var isDetecting = false

    private var mCamera: LensCamera? = null

    /**
     * Driver for the doorbell button;
     */
    private var mButtonInputDriver: ButtonInputDriver? = null

    /**
     * A [Handler] for running Camera tasks in the background.
     */
    private var mCameraHandler: Handler? = null

    /**
     * An additional thread for running Camera tasks that shouldn't block the UI.
     */
    private var mCameraThread: HandlerThread? = null

    lateinit var originalBitmapImage: Bitmap

    private val gson = Gson()

    lateinit var sheetBehavior: BottomSheetBehavior<*>

    private val paint by lazy {
        Paint().apply {
            color = Color.GREEN
            style = Paint.Style.STROKE
            strokeWidth = 10f
        }
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // We need permission to access the camera

        sheetBehavior = BottomSheetBehavior.from(bottomLayout)
        sheetBehavior.isHideable = true
        sheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN

        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            // A problem occurred auto-granting the permission
            Log.e(TAG, "No permission")
            return
        }

        // Creates new handlers and associated threads for camera and networking operations.
        mCameraThread = HandlerThread("CameraBackground")
        mCameraThread!!.start()
        mCameraHandler = Handler(mCameraThread!!.looper)

        // Initialize the button driver
        initPIO()

        // Camera code is complicated, so we've shoved it all in this closet class for you.
        mCamera = LensCamera.getInstance()
        mCamera!!.initializeCamera(this, mCameraHandler, mOnImageAvailableListener)

    }

    private val mOnImageAvailableListener = ImageReader.OnImageAvailableListener { reader ->
        val image = reader.acquireLatestImage()
        getEmotions(image)
    }

    private fun getEmotions(image: Image) {

        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.capacity())
        buffer.get(bytes)

        originalBitmapImage = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, null)

        runOnUiThread {
            ivPreview.setImageBitmap(originalBitmapImage)
        }
        if (isNetwork(this))
            getFacesOkHttp(Base64.encodeToString(bytes, Base64.DEFAULT), image)
        else {
            Toast.makeText(this, "No network detected!", Toast.LENGTH_SHORT).show()
            progress.visibility = GONE
        }
    }

    private fun getFacesOkHttp(base64EncodedImage: String, image: Image) {
        val client = OkHttpClient.Builder().build()

        val requestImage = RequestImage(base64EncodedImage)
        val requestObject = FaceDetectionRequest(arrayListOf(app.harshit.emotiondetection.Request(requestImage)))

        val body = RequestBody.create(MediaType.parse("application/json"), gson.toJson(requestObject))

        val request = Request.Builder()
            .url("https://vision.googleapis.com/v1/images:annotate?key=$API_KEY")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                isDetecting = false
                progress.visibility = View.GONE
                image.close()
            }

            override fun onResponse(call: Call, response: Response) {
                val result = response.body()?.string()
                val faceDetectionResponse = gson.fromJson(result, FaceDetectionResponse::class.java)

                //Draw a rectangle around the faces
                //http://joerg-richter.fuyosoft.com/?p=120
                val tempBitmap =
                    Bitmap.createBitmap(originalBitmapImage.width, originalBitmapImage.height, Bitmap.Config.RGB_565)
                val tempCanvas = Canvas(tempBitmap)
                tempCanvas.drawBitmap(originalBitmapImage, 0f, 0f, null)

                val detectedFaces = faceDetectionResponse.responses

                try {
                    tempCanvas.drawRect(
                        getRectFromVertices(
                            detectedFaces[0]
                                .faceAnnotations[0]
                                .boundingPoly
                                .vertices
                        ),
                        paint
                    )
                    runOnUiThread {
                        ivPreview.setImageDrawable(BitmapDrawable(resources, tempBitmap))
                        tvEmotion.text = ""
                        with(detectedFaces[0].faceAnnotations[0]) {
                            if (angerLikelihood != Emotion.VERY_UNLIKELY && angerLikelihood != Emotion.UNKNOWN)
                                tvEmotion.append("You look Angry 😡\n")
                            if (joyLikelihood != Emotion.VERY_UNLIKELY && joyLikelihood != Emotion.UNKNOWN)
                                tvEmotion.append("You look Happy 😁\n")
                            if (sorrowLikelihood != Emotion.VERY_UNLIKELY && sorrowLikelihood != Emotion.UNKNOWN)
                                tvEmotion.append("You look Sad ☹️\n")
                            if (surpriseLikelihood != Emotion.VERY_UNLIKELY && surpriseLikelihood != Emotion.UNKNOWN)
                                tvEmotion.append("You look Surprised 😯\n")
                        }
                        if (tvEmotion.text.isBlank())
                            tvEmotion.text = "I'm not sure how you look! 🤷🏼‍♂️ 🤷🏼‍♀️"
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        tvEmotion.text = "No face detected!"
                    }
                }
                runOnUiThread {
                    sheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                    progress.visibility = View.GONE
                }
                isDetecting = false
                image.close()
            }
        })
    }

    private fun initPIO() {
        try {
            mButtonInputDriver = ButtonInputDriver(
                BoardDefaults.getGPIOForButton(),
                com.google.android.things.contrib.driver.button.Button.LogicState.PRESSED_WHEN_LOW,
                KeyEvent.KEYCODE_ENTER
            )
            mButtonInputDriver!!.register()
        } catch (e: IOException) {
            mButtonInputDriver = null
            Log.w(TAG, "Could not open GPIO pins", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mCamera!!.shutDown()
        mCameraThread!!.quitSafely()
        try {
            mButtonInputDriver!!.close()
        } catch (e: IOException) {
            Log.e(TAG, "button driver error", e)
        }

    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_ENTER) {
            tvAbout.visibility = GONE
            Log.d(TAG, "button pressed")
            if (!isDetecting) {
                mCamera!!.takePicture()
                progress.visibility = View.VISIBLE
                sheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            }
            isDetecting = true
            return true
        }
        return super.onKeyUp(keyCode, event)
    }

    companion object {
        private val TAG = "MainActivity"
    }

    private fun isNetwork(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetworkInfo = connectivityManager.activeNetworkInfo
        return activeNetworkInfo != null && activeNetworkInfo.isConnected
    }

}
