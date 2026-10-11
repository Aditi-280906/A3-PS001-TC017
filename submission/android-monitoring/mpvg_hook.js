Java.perform(function () {

    console.log("[RANSCOPE] MPVG hook loaded");

    // --------------------------------------------------
    // Hook requestPermissions()
    // --------------------------------------------------

    try {

        var ActivityCompat =
            Java.use("androidx.core.app.ActivityCompat");

        var requestPermissions =
            ActivityCompat.requestPermissions.overload(
                "android.app.Activity",
                "[Ljava.lang.String;",
                "int"
            );

        requestPermissions.implementation = function (
            activity,
            permissions,
            requestCode
        ) {

            var perms = [];

            for (var i = 0; i < permissions.length; i++) {
                perms.push(String(permissions[i]));
            }

            send({
                layer: "MPVG",
                event: "requestPermissions",
                package: "com.ranscope.android",
                permissions: perms,
                request_code: requestCode,
                timestamp: Date.now() / 1000.0
            });

            console.log(
                "[RANSCOPE][MPVG] requestPermissions: " +
                perms.join(", ")
            );

            return requestPermissions.call(
                this,
                activity,
                permissions,
                requestCode
            );
        };

    } catch (e) {

        console.log(
            "[RANSCOPE][MPVG] requestPermissions hook error: " +
            e
        );
    }


    // --------------------------------------------------
    // Hook checkSelfPermission()
    // --------------------------------------------------

    try {

        var ContextCompat =
            Java.use("androidx.core.content.ContextCompat");

        var checkPermission =
            ContextCompat.checkSelfPermission.overload(
                "android.content.Context",
                "java.lang.String"
            );

        checkPermission.implementation = function (
            context,
            permission
        ) {

            var result =
                checkPermission.call(
                    this,
                    context,
                    permission
                );

            send({
                layer: "MPVG",
                event: "checkSelfPermission",
                package: "com.ranscope.android",
                permission: String(permission),
                granted: result === 0,
                timestamp: Date.now() / 1000.0
            });

            return result;
        };

    } catch (e) {

        console.log(
            "[RANSCOPE][MPVG] checkSelfPermission hook error: " +
            e
        );
    }

});
