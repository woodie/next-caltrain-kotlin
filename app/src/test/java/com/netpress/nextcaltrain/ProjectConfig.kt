package com.netpress.nextcaltrain

import com.netpress.justbeforeeach.JustBeforeEachExtension
import io.kotest.core.config.AbstractProjectConfig
import io.kotest.core.spec.SpecExecutionOrder
import io.kotest.core.test.TestCaseOrder

// Pins spec/test execution order so full-suite output is reproducible; see docs/COMMENTS.md.
object ProjectConfig : AbstractProjectConfig() {
    override val specExecutionOrder = SpecExecutionOrder.Lexicographic
    override val testCaseOrder = TestCaseOrder.Sequential

    // Without this, justBeforeEach (GoodTimesSpec.kt) is a silent no-op --
    // see just-before-each's own README "Setup".
    override fun extensions() = listOf(JustBeforeEachExtension)
}
