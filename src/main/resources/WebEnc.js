
/* global sjcl, ArrayBuffer */

var WebEnc = WebEnc || {};

(function (WebEnc) {

    var saltSize = 8;
    var ivSize = 16;
    var passCookieName = "passPhrase";

    WebEnc.config = WebEnc.config || {};
    WebEnc.config.passPhrase = null;

    /**
     * Inits WebEnc and if desired scans the current page for tags
     * @param {boolean} processHTML
     * @returns {undefined}
     */
    WebEnc.init = function (processHTML) {
        // unlock with cookie
        var pass = _readCookie(passCookieName);
        if (pass !== null) {
            if (WebEnc.testPassPhrase(pass) && processHTML) {
                WebEnc.processHTML();
            }
        }
    };

    /**
     * Is the password correct ?
     * @returns {Boolean}
     */
    WebEnc.isUnlocked = function () {
        return WebEnc.config.passPhrase !== null;
    };

    /**
     * Test the password and sets a cookie with the password
     * if it is valid.
     * @param {type} pass
     * @param {Boolean} setCookie
     * @returns {Boolean}
     */
    WebEnc.testPassPhrase = function (pass, setCookie) {
        if (WebEnc.config.passPhrase === pass ||
                (WebEnc.config.passPhrase !== null && (pass === null || typeof pass === 'undefined'))) {
            // already unlocked
            return true;
        }

        WebEnc.config.passPhrase = pass;
        var testData = sjcl.codec.base64.toBits(WebEnc.config.testData);
        try {
            testData = WebEnc.decrypt(testData);
            var hash = sjcl.codec.base64.fromBits(sjcl.hash.sha256.hash(testData));
            if (hash !== WebEnc.config.testHash) {
                WebEnc.config.passPhrase = null;
            }
        } catch (ex) {
            WebEnc.config.passPhrase = null;
        }
        if (WebEnc.config.passPhrase !== null) {
            if (typeof setCookie !== 'undefined' && setCookie) {
                _createCookie(passCookieName, pass, 30);
            }
            return true;
        }
        return false;
    };

    /**
     * Decrypts and ArrayBuffer or a bitArray and returns the decrypted data
     * @param {bitArray or ArrayBuffer} input
     * @returns {bitArray}
     */
    WebEnc.decrypt = function (input) {
        var salt, iv, enc;

        if (input instanceof ArrayBuffer) {
            salt = _convert(input, 0, saltSize);
            iv = _convert(input, saltSize, ivSize);
            enc = _convert(input, ivSize + saltSize, input.byteLength - ivSize - saltSize);
        } else {
            salt = sjcl.bitArray.bitSlice(input, 0, saltSize * 8);
            iv = sjcl.bitArray.bitSlice(input, saltSize * 8, saltSize * 8 + ivSize * 8);
            enc = sjcl.bitArray.bitSlice(input, saltSize * 8 + ivSize * 8);
        }
        // init key and aes
        var key = sjcl.misc.pbkdf2(WebEnc.config.passPhrase, salt, WebEnc.config.iterations, WebEnc.keySize);
        sjcl.beware["CBC mode is dangerous because it doesn't protect message integrity."]();
        var aes = new sjcl.cipher.aes(key);

        // decrypt
        var p = sjcl.json.defaults;
        p.iv = iv;
        p.mode = "cbc";
        p.ks = 256;

        return sjcl.mode["cbc"].decrypt(aes, enc, iv);
    };

    /**
     * Loads the given path using an XHR request
     * @param {String} path
     * @param {function} onSuccess
     * @returns {undefined}
     */
    WebEnc.load = function (path, onSuccess) {

        var xhr = new XMLHttpRequest();
        xhr.open('GET', path, true);
        xhr.responseType = 'arraybuffer'; // this will accept the response as an ArrayBuffer
        xhr.onload = function (e) {
            var data = xhr.response;
            if (typeof onSuccess !== 'undefined') {
                onSuccess(WebEnc.decrypt(data));
            }
        };
        xhr.send();
    };

    /**
     * Decrypts the given path as UTF8 String
     * @param {String} path
     * @param {function} onSuccess
     * @returns {undefined}
     */
    WebEnc.decryptUTF8 = function (path, onSuccess) {
        this.load(path, function (raw) {
            if (typeof onSuccess !== 'undefined') {
                onSuccess(sjcl.codec.utf8String.fromBits(raw));
            }
        });
    };

    /**
     * Displays the content of the given path at the given domTarget.
     * If wanted rescans the current document for tags
     * @param {String} path
     * @param {DomElement} domTarget
     * @param {boolean} processHTML
     * @returns {undefined}
     */
    WebEnc.displayHTML = function (path, domTarget, processHTML) {
        this.decryptUTF8(path, function (text) {
            if (typeof domTarget !== 'undefined') {
                domTarget.innerHTML = text;
                if (typeof processHTML !== 'undefined' && processHTML) {
                    WebEnc.processHTML();
                }
            }
        });
    };

    /**
     * Displays the image given with the path at the given img DomElement
     * @param {String} path
     * @param {DomElement} domTarget
     * @returns {undefined}
     */
    WebEnc.displayImage = function (path, domTarget) {
        this.load(path, function (raw) {
            if (typeof domTarget !== 'undefined') {
                domTarget.src = "data:image/" + path.split('.').pop() + ";base64," + sjcl.codec.base64.fromBits(raw);
            }
        });
    };

    /**
     * Opens a save dialog so that the user can save the given path using the given type
     * @param {String} path
     * @param {String/Mime} type
     * @returns {undefined}
     */
    WebEnc.saveFile = function (path, type) {
        WebEnc.load(path, function (raw) {
            var opt = {};
            var name = path.split('/').pop();
            if (typeof type !== 'undefined' && type.length > 0) {
                opt.type = type;
            }
            var blob = new Blob([sjcl.codec.arrayBuffer.fromBits(raw)], opt);
            saveAs(blob, name);
        });
    };

    /**
     * Looks for tags in the html document and processes them
     * @returns {undefined}
     */
    WebEnc.processHTML = function () {
        if (WebEnc.config.passPhrase === null) {
            return;
        }
        // images
        var images = _getAllElements('img', 'data-enc-img');
        images.forEach(function (img) {
            var src = img.getAttribute('data-enc-img');
            img.removeAttribute('data-enc-img');
            if (src !== null && src.length > 0) {
                WebEnc.displayImage(src, img);
            }
        });
        // links and buttons download
        var buttons = _getAllElements('button', 'data-enc-save');
        buttons = buttons.concat(_getAllElements('a', 'data-enc-save'));
        buttons.forEach(function (but) {
            var src = but.getAttribute('data-enc-save');
            var type = but.getAttribute('data-enc-type');
            but.removeAttribute('data-enc-save');
            but.removeAttribute('data-enc-type');
            but.setAttribute('href', '#');

            if (src !== null && src.length > 0) {
                but.addEventListener("click", function (ev) {
                    WebEnc.saveFile(src, type);
                    ev.preventDefault();
                    return false;
                });
            }
        });
        // links and buttons replace
        var buttons = _getAllElements('button', 'data-enc-link');
        buttons = buttons.concat(_getAllElements('a', 'data-enc-link'));
        buttons.forEach(function (but) {
            var src = but.getAttribute('data-enc-link');
            var target = but.getAttribute('data-enc-target');
            but.removeAttribute('data-enc-link');
            but.removeAttribute('data-enc-target');
            but.setAttribute('href', '#');

            if (src !== null && src.length > 0) {
                but.addEventListener("click", function (ev) {
                    target = document.getElementById(target);
                    if (target !== null) {
                        WebEnc.displayHTML(src, target, true);
                    }
                    ev.preventDefault();
                    return false;
                });
            }
        });


    };


    function _convert(arrayBuffer, start, end) {
        var typedArray = new Uint8Array(arrayBuffer, start, end);
        return sjcl.codec.bytes.toBits(typedArray);
    }

    function _createCookie(name, value, days) {
        if (days) {
            var date = new Date();
            date.setTime(date.getTime() + (days * 24 * 60 * 60 * 1000));
            var expires = "; expires=" + date.toGMTString();
        } else
            var expires = "";
        document.cookie = name + "=" + value + expires + "; path=/";
    }

    function _readCookie(name) {
        var nameEQ = name + "=";
        var ca = document.cookie.split(';');
        for (var i = 0; i < ca.length; i++) {
            var c = ca[i];
            while (c.charAt(0) === ' ')
                c = c.substring(1, c.length);
            if (c.indexOf(nameEQ) === 0)
                return c.substring(nameEQ.length, c.length);
        }
        return null;
    }

    function _getAllElements(tagName, attribute) {
        var matchingElements = [];
        var allElements = document.getElementsByTagName(tagName);
        for (var i = 0, n = allElements.length; i < n; i++) {
            if (allElements[i].getAttribute(attribute) !== null) {

                matchingElements.push(allElements[i]);
            }
        }
        return matchingElements;
    }

}(WebEnc));


// test
document.addEventListener("DOMContentLoaded", function () {

    WebEnc.init(true);
    console.log("Init unlock " + WebEnc.isUnlocked());
    if (WebEnc.testPassPhrase("blabla", true)) {
        console.log("PAss unlock " + WebEnc.isUnlocked());
        //WebEnc.displayHTML('/hello.html', document.getElementById("cont"));

        //WebEnc.displayImage('/sam.png', document.getElementById("testImg"));
    }

});

