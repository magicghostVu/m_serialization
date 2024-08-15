package basicPacket

import m_serialization.annotations.MSerialization
import serverEnum.LeaveRoomReason

@MSerialization
class QuickMatchingCmd(val maxPlayer: Int, val gameConfigVersion: Int) {
}

@MSerialization
class JoinTable(val tableId: Int, val gameConfigVersion: Int) {
}

@MSerialization
class LeaveRoomMsg(val uid: Int, val leaveReason: LeaveRoomReason) {}

@MSerialization
class ReceivedInviteJoin(val inviterId: Int,val inviterName:String, val tableId: Int, val maxPlayer: Int, val numUserJoined: Int) {}