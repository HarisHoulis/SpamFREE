import * as functions from 'firebase-functions';
import * as admin from 'firebase-admin';
admin.initializeApp();

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

/* HTTP method to populate the DB with codes. */
export const storeCodesToDb = functions.region('europe-west1')
    .https
    .onRequest((request, response) => {
        const amountOfCodes = 50;
        const lowerBound = 10000;
        const upperBound = 99999;

        const codeIds = [];
        const codesMap = {};
        let ts = new Date().valueOf();
        while (codeIds.length < amountOfCodes) {
            const id = (Math.floor(Math.random() * (upperBound - lowerBound + 1)) + lowerBound).toString();
            if (codeIds.indexOf(id) === -1) {
                codeIds.push(id);
                const code = new Code(id.toString(), ts);
                codesMap[id] = code;
            }
            ts++;
        }

        admin.database().ref('/codes').set(codesMap)
            .then(_ => {
                response.send('success');
            })
            .catch(_ => {
                response.status(500).send()
            })
    });