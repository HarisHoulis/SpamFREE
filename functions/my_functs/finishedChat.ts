import * as functions from 'firebase-functions';
import * as admin from 'firebase-admin';

class Chat {
    public codeId: string = "";
    public ownerId: string = "";
    public ownerImage: string = "";
    public memberImage: string = "";
    public ownerName: string = "";
    public memberName: string = "";
    public lastMessage: string = "";
    public messages: string = "";
    public finished: boolean = false;
}

const checkForFinishedChat = functions.database
    .ref('/messages/{chatId}/{messageId}')
    .onCreate(async (snapshot, context) => {
        try {
            const chatId = context.params.chatId;
            const chatRef = admin.database().ref(`/chats/${chatId}`);

            const getSentMessagesPromise = snapshot.ref.parent.once('value');
            const getChatPromise = await chatRef.once('value');
            const chat: Chat = getChatPromise.val();
            return getSentMessagesPromise
                .then(async (sentMessagesSnapShot) => {
                    try {
                        const sentMessages = sentMessagesSnapShot.numChildren();
                        const messagesLimit = Number(chat.messages);

                        // If all parties have sent their entitled No. of messages, 
                        // mark the chat as 'finished' and remov it from /active_chats
                        if (sentMessages === messagesLimit * 2) {
                            // Remove active chat
                            const chatMembersSnapshot = await admin.database().ref(`/chat_members/${chatId}`).once('value');
                            const membersId: string[] = Object.keys(chatMembersSnapshot.val());
                            membersId.forEach(memberId => {
                                admin.database().ref(`/active_chats/${memberId}/${chatId}`).remove();
                            });

                            // Set chat as 'finished'
                            return chatRef.child('finished').set(true);
                        }
                        return null;
                    } catch (err) {
                        console.log(err);
                        return null;
                    }
                })
                .catch((err) => {
                    console.log(err);
                })
        } catch (err) {
            console.log(err);
        }
    });

module.exports = {
    checkForFinishedChat
}

