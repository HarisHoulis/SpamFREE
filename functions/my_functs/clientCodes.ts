import * as functions from 'firebase-functions';
import * as admin from 'firebase-admin';

class Code {
    public id: string;
    public used: boolean = false;
    public messages: string = '5';
    public assignedUid: string = "";
    public hasActiveRequest: boolean = false;
    public timestamp: number;

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

        // Get the code
        let code: Code;
        codeSnapshot.forEach((child) => {
            code = child.val();
            return true;
        })

        // Update code's timestamp
        code.timestamp = admin.database.ServerValue.TIMESTAMP;

        const assignToUserPromise = codeSnapshot.child(`${code.id}/assignedUid`).ref.set(uid);
        const addToClientCodesPromise = admin.database().ref(`/client_codes/${uid}/${code.id}`).set(code);
        return Promise.all([assignToUserPromise, addToClientCodesPromise]);
    }
    catch (err) {
        console.log(err);
        return err;
    }
}

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
})

/* Assign code to newly created user.
*  TRIGGERED when a new user is added to /users.
*/
const assignCodeToNewUser = functions
    .database.ref('/users/{uid}')
    .onCreate(async (_, context) => {
        const uid = context.params.uid;
        return assignCodeToUser(uid);
    })

/* Assign new code to existing user
*  TRIGGERED when a user's code is marked 'used = true'
*/
const assignCodeToExistingUser = functions
    .database.ref('/client_codes/{uid}/{codeId}')
    .onUpdate(async (change, context) => {
        const before = change.before.val();
        const after = change.after.val();

        if (!change.before.exists || before === after || before.used === after.used) {
            return null;
        }

        if (after.used === true) {
            const uid = context.params.uid;
            return assignCodeToUser(uid);
        }
        return null;
    });

module.exports = {
    assignCodeToNewUser,
    assignCodeToExistingUser,
    requestNewCode
}