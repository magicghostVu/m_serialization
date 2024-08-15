package serverEnum

import m_serialization.annotations.MSerialization

@MSerialization
enum class ChatType {
    PRIVATE_CHAT,
    ROOM_CHAT,
    SERVER_CHAT
}

