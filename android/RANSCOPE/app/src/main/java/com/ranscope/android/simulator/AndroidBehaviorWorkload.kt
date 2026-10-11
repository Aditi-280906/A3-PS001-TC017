package com.ranscope.android.simulator

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

class AndroidBehaviorWorkload(
    private val activity: Activity
) {

    companion object {
        private const val TAG = "RANSCOPE_BEHAVIOR"
        private const val EAP_TAG = "RANSCOPE_EAP"
        private const val PERMISSION_REQUEST_CODE = 9101
    }

    private val mainHandler =
        Handler(Looper.getMainLooper())


    // ========================================================
    // EAP EVENT
    // ========================================================

    data class EapEvent(
        val event: String,
        val packageName: String,
        val algorithm: String,
        val inputLength: Int,
        val outputLength: Int,
        val timestampMs: Long
    )


    // ========================================================
    // ANDROID TELEMETRY EVENT
    // ========================================================

    data class AndroidEvent(
        val layer: String,
        val event: String,
        val packageName: String,
        val timestampMs: Long,
        val permission: String? = null,
        val granted: Boolean? = null,
        val cameraId: String? = null,
        val cameraIds: List<String>? = null
    )


    // ========================================================
    // BENIGN BEHAVIOUR
    // ========================================================

    fun runBenignBehavior(
        onFinished: (() -> Unit)? = null,
        onEapEvent: ((EapEvent) -> Unit)? = null,
        onAndroidEvent: ((AndroidEvent) -> Unit)? = null
    ) {

        Log.i(
            TAG,
            "BENIGN behavior started"
        )


        // ====================================================
        // EAP TEST
        // ====================================================

        try {

            val keyBytes =
                ByteArray(16) { 0x01 }

            val inputData =
                ByteArray(1024) { 0x41 }

            val secretKey =
                SecretKeySpec(
                    keyBytes,
                    "AES"
                )

            val cipher =
                Cipher.getInstance(
                    "AES/ECB/PKCS5Padding"
                )

            cipher.init(
                Cipher.ENCRYPT_MODE,
                secretKey
            )


            // Actual EAP operation
            val encryptedData =
                cipher.doFinal(inputData)


            val eapEvent =
                EapEvent(
                    event = "cipher_doFinal",
                    packageName =
                        activity.packageName,
                    algorithm =
                        cipher.algorithm,
                    inputLength =
                        inputData.size,
                    outputLength =
                        encryptedData.size,
                    timestampMs =
                        System.currentTimeMillis()
                )


            Log.i(
                EAP_TAG,
                "EAP_CIPHER_DO_FINAL," +
                        "input_length=${eapEvent.inputLength}," +
                        "output_length=${eapEvent.outputLength}," +
                        "algorithm=${eapEvent.algorithm}"
            )


            // Return the REAL event to DatasetGenerator
            onEapEvent?.invoke(
                eapEvent
            )


            Log.i(
                TAG,
                "EAP test completed: " +
                        "${encryptedData.size} bytes"
            )

        } catch (e: Exception) {

            Log.e(
                TAG,
                "EAP test failed",
                e
            )
        }


        // ====================================================
        // MPVG TEST
        // ====================================================

        val cameraPermission =
            Manifest.permission.CAMERA

        val benignPermissionGranted =
            ContextCompat.checkSelfPermission(
                activity,
                cameraPermission
            ) == PackageManager.PERMISSION_GRANTED

        onAndroidEvent?.invoke(
            AndroidEvent(
                layer = "MPVG",
                event = "checkSelfPermission",
                packageName = activity.packageName,
                permission = cameraPermission,
                granted = benignPermissionGranted,
                timestampMs = System.currentTimeMillis()
            )
        )

        if (!benignPermissionGranted) {

            Log.i(
                TAG,
                "Requesting CAMERA permission"
            )

            onAndroidEvent?.invoke(
                AndroidEvent(
                    layer = "MPVG",
                    event = "requestPermissions",
                    packageName = activity.packageName,
                    permission = cameraPermission,
                    timestampMs = System.currentTimeMillis()
                )
            )

            ActivityCompat.requestPermissions(
                activity,
                arrayOf(cameraPermission),
                PERMISSION_REQUEST_CODE
            )
        }


        // ====================================================
        // CAMERA
        // ====================================================

        mainHandler.postDelayed({

            openCameraOnce(
                onFinished = {

                    Log.i(
                        TAG,
                        "BENIGN behavior finished"
                    )

                    onFinished?.invoke()
                },
                onAndroidEvent = onAndroidEvent
            )

        }, 700)
    }


    // ========================================================
    // MALICIOUS-LIKE BEHAVIOUR
    // ========================================================

    fun runMaliciousLikeBehavior(
        onFinished: (() -> Unit)? = null,
        onEapEvent: ((EapEvent) -> Unit)? = null,
        onAndroidEvent: ((AndroidEvent) -> Unit)? = null
    ) {

        Log.i(
            TAG,
            "MALICIOUS-LIKE behavior started"
        )


        // ====================================================
        // EAP TEST
        // ====================================================

        try {

            val keyBytes =
                ByteArray(16) { 0x02 }

            val inputData =
                ByteArray(1024) { 0x42 }

            val secretKey =
                SecretKeySpec(
                    keyBytes,
                    "AES"
                )

            val cipher =
                Cipher.getInstance(
                    "AES/ECB/PKCS5Padding"
                )

            cipher.init(
                Cipher.ENCRYPT_MODE,
                secretKey
            )


            // Actual EAP operation
            val encryptedData =
                cipher.doFinal(inputData)


            val eapEvent =
                EapEvent(
                    event = "cipher_doFinal",
                    packageName =
                        activity.packageName,
                    algorithm =
                        cipher.algorithm,
                    inputLength =
                        inputData.size,
                    outputLength =
                        encryptedData.size,
                    timestampMs =
                        System.currentTimeMillis()
                )


            Log.i(
                EAP_TAG,
                "EAP_CIPHER_DO_FINAL," +
                        "input_length=${eapEvent.inputLength}," +
                        "output_length=${eapEvent.outputLength}," +
                        "algorithm=${eapEvent.algorithm}"
            )


            // Return REAL event
            onEapEvent?.invoke(
                eapEvent
            )


            Log.i(
                TAG,
                "Malicious-like EAP test completed: " +
                        "${encryptedData.size} bytes"
            )

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Malicious-like EAP test failed",
                e
            )
        }


        // ====================================================
        // PERMISSION BURST
        // ====================================================

        val permissions =
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.POST_NOTIFICATIONS,
                Manifest.permission.READ_CONTACTS
            )


        // Permission checks
        for (permission in permissions) {

            val granted =
                ContextCompat.checkSelfPermission(
                    activity,
                    permission
                ) == PackageManager.PERMISSION_GRANTED

            onAndroidEvent?.invoke(
                AndroidEvent(
                    layer = "MPVG",
                    event = "checkSelfPermission",
                    packageName = activity.packageName,
                    permission = permission,
                    granted = granted,
                    timestampMs = System.currentTimeMillis()
                )
            )
        }


        // Permission requests
        requestPermissionBurst(
            permissions = permissions,
            index = 0,
            onAndroidEvent = onAndroidEvent
        )


        // ====================================================
        // CAMERA
        // ====================================================

        mainHandler.postDelayed({

            openCameraOnce(
                onFinished = {

                    Log.i(
                        TAG,
                        "MALICIOUS-LIKE behavior finished"
                    )

                    onFinished?.invoke()
                },
                onAndroidEvent = onAndroidEvent
            )

        }, 1800)
    }


    // ========================================================
    // PERMISSION BURST
    // ========================================================

    private fun requestPermissionBurst(
        permissions: Array<String>,
        index: Int,
        onAndroidEvent: ((AndroidEvent) -> Unit)? = null
    ) {

        if (
            index >= permissions.size
        ) {
            return
        }


        val permission =
            permissions[index]


        Log.i(
            TAG,
            "Permission burst request: $permission"
        )


        onAndroidEvent?.invoke(
            AndroidEvent(
                layer = "MPVG",
                event = "requestPermissions",
                packageName = activity.packageName,
                permission = permission,
                timestampMs = System.currentTimeMillis()
            )
        )


        ActivityCompat.requestPermissions(
            activity,
            arrayOf(permission),
            PERMISSION_REQUEST_CODE + index
        )


        mainHandler.postDelayed({

            requestPermissionBurst(
                permissions,
                index + 1,
                onAndroidEvent
            )

        }, 300)
    }


    // ========================================================
    // CAMERA
    // ========================================================

    private fun openCameraOnce(
        onFinished: (() -> Unit)? = null,
        onAndroidEvent: ((AndroidEvent) -> Unit)? = null
    ) {

        // ----------------------------------------------------
        // CAMERA PERMISSION CHECK
        // ----------------------------------------------------

        val cameraPermissionGranted =
            ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED

        onAndroidEvent?.invoke(
            AndroidEvent(
                layer = "MPVG",
                event = "checkSelfPermission",
                packageName = activity.packageName,
                permission = Manifest.permission.CAMERA,
                granted = cameraPermissionGranted,
                timestampMs = System.currentTimeMillis()
            )
        )

        if (!cameraPermissionGranted) {

            Log.w(
                TAG,
                "Camera permission not granted"
            )

            onFinished?.invoke()
            return
        }


        try {

            val cameraManager =
                activity.getSystemService(
                    Context.CAMERA_SERVICE
                ) as CameraManager


            val cameraIds =
                cameraManager.cameraIdList


            // ------------------------------------------------
            // CAIT: CAMERA ID ENUMERATION
            // ------------------------------------------------

            onAndroidEvent?.invoke(
                AndroidEvent(
                    layer = "CAIT",
                    event = "camera_id_list",
                    packageName = activity.packageName,
                    cameraIds = cameraIds.toList(),
                    timestampMs = System.currentTimeMillis()
                )
            )


            if (
                cameraIds.isEmpty()
            ) {

                Log.w(
                    TAG,
                    "No camera available"
                )

                onFinished?.invoke()
                return
            }


            val cameraId =
                cameraIds[0]


            Log.i(
                TAG,
                "Opening camera: $cameraId"
            )


            cameraManager.openCamera(

                cameraId,

                object :
                    CameraDevice.StateCallback() {

                    override fun onOpened(
                        camera: CameraDevice
                    ) {

                        // ------------------------------------
                        // CAIT: ACTUAL CAMERA OPEN
                        // ------------------------------------

                        onAndroidEvent?.invoke(
                            AndroidEvent(
                                layer = "CAIT",
                                event = "camera_open",
                                packageName = activity.packageName,
                                cameraId = cameraId,
                                timestampMs = System.currentTimeMillis()
                            )
                        )


                        Log.i(
                            TAG,
                            "Camera opened successfully"
                        )

                        camera.close()

                        Log.i(
                            TAG,
                            "Camera closed"
                        )

                        onFinished?.invoke()
                    }


                    override fun onDisconnected(
                        camera: CameraDevice
                    ) {

                        Log.w(
                            TAG,
                            "Camera disconnected"
                        )

                        camera.close()

                        onFinished?.invoke()
                    }


                    override fun onError(
                        camera: CameraDevice,
                        error: Int
                    ) {

                        Log.w(
                            TAG,
                            "Camera error: $error"
                        )

                        camera.close()

                        onFinished?.invoke()
                    }
                },

                mainHandler
            )

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Camera access failed",
                e
            )

            onFinished?.invoke()
        }
    }
}