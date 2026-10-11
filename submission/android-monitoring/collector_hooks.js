// ============================================================
// RANSCOPE V2 FRIDA COLLECTOR
//
// Layers:
//   MPVG -> permission activity
//   SFIM -> filesystem API activity
//   EAP  -> crypto API activity
//   CAIT -> camera/API activity
// ============================================================


function emitEvent(record) {

    send(record);

    console.log(
        "[RANSCOPE_EVENT]" +
        JSON.stringify(record)
    );
}


Java.perform(function () {

    console.log(
        "[RANSCOPE] V2 collector hook loaded"
    );


    // ========================================================
    // MPVG
    // requestPermissions()
    // ========================================================

    try {

        var ActivityCompat =
            Java.use(
                "androidx.core.app.ActivityCompat"
            );

        var requestPermissions =
            ActivityCompat.requestPermissions.overload(
                "android.app.Activity",
                "[Ljava.lang.String;",
                "int"
            );

        requestPermissions.implementation =
            function (
                activity,
                permissions,
                requestCode
            ) {

                var perms = [];

                for (
                    var i = 0;
                    i < permissions.length;
                    i++
                ) {

                    perms.push(
                        String(permissions[i])
                    );
                }

                emitEvent({

                    layer: "MPVG",

                    event:
                        "requestPermissions",

                    package:
                        "com.ranscope.android",

                    permissions:
                        perms,

                    request_code:
                        requestCode,

                    timestamp:
                        Date.now() / 1000.0
                });

                return requestPermissions.call(
                    this,
                    activity,
                    permissions,
                    requestCode
                );
            };

        console.log(
            "[RANSCOPE] MPVG requestPermissions ready"
        );

    } catch (e) {

        console.log(
            "[RANSCOPE] MPVG requestPermissions error: " +
            e
        );
    }


    // ========================================================
    // MPVG
    // checkSelfPermission()
    // ========================================================

    try {

        var ContextCompat =
            Java.use(
                "androidx.core.content.ContextCompat"
            );

        var checkPermission =
            ContextCompat.checkSelfPermission.overload(
                "android.content.Context",
                "java.lang.String"
            );

        checkPermission.implementation =
            function (
                context,
                permission
            ) {

                var result =
                    checkPermission.call(
                        this,
                        context,
                        permission
                    );

                emitEvent({

                    layer: "MPVG",

                    event:
                        "checkSelfPermission",

                    package:
                        "com.ranscope.android",

                    permission:
                        String(permission),

                    granted:
                        result === 0,

                    timestamp:
                        Date.now() / 1000.0
                });

                return result;
            };

        console.log(
            "[RANSCOPE] MPVG checkSelfPermission ready"
        );

    } catch (e) {

        console.log(
            "[RANSCOPE] MPVG checkSelfPermission error: " +
            e
        );
    }


    // ========================================================
    // SFIM
    // FileInputStream(File)
    // ========================================================

    try {

        var FileInputStream =
            Java.use(
                "java.io.FileInputStream"
            );

        var fisFile =
            FileInputStream.$init.overload(
                "java.io.File"
            );

        fisFile.implementation =
            function (file) {

                emitEvent({

                    layer: "SFIM",

                    event:
                        "file_read_open",

                    package:
                        "com.ranscope.android",

                    path:
                        String(file.getAbsolutePath()),

                    timestamp:
                        Date.now() / 1000.0
                });

                return fisFile.call(
                    this,
                    file
                );
            };

        console.log(
            "[RANSCOPE] SFIM FileInputStream ready"
        );

    } catch (e) {

        console.log(
            "[RANSCOPE] SFIM FileInputStream error: " +
            e
        );
    }


    // ========================================================
    // SFIM
    // FileOutputStream(File)
    // ========================================================

    try {

        var FileOutputStream =
            Java.use(
                "java.io.FileOutputStream"
            );

        var fosFile =
            FileOutputStream.$init.overload(
                "java.io.File"
            );

        fosFile.implementation =
            function (file) {

                emitEvent({

                    layer: "SFIM",

                    event:
                        "file_write_open",

                    package:
                        "com.ranscope.android",

                    path:
                        String(file.getAbsolutePath()),

                    timestamp:
                        Date.now() / 1000.0
                });

                return fosFile.call(
                    this,
                    file
                );
            };

        console.log(
            "[RANSCOPE] SFIM FileOutputStream ready"
        );

    } catch (e) {

        console.log(
            "[RANSCOPE] SFIM FileOutputStream error: " +
            e
        );
    }


    // ========================================================
    // SFIM
    // File.renameTo()
    // ========================================================

    try {

        var JavaFile =
            Java.use(
                "java.io.File"
            );

        var renameTo =
            JavaFile.renameTo.overload(
                "java.io.File"
            );

        renameTo.implementation =
            function (destination) {

                var sourcePath =
                    String(
                        this.getAbsolutePath()
                    );

                var destinationPath =
                    String(
                        destination.getAbsolutePath()
                    );

                var result =
                    renameTo.call(
                        this,
                        destination
                    );

                emitEvent({

                    layer: "SFIM",

                    event:
                        "file_rename",

                    package:
                        "com.ranscope.android",

                    source:
                        sourcePath,

                    destination:
                        destinationPath,

                    success:
                        result,

                    timestamp:
                        Date.now() / 1000.0
                });

                return result;
            };

        console.log(
            "[RANSCOPE] SFIM renameTo ready"
        );

    } catch (e) {

        console.log(
            "[RANSCOPE] SFIM renameTo error: " +
            e
        );
    }


    // ========================================================
    // SFIM
    // File.delete()
    // ========================================================

    try {

        var JavaFileDelete =
            Java.use(
                "java.io.File"
            );

        var deleteFile =
            JavaFileDelete.delete.overload();

        deleteFile.implementation =
            function () {

                var path =
                    String(
                        this.getAbsolutePath()
                    );

                var result =
                    deleteFile.call(
                        this
                    );

                emitEvent({

                    layer: "SFIM",

                    event:
                        "file_delete",

                    package:
                        "com.ranscope.android",

                    path:
                        path,

                    success:
                        result,

                    timestamp:
                        Date.now() / 1000.0
                });

                return result;
            };

        console.log(
            "[RANSCOPE] SFIM delete ready"
        );

    } catch (e) {

        console.log(
            "[RANSCOPE] SFIM delete error: " +
            e
        );
    }


    // ========================================================
    // EAP
    // Cipher.doFinal(byte[])
    // ========================================================

    try {

        var Cipher =
            Java.use(
                "javax.crypto.Cipher"
            );

        var doFinalBytes =
            Cipher.doFinal.overload(
                "[B"
            );

        doFinalBytes.implementation =
            function (input) {

                var algorithm =
                    String(
                        this.getAlgorithm()
                    );

                var inputLength =
                    input == null
                        ? 0
                        : input.length;

                var result =
                    doFinalBytes.call(
                        this,
                        input
                    );

                var outputLength =
                    result == null
                        ? 0
                        : result.length;

                emitEvent({

                    layer: "EAP",

                    event:
                        "cipher_doFinal",

                    package:
                        "com.ranscope.android",

                    algorithm:
                        algorithm,

                    input_length:
                        inputLength,

                    output_length:
                        outputLength,

                    timestamp:
                        Date.now() / 1000.0
                });

                return result;
            };

        console.log(
            "[RANSCOPE] EAP Cipher.doFinal ready"
        );

    } catch (e) {

        console.log(
            "[RANSCOPE] EAP Cipher.doFinal error: " +
            e
        );
    }


    // ========================================================
    // CAIT
    // Camera ID enumeration
    // ========================================================

    try {

        var CameraManager =
            Java.use(
                "android.hardware.camera2.CameraManager"
            );

        var getCameraIdList =
            CameraManager.getCameraIdList.overload();

        getCameraIdList.implementation =
            function () {

                var result =
                    getCameraIdList.call(
                        this
                    );

                var ids = [];

                for (
                    var i = 0;
                    i < result.length;
                    i++
                ) {

                    ids.push(
                        String(result[i])
                    );
                }

                emitEvent({

                    layer: "CAIT",

                    event:
                        "camera_id_list",

                    package:
                        "com.ranscope.android",

                    camera_ids:
                        ids,

                    timestamp:
                        Date.now() / 1000.0
                });

                return result;
            };

        console.log(
            "[RANSCOPE] CAIT camera ID ready"
        );

    } catch (e) {

        console.log(
            "[RANSCOPE] CAIT camera ID error: " +
            e
        );
    }


    // ========================================================
    // CAIT
    // Camera open
    // ========================================================

    try {

        var CameraManagerOpen =
            Java.use(
                "android.hardware.camera2.CameraManager"
            );

        var openCamera =
            CameraManagerOpen.openCamera.overload(
                "java.lang.String",
                "android.hardware.camera2.CameraDevice$StateCallback",
                "android.os.Handler"
            );

        openCamera.implementation =
            function (
                cameraId,
                callback,
                handler
            ) {

                emitEvent({

                    layer: "CAIT",

                    event:
                        "camera_open",

                    package:
                        "com.ranscope.android",

                    camera_id:
                        String(cameraId),

                    timestamp:
                        Date.now() / 1000.0
                });

                return openCamera.call(
                    this,
                    cameraId,
                    callback,
                    handler
                );
            };

        console.log(
            "[RANSCOPE] CAIT camera open ready"
        );

    } catch (e) {

        console.log(
            "[RANSCOPE] CAIT camera open error: " +
            e
        );
    }


    // ========================================================
    // FINISHED
    // ========================================================

    console.log(
        "[RANSCOPE] V2 collector hooks initialized"
    );

});
