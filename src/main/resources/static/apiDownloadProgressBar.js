let appName = "Patent Investor";

$(document).ready(function(){
    function progressBarFunction() {
        //alert("button clicked");
        document.getElementById("progressionBar").style.display = 'block';
        //document.getElementById("progressionText").style.visibility = 'visible';

        var current_progress = 0;//[[${current_progress}]];
        var interval = setInterval(function() {
            /*const fetchPromise = fetch("/stocks/progress-bar-value");
            fetchPromise.then(function(response) {
                const jsonPromise = response.json();
                jsonPromise.then( function(json) {
                  current_progress = json.percentValue;
                  //alert("current_progress1 = " + json.percentValue);
                });
            });*/
            fetch("/stocks/progress-bar-value")
                .then(response => response.json())
                .then(json => current_progress = json.percentValue);
            //alert("current_progress2 = " + current_progress);
            $("#dynamic")
            .css("width", current_progress + "%")
            .attr("aria-valuenow", current_progress)
            .text(current_progress + "% Complete");

            let leftoverTime = 10 - parseInt(String(current_progress.toString().charAt(0)));

            if (current_progress >= 10 && current_progress  < 91){
                 $("#timeLeft")
                 .text(`${leftoverTime} minutes`);
            } else if (current_progress  >= 91){
                $("#timeLeft")
                .text("a few seconds");
            }
//            console.log("percentValue", leftoverTime);
//            alert(`${leftoverTime} minutes`);
            if (current_progress >= 100){
                clearInterval(interval);
            };
        }, 2000);
    }
    $("#refresh-button").click(progressBarFunction);
});