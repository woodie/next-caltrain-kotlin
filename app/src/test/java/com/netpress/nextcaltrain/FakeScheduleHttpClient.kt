package com.netpress.nextcaltrain

import java.net.URI

// Matches huck's FakeScanHttpClient -- errors loudly if called with no handler set.
class FakeScheduleHttpClient(var getHandler: ((URI) -> ScheduleHttpResult)? = null) : ScheduleHttpClient {
    override fun get(url: URI): ScheduleHttpResult =
        getHandler?.invoke(url) ?: error("FakeScheduleHttpClient.get: no getHandler set")
}
