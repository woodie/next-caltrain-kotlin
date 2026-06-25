package com.netpress.nextcaltrain

import io.kotest.core.config.AbstractProjectConfig
import io.kotest.core.spec.SpecExecutionOrder
import io.kotest.core.test.TestCaseOrder

/**
 * Tests *within* a spec already run sequentially (declaration order) by Kotest's
 * own default. But ordering *between* spec classes (CaltrainScheduleSpec,
 * GoodTimesSpec, ScheduleSpec, etc.) is undefined by default -- Kotest's docs call
 * it "essentially random, in whatever order the discovery mechanism finds them"
 * (https://kotest.io/docs/5.9.x/framework/spec-ordering.html). That's what made a
 * full `./test.sh` run hard to diff section-by-section against the Swift sibling's
 * output: each spec's own test order was already stable, but which spec printed
 * first/second/etc. could shuffle between runs.
 *
 * Pin both explicitly so output is reproducible run over run:
 *   - specExecutionOrder: alphabetical by class name, matching the Swift sibling
 *     (xcodebuild/XCTest there runs alphabetically by default; see this repo's
 *     project.yml for the analogous `randomExecutionOrder: false` pin).
 *   - testCaseOrder: sequential/declaration order -- already Kotest's default,
 *     pinned here so it can't silently change on a future Kotest upgrade.
 */
object ProjectConfig : AbstractProjectConfig() {
    override val specExecutionOrder = SpecExecutionOrder.Lexicographic
    override val testCaseOrder = TestCaseOrder.Sequential
}
