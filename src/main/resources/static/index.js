function base64ToArrayBuffer(base64) {
    var binary = window.atob(base64);
    var len = binary.length;
    var bytes = new Uint8Array(len);
    for (var i = 0; i < len; i++) {
        bytes[i] = binary.charCodeAt(i);
    }
    return bytes.buffer;
}


function arrayBufferToBase64(buffer) {
    var binary = '';
    var bytes = new Uint8Array(buffer);
    var len = bytes.byteLength;
    for (var i = 0; i < len; i++) {
        binary += String.fromCharCode(bytes[i]);
    }
    return window.btoa(binary);
}

function onRegister() {
    axios.get('/attestation/options', { params: { email: document.getElementById('usernameRegister').value } })
        .then(function (res) {
            var options = Object.assign({}, res.data);
            options.challenge = base64ToArrayBuffer(options.challenge.value);
            options.user.id = base64ToArrayBuffer(options.user.id);
            options.excludeCredentials.map(function (c) {
                return Object.assign({}, c, { id: base64ToArrayBuffer(c.id) });
            });

            console.log(options);

            return navigator.credentials.create({ publicKey: options });
        })
        .then(function (assertion) {
            console.log(assertion);

            var body = {
                clientDataJSON: arrayBufferToBase64(assertion.response.clientDataJSON),
                attestationObject: arrayBufferToBase64(assertion.response.attestationObject)
            };

            return axios.post('/attestation/result', body);
        })
        .then(function (res) {
            console.log(res);
            alert('Registered')
        })
        .catch(function (err) {
            alert(err)
        });
}

function onLogin() {
    axios.get('/assertion/options', { params: { email: document.getElementById('usernameLogin').value } })
        .then(function (res) {
            var options = Object.assign({}, res.data);
            options.challenge = base64ToArrayBuffer(res.data.challenge.value);
            options.allowCredentials =
                    options.allowCredentials.map(function (c) {
                        return Object.assign({}, c, { id: base64ToArrayBuffer(c.id) }) });

            return navigator.credentials.get({ publicKey: options })
        })
        .then(function (assertion) {
            console.log(assertion);

            var body = {
                credentialId: arrayBufferToBase64(assertion.rawId),
                clientDataJSON: arrayBufferToBase64(assertion.response.clientDataJSON),
                authenticatorData: arrayBufferToBase64(assertion.response.authenticatorData),
                signature: arrayBufferToBase64(assertion.response.signature),
                userHandle: arrayBufferToBase64(assertion.response.userHandle),
            };

            return axios.post('/assertion/result', body);
        })
        .then(function (res) {
            console.log(res);
            alert('Logined')
        })
        .catch(function (err) {
            alert(err)
        })
}