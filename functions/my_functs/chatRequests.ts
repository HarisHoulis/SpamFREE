import * as functions from 'firebase-functions';
import * as admin from 'firebase-admin';

enum RequestStatus {
    PENDING, ACCEPTED, REJECTED
}

enum CodeStatus {
    UNUSED, ACTIVE, USED
}

class Code {
    public id: string;
    public messages: string = '5';
    public assignedUid: string = "";
    public hasActiveRequest: boolean = false;
    public timestamp: number;
    public status: CodeStatus = CodeStatus.UNUSED;

    constructor(id: string, timestamp: number) {
        this.id = id;
        this.timestamp = timestamp;
    }
}

class ChatRequest {
    public codeId: String = "";
    public senderId: String = "";
    public receiverId: String = "";
    public senderName: String = "";
    public receiverName: String = "";
    public senderImage: String = "";
    public receiverImage: String = "";
    public senderToken: String = "";
    public timestamp: undefined;
    public incoming: Boolean = false;
    public status: RequestStatus = RequestStatus.PENDING;
    public messages: String = ""
}

/* Helper method to check if a request's code is valid. */
const checkCodeValidity = async function (request: ChatRequest): Promise<[number, Code]> {
    try {
        const codeId = request.codeId;
        let requestResponseMessage = -1;

        // Check if code exists
        const codeSnapshot = await admin.database().ref(`/codes/${codeId}`).once('value');
        if (!codeSnapshot.exists() || !codeSnapshot.hasChildren()) {
            requestResponseMessage = 0; // Code doesn't exist
            return [requestResponseMessage, null];
        }

        // Get the code
        const code = codeSnapshot.val();
        // Check if code is valid
        const senderId = request.senderId;
        const receiverId = code.assignedUid;
        if (receiverId === "" || receiverId === senderId || code.status !== CodeStatus[CodeStatus.UNUSED] || code.hasActiveRequest) {
            requestResponseMessage = 1; // Invalid code
            return [requestResponseMessage, code];
        }

        // Check if there is an active chat between sender and receiver 
        const senderActiveChatsSnap = await admin.database().ref(`/active_chats/${senderId}`).once('value');
        const receiverActiveChatsSnap = await admin.database().ref(`/active_chats/${receiverId}`).once('value');
        let hasActiveChat = false;
        if (senderActiveChatsSnap.exists() && receiverActiveChatsSnap.exists()) {
            const senderActivChats: string[] = Object.keys(senderActiveChatsSnap.val());
            const receiverActiveChats: string[] = Object.keys(receiverActiveChatsSnap.val());

            hasActiveChat = senderActivChats.some((v) => {
                return receiverActiveChats.indexOf(v) >= 0
            })
        }
        if (hasActiveChat) {
            requestResponseMessage = 3; // Existing chat
        }
        return [requestResponseMessage, code];
    } catch (err) {
        console.log(err);
        return null;
    }
}

/* Helper method that handled the creation of a new request.
*  It also notifies both users (sender and receiver of the request) about the result.
*/
const handleNewChatRequest = async function (snapshot: functions.database.DataSnapshot) {
    try {
        const request = snapshot.val();
        const codeCheckResult = await checkCodeValidity(request);
        if (codeCheckResult === null) {
            return null;
        }

        const requestResponseCode = codeCheckResult[0];
        const code = codeCheckResult[1];
        // If code can't be used, send response to sender
        if (requestResponseCode !== -1) {
            const requestpayload = {
                data: {
                    reqResp: requestResponseCode.toString()
                }
            }
            const token = [request.senderToken];
            const removeOutgointReqPromise = snapshot.ref.remove();
            const failedReqResponsePromise = admin.messaging().sendToDevice(token, requestpayload, { priority: 'high' });
            return Promise.all([removeOutgointReqPromise, failedReqResponsePromise])
        }
        const receiverId = code.assignedUid;
        const codeId = code.id;

        // Get snapshot of receiver
        const receiverSnapshot = await admin.database().ref(`users/${receiverId}`).once('value');
        const receiverName = receiverSnapshot.val().name;
        const receiverImage = receiverSnapshot.val().image;
        const tokens: string[] = Object.keys(receiverSnapshot.val().deviceTokens)

        if (tokens === undefined || tokens.length === 0) {
            console.log('No device tokens found!');
            return null;
        }

        // Get sender (member) image
        const senderImageSnapshot = await admin.database().ref(`/users/${request.senderId}/image`).once('value');
        const senderImage = senderImageSnapshot.val();

        // Update request's properties
        request.receiverId = receiverId;
        request.receiverName = receiverName;
        request.receiverImage = receiverImage;
        request.senderImage = senderImage;
        request.messages = code.messages;

        // Promise to change 'hasActiveRequest' property of the code at /codes
        const activeReqPromise = admin.database().ref(`/codes/${codeId}/hasActiveRequest`).set(true);
        //admin.database().ref(`/client_codes/${receiverId}/${codeId}`).set(true);

        // Promise to add request to /outgoing_requests
        const addToOutgoingRequestsPromise = admin.database().ref(`/outgoing_requests/${request.senderId}/${codeId}`).set(request); // codeId == requestId
        // Promise to add request to /incoming_requests
        request.incoming = true;
        const addToIncomingRequestsPromise = admin.database().ref(`/incoming_requests/${receiverId}/${codeId}`).set(request); // codeId == requestId
        // Promise to remove request from /unchecked_outgoing_requests
        const removeFromUncheckedRequestsPromise = snapshot.ref.remove();

        // Payload for request's receiver
        const receiverPayload = {
            notification: {
                tile: 'You have a new request!',
                body: `${request.senderName} sent you a chat request.`
            },
        };
        // Payload for request's sender
        const requestPayload = {
            notification: {
                tile: 'Your request was sent',
                body: `You succesfuly sent a request to ${request.receiverName}`
            },
            data: {
                reqResp: '4'
            }
        };

        // Notification's promises
        const receiverNotificationPromise = admin.messaging().sendToDevice(tokens, receiverPayload);
        const senderNotificationPromise = admin.messaging().sendToDevice(request.senderToken, requestPayload)

        // Execute all promises...
        return Promise.all([activeReqPromise, addToOutgoingRequestsPromise, addToIncomingRequestsPromise, removeFromUncheckedRequestsPromise,
            receiverNotificationPromise, senderNotificationPromise])
            .then((promisesResult) => {
                // For each message check if there was an error.
                const receiverNotificationResponse = promisesResult[5];
                const tokensToRemove = [];
                // Cleanup the tokens who are not registered anymore.
                receiverNotificationResponse.results.forEach((result, index) => {
                    const error = result.error;
                    if (error) {
                        if (error.code === 'messaging/invalid-registration-token' ||
                            error.code === 'messaging/registration-token-not-registered') {
                            tokensToRemove.push(receiverSnapshot.ref.child('deviceTokens').child(tokens[index]).remove());
                        }
                    }
                });
                return Promise.all(tokensToRemove);
            });
    } catch (err) {
        console.log(err);
        return err;
    }
}

/* Method that handles all UNCHECKED requests sent by a user.
*  TRIGGERED when a new request is added at /unchecked_outgoing_requests
*/
const handleChatRequest = functions
    .database.ref('/unchecked_outgoing_requests/{uid}/{rid}')
    .onWrite((change, _) => {
        const before = change.before;
        const after = change.after;

        // Newly created request
        if (!before.exists()) {
            return handleNewChatRequest(after);
        } else if (before.exists() && after.exists()) { // Rquest already exists
            const request = after.val();
            const payload = {
                data: {
                    reqResp: '2' // Existing request
                }
            }
            return admin.messaging().sendToDevice([request.senderToken], payload);
        } else { // Request deleted
            return null;
        }
    });


/* Method that handles the result of a request (ACCEPTED or REJECTED).
*  TRIGGERED when the receiver of the request either ACCEPTS it or REJECTS it.
*/
const handleRequestStatusChange = functions
    .database.ref('/incoming_requests/{uid}/{rid}')
    .onDelete(async (snapshot) => {
        try {
            const request: ChatRequest = snapshot.val();
            const codeId = request.codeId;
            const senderId = request.senderId;
            const receiverId = request.receiverId; // owner of the code
            const statusString = request.status.toString();

            if (statusString === RequestStatus[RequestStatus.ACCEPTED]) {
                console.log(senderId);
                // Remove request from /outgoing_requests
                const removeOutgoingReq = admin.database().ref(`/outgoing_requests/${senderId}/${codeId}`).remove();

                // Set code's property 'status' to 'ACTIVE'
                const setCodeActive = admin.database().ref(`/codes/${codeId}/status`).set(CodeStatus[CodeStatus.ACTIVE]);

                // Add active chats for both users
                const setSenderActiveChat = admin.database().ref(`/active_chats/${senderId}/${codeId}`).set(true);
                const setReceiverActiveChat = admin.database().ref(`/active_chats/${receiverId}/${codeId}`).set(true);

                return Promise.all([removeOutgoingReq, setCodeActive,
                    setSenderActiveChat, setReceiverActiveChat]);
            } else if (statusString === RequestStatus[RequestStatus.REJECTED]) {
                // Revert hasActiveRequest property of code
                const hasActiveReqPromise = admin.database().ref(`/codes/${codeId}/hasActiveRequest`).set(false);

                // Remove request from /outgoing_requests
                const removeOutReqPromise = admin.database().ref(`/outgoing_requests/${senderId}/${codeId}`).remove();
                return Promise.all([hasActiveReqPromise, removeOutReqPromise])
            }
        } catch (err) {
            console.log(err);
            return err;
        }
    });

module.exports = {
    handleChatRequest,
    handleRequestStatusChange
}