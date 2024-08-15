package serverEnum

import m_serialization.annotations.MSerialization

@MSerialization
enum class TutorialStep {
    START,
    COMPLETE,
    COMPLETE_SAGA1,
    COMPLETE_SAGA2,
    COMPLETE_SAGA3,
    COMPLETE_SHORT
}
@MSerialization
enum class DevicePlatform {
    WEB,
    ANDROID,
    IOS,
    UNKNOWN
}