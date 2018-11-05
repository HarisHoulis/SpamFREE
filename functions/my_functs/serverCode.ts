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

const MINIMUM_NUMBER_OF_UNUSED_CODES = 10;
const NUMBER_OF_CODES_TO_ADD = 50;

/* Helper method to populate the DB with codes. */
const addCodesToDB = function (numOfCodes: number): Promise<void> {
    const lowerBound = 10000;
    const upperBound = 99999;

    const codeIds = [];
    const codesMap = {};
    let ts = new Date().valueOf();
    while (codeIds.length < numOfCodes) {
        const id = (Math.floor(Math.random() * (upperBound - lowerBound + 1)) + lowerBound).toString();
        if (codeIds.indexOf(id) === -1) {
            codeIds.push(id);
            const code = new Code(id.toString(), ts);
            codesMap[id] = code;
        }
        ts++;
    }
    return admin.database().ref('/codes').update(codesMap);
};

const checkIfNewCodesAreNeeded = functions.database.ref('/codes/{codeId}/used')
    .onUpdate((change, context) => {
        const before: boolean = change.before.val();
        const after: boolean = change.after.val();

        if (before === after) {
            return null;
        }

        if (after === true) {
            admin.database().ref('/codes').orderByChild('value').equalTo(false).once('value')
                .then(snapshot => {
                    if (!snapshot.exists() || snapshot.numChildren() < MINIMUM_NUMBER_OF_UNUSED_CODES) {
                        return addCodesToDB(NUMBER_OF_CODES_TO_ADD);
                    }
                    return null;
                })
        }
    });

module.exports = {
    checkIfNewCodesAreNeeded
}