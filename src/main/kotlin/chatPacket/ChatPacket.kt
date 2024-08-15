package chatPacket

import m_serialization.annotations.MSerialization
import serverEnum.ChatType

@MSerialization
class ChatLine(val fromUId: Int, val content: String, val emoId: Int, val chatTime: Long) {}

@MSerialization
class SendChatCmd(val chatType: ChatType, val toUId: Int, val content: String, val emoId: Int) {}

@MSerialization
class ChatHistoryCmd(val chatType: ChatType) {}


@MSerialization
class SendChatMsg(val chatType: ChatType, val fromUId: Int, val content: String, val emoId: Int) {}

@MSerialization
class ChatHistoryMsg(val chatType: ChatType, val messages: List<ChatLine>) {}

