const CLIENT_ID = "154954789740-8cn2jar3k6hj2726kla6bclqgnoo863s.apps.googleusercontent.com";
const URL = "http://juuffy.net:8080";
const content = $('#content');

function start() {
    gapi.load('auth2', function () {
        auth2 = gapi.auth2.init({
            client_id: CLIENT_ID,
            scope: "https://www.googleapis.com/auth/calendar.events"
        });
    });
    getFreeHours();
}

$('#googlebtn').click(function () {
// signInCallback defined in step 6.
    auth2.grantOfflineAccess().then(signInCallback);
});

function signInCallback(authResult) {
    console.log('authResult', authResult);
    if (authResult['code']) {

        // Hide the sign-in button now that the user is authorized
        $('#googlebtn').attr('style', 'display: none');

        // Send the code to the server
        $.ajax({
            type: 'POST',
            url: `${URL}/storeauthcode`,
            // Always include an `X-Requested-With` header in every AJAX request,
            // to protect against CSRF attacks.
            headers: {
                'X-Requested-With': 'XMLHttpRequest'
            },
            contentType: 'application/octet-stream; charset=utf-8',
            success: function (result) {
                // Handle or verify the server response.
            },
            processData: false,
            data: authResult['code']
        });
    } else {
        // There was an error.
    }
}

$('#searchmovie-btn').click(function () {
    $.ajax({
        type: 'GET',
        url: `${URL}/api/movies?t=${$('#searchmovie-field').val()}`,
        // Always include an `X-Requested-With` header in every AJAX request,
        // to protect against CSRF attacks.
        headers: {
            'X-Requested-With': 'XMLHttpRequest'
        },
        success: function (result) {
            content.empty();
            let ul = $('<ul id="result"></ul>');

            for (let i = 0; i < result.length; i++) {
                let li;
                if (result[i].title === undefined) {
                    li = `<li class="clickable">${result[i].Title} (${result[i].Year}) <p class="invisible">${result[i].imdbID}</p></li>`;
                } else {
                    li = `<li class="clickable">${result[i].title} (${result[i].released}) <p class="invisible">${result[i].id}</p></li>`;
                }
                ul.append(li);
                content.append(ul);
            }
            $('#result li').click(function () {
                showMovie($(this).children(1).text());
            });
        },
        processData: false,
    });

});

function showMovie(imdbID) {
    $.ajax({
        type: 'GET',
        url: `${URL}/api/movie?id=${imdbID}`,
        // Always include an `X-Requested-With` header in every AJAX request,
        // to protect against CSRF attacks.
        headers: {
            'X-Requested-With': 'XMLHttpRequest'
        },
        success: function (result) {
            let ul = $('<ul id="result"></ul>');

            let title = `<li>${result.Title}</li>` || `<li>${result.title}</li>`;
            let genre = `<li>${result.Genre}</li>` || `<li>${result.genre}</li>`;
            let plot = `<li>${result.Plot}</li>` || `<li>${result.plot}</li>`;
            let poster = `<li><img src="${result.Poster}"/></li>` || `<li><img src="${result.poster}"/></li>`;
            let released = `<li>${result.Year}</li>` || `<li>${result.released}</li>`;
            let id = `<li>ID: ${result.imdbID}</li>` || `<li>${result.id}</li>`;

            ul.append(title);
            ul.append(released);
            ul.append(genre);
            ul.append(poster);
            ul.append(plot);
            ul.append(id);
            content.empty();
            content.append(ul);
        },
        processData: false,
    });
}

function getFreeHours() {
    let ul = $('#dates ul');
    $.ajax({
        type: 'GET',
        url: `${URL}/getdates`,
        // Always include an `X-Requested-With` header in every AJAX request,
        // to protect against CSRF attacks.
        headers: {
            'X-Requested-With': 'XMLHttpRequest'
        },
        success: function (result) {
            ul.empty();

            for (let i = 0; i < result.length; i++) {
                let date = `<li>${result[i]}</li>`;
                ul.append(date);
            }
            let dateContainer = $('#dates');
            dateContainer.append(ul);

            $('#dates li').css("cursor", "pointer").click(function () {
                addToCalendar($(this).text());
            });
        },
        processData: false,
    });
}


function addToCalendar(time) {
    content.empty();

    let pTime = $(`<p id="date">${time}</p>`);
    let pMovie = $(`<p>Vilken film vill du boka? ID hittar du på filmens informationssida!</p>`);
    let form = $('<input type="text" id="movie-field" placeholder="movie id">');
    let btn = $('<button id="bookmovie-btn">Boka</button>');

    //TODO add the date to calendar

    content.empty();
    content.append(pTime);
    content.append(pMovie);
    content.append(form);
    content.append(btn);




    $('#bookmovie-btn').click(function () {
        let id = $('#movie-field').val();
        let bookinginfo = $('#date').text() + ',' + id;
        if (id != null) {
            $.ajax({
                type: 'POST',
                url: `${URL}/bookdate`,
                headers: {
                    'X-Requested-With': 'XMLHttpRequest'
                },
                contentType: 'application/octet-stream; charset=utf-8',
                success: function (result) {
                    console.log(result);
                },
                data: bookinginfo,
                dataType: String,
                processData: false,
            });
            content.empty();
            let p = $('<p>Du har bokat en filmkväll! Köp popcorn!</p>');
            content.append(p);
        }
    });
}



