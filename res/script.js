const apiKey = "";

var tableData = [];

window.onload = function () {
    getEvents().then((events) => {
        createTable(events);
    });
    setTitle();
};

function createTable(events) {
    var table = document.getElementById("tabEvents");
    const titleHeadings = ["Name", "Start", "End", "Location"];

    var columns = parseInt(titleHeadings.length);
    var rows = parseInt(events["data"].length);

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
        cell.textContent = events["data"][i]["name"];
        row.appendChild(cell);

        cell = document.createElement("td");
        time = new Date(events["data"][i]["starttime"]);
        hours = time.getHours() < 10 ? '0' + time.getHours() : time.getHours();
        minutes = time.getMinutes() < 10 ? '0' + time.getMinutes() : time.getMinutes();
        cell.textContent = hours + ":" + minutes;
        row.appendChild(cell);

        cell = document.createElement("td");
        time = new Date(events["data"][i]["endtime"]);
        hours = time.getHours() < 10 ? '0' + time.getHours() : time.getHours();
        minutes = time.getMinutes() < 10 ? '0' + time.getMinutes() : time.getMinutes();
        cell.textContent = hours + ":" + minutes;
        row.appendChild(cell);

        cell = document.createElement("td");
        cell.textContent = events["data"][i]["locations"]["0"]["name"];
        row.appendChild(cell);

        tableBody.appendChild(row);
        tableData.push(rowData);
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

async function getEvents() {

    const apiURL = "https://proxy.corsfix.com/?https://everymantheatre.yesplan.be/api/events/date:" + getFormattedDateToday() + "?api_key=" + apiKey;

    return await fetch(apiURL)
        .then(Response => {
            if (!Response.ok) {
                throw new Error("API call failed.");
            }
            return Response.json();
        })
        .then(events => {
            return events
        })
        .catch(error => {
            console.error("Error: ", error);
        });
}