import * as functions from 'firebase-functions';
import * as admin from 'firebase-admin';

/* HTTP function called from the client (Android app) when a user changes their profile picture.
* It is responsible for the user's profile image consistency amongst their chat(s).
 */
exports.updateUserImageInChats = functions.region('europe-west1')
    .https
    .onCall(async (data, context) => {
        if (!context.auth) {
            throw new functions.https.HttpsError('failed-precondition', 'The function must be called ' +
                'while authenticated.');
        }

        const imageUrl: string = 'default';
        const uid = context.auth.uid;

        // Get user codes
        const userCodesSnap = await admin.database().ref(`/client_codes/${uid}/`).once('value');
        const userCodes = Object.keys(userCodesSnap.val());

        // Get user's chats' IDs
        const updateImagePromises = [];
        const chatsRef = admin.database().ref('/chats');
        await admin.database().ref(`/user_chats/${uid}`)
            .once('value', snap => {
                snap.forEach((chatSnap) => {
                    const chatId = chatSnap.key;
                    let property: string;

                    // Decide if it's user or member's image that needs update
                    if (userCodes.indexOf(chatId) !== -1) {
                        property = 'ownerImage';
                    } else {
                        property = 'memberImage';
                    }

                    // Create and add the promise to the array
                    const updateImagePromise = chatsRef.child(chatId).child(property).set(imageUrl);
                    updateImagePromises.push(updateImagePromise);
                    return true;
                })
            });
        return Promise.all(updateImagePromises);
    });