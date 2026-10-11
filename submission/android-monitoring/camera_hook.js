Java.perform(function () {

    console.log("[RANSCOPE] Camera hook loaded");

    // --------------------------------------------------
    // Hook camera ID enumeration
    // --------------------------------------------------

    try {

        var CameraManager =
            Java.use("android.hardware.camera2.CameraManager");

        var getCameraIdList =
            CameraManager.getCameraIdList.overload();

        getCameraIdList.implementation = function () {

            var result =
                getCameraIdList.call(this);

            var ids = [];

            for (var i = 0; i < result.length; i++) {
                ids.push(String(result[i]));
            }

            send({
                layer: "CAIT",
                event: "camera_id_list",
                package: "com.ranscope.android",
                camera_ids: ids,
                timestamp: Date.now() / 1000.0
            });

            console.log(
                "[RANSCOPE][CAIT] camera_id_list: " +
                ids.join(", ")
            );

            return result;
        };

    } catch (e) {

        console.log(
            "[RANSCOPE][CAIT] getCameraIdList hook error: " +
            e
        );
    }


    // --------------------------------------------------
    // Hook CameraManager.openCamera()
    // --------------------------------------------------

    try {

        var CameraManager =
            Java.use("android.hardware.camera2.CameraManager");

        var openCamera =
            CameraManager.openCamera.overload(
                "java.lang.String",
                "android.hardware.camera2.CameraDevice$StateCallback",
                "android.os.Handler"
            );

        openCamera.implementation = function (
            cameraId,
            callback,
            handler
        ) {

            send({
                layer: "CAIT",
                event: "camera_open",
                package: "com.ranscope.android",
                camera_id: String(cameraId),
                timestamp: Date.now() / 1000.0
            });

            console.log(
                "[RANSCOPE][CAIT] camera_open: " +
                String(cameraId)
            );

            return openCamera.call(
                this,
                cameraId,
                callback,
                handler
            );
        };

    } catch (e) {

        console.log(
            "[RANSCOPE][CAIT] openCamera hook error: " +
            e
        );
    }

});
