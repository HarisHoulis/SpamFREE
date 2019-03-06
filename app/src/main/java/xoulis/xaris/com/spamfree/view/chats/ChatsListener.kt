package xoulis.xaris.com.spamfree.view.chats

import xoulis.xaris.com.spamfree.data.vo.Chat

interface ChatsListener {
    fun onChatsFetched()

    fun onChatRoomClicked(chat: Chat)
}