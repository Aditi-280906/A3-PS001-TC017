package com.ranscope.android.detection

import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min

/**
 * One 5-second temporal feature window.
 *
 * Collects real Android filesystem telemetry and converts it
 * into the feature groups expected by RANSCOPE-M.
 *
 * Missing entropy is treated as missing telemetry, NOT as
 * zero entropy.
 */
class TemporalWindow(
    val windowStartMs: Long,
    val windowEndMs: Long
) {

    // ========================================================
    // SFIM FEATURES
    // ========================================================

    var sfimEventCount = 0

    var sfimWriteCount = 0

    var sfimRenameCount = 0

    var sfimUnlinkCount = 0

    var sfimCreateCount = 0

    var sfimDepthSum = 0.0

    var sfimDirEntropySum = 0.0

    var sfimTotalBytes = 0L

    var sfimWriteEntropySum = 0.0

    var sfimEntropyCount = 0


    // ========================================================
    // EAP FEATURES
    // ========================================================

    var eapWriteCount = 0

    var eapExecuteCount = 0

    var eapRenameCount = 0

    var eapUnlinkCount = 0

    var eapFilesChanged = 0


    // ========================================================
    // REAL ENTROPY OBSERVATIONS
    // ========================================================

    /*
     * Only actual entropy observations are stored here.
     *
     * R1 may produce MODIFY/CLOSE_WRITE events where the file
     * is temporarily unavailable and entropy is null.
     *
     * Those observations are simply not added.
     */
    private val entropyObservations =
        mutableListOf<Double>()


    // ========================================================
    // MPVG FEATURES
    // ========================================================

    var mpvgEventCount = 0

    var mpvgPermissionRequests = 0

    var mpvgPermissionGrants = 0

    var mpvgPermissionRevokes = 0

    var mpvgPermissionVp = 0

    var mpvgPermissionVpCumulative = 0

    var mpvgRecentVp10s = 0


    // ========================================================
    // CAIT FEATURES
    // ========================================================

    var caitEventCount = 0

    private val caitCounts =
        mutableMapOf<String, Int>()


    // ========================================================
    // FILE TRACKING
    // ========================================================

    private val changedPaths =
        mutableSetOf<String>()


    // ========================================================
    // ADD FILE OBSERVER EVENT
    // ========================================================

    fun addFileEvent(
        event: String,
        path: String,
        depth: Int = 0,
        fileSize: Long = 0L,
        dirEntropy: Double = 0.0,
        writeEntropy: Double? = null
    ) {

        sfimEventCount++

        when (event) {

            "MODIFY",
            "WRITE" -> {
                sfimWriteCount++
                eapWriteCount++
            }

            "MOVED_FROM",
            "MOVED_TO",
            "RENAME" -> {
                sfimRenameCount++
                eapRenameCount++
            }

            "DELETE",
            "UNLINK" -> {
                sfimUnlinkCount++
                eapUnlinkCount++
            }

            "CREATE" -> {
                sfimCreateCount++
            }
        }


        // ----------------------------------------------------
        // SFIM numerical features
        // ----------------------------------------------------

        sfimDepthSum += depth.toDouble()

        sfimDirEntropySum += dirEntropy

        if (fileSize > 0) {
            sfimTotalBytes += fileSize
        }


        // ----------------------------------------------------
        // REAL WRITE ENTROPY
        // ----------------------------------------------------

        if (writeEntropy != null &&
            writeEntropy.isFinite() &&
            writeEntropy >= 0.0
        ) {

            sfimWriteEntropySum += writeEntropy

            sfimEntropyCount++

            entropyObservations.add(writeEntropy)
        }


        // ----------------------------------------------------
        // Files changed
        // ----------------------------------------------------

        when (event) {

            "MODIFY",
            "WRITE",
            "CREATE",
            "DELETE",
            "UNLINK",
            "MOVED_FROM",
            "MOVED_TO",
            "RENAME" -> {

                if (path.isNotBlank()) {
                    changedPaths.add(path)
                }
            }
        }
    }


    // ========================================================
    // ADD MPVG EVENT
    // ========================================================

    fun addMpvgEvent(
        eventType: String,
        suspicionValue: Int = 0
    ) {

        mpvgEventCount++

        when (eventType) {

            "permission_request" -> {
                mpvgPermissionRequests++
            }

            "permission_granted" -> {
                mpvgPermissionGrants++
            }

            "permission_revoked" -> {
                mpvgPermissionRevokes++
            }

            "permission_vp" -> {

                mpvgPermissionVp +=
                    suspicionValue

                mpvgPermissionVpCumulative +=
                    suspicionValue

                mpvgRecentVp10s +=
                    suspicionValue
            }
        }
    }


    // ========================================================
    // ADD CAIT EVENT
    // ========================================================

    fun addCaitEvent(
        eventType: String
    ) {

        caitEventCount++

        caitCounts[eventType] =
            (caitCounts[eventType] ?: 0) + 1
    }


    // ========================================================
    // CALCULATED SFIM FEATURES
    // ========================================================

    fun sfimMeanDepth(): Double {

        if (sfimEventCount == 0) {
            return 0.0
        }

        return sfimDepthSum /
                sfimEventCount
    }


    fun sfimMeanDirEntropy(): Double {

        if (sfimEventCount == 0) {
            return 0.0
        }

        return sfimDirEntropySum /
                sfimEventCount
    }


    fun sfimMeanWriteEntropy(): Double {

        if (sfimEntropyCount == 0) {
            return 0.0
        }

        return sfimWriteEntropySum /
                sfimEntropyCount
    }


    // ========================================================
    // EAP ENTROPY
    // ========================================================

    /**
     * Mean observed Shannon entropy during this window.
     *
     * Entropy values that were unavailable are excluded.
     *
     * Normal byte entropy is approximately 0..8.
     */
    fun eapEntropy(): Double {

        if (entropyObservations.isEmpty()) {
            return 0.0
        }

        return entropyObservations.average()
    }


    /**
     * First-order entropy change.
     *
     * Calculated from consecutive real entropy observations
     * inside this 5-second window.
     *
     * Positive values indicate increasing entropy.
     */
    fun eapDHEntropy(): Double {

        if (entropyObservations.size < 2) {
            return 0.0
        }

        var sumDelta = 0.0
        var count = 0

        for (i in 1 until entropyObservations.size) {

            val previous =
                entropyObservations[i - 1]

            val current =
                entropyObservations[i]

            sumDelta +=
                current - previous

            count++
        }

        if (count == 0) {
            return 0.0
        }

        return sumDelta / count
    }


    /**
     * Second-order entropy change.
     *
     * Measures acceleration/deceleration of the entropy
     * change inside this temporal window.
     */
    fun eapD2HEntropy(): Double {

        if (entropyObservations.size < 3) {
            return 0.0
        }

        val firstDifferences =
            mutableListOf<Double>()

        for (i in 1 until entropyObservations.size) {

            firstDifferences.add(
                entropyObservations[i] -
                        entropyObservations[i - 1]
            )
        }

        if (firstDifferences.size < 2) {
            return 0.0
        }

        var sumSecondDifference = 0.0
        var count = 0

        for (i in 1 until firstDifferences.size) {

            sumSecondDifference +=
                firstDifferences[i] -
                        firstDifferences[i - 1]

            count++
        }

        if (count == 0) {
            return 0.0
        }

        return sumSecondDifference / count
    }


    /**
     * EAP behavioral score.
     *
     * This is a feature-level heuristic for the Android
     * telemetry dataset. It is NOT the final trained model.
     *
     * Higher values indicate stronger evidence of rapid
     * content transformation / destructive file activity.
     */
    fun eapScore(): Double {

        if (sfimEventCount == 0) {
            return 0.0
        }

        val entropy =
            eapEntropy()

        val positiveEntropyChange =
            max(0.0, eapDHEntropy())

        val positiveAcceleration =
            max(0.0, eapD2HEntropy())

        /*
         * Normalize entropy to approximately 0..1.
         */
        val entropySignal =
            (entropy / 8.0)
                .coerceIn(0.0, 1.0)

        /*
         * Entropy acceleration is naturally much smaller
         * than the 0..8 entropy range.
         *
         * Scale conservatively and clamp.
         */
        val changeSignal =
            (positiveEntropyChange / 2.0)
                .coerceIn(0.0, 1.0)

        val accelerationSignal =
            (positiveAcceleration / 2.0)
                .coerceIn(0.0, 1.0)

        /*
         * Activity signal:
         *
         * many writes + renames + deletes + changed files
         * indicate stronger transformation behavior.
         */
        val activity =
            (
                    eapWriteCount +
                            eapRenameCount +
                            eapUnlinkCount
                    ).toDouble()

        val activitySignal =
            (activity / 50.0)
                .coerceIn(0.0, 1.0)

        /*
         * Combine real observations.
         *
         * This is only an EAP feature score.
         * The final RANSCOPE-M ensemble remains separate.
         */
        return (
                0.40 * entropySignal +
                        0.20 * changeSignal +
                        0.10 * accelerationSignal +
                        0.30 * activitySignal
                ).coerceIn(0.0, 1.0)
    }


    // ========================================================
    // FILE COUNT
    // ========================================================

    fun uniqueFilesChanged(): Int {

        return changedPaths.size
    }


    // ========================================================
    // CAIT COUNT ACCESS
    // ========================================================

    fun getCaitCount(
        eventType: String
    ): Int {

        return caitCounts[
            eventType
        ] ?: 0
    }


    // ========================================================
    // FEATURE VECTOR
    // ========================================================

    fun toFeatureMap(): Map<String, Double> {

        val features =
            linkedMapOf<String, Double>()


        // ----------------------------------------------------
        // SFIM
        // ----------------------------------------------------

        features[
            "sfim_event_count"
        ] =
            sfimEventCount.toDouble()

        features[
            "sfim_write_count"
        ] =
            sfimWriteCount.toDouble()

        features[
            "sfim_rename_count"
        ] =
            sfimRenameCount.toDouble()

        features[
            "sfim_unlink_count"
        ] =
            sfimUnlinkCount.toDouble()

        features[
            "sfim_create_count"
        ] =
            sfimCreateCount.toDouble()

        features[
            "sfim_mean_depth"
        ] =
            sfimMeanDepth()

        features[
            "sfim_mean_dir_entropy"
        ] =
            sfimMeanDirEntropy()

        features[
            "sfim_total_bytes"
        ] =
            sfimTotalBytes.toDouble()

        features[
            "sfim_mean_write_entropy"
        ] =
            sfimMeanWriteEntropy()


        // ----------------------------------------------------
        // EAP
        // ----------------------------------------------------

        features[
            "eap_bucket_count"
        ] =
            if (sfimEventCount > 0) 1.0
            else 0.0

        features[
            "eap_write_count"
        ] =
            eapWriteCount.toDouble()

        features[
            "eap_execute_count"
        ] =
            eapExecuteCount.toDouble()

        features[
            "eap_rename_count"
        ] =
            eapRenameCount.toDouble()

        features[
            "eap_unlink_count"
        ] =
            eapUnlinkCount.toDouble()

        features[
            "eap_files_changed"
        ] =
            uniqueFilesChanged().toDouble()

        features[
            "eap_H"
        ] =
            eapEntropy()

        features[
            "eap_dH"
        ] =
            eapDHEntropy()

        features[
            "eap_d2H"
        ] =
            eapD2HEntropy()

        features[
            "eap_score"
        ] =
            eapScore()


        // ----------------------------------------------------
        // MPVG
        // ----------------------------------------------------

        features[
            "mpvg_event_count"
        ] =
            mpvgEventCount.toDouble()

        features[
            "mpvg_permission_requests"
        ] =
            mpvgPermissionRequests.toDouble()

        features[
            "mpvg_permission_grants"
        ] =
            mpvgPermissionGrants.toDouble()

        features[
            "mpvg_permission_revokes"
        ] =
            mpvgPermissionRevokes.toDouble()

        features[
            "mpvg_permission_vp"
        ] =
            mpvgPermissionVp.toDouble()

        features[
            "mpvg_permission_vp_cumulative"
        ] =
            mpvgPermissionVpCumulative.toDouble()

        features[
            "mpvg_recent_vp_10s"
        ] =
            mpvgRecentVp10s.toDouble()


        // ----------------------------------------------------
        // CAIT
        // ----------------------------------------------------

        features[
            "cait_event_count"
        ] =
            caitEventCount.toDouble()


        for ((eventType, count) in caitCounts) {

            features[
                "cait_$eventType"
            ] =
                count.toDouble()
        }


        return features
    }
}