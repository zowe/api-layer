function unsecureCall() {
    var name = $('#name').val();
    $.ajax({
        url: "api/v1/" + ((name !== undefined) ? (name + "/greeting") : "greeting")
    }).then(function (data) {
        $('.hello-date').text(data.date);
        $('.hello-content').text(data.content);
    });
}



function openWebsocket() {
    var loc = window.location, wsUri;
    if (loc.protocol === "https:") {
        wsUri = "wss:";
    } else {
        wsUri = "ws:";
    }
    wsUri += "//" + loc.host;
    console.log(loc.pathname);
    if (loc.pathname.startsWith("/ui/")) {
        // UI applications accessed via the gateway will use a URL with "/ui/" at the beginning,
        // so we need to replace it with "/ws/":
        wsUri += loc.pathname.replace("/ui/", "/ws/") + "uppercase";
    }
    else {
        wsUri += loc.pathname + "ws/uppercase";
    }
    console.log("Opening Websocket connection " + wsUri);
    return new WebSocket(wsUri);
}

var connection = openWebsocket();

connection.onopen = function () {
    $('#ws-content').text('Websocket connection established');
};
connection.onerror = function (error) {
    $('#ws-content').text('Websocket connection failed');
    console.log("Opening Websocket connection failed", error);
};

connection.onmessage = function (message) {
    console.log("Websocket message received", message);
    $('#ws-content').text(message.data);
}

function websocketCall() {
    var text = $('#ws-text').val();
    connection.send(text);
}
