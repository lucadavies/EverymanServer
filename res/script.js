window.onload = function () {
    getAPIKey()
        .then(getAllEvents)
        .then(sortEvents)
        .then(createTable);
    setTitle();
};

function createTable(events) {
    console.log(events);
    var table = document.getElementById("tabEvents");
    const titleHeadings = ["Name", "Start", "End", "Location"];

    var columns = parseInt(titleHeadings.length);
    var rows = parseInt(events.length);

    // Remove spinner.
    document.getElementById("loaderTable").remove();

    // Clear existing table
    while (table.firstChild) {
        table.removeChild(table.firstChild);
    }

    // Create table header
    var header = document.createElement("thead");
    var headerRow = document.createElement("tr");


    for (var i = 0; i < columns; i++) {
        var th = document.createElement("th");
        th.textContent = titleHeadings[i];
        headerRow.appendChild(th);
    }
    header.appendChild(headerRow)
    table.appendChild(header);

    var tableBody = document.createElement("tbody");

    // Create table rows
    for (var i = 0; i < rows; i++) {
        var rowData = [];
        var row = document.createElement("tr");
        var cell;
        var time;
        var hours;
        var minutes;

        cell = document.createElement("td");
        cell.textContent = events[i]["name"];
        row.appendChild(cell);

        cell = document.createElement("td");
        time = new Date(events[i]["starttime"]);
        hours = time.getHours() < 10 ? '0' + time.getHours() : time.getHours();
        minutes = time.getMinutes() < 10 ? '0' + time.getMinutes() : time.getMinutes();
        //cell.textContent = hours + ":" + minutes;
        cell.textContent = time;
        row.appendChild(cell);

        cell = document.createElement("td");
        time = new Date(events[i]["endtime"]);
        hours = time.getHours() < 10 ? '0' + time.getHours() : time.getHours();
        minutes = time.getMinutes() < 10 ? '0' + time.getMinutes() : time.getMinutes();
        cell.textContent = hours + ":" + minutes;
        row.appendChild(cell);

        cell = document.createElement("td");
        try {
            cell.textContent = events[i]["locations"]["0"]["name"];
        }
        catch (error) {
            // TODO: do better than this lazy handling for events having no location.
            console.error(error);
        }

        row.appendChild(cell);

        tableBody.appendChild(row);
    }

    table.appendChild(tableBody);
}

function setTitle() {
    var title = document.getElementById("title");
    title.textContent = "[" + getFormattedDateToday() + "] Today's Events:";
}

function getFormattedDateToday() {
    var date = new Date();

    var day = date.getDate();
    var month = date.getMonth() + 1; // Correct for JS indexing of months
    var year = date.getFullYear();

    day = day < 10 ? '0' + day : day;
    month = month < 10 ? '0' + month : month;

    return day + "-" + month + "-" + year;
}

async function getTodayEvents(apiKey) {
    const apiURL = "https://proxy.corsfix.com/?https://everymantheatre.yesplan.be/api/events/date:" + getFormattedDateToday() + "?api_key=" + apiKey;
    return await makeYesPlanAPICall(apiURL, apiKey);
}

async function getAllEvents(apiKey) {
    const apiURL = "https://proxy.corsfix.com/?https://everymantheatre.yesplan.be/api/events?api_key=" + apiKey;
    const allResps = await makeYesPlanAPICall(apiURL, apiKey);
    return allResps["data"];
}

function sortEvents(events) {
    return events.sort((a, b) => { return new Date(a["starttime"]).getTime() - new Date(b["starttime"]).getTime() });
}

async function makeYesPlanAPICall(apiUrl, apiKey) {
    var resp = await fetch(apiUrl)
        .then(Response => {
            if (!Response.ok) {
                throw new Error("API call failed.");
            }
            return Response.json();
        })
        .catch(error => {
            console.error("Error: ", error);
        });
    if (resp["pagination"]["next"] === undefined) {
        console.log("One page of events.");
        return resp;
    }
    else {
        console.log("Getting next page...");

        const nextURL = "https://proxy.corsfix.com/?" + resp["pagination"]["next"] + "&api_key=" + apiKey
        return makeYesPlanAPICall(nextURL, apiKey).then(nextResp => {
            const obj = { "pagination": nextResp["pagination"], "data": resp["data"].concat(nextResp["data"]) };
            return obj;
        });
    }
}

async function getAPIKey() {

    return await fetch("/res/apiKey.txt")
        .then(Response => {
            if (!Response.ok) {
                throw new Error("Failed to acquire API key.");
            }
            return Response.text();
        })
        .catch(error => {
            console.error("Error: ", error);
        });
}
