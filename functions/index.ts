import * as fs from 'fs';
import * as path from 'path';
import * as functions from 'firebase-functions';
import * as admin from 'firebase-admin';
admin.initializeApp(functions.config().firebase);

const FUNCTIONS_FOLDER = './my_functs';
fs.readdirSync(path.resolve(__dirname, FUNCTIONS_FOLDER)).forEach(file => {
    if (file.endsWith('.js')) {
        const fileBaseName = file.slice(0, -3); // Remove the '.js' extension
        const thisFunction = require(`${FUNCTIONS_FOLDER}/${fileBaseName}`);
        for (const i in thisFunction) {
            exports[i] = thisFunction[i];
        }
    }
});
