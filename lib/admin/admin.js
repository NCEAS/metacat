/*
 *     Purpose: Default style sheet for admin web pages
 */

function createExclusionList() {
    exclusionList = new Array();
}

function addExclusion(exclusionName) {
    exclusionList.push(exclusionName);
}

function validateForm(form_ref) {
    for (i = 0; i < form_ref.length; i++) {
        if (form_ref.elements[i].value == "") {
            excludeThisField = "false";
            for (j = 0; j < exclusionList.length; j++) {
                if (exclusionList[j] == form_ref.elements[i].id) {
                    excludeThisField = "true";
                }
            }
            if (excludeThisField == "true") {
                alert("All form fields must be populated");
                return false;
            }
        }
    }
    return true;
}

function submitForm(form_ref) {
    form_ref.submit();
}

function validateAndSubmitForm(form_ref) {
    if (!validateForm(form_ref)) {
        return false;
    }
    form_ref.submit();
}

function forward(location) {
    window.location = location;
}

function toggleHiddenRow(thisObj, id) {
    if (thisObj.checked) {
        showRow(id);
    } else {
        hideObject(id);
    }
}

function toggleHiddenInline(thisObj, id) {
    if (thisObj.checked) {
        showInline(id);
    } else {
        hideObject(id);
    }
}

function toggleHiddenTable(thisObj, id) {
    if (thisObj.checked) {
        showSection(id);
    } else {
        hideObject(id);
    }
}

function hideObject(objectID) {
    document.getElementById(objectID).style.display = 'none';
}

function showRow(objectID) {
    document.getElementById(objectID).style.display = 'table-row';
}

function showInline(objectID) {
    document.getElementById(objectID).style.display = 'inline';
}

function showSection(objectID) {
    document.getElementById(objectID).style.display = 'table';
}

function toggleHiddenDefaultText(radioName, activeSkinName) {
    radioList = document.getElementsByName(radioName);
    for (i = 0; i < radioList.length; i++) {
        radioId = radioList[i].id;
        nameArray = radioId.split("-", 1);
        radioSkinName = nameArray[0];
        if (radioSkinName == activeSkinName) {
            document.getElementById("hiding-default-" + radioSkinName).style.display = 'inline';
        } else {
            document.getElementById("hiding-default-" + radioSkinName).style.display = 'none';
        }
    }
}

function helpWindow(context, helpFile) {
    fileLoc = context + "/" + helpFile;
    window.open(fileLoc, 'mywindow', 'width=1050,height=800,scrollbars=yes,location=no,status=no');
}

function toggleHiddenInputField(targetRadioId, inputFieldName) {
    radio = document.getElementById(targetRadioId);
    if (radio.checked) {
        document.getElementById(inputFieldName).style.display = 'inline';
    } else {
        document.getElementById(inputFieldName).style.display = 'none';
    }

}

function loginOnloadHandler(metacatLoginUri, cnTokenUrl, cnLogoutUrl, orcidFlow, loggingOut) {

    if (loggingOut === 'true') {
        localStorage.clear();
        sessionStorage.clear();
        console.log("logged out");
    } else {
        if (orcidFlow === 'true') {
            console.log("orcid auth flow: querying CN for newest token");
            getTokenFromCN();
        } else {
            console.log("Need to login via orcid");
        }
    }

    function errorModal() {
        const errorMessage = "Problem retrieving token."
            + "<p>NOTE: We use DataONE authentication services for login, which uses "
            + "third-party cookies created by DataONE. <em>Login won't work, unless your"
            + " browser has third-party cookies enabled</em> for this website.</p>";
        console.log(errorMessage);
        document.getElementById('errorModalMessage').innerHTML = errorMessage;
        document.getElementById('errorModal').style.display = 'block';
    }

    function getTokenFromCN() {

        const orcidLink = document.getElementById('orcidLogin');
        const orcidHrefOrig = orcidLink.href
        const orcidLogoImgElement = document.getElementById('orcidLogo');
        const orcidSpinnerElement = document.createElement('div');

        disableOrcidButton(orcidLink, orcidHrefOrig, orcidLogoImgElement, orcidSpinnerElement);

        const xhr = new XMLHttpRequest();

        xhr.onreadystatechange = function () {
            if (xhr.readyState === XMLHttpRequest.DONE) {
                if (xhr.status === 200 && xhr.responseText.length !== 0) {
                    console.log("Got token; sending login request to metacat");
                    requestWithAuthHeader(metacatLoginUri, xhr.responseText);
                } else {
                    errorModal();
                    enableOrcidButton(
                        orcidLink, orcidHrefOrig, orcidLogoImgElement, orcidSpinnerElement);
                }
            }
        }
        xhr.open('GET', cnTokenUrl, true);
        xhr.setRequestHeader('Cache-Control', 'no-cache');
        xhr.withCredentials = true; // Include credentials (cookies) in the request
        xhr.send();
    }

    function requestWithAuthHeader(uri, token) {

        const xhr2 = new XMLHttpRequest();
        xhr2.open('GET', uri, true);
        xhr2.setRequestHeader('Authorization', 'Bearer ' + token);
        xhr2.setRequestHeader('Cache-Control', 'no-cache');
        xhr2.withCredentials = true;

        xhr2.onreadystatechange = function () {
            if (xhr2.readyState === XMLHttpRequest.DONE && (xhr2.status <= 302)) {
                document.getElementById('bodyContent').innerHTML = xhr2.responseText;
            }
        };
        xhr2.send();
    }

    function disableOrcidButton(orcidLink, orcidHrefOrig, orcidLogoImgElement, orcidSpinnerElement) {
        orcidLink.href = 'javascript:void(0);';
        orcidLink.classList.add('orcid-btn-disabled');

        orcidSpinnerElement.className = 'spinner';
        orcidLogoImgElement.parentNode.replaceChild(orcidSpinnerElement, orcidLogoImgElement);
    }

    function enableOrcidButton(orcidLink, orcidHrefOrig, orcidLogoImgElement, orcidSpinnerElement) {
        orcidLink.href = orcidHrefOrig;
        orcidLink.classList.remove('orcid-btn-disabled');
        orcidSpinnerElement.parentNode.replaceChild(orcidLogoImgElement, orcidSpinnerElement);
    }
}
