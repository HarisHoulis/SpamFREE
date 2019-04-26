import * as functions from 'firebase-functions';
import * as admin from 'firebase-admin';

enum CodeStatus {
    UNUSED, ACTIVE, USED, EXPIRED
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

/* Helper method to assign code to users */
const assignCodeToUser = async function (uid: string) {
    try {
        const getUnassignedCodeQuery = admin.database().ref('/codes').orderByChild("assignedUid").equalTo("").limitToFirst(1);
        const codeSnapshot = await getUnassignedCodeQuery.once('value');
        // Assign code to user
        const codeKey = Object.keys(codeSnapshot.val())[0];
        const assignPromise = codeSnapshot.ref.child(codeKey).update({
            assignedUid: uid,
            timestamp: admin.database.ServerValue.TIMESTAMP
        });
        // Add code index to '/user_codes'
        const indexPromise = admin.database().ref(`/user_codes/${uid}/${codeKey}`).set(true);
        return Promise.all([assignPromise, indexPromise]);
    }
    catch (err) {
        console.log(err);
        return err;
    }
};

/* HTTP function called from the client (Android app), in order to assign new code to a user */
const requestNewCode = functions.region('europe-west1').https.onCall((data, context) => {
    if (!context.auth) {
        throw new functions.https.HttpsError('failed-precondition', 'The function must be called ' +
            'while authenticated.');
    }

    const uid = context.auth.uid;
    return assignCodeToUser(uid)
        .catch(error => {
            throw new functions.https.HttpsError('unknown', error.message, error);
        });
});

/* Assign code to newly created user.
*  TRIGGERED when a new user is added to /users.
*/
const assignCodeToNewUser = functions
    .database.ref('/users/{uid}')
    .onCreate((_, context) => {
        const uid = context.params.uid;
        return assignCodeToUser(uid);
    });

/* Assign new code to existing user
*  TRIGGERED when a property 'status' of a code, changes
*/
const assignCodeToExistingUser = functions
    .database.ref('/codes/{codeId}')
    .onUpdate(async (change, context) => {
        const before = change.before.val();
        const after = change.after.val();
        const statusBefore = change.before.child('status').val();
        const statusAfter = change.after.child('status').val();

        const codeId = context.params.codeId;
        const uid = change.after.child('assignedUid').val();

        if (before === after || statusBefore === statusAfter || 
            (statusAfter !== CodeStatus[CodeStatus.USED] && statusAfter !== CodeStatus[CodeStatus.EXPIRED])) {
            return null;
        }

        let finishdChatPromise: Promise<void>
        if (statusAfter === CodeStatus[CodeStatus.EXPIRED]) {
            const codeSnapshot = await admin.database().ref(`/chats/${codeId}`).once('value');
            if (codeSnapshot.exists()) {
                finishdChatPromise = codeSnapshot.child('finished').ref.set(true);
            }
        }
        return Promise.all([finishdChatPromise, assignCodeToUser(uid)]);
    });

module.exports = {
    assignCodeToNewUser,
    assignCodeToExistingUser,
    requestNewCode
}