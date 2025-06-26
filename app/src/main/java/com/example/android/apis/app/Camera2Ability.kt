package com.example.android.apis.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.media.ImageReader
import android.media.ImageReader.OnImageAvailableListener
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.SparseIntArray
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.android.apis.R
import java.util.Arrays


class Camera2Ability : AppCompatActivity(), View.OnClickListener {
    val ORIENTATIONS: SparseIntArray = SparseIntArray()

    ///为了使照片竖直显示
    init {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    private var mSurfaceView: SurfaceView? = null
    private var mSurfaceHolder: SurfaceHolder? = null
    private var iv_show: ImageView? = null
    private var mCameraManager: CameraManager? = null //摄像头管理器
    private var childHandler: Handler? = null
    var mainHandler: Handler? = null
    private var mCameraID: String? = null //摄像头Id 0 为后  1 为前
    private var mImageReader: ImageReader? = null
    private var mCameraCaptureSession: CameraCaptureSession? = null
    private var mCameraDevice: CameraDevice? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(null)
        try {
            title = "Camera 2 API"
            setContentView(R.layout.activity_camera2)
            initVIew()
        }catch (e : Exception)
        {
            e.printStackTrace()
        }
    }


    /**
     * 初始化
     */
    private fun initVIew() {
        iv_show = findViewById<View?>(R.id.iv_show_camera2_activity) as ImageView?
        //mSurfaceView
        mSurfaceView = findViewById<View?>(R.id.surface_view_camera2_activity) as SurfaceView?
        mSurfaceView!!.setOnClickListener(this)
        mSurfaceHolder = mSurfaceView!!.getHolder()
        mSurfaceHolder!!.setKeepScreenOn(true)
        // mSurfaceView添加回调
        mSurfaceHolder!!.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) { //SurfaceView创建
                // 初始化Camera
                initCamera2()
            }

            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int
            ) {
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) { //SurfaceView销毁
                // 释放Camera资源
                if (null != mCameraDevice) {
                    mCameraDevice!!.close()
                    this@Camera2Ability.mCameraDevice = null
                }
            }
        })
    }

    /**
     * 初始化Camera2
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private fun initCamera2() {
        val handlerThread = HandlerThread("Camera2")
        handlerThread.start()
        childHandler = Handler(handlerThread.getLooper())
        mainHandler = Handler(getMainLooper())
        mCameraID = "" + CameraCharacteristics.LENS_FACING_FRONT //后摄像头
        mImageReader = ImageReader.newInstance(1080, 1920, ImageFormat.JPEG, 1)
        mImageReader!!.setOnImageAvailableListener(object : OnImageAvailableListener {
            //可以在这里处理拍照得到的临时照片 例如，写入本地
            override fun onImageAvailable(reader: ImageReader) {
                mCameraDevice!!.close()
                mSurfaceView!!.setVisibility(View.GONE)
                iv_show!!.setVisibility(View.VISIBLE)
                // 拿到拍照照片数据
                val image = reader.acquireNextImage()
                val buffer = image.getPlanes()[0].getBuffer()
                val bytes = ByteArray(buffer.remaining())
                buffer.get(bytes) //由缓冲区存入字节数组
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                if (bitmap != null) {
                    iv_show!!.setImageBitmap(bitmap)
                }
            }
        }, mainHandler)
        //获取摄像头管理
        mCameraManager = getSystemService(CAMERA_SERVICE) as CameraManager?
        try {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.CAMERA
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        Manifest.permission.CAMERA
                    ),
                    13800
                )
                return
            }
            //打开摄像头
            mCameraManager!!.openCamera(mCameraID!!, stateCallback, mainHandler)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }


    /**
     * 摄像头创建监听
     */
    private val stateCallback: CameraDevice.StateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) { //打开摄像头
            mCameraDevice = camera
            //开启预览
            takePreview()
        }

        override fun onDisconnected(camera: CameraDevice) { //关闭摄像头
            if (null != mCameraDevice) {
                mCameraDevice!!.close()
                this@Camera2Ability.mCameraDevice = null
            }
        }

        override fun onError(camera: CameraDevice, error: Int) { //发生错误
            Toast.makeText(this@Camera2Ability, "摄像头开启失败", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == 13800) finish()
    }

    /**
     * 开始预览
     */
    private fun takePreview() {
        try {
            // 创建预览需要的CaptureRequest.Builder
            val previewRequestBuilder =
                mCameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            // 将SurfaceView的surface作为CaptureRequest.Builder的目标
            previewRequestBuilder.addTarget(mSurfaceHolder!!.getSurface())
            // 创建CameraCaptureSession，该对象负责管理处理预览请求和拍照请求
            mCameraDevice!!.createCaptureSession(
                Arrays.asList<Surface?>(
                    mSurfaceHolder!!.getSurface(),
                    mImageReader!!.getSurface()
                ), object : CameraCaptureSession.StateCallback() // ③
                {
                    override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
                        if (null == mCameraDevice) return
                        // 当摄像头已经准备好时，开始显示预览
                        mCameraCaptureSession = cameraCaptureSession
                        try {
                            // 自动对焦
                            previewRequestBuilder.set<Int?>(
                                CaptureRequest.CONTROL_AF_MODE,
                                CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                            )
                            // 打开闪光灯
                            previewRequestBuilder.set<Int?>(
                                CaptureRequest.CONTROL_AE_MODE,
                                CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH
                            )
                            // 显示预览
                            val previewRequest = previewRequestBuilder.build()
                            mCameraCaptureSession!!.setRepeatingRequest(
                                previewRequest,
                                null,
                                childHandler
                            )
                        } catch (e: CameraAccessException) {
                            e.printStackTrace()
                        }
                    }

                    override fun onConfigureFailed(cameraCaptureSession: CameraCaptureSession) {
                        Toast.makeText(this@Camera2Ability, "配置失败", Toast.LENGTH_SHORT).show()
                    }
                }, childHandler
            )
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    /**
     * 点击事件
     */
    public override fun onClick(v: View?) {
        takePicture()
    }

    /**
     * 拍照
     */
    private fun takePicture() {
        if (mCameraDevice == null) return
        // 创建拍照需要的CaptureRequest.Builder
        val captureRequestBuilder: CaptureRequest.Builder
        try {
            captureRequestBuilder =
                mCameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
            // 将imageReader的surface作为CaptureRequest.Builder的目标
            captureRequestBuilder.addTarget(mImageReader!!.getSurface())
            // 自动对焦
            captureRequestBuilder.set<Int?>(
                CaptureRequest.CONTROL_AF_MODE,
                CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
            )
            // 自动曝光
            captureRequestBuilder.set<Int?>(
                CaptureRequest.CONTROL_AE_MODE,
                CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH
            )
            // 获取手机方向
            val rotation = getWindowManager().getDefaultDisplay().getRotation()
            // 根据设备方向计算设置照片的方向
            captureRequestBuilder.set<Int?>(
                CaptureRequest.JPEG_ORIENTATION,
                ORIENTATIONS.get(rotation)
            )
            //拍照
            val mCaptureRequest = captureRequestBuilder.build()
            mCameraCaptureSession!!.capture(mCaptureRequest, null, childHandler)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }
}