package com.ranscope.android.simulator

data class SimulatorConfig(
    val fileCount: Int = 100,
    val directoryCount: Int = 10,
    val seed: Long = System.currentTimeMillis()
)