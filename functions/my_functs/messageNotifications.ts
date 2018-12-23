import * as functions from 'firebase-functions';
import * as admin from 'firebase-admin';

class ChatMessage {
    public chatId: string = "";
    public senderId: string = "";
    public senderName: string = "";
    public senderImage: string = "";
    public message: string = "";
    public timestamp: undefined;
}

/* Method responsible to deliver notifications to a chat's message receiver. 
	TRIGGERED when a new message is added at /messages/{chatId}.
*/
const sendNewMessageNotification = functions
    .database.ref('/messages/{chatId}/{messageId}')
    .onCreate(async (snapshot, context) => {
        const newMessage: ChatMessage = snapshot.val();
        const senderName = newMessage.senderName;
        const senderImage = newMessage.senderImage;
        const senderId = newMessage.senderId;
        const messageBody = newMessage.message;
        const chatId = newMessage.chatId;

        // Find receiver's ID
        const chatMembersSnapshot = await admin.database().ref(`/chat_members/${chatId}`).once('value');
        const membersId: string[] = Object.keys(chatMembersSnapshot.val());
        let receiverId: string;
        membersId.forEach(memberId => {
            if (memberId !== senderId) {
                receiverId = memberId;
                return;
            }
        });

        // Get receiver's device tokens
        const receiverDeviceTokensSnapshot = await admin.database().ref(`/users/${receiverId}/deviceTokens`).once('value');
        const tokens = Object.keys(receiverDeviceTokensSnapshot.val())

        const payload = {
            notification: {
                title: senderName,
                body: messageBody,
                icon: senderImage,
                tag: chatId
            },
            data: {
                'messageChatId': chatId
            }
        };

        const options = {
            priority: 'high',
        };

        return admin.messaging().sendToDevice(tokens, payload, options);
    });

module.exports = {
    sendNewMessageNotification
}