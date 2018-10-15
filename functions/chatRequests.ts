/* Helper method to check if a request's code is valid. */
const checkCodeValidity = async function (request: any): Promise<[number, Code]> {
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
        if (receiverId === "" || receiverId === senderId || code.used || code.hasActiveRequest) {
            requestResponseMessage = 1; // Invalid code
            return [requestResponseMessage, code];
        }

        // Check if there is an active chat between sender and receiver 
        let hasActiveChat = false;
        const userChatsSnapshot = await admin.database().ref(`/user_chats/${senderId}/${codeId}`).once('value');
        hasActiveChat = userChatsSnapshot.exists();
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

        // Promise to change 'hasActiveRequest' property of the code at /codes & /client_codes
        const activeReqPromise1 = admin.database().ref(`/codes/${codeId}/hasActiveRequest`).set(true);
        const activeReqPromise2 = admin.database().ref(`/client_codes/${receiverId}/${codeId}/hasActiveRequest`).set(true);
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
        return Promise.all([activeReqPromise1, activeReqPromise2, addToOutgoingRequestsPromise, addToIncomingRequestsPromise, removeFromUncheckedRequestsPromise,
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
export const handleChatRequest = functions.region('europe-west1')
    .database.ref('/unchecked_outgoing_requests/{uid}/{rid}')
    .onWrite((change, context) => {
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
export const handleRequestStatusChange = functions.region('europe-west1')
    .database.ref('/incoming_requests/{uid}/{rid}')
    .onDelete(async (snapshot) => {
        try {
            const request = snapshot.val();
            const codeId = request.codeId;
            const senderId = request.senderId;

            if (request.status === 'ACCEPTED') {
                // Remove request from /outgoing_requests
                const removeOutgoingReq = admin.database().ref(`/outgoing_req/${senderId}/${codeId}`).remove();

                // Set code's property 'used' to true
                const setCodeUsed = admin.database().ref(`/codes/${codeId}/used`).set(true);
                const setClientCodeUsed = admin.database().ref(`/client_codes/${request.receiverId}/${codeId}/used`).set(true)
                return Promise.all([removeOutgoingReq, setCodeUsed, setClientCodeUsed]);
            } else if (request.status === 'REJECTED') {
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