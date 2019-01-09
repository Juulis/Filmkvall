$('.message a').click(function () {
    $('form').animate({height: "toggle", opacity: "toggle"}, "slow");
});

$('#login-btn').click(function () {
    let name = $('#login-name').val();
    let pw = $('#login-pw').val();
    let user = {
        name: name,
        pw: pw
    };
    $.ajax({
        type: "POST",
        url: '/login',
        data: user,
        success: function (response) {
            console.log("response: ",response);
            if (response === "logged in") {
                console.log("logged in");
                return "logged in";
            }
            else if (response === "not authorized"){
                console.log("wrong pw");
                return "Wrong user credentials!";
            }
                }
    });
});