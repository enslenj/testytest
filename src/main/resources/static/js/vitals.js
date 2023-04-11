function enableDatePicker(sel) {
    let minDate = new Date();
    if (minDate.getMonth() === 0) { // January
        minDate.setMonth(11);
        minDate.setFullYear(minDate.getFullYear() - 1);
    } else {
        minDate.setMonth(minDate.getMonth() - 1);
    }
    minDate.setDate(1);

    $(sel).datepicker({
        changeMonth: true,
        changeYear: true,
        showButtonPanel: true,
        showOn: 'focus', //'button',
        constrainInput: true,
        dateFormat: 'mm-dd-yy',
        minDate: minDate,
        maxDate: 0,
        gotoCurrent: true //,
//        yearRange: (currentYear - 1) + ':' + currentYear
    });
}

function buildVitalsData() {
    let data = {};
    data.systolic1 = $('#systolic1').val();
    data.diastolic1 = $('#diastolic1').val();
    let pulse1 = $('#pulse1').val();
    if (pulse1 !== '') data.pulse1 = pulse1;
    let systolic2 = $('#systolic2').val();
    if (systolic2 !== '') data.systolic2 = systolic2;
    let diastolic2 = $('#diastolic2').val();
    if (diastolic2 !== '') data.diastolic2 = diastolic2;
    let pulse2 = $('#pulse2').val();
    if (pulse2 !== '') data.pulse2 = pulse2;

    let readingDate = $('#readingDate').datepicker('getDate');
    let timeArr = $('#readingTime').val().match(/(\d+):(\d+)\s+(am|pm)/);
    let h = parseInt(timeArr[1]);
    let m = parseInt(timeArr[2]);
    let ampm = timeArr[3];
    if      (h === 12 && ampm === 'am') h = 0;
    else if (h  <  12 && ampm === 'pm') h += 12;
    readingDate.setHours(h);
    readingDate.setMinutes(m);

    data.readingDateTS = $.datepicker.formatDate('@', readingDate);

    data.followedInstructions = $('input[type=radio][name=confirm]:checked').val() === 'yes';

    return data;
}

function createVitals(vitalsData, _callback) {
    $.ajax({
        method: "POST",
        url: "/vitals/create",
        data: vitalsData
    }).done(function(vitals, textStatus, jqXHR) {
        _callback(jqXHR.status, vitals);
    });
}

function appendReadingToTable(obj) {
    let container = $('#vitalsTable tbody');
    let unsortedData = $(container).find('tr');

    // note : keep this section synced with vitals.mustache

    // todo : obj was HomeBloodPressureReading, ***IS NOW*** AbstractVitalsModel

    let html = "<tr class='data' data-timestamp='" + obj.readingDateTimestamp + "'>" +
        "<td>" + obj.readingDateString + "</td>" +
        "<td>" + obj.readingType + "</td>" +
        "<td>" + obj.value + "</td>" +
        "</tr>\n";

    // now sort
    // adapted from https://stackoverflow.com/questions/6133723/sort-divs-in-jquery-based-on-attribute-data-sort

    let sortedData = $(unsortedData).add(html).sort(function(a,b) {
        let tsA = $(a).data('timestamp');
        let tsB = $(b).data('timestamp');
        return (tsA > tsB) ? 1 : (tsA < tsB) ? -1 : 0;
    });

    $(container).html(sortedData);
}