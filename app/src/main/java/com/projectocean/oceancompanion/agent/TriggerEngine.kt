package com.projectocean.oceancompanion.agent

import java.time.LocalTime

class TriggerEngine {
    fun evaluate(studyMinutes: Int, now: LocalTime = LocalTime.now()): Trigger? {
        return when {
            studyMinutes >= 120 -> Trigger.RestReminder
            now.hour >= 23 -> Trigger.LateNightReading
            else -> null
        }
    }
}

enum class Trigger(val message: String) {
    RestReminder("\u4f60\u5df2\u7ecf\u5b66\u4e60\u4e00\u6bb5\u65f6\u95f4\u4e86\uff0c\u8981\u4e0d\u8981\u77ed\u6682\u4f11\u606f\u4e00\u4e0b\uff1f"),
    LateNightReading("\u73b0\u5728\u6709\u4e9b\u665a\u4e86\u3002\u9700\u8981 Ocean \u5e2e\u4f60\u5148\u603b\u7ed3\u4e00\u4e0b\u518d\u4f11\u606f\u5417\uff1f")
}
