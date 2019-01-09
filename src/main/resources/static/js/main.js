const CLIENT_ID = "154954789740-8cn2jar3k6hj2726kla6bclqgnoo863s.apps.googleusercontent.com";
const URL = "http://juuffy.net:8080";

function start() {
    gapi.load('auth2', function () {
        auth2 = gapi.auth2.init({
            client_id: CLIENT_ID,
            scope: "https://www.googleapis.com/auth/calendar.events"
        });
    });
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
        type: 'POST',
        url: `${URL}/api/movies?t=${$('#searchmovie-field').val()}`,
        // Always include an `X-Requested-With` header in every AJAX request,
        // to protect against CSRF attacks.
        headers: {
            'X-Requested-With': 'XMLHttpRequest'
        },
        success: function (result) {
            $('#result').empty();

            for (let i = 0; i < result.length; i++) {
                let li;
                if (result[i].title === undefined) {
                    li = `<li class="clickable">${result[i].Title} (${result[i].Year}) <p class="invisible">${result[i].imdbID}</p></li>`;
                } else {
                    li = `<li class="clickable">${result[i].title} (${result[i].released}) <p class="invisible">${result[i].id}</p></li>`;
                }
                $('#result').append(li);
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
        type: 'POST',
        url: `${URL}/api/movie?id=${imdbID}`,
        // Always include an `X-Requested-With` header in every AJAX request,
        // to protect against CSRF attacks.
        headers: {
            'X-Requested-With': 'XMLHttpRequest'
        },
        success: function (result) {

            let ul = $('#result');
            ul.empty();

            let title = `<li>${result.Title}</li>` || `<li>${result.title}</li>`;
            let genre = `<li>${result.Genre}</li>` || `<li>${result.genre}</li>`;
            let plot = `<li>${result.Plot}</li>` || `<li>${result.plot}</li>`;
            let poster = `<li><img src="${result.Poster}"/></li>` || `<li><img src="${result.poster}"/></li>`;
            let released = `<li>${result.Year}</li>` || `<li>${result.released}</li>`;

            ul.append(title);
            ul.append(released);
            ul.append(genre);
            ul.append(poster);
            ul.append(plot);

        },
        processData: false,
    });
}

// $('.group').css("cursor","pointer").click(function(){
//
//     console.log($(this).text());
// });