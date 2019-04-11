import * as functions from 'firebase-functions';
import * as admin from 'firebase-admin';

enum CodeStatus {
    UNUSED, ACTIVE, USED
}

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
            const chatId = context.params.chatId; // same as codeId
            const chatRef = admin.database().ref(`/chats/${chatId}`);

            const getSentMessagesPromise = snapshot.ref.parent.once('value');
            const getChatPromise = await chatRef.once('value');
            const chat: Chat = getChatPromise.val();
            return getSentMessagesPromise
                .then(async (sentMessagesSnapShot) => {
                    try {
                        const sentMessages = sentMessagesSnapShot.numChildren();
                        const messagesLimit = Number(chat.messages);
                        const removePromises = []

                        // If all parties have sent their entitled No. of messages, 
                        // mark the chat as 'finished' and remove it from /active_chats
                        if (sentMessages === messagesLimit * 2) {
                            // Remove active chat
                            const chatMembersSnapshot = await admin.database().ref(`/chat_members/${chatId}`).once('value');
                            const membersId: string[] = Object.keys(chatMembersSnapshot.val());
                            membersId.forEach(memberId => {
                                const removePromise = admin.database().ref(`/active_chats/${memberId}/${chatId}`).remove();
                                removePromises.push(removePromise)
                            });

                            // Change code's 'status' to 'USED"
                            const codeUsedPromise = admin.database().ref(`/codes/${chatId}/status`).set(CodeStatus[CodeStatus.USED]);

                            // Set chat as 'finished'
                            const chatFinishedPromise = chatRef.child('finished').set(true);
                            return Promise.all([removePromises, codeUsedPromise, chatFinishedPromise])
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

