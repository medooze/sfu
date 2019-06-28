function GetStats() {
    this.receiverUrl = "";
    this.userId = "";
    this.roomId = "";
    this.sfu = "";
    this.pcObject = null;
    this.alwaysSendEverything = true;
    this.lastStatsObjValues = {};
    this.publishing = null;
    this.useGoogStatsForChrome = true;
}

GetStats.prototype.init = function (receiverUrl, userId, roomId, sfu, pcObject, alwaysSendEverything = true) {
    this.receiverUrl = receiverUrl;
    this.userId = userId;
    this.roomId = roomId;
    this.sfu = sfu;
    this.pcObject = pcObject;
    this.alwaysSendEverything = alwaysSendEverything;
}

GetStats.prototype.startPublishing = function (interval) {
    let publishingFunction = function () {
        this.getStatsValues()
        .then(statsObj => this.publish(statsObj))
        .then(resp => console.log(resp))
        .catch(err => console.log("Error in publishing getStats data: ", err))
    }

    this.publishing = setInterval(publishingFunction.bind(this), interval);
}

GetStats.prototype.stopPublishing = function () {
    clearInterval(this.publishing);
}

// Getstats from pc.getStats() function and return a stats objects
// The stats object is filled with all stats or only changing ones depending on the value of alwaysSendEverything 
GetStats.prototype.getStatsValues = async function () { 
    return this.pcObject.getStats().then((data) => {
        let statsObj = {};
        compareValueOf = this.compareValueOf.bind(this);
        data.forEach(res => {
            if (!this.alwaysSendEverything) {
                const newValue = this.compareValueOf(res);
                if (newValue) statsObj[res.id] = res;
            } else {
                statsObj[res.id] = res;
                // statsObj.push(res); ==> XXX to re-test. If not OK, uncommment this line and comment the line above
            }
        });
        return statsObj;
    }).catch();
}

// Compare an item with it's previous iteration in the lastStatsObjValues and return a boolean
// if it's the same, then return false
// else replace its value in the lastStatsObjValues and return true
GetStats.prototype.compareValueOf = function (item) {
    // deepcopy of item and remove timestamp to compare all other fields
    compareItem = cloneObject(item);
    delete compareItem.timestamp;
    if (compareItem.type != "local-candidate" && compareItem.type != "remote-candidate") {
        if (JSON.stringify(compareItem) === JSON.stringify(this.lastStatsObjValues[compareItem.id])) {
            return false;
        }
    }
    this.lastStatsObjValues[compareItem.id] = cloneObject(compareItem);
    return true;
}

function cloneObject(obj) {
    var clone = {};
    for(var i in obj) {
        if(obj[i] != null &&  typeof(obj[i])=="object")
            clone[i] = cloneObject(obj[i]);
        else
            clone[i] = obj[i];
    }
    return clone;
}

GetStats.prototype.publish = function (statsObj) {
    body = {
        "userId": this.userId,
        "roomId": this.roomId,
        "sfu": this.sfu,
        "stats": statsObj
    }

    return new Promise((resolve, reject) => {
        var xhr = new XMLHttpRequest();
        xhr.open("POST", this.receiverUrl);
        xhr.onload = function () {
            if (this.status >= 200 && this.status < 300) {
                resolve(xhr.response);
            } else {
                reject({
                    status: this.status,
                    statusText: xhr.statusText
                });
            }
        }
        xhr.onerror = function () {
            reject({
                status: this.status,
                statusText: xhr.statusText
            });
        };
        xhr.send(JSON.stringify(body));
    })
}