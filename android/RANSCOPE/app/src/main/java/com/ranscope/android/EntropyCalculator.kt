package com.ranscope.android

import java.io.File
import java.io.FileInputStream
import kotlin.math.log2

object EntropyCalculator {

    /*
     * Maximum number of bytes inspected from one file.
     *
     * Files smaller than this are read completely.
     * Larger files are sampled from the beginning.
     */
    private const val MAX_SAMPLE_BYTES = 1_048_576L // 1 MB


    /**
     * Calculates Shannon entropy of the bytes in a file.
     *
     * Entropy range:
     *
     * 0.0 -> very uniform / repetitive data
     * 8.0 -> highly random-looking byte distribution
     *
     * Returns null if the file cannot be read.
     */
    fun calculateFileEntropy(
        file: File
    ): Double? {

        // File must exist.
        if (!file.exists()) {
            return null
        }

        // We only calculate entropy for regular files.
        if (!file.isFile) {
            return null
        }

        // File must be readable.
        if (!file.canRead()) {
            return null
        }

        return try {

            /*
             * There are 256 possible byte values:
             *
             * 0 through 255
             */
            val frequency =
                LongArray(256)

            var totalBytes =
                0L


            /*
             * Open the file safely.
             */
            FileInputStream(file).use { input ->

                val buffer =
                    ByteArray(8192)


                /*
                 * Read at most MAX_SAMPLE_BYTES.
                 */
                while (
                    totalBytes <
                    MAX_SAMPLE_BYTES
                ) {

                    val remaining =
                        MAX_SAMPLE_BYTES -
                                totalBytes


                    val bytesToRead =
                        minOf(
                            buffer.size.toLong(),
                            remaining
                        ).toInt()


                    val bytesRead =
                        input.read(
                            buffer,
                            0,
                            bytesToRead
                        )


                    /*
                     * End of file.
                     */
                    if (bytesRead <= 0) {
                        break
                    }


                    /*
                     * Count every byte value.
                     */
                    for (
                    i in 0 until bytesRead
                    ) {

                        val byteValue =
                            buffer[i]
                                .toInt() and 0xFF


                        frequency[byteValue]++
                    }


                    totalBytes +=
                        bytesRead
                }
            }


            /*
             * Empty files have no useful
             * entropy measurement.
             */
            if (totalBytes == 0L) {
                return null
            }


            /*
             * Shannon entropy:
             *
             * H = -Σ p(x) log2(p(x))
             */
            var entropy =
                0.0


            for (count in frequency) {

                if (count == 0L) {
                    continue
                }


                val probability =
                    count.toDouble() /
                            totalBytes.toDouble()


                entropy -=
                    probability *
                            log2(probability)
            }


            entropy

        } catch (e: Exception) {

            /*
             * If the file disappears or becomes
             * inaccessible while being monitored,
             * simply return no measurement.
             */
            null
        }
    }
}