'use strict';

/***
 * Exported functions to be used in the testing scripts.
 */
module.exports = {
    uploadRandomizedUser,
    uploadRandomizedUpdateData
};

const fs = require('fs'); // Needed for access to blobs.

// All endpoints starting with the following prefixes will be aggregated in the same for the statistics
var statsPrefix = [["/rest/media/", "GET"],
    ["/rest/media", "POST"]
]

// Function used to compress statistics
global.myProcessEndpoint = function (str, method) {
    var i = 0;
    for (i = 0; i < statsPrefix.length; i++) {
        if (str.startsWith(statsPrefix[i][0]) && method == statsPrefix[i][1])
            return method + ":" + statsPrefix[i][0];
    }
    return method + ":" + str;
}

// Returns a random username constructed from lowercase letters.
function randomUsername(char_limit) {
    const letters = 'abcdefghijklmnopqrstuvwxyz';
    let username = 'user';
    let num_chars = Math.floor(Math.random() * char_limit + 5); // Random number of characters between 5 and (5 + char_limit)
    for (let i = 0; i < num_chars; i++) {
        username += letters[Math.floor(Math.random() * letters.length)];
    }
    return username;
}

// Returns a random password, drawn from printable ASCII characters
function randomPassword(pass_len) {
    const skip_value = 33;
    const lim_values = 94;

    let password = '';
    for (let i = 0; i < pass_len; i++) {
        let chosen_char = Math.floor(Math.random() * lim_values) + skip_value;
        if (chosen_char == "'" || chosen_char == '"')
            i -= 1;
        else
            password += chosen_char
    }
    return password;
}

/**
 * Register a random user.
 */
function uploadRandomizedUser(requestParams, context, ee, next) {
    let username = randomUsername(13);
    let password = randomPassword(15);
    let email = username + "@campus.fct.unl.pt";

    if (context.vars === undefined) {
        context.vars = {};
    }

    const user = {
        id: username,
        pwd: password,
        email: email,
        displayName: username
    };
    requestParams.body = JSON.stringify(user);

    context.vars.userId = username;
    context.vars.pwd = password;

    return next();
}

/**
 * Upload randomized data for updating a user.
 */
function uploadRandomizedUpdateData(requestParams, context, ee, next) {
    let newDisplayName = randomUsername(10);
    const updateData = {
        displayName: newDisplayName
    };
    requestParams.body = JSON.stringify(updateData);
    return next();
}
