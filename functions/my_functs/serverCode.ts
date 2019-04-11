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

const checkIfNewCodesAreNeeded = functions.database
    .ref('/codes/{codeId}/status')
    .onUpdate((change, _) => {
        const before = change.before.val();
        const after = change.after.val();

        if (before === after || (after !== CodeStatus[CodeStatus.ACTIVE] && after !== CodeStatus[CodeStatus.EXPIRED])) {
            return null;
        }

        return admin.database().ref('/codes').orderByChild('status').equalTo(CodeStatus[CodeStatus.UNUSED]).once('value')
            .then(snapshot => {
                console.log(snapshot.numChildren());
                if (!snapshot.exists() || snapshot.numChildren() < MINIMUM_NUMBER_OF_UNUSED_CODES) {
                    return addCodesToDB(NUMBER_OF_CODES_TO_ADD);
                }
                return null;
            })
            .catch(err => {
                console.log(err);
                return null;
            })
    });

module.exports = {
    checkIfNewCodesAreNeeded
}