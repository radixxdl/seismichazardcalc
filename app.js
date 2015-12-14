/**
 * Created by Dominic on 19/11/2015.
 */

var valuesInt = [50, 10, 2];
var lonMin = 166;
var lonMax = 179;
var latMin = -48;
var latMax = -34;
var vs30Min = 150;
var vs30Max = 1800;
var z1Min = 0;
var z1Max = 1000;
var disaggMin = 0.001;
var disaggMax = 0.95;

$(document).ready(function() {

    var ajaxRunning;

    updateCoords();
    updateDefaultZ1();

    $('#nzhcc').submit(function(e) {

        // Stop form from executing
        e.preventDefault();

        // Prepare page for calculation
        updatePage(true, false);

        // Retrieve data from servlet
        ajaxRunning = $.ajax({
            type: $(this).attr('method'),
            url: $(this).attr('action'),
            data: $(this).serialize()
        })

            // Display response
            .done(function(data) {

                // Update table with new data
                var dataArray = data.hazFunction.points.list;
                $('#response-data').text(JSON.stringify(dataArray));
                refreshTable(dataArray);
                updatePage(false, false);

            })

            // Display error message
            .fail(function() {
                $('#result-table').text("Error encountered, please contact the site administrator.");
                updatePage(false, false);
            })

    });

    $('#modify').click(function() {
        validateInputs();
        updatePage(false, true);
    });

    $('#clear').click(function(e) {
        e.preventDefault();
        $('#nzhcc')[0].reset();
        //$('#specpc').val("0");
        validateInputs();
        updatePage(false, true);
    });

    $('#cancel').click(function() {
        if (ajaxRunning) {ajaxRunning.abort();}
        updatePage(false, true);
    });

    $('#lat').add('#lon').add('#vs30').add('#z1pt0').change(function() {
        validateInputs();
    });

    $('#address').change(function() {
        if ($('#defaultLatLon').is(':checked')) {updateCoords()}
    });

    $('#defaultLatLon').click(function() {
        toggleCoords();
    });

    $('#vs30').change(function() {
        if ($('#defaultZ1').is(':checked')) {updateDefaultZ1()}
    });

    $('#defaultZ1').click(function() {
        toggleDefaultZ1();
    });

    //$('#specpc').change(function() {
    //    var spec = $('#specpc');
    //    if (spec.val() == "") {spec.val("0")}
    //    var dataArray = $('#response-data').text();
    //    dataArray = JSON.parse(dataArray);
    //    refreshTable(dataArray);
    //    updatePage(false, false);
    //});

    $('#displaytable').click(function() {
        toggleResultTable();
    });

    $('#disagg').submit(function(e) {

        // Stop form from executing
        e.preventDefault();

        // Prepare page for calculation
        updatePageForDisagg(true, false);
        var dresult = $('#disaggresult');
        var strData = $('#nzhcc').serialize() + "&" + $(this).serialize();

        // Retrieve data from servlet
        ajaxRunning = $.ajax({
            type: $(this).attr('method'),
            url: $(this).attr('action'),
            data: strData
        })

            // Display response
            .done(function(data) {

                // Display disaggregation table and link
                var disagg = data.disaggWebAddr;
                dresult.append("<div><img id='' src='" + disagg + "DisaggregationPlot.png' /></div>");
                dresult.append("<div>For further details and higher resolution plots, please click on <a href='" + disagg + "' target='_blank'>this link</a>.</div>");
                updatePageForDisagg(false, false);
            })

            // Display error message
            .fail(function() {
                dresult.append("Error encountered, please contact the site administrator.");
                updatePageForDisagg(false, false);
            })

    });

    $('#disagg-modify').click(function() {
        validateDisaggInputs();
        updatePageForDisagg(false, true);
    });

    $('#disagg-clear').click(function() {
        $('#disagg')[0].reset();
        validateDisaggInputs();
        updatePageForDisagg(false, true);
    });

    $('#disagg-cancel').click(function() {
        if (ajaxRunning) {ajaxRunning.abort();}
        updatePageForDisagg(false, true);
    });

    $('#inputs-var').find('label').not('.chklabel').add($('.help')).tooltip({});

});

function refreshTable(array) {

    // Prepare variable and fields for interpolation
    var xNum = 0; var xNumOld;
    var yNum = 0; var yNumOld;
    //var valSpecInt = parseFloat($('#specpc').val());

    // Set up table to display data
    var tbl = $("<table/>").attr('id', "xyTable");
    var divTbl = $('#result-table');
    divTbl.html("");
    divTbl.append(tbl);

    // Set up table to display interpolated data
    var tblInt = $("<table/>").attr('id', "intTable");
    var divInt = $('#result-interpolated');
    divInt.html("");
    divInt.append(tblInt);

    // Insert table headings
    var head1 = "Spectral Acceleration (g)";
    var head2 = "Probability of Exceedance (%)";
    var strHeads = "<th>" + head1 + "</th><th>" + head2 + "</th>";
    tbl.append(strHeads);
    tblInt.append(strHeads);

    var xInt, yInt;
    var td1, td2;
    var xSpecInt;
    var hasInterpolatedSpecInt = false;
    var j = 0;

    // Insert table data
    for (var i = 0; i < array.length; i++) {

        // Take previous point data for interpolation
        xNumOld = xNum;
        yNumOld = yNum;

        // Format x and y values
        xNum = array[i]["x"];
        yNum = array[i]["y"] * 100;

        // Fill table with numbers within a certain range
        // First check: lower number rounded to 3sf is 0.999 or lower
        // Second check: higher number rounded to 3sf is 1.00e-6 or greater
        if ((yNum < 98.95) && (yNumOld > 0.09995)) {

            // Format xNum and yNum to the most readable format
            if (xNumOld > 0.01) {xNumOld = xNumOld.toPrecision(3)} else {xNumOld = xNumOld.toExponential(2)}
            //if (yNumOld > 0.01) {yNumOld = yNumOld.toPrecision(3)} else {yNumOld = yNumOld.toExponential(2)}
            yNumOld = yNumOld.toPrecision(3);

            td1 = "<td>" + xNumOld + "</td>";
            td2 = "<td>" + yNumOld + "</td>";
            tbl.append("<tr>" + td1 + td2);
        }

        // Check default probabilities
        if (j < valuesInt.length) {

            if (yNum < valuesInt[j]) {

                yInt = valuesInt[j];
                xInt = interpolateX(yInt, xNumOld, xNum, yNumOld, yNum);
                td1 = "<td>" + xInt + "</td>";
                td2 = "<td>" + yInt + "</td>";
                tblInt.append("<tr>" + td1 + td2);
                j++;
            }
        }

        // Check specified probability
        //if (!hasInterpolatedSpecInt && (valSpecInt != 0)) {
        //    if (yNum < valSpecInt / 100) {
        //        xSpecInt = interpolateX(valSpecInt, xNumOld, xNum, yNumOld, yNum);
        //        hasInterpolatedSpecInt = true;
        //    }
        //}
    }

    // Add specified probability to summary table
    //if (valSpecInt != 0) {
    //    td1 = "<td><b>" + xSpecInt + "</b></td>";
    //    td2 = "<td><b>" + valSpecInt + "</b></td>";
    //    tblInt.append("<tr>" + td1 + td2);
    //}
}

function drawChart(array) {

    //var plot = $.jqplot('xyChart', )
}

function interpolateX(target, xOld, xNew, yOld, yNew) {

    // Input must be in percentage, e.g. 50%
    // Convert target to log for interpolation
    target = Math.log(target);

    // Convert data to logs for interpolation
    var x0 = Math.log(xOld);
    var x1 = Math.log(xNew);
    var y0 = Math.log(yOld);
    var y1 = Math.log(yNew);

    var xNum = parseFloat((target - y1)/(y0 - y1)*(x0 - x1)) + parseFloat(x1);
    xNum = Math.exp(xNum);

    // Convert result back for presentation
    if (xNum > 0.01) {xNum = xNum.toPrecision(3)} else {xNum = xNum.toExponential(2)}
    return xNum;
}

function updateCoords() {

    // Get latitude and longitude from Google's geocode api
    $.ajax({
            async: false,
            type: "GET",
            dataType: "json",
            url: "http://maps.googleapis.com/maps/api/geocode/json",
            data: {'address': $('#address').val(), 'sensor': false}
        })
        .done(function(data){
            if (data.results.length) {

                $('#lat').val(data.results[0].geometry.location.lat.toFixed(3));
                $('#lon').val(data.results[0].geometry.location.lng.toFixed(3));
                $('#address-warn').toggle(false);

            } else {
                $('#lat').val('n/a');
                $('#lon').val('n/a');
                $('#address-warn').toggle(true);
            }
        });

    // Check that the coordinates are in the valid range
    validateInputs();
}

function toggleCoords() {

    var isDefaultLatLon = $('#defaultLatLon').is(':checked');
    var addr = $('#address');

    var vis;
    if (isDefaultLatLon) {vis = 'visible'} else {vis = 'hidden'}

    // Toggle fields depending on checkbox is selected
    addr.css('visibility', vis);
    $('#lon').prop('readonly', isDefaultLatLon);
    $('#lat').prop('readonly', isDefaultLatLon);
    if (isDefaultLatLon) {updateCoords()}
}

function updateDefaultZ1() {

    // Use Chiou and Young's formula to calculate z1
    var vs30 = parseFloat($('#vs30').val());
    var lnZ1 = 28.5 - 3.82/8 * Math.log(Math.pow(vs30,8) + Math.pow(378.7,8));
    var z1 = Math.exp(lnZ1).toPrecision(3);
    $('#z1pt0').val(z1);
}

function toggleDefaultZ1() {
    var isDefaultZ1 = $('#defaultZ1').is(':checked');
    $('#z1pt0').prop('readonly', isDefaultZ1);
    if (isDefaultZ1) {updateDefaultZ1()}
}

function toggleResultTable() {
    $('#result-table').toggle($('#displaytable').is(':checked'));
}

function lockInputs(toBeLocked) {

    // Lock input fields
    var form = $('#nzhcc');
    form.find('input').prop('readonly', toBeLocked);
    form.find(':checkbox').prop('disabled', toBeLocked);
    form.find('textarea').prop('readonly', toBeLocked);

    // Show readonly text instead of dropdown
    var pdval = $('#period');
    var pdsel = $('#periodSelect');
    if (toBeLocked) {pdval.val(pdsel.find(':selected').val())}
    pdval.toggle(toBeLocked);
    pdsel.toggle(!toBeLocked);

    // Check toggle fields - also validates inputs
    toggleDefaultZ1();
    toggleCoords();
}

function validateInputs() {

    // Check each input field and turn on warning if required
    var lon = parseFloat($('#lon').val());
    $('#lon-warn').toggle((lon < lonMin) || (lon > lonMax));
    var lat = parseFloat($('#lat').val());
    $('#lat-warn').toggle((lat < latMin) || (lat > latMax));
    var vs30 = parseFloat($('#vs30').val());
    $('#vs30-warn').toggle((vs30 < vs30Min) || (vs30 > vs30Max));
    var z1pt0 = parseFloat($('#z1pt0').val());
    $('#z1pt0-warn').toggle((z1pt0 < z1Min) || (z1pt0 > z1Max));

    // Disable compute button if any warnings are visible
    var isNotValid = ($('#inputs-var').find('.warning:visible').length > 0);
    $('#compute').prop('disabled', isNotValid);
    $('#clear').toggle(isNotValid);
}

function updatePage(isCalculating, wasCancelled) {

    // Update page to finalise calculation process
    lockInputs(isCalculating || !wasCancelled);
    $('#modify').toggle(!isCalculating && !wasCancelled);
    $('#compute').toggle(!isCalculating && wasCancelled);
    $('#clear').toggle(!isCalculating && !wasCancelled);
    $('#processing').toggle(isCalculating);
    $('.result').toggle(!isCalculating && !wasCancelled);
    $('#disagg').toggle(!isCalculating && !wasCancelled);

    if (wasCancelled) {
        $('#displayall').attr('checked', false);
        $('#result-table').html("");
        $('#disaggresult').html("");
    } else {
        if (!isCalculating) {toggleResultTable()}
    }
}

function lockDisaggInputs(toBeLocked) {

    // Toggle input fields
    $('#disagg').find('input').prop('readonly', toBeLocked);
}

function validateDisaggInputs() {

    // Check input field and turn on warning if required
    var disaggval = parseFloat($('#disaggval').val());
    $('#disaggval-warn').toggle((disaggval < disaggMin) || (disaggval > disaggMax));

    // Disable compute button if any warnings are visible
    var isNotValid = ($('#disagg-var').find('.warning:visible').length > 0);
    $('#disagg-compute').prop('disabled', isNotValid);
    $('#disagg-clear').toggle(isNotValid);
}

function updatePageForDisagg(isCalculating, wasCancelled) {

    // Update page as required
    lockDisaggInputs(isCalculating || !wasCancelled);
    $('#disagg-modify').toggle(!isCalculating && !wasCancelled);
    $('#disagg-compute').toggle(!isCalculating && wasCancelled);
    $('#disagg-clear').toggle(!isCalculating && !wasCancelled);
    $('#disagg-processing').toggle(isCalculating);
    $('#disaggresult').toggle(!isCalculating && !wasCancelled);
    if (wasCancelled) {$('#disaggresult').html("")}
}