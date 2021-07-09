function enableDatePicker(sel) {
    let minDate = new Date();
    minDate.setDate(minDate.getDate() + 1);

    $(sel).datepicker({
        changeMonth: true,
        changeYear: true,
        showButtonPanel: true,
        showOn: 'focus', //'button',
        constrainInput: true,
        dateFormat: 'mm-dd-yy',
        minDate: minDate,
        gotoCurrent: true //,
    });
}

async function applyGoals() {
    let goals = await getRecordedGoals();
    $('.recommendation').each(function () {
        let cardsContainer = $(this).find('.cardsContainer');
        goals.forEach(function(goal) {
            $(cardsContainer).find('input.goal[data-extGoalId="' + goal.extGoalId + '"').each(function () {
                if ($(this).attr('type') === 'checkbox') {
                    $(this).prop('checked', true);
                }
            });
        });
    });
}

async function executeRecommendations(_callback) {
    $('.recommendation').each(function () {
        let recommendationId = $(this).attr('data-id');
        let cardsContainer = $(this).find('.cardsContainer');
        executeRecommendation(recommendationId, function (cards) {
            _callback(cardsContainer, cards);
        });
    });
}

async function executeRecommendation(id, _callback) {
    let formData = new FormData();
    formData.append("id", id);
    let response = await fetch("/recommendations/execute", {
        method: "POST",
        body: formData
    });
    let cards = await response.json();
    _callback(cards);
}

async function getCachedRecommendations(_callback) {
    $(".recommendation").each(function () {
        let recommendationId = $(this).attr('data-id');
        let cardsContainer = $(this).find('.cardsContainer');
        getCachedRecommendation(recommendationId, function (cards) {
            _callback(cardsContainer, cards);
            enableDatePicker($(cardsContainer).find('.goalTargetDate'));
        });
    });
}

// get a recommendation from cache, but do not execute it
async function getCachedRecommendation(id, _callback) {
    let formData = new FormData();
    formData.append("id", id);
    let response = await fetch("/recommendations/getCached", {
        method: "POST",
        body: formData
    });
    let cards = await response.json();
    _callback(cards);
}

function renderCards(cards) {
    let html = "";
    if (cards === undefined || cards === null || cards.length === 0) {
        return html;
    }

    cards.forEach(function (card) {
        html += "<div class='card " + card.indicator + "'>\n";
        html += "<table style='width:100%'><tr><td>\n";
        html += "<div class='circle'><span>XX</span></div>\n"
        html += "</td><td>\n";
        html += "<div class='content'>\n";
        html += "<span class='summary heading'>" + card.summary + "</span>\n";

        if (card.rationale !== null) {
            html += "<span class='rationale'>" + card.rationale + "</span>\n";
        }

        if (card.source.label !== null && card.source.url !== null) {
            html += "<span class='source'>";
            html += "<a href='" + card.source.url + "' target='_blank' rel='noopener noreferrer'>" +
                card.source.label + "</a>";
            html += "</span>\n";
        }

        if (card.links !== null) {
            html += "<div class='links'>";
            card.links.forEach(function(link) {
                html += "<a class='link' href='" + link.url + "'>" + link.label + "</a>\n";
            });
            html += "</div>\n";
        }

        html += buildCounselingHTML(card.suggestions);

        html += "</td><td>\n";

        html += buildGoalsHTML(card.suggestions);

        html += buildLinksHTML(card.suggestions);
        // if (card.selectionBehavior !== null) {
        //     html += "<span class='selectionBehavior'>" + card.selectionBehavior + "</span>\n";
        // }

        html += "</div>\n";
        html += "</td></tr></table>\n";
        html += "</div>\n";
    });
    return html;
}

function buildCounselingHTML(suggestions) {
    let html = "";
    if (suggestions !== null) {
        suggestions.forEach(function(s) {
            if (s.type === 'counseling-link') {
                html += "<div class='counseling' data-id='" + s.id + "' data-reference-system='" + s.references.system + "' data-reference-code='" + s.references.code + "'>";
                html += "<span class='heading'>" + s.label + "</span>";
                if (s.actions !== null) {
                    html += "<ul class='actions'>";
                    s.actions.forEach(function(action) {
                        html += "<li class='action'><a href='" + action.url + "'>" + action.label + "</a></li>";
                    });
                    html += "</ul>";
                }
                html += "</div>\n";
            }
        });
    }
    return html !== "" ?
        "<div class='counselingContainer'>" + html + "</div>" :
        "";
}

function buildGoalsHTML(suggestions) {
    let html = "";
    if (suggestions !== null) {
        suggestions.forEach(function(s) {
            if (s.type === 'goal' || s.type === 'bp-goal') {
                let c = s.type === 'bp-goal' ? 'bpGoal' : 'goal';

                html += "<div class='" + c + "' data-id='" + s.id + "' data-reference-system='" + s.references.system + "' data-reference-code='" + s.references.code + "'>";
                html += "<span class='heading'>" + s.label + "</span>";
                html += "<table><tr>";

                html += "<td>";
                if (s.type === 'goal') {
                    if (s.actions === null || s.actions.length === 0) {
                        // freeform input
                        html += "<div class='action'>";
                        html += "<input type='text' placeholder='Describe your goal here' />";
                        html += "</div>\n";

                    } else {
                        // predefined multiple-choice goal, these are radio buttons
                        let i = 0;
                        let x = randomChars(5);
                        s.actions.forEach(function (action) {
                            html += "<div class='action'>";
                            html += "<input name='action" + x + "' type='radio' id='action" + x + "_" + i + "' value='" + action.label + "' />";
                            html += "<label for='action" + x + "_" + i + "'>" + action.label + "</label>";
                            html += "</div>\n";
                            i++;
                        });

                        html += "<div class='action'>";
                        html += "<input name='action" + x + "' type='radio' class='freetext' />";
                        html += "<input type='text' class='freetextResponse' placeholder='Describe your goal here' disabled/>";
                        html += "</div>";
                    }

                } else if (s.type === 'bp-goal') {
                    if (s.actions === null || s.actions.length === 0) {
                        // freeform input
                        html += "<div class='action'>";
                        html += "<input type='text' class='systolic' placeholder='Systolic' /> /";
                        html += "<input type='text' class='diastolic' placeholder='Diastolic' />";
                        html += "</div>\n";

                    } else {
                        // predefined multiple-choice goal, these are radio buttons
                        let i = 0;
                        let x = randomChars(5);
                        s.actions.forEach(function (action) {
                            let bpdata = parseBPData(action.label);
                            html += "<div class='action'>";
                            html += "<input name='action" + x + "' type='radio' id='action" + x + "_" + i + "' value='" + action.label + "' data-systolic='" + bpdata.systolic + "' data-diastolic='" + bpdata.diastolic + "' />";
                            html += "<label for='action" + x + "_" + i + "'>" + action.label + "</label></div>\n";
                            i++;
                        });

                        html += "<div class='action'>";
                        html += "<input name='action" + x + "' type='radio' class='custom' />";
                        html += "<input type='text' class='customResponse systolic' placeholder='Systolic' disabled/> / ";
                        html += "<input type='text' class='customResponse diastolic' placeholder='Diastolic' disabled/>";
                        html += "</div>\n";
                    }
                }
                html += "</td>";

                html += "<td><div class='commitToGoalButton'><span>Commit to Goal</span></div></td>\n";

                html += "</tr><tr>";

                let y = randomChars(5);
                html += "<td><label for='goalTargetDate" + y + "'>When do you want to achieve this goal?</label></td>";
                html += "<td><input id='goalTargetDate" + y + "' type='text' class='goalTargetDate' placeholder='--Select Date--' readOnly/></td>";

                html += "</tr></table>";
                html += "</div>\n";

            } else if (s.type === 'update-goal') {
                html += "<div class='goal' data-id='" + s.id + "' data-reference-system='" + s.references.system + "' data-reference-code='" + s.references.code + "'>";
                html += "<span class='heading'>" + s.label + "</span>";
                html += "<table><tr><td>";

                let id = randomChars(5);

                html += "<div><label for='achievementStatus" + id + "'>Achievement Status:</label> <select id='achievementStatus" + id + "' class='achievementStatus'>";

                let a_arr = ['IN_PROGRESS', 'ACHIEVED', 'NOT_ACHIEVED'];
                let a_status = s.goal.achievementStatus;
                a_arr.forEach(function(value) {
                    html += "<option value='" + value + "'";
                    if (value === a_status) {
                        html += " selected";
                    }
                    html += ">" + toLabel(value) + "</option>\n";
                });

                html += "</select></div>\n";

                html += "</td><td>";

                html += "<div class='updateGoalButton'><span>Record Progress</span></div></td>\n";
                html += "</td>";
                html += "</tr><tr>";

                html += "</td></tr></table>";
                html += "</div>\n";
            }
        });
    }
    return html !== "" ?
        "<div class='goalsContainer'>" + html + "</div>" :
        "";
}

function buildLinksHTML(suggestions) {
    let html = "";
    if (suggestions !== null) {
        suggestions.forEach(function(s) {
            if (s.type === 'suggestion-link') {
                html += "<div class='link'>";
                html += "<span class='heading'>" + s.label + "</span>";
                html += "<table><tbody>";

                // Iterate through links
                s.actions.forEach(function(action) {
                	html += "<tr><td>";
                	html += "<a href='" + action.url + "'>" + action.label + "</a>";
                	html += "</td></tr>";
                });
 
                html += "</tbody></table>";
                html += "</div>\n";
            }
        });
    }
    return html !== "" ?
        "<div class='linksContainer'>" + html + "</div>" :
        "";
}

function parseBPData(s) {
    // expected format: "{systolic}/{diastolic}"
    let regex = /^.*?(\d+)\s*\/\s*(\d+).*?$/
    let match = regex.exec(s);

    let obj = {};
    obj.systolic = match[1];
    obj.diastolic = match[2];
    return obj;
}

function buildGoalData(button) {
    let goal = $(button).closest('.goal');
    let g = {};
    g.extGoalId = $(goal).attr('data-id');
    g.referenceSystem = $(goal).attr('data-reference-system');
    g.referenceCode = $(goal).attr('data-reference-code');
    g.goalText = getGoalText(goal);
    g.systolicTarget = 0;
    g.diastolicTarget = 0;
    g.targetDate = $(goal).find('.goalTargetDate').datepicker('getDate');
    return g;
}

function buildBPGoalData(button) {
    let goal = $(button).closest('.bpGoal');
    let g = {};
    g.extGoalId = $(goal).attr('data-id');
    g.referenceSystem = $(goal).attr('data-reference-system');
    g.referenceCode = $(goal).attr('data-reference-code');
    let target = getGoalBPTarget(goal);
    g.systolicTarget = target.systolic;
    g.diastolicTarget = target.diastolic;
    g.targetDate = $(goal).find('.goalTargetDate').datepicker('getDate');
    return g;
}

function getGoalText(goal) {
    let action = $(goal).find('.action');

    if ($(action).length === 1) {
        return $(action).find('input').val();

    } else {
        let radio = $(action).find("input[type='radio']:checked");
        if ($(radio).hasClass('freetext')) {
            return $(radio).siblings('input.freetextResponse').val();

        } else {
            return $(radio).val();
        }
    }
}

function getGoalBPTarget(bpGoal) {
    let action = $(bpGoal).find('.action');

    if ($(action).length === 1) {
        let obj = {};
        obj.systolic = $(action).find('input.systolic').val();
        obj.diastolic = $(action).find('input.diastolic').val();
        return obj;

    } else {
        let radio = $(action).find("input[type='radio']:checked");
        if ($(radio).hasClass('custom')) {
            let obj = {};
            obj.systolic = $(radio).siblings('input.customResponse.systolic').val();
            obj.diastolic = $(radio).siblings('input.customResponse.diastolic').val();
            return obj;

        } else {
            let obj = {};
            obj.systolic = $(radio).attr('data-systolic');
            obj.diastolic = $(radio).attr('data-diastolic');
            return obj;
        }
    }
}

function buildGoalUpdateData(button) {
    let goal = $(button).closest('.goal');
    let g = {};
    g.extGoalId = $(goal).attr('data-id');
    g.achievementStatus = $(goal).find('.achievementStatus').find(':selected').val();
    return g;
}

function buildCounselingData(a) {
    let counseling = $(a).closest('.counseling');
    let c = {};
    c.extCounselingId = $(counseling).attr('data-id');
    c.referenceSystem = $(counseling).attr('data-reference-system');
    c.referenceCode = $(counseling).attr('data-reference-code');
    c.counselingText = a.innerText;
    return c;
}

async function registerCounselingReceived(c, _callback) {
    let formData = new FormData();
    formData.append("extCounselingId", c.extCounselingId);
    formData.append("referenceSystem", c.referenceSystem);
    formData.append("referenceCode", c.referenceCode);
    formData.append("counselingText", c.counselingText);

    const response = await fetch("/counseling/create", {
        method: "POST",
        body: formData
    });

    await response.text();

    _callback(response.status);
}

async function createGoal(g, _callback) {
    let targetDateTS = $.datepicker.formatDate('@', g.targetDate);

    let formData = new FormData();
    formData.append("extGoalId", g.extGoalId);
    formData.append("referenceSystem", g.referenceSystem);
    formData.append("referenceCode", g.referenceCode);
    formData.append("goalText", g.goalText);
    formData.append("targetDateTS", targetDateTS);

    let response = await fetch("/goals/create", {
        method: "POST",
        body: formData
    });

    await response.text();

    _callback(response.status);
}

async function createBPGoal(g, _callback) {
    let targetDateTS = $.datepicker.formatDate('@', g.targetDate);

    let formData = new FormData();
    formData.append("extGoalId", g.extGoalId);
    formData.append("referenceSystem", g.referenceSystem);
    formData.append("referenceCode", g.referenceCode);
    formData.append("systolicTarget", g.systolicTarget);
    formData.append("diastolicTarget", g.diastolicTarget);
    formData.append("targetDateTS", targetDateTS);

    let response = await fetch("/goals/createbp", {
        method: "POST",
        body: formData
    });

    await response.text();

    _callback(response.status);
}

async function updateGoal(g, _callback) {
    let formData = new FormData();
    formData.append("extGoalId", g.extGoalId);
    formData.append("achievementStatus", g.achievementStatus);

    let response = await fetch("/goals/update", {
        method: "POST",
        body: formData
    });

    await response.text();

    _callback(response.status);
}

function hide(el) {
    $(el).fadeOut();
}

$(document).ready(function() {
    enableHover('.commitToGoalButton');
    enableHover('.updateGoalButton');

    $(document).on('click', '.goal .commitToGoalButton', function() {
        let g = buildGoalData(this);
        let container = $(this).closest('.goal');

        createGoal(g, function(status) {
            if (status === 200) {
                hide(container);
            }
        });
    });

    $(document).on('click', '.bpGoal .commitToGoalButton', function() {
        let g = buildBPGoalData(this);
        let container = $(this).closest('.bpGoal');

        createBPGoal(g, function(status) {
            if (status === 200) {
                hide(container);
            }
        });
    });

    $(document).on('click', '.goal .updateGoalButton', function() {
        let g = buildGoalUpdateData(this);
        let container = $(this).closest('.goal');

        updateGoal(g, function(status) {
            if (status === 200) {
                hide(container);
            }
        });
    });

    $(document).on('click', '.counseling .actions a', function(event) {
        event.preventDefault();
        let a = $(this);
        let c = buildCounselingData(this);
        registerCounselingReceived(c, function(status) {
            window.location.href = $(a).attr('href');
        });
    });

    $(document).on('click', '.goal .action input[type="radio"]', function() {
        let el = $(this).closest('.goal').find('input.freetextResponse');
        if ($(this).hasClass('freetext')) {
            $(el).prop('disabled', false);
            $(el).focus();

        } else {
            $(el).prop('disabled', true);
        }
    });

    $(document).on('click', '.bpGoal .action input[type="radio"]', function() {
        let el = $(this).closest('.bpGoal').find('input.customResponse');
        if ($(this).hasClass('custom')) {
            $(el).prop('disabled', false);
            $(el).first().focus();

        } else {
            $(el).prop('disabled', true);
        }
    });
});