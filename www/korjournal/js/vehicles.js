$(document).ready(function() {
    $('[data-toggle="tooltip"]').tooltip();
    //toggle `popup` / `inline` mode
    $.fn.editable.defaults.mode = 'inline';
    $.fn.editable.defaults.emptytext = 'Lägg till...';

    $('.vehicle_name').each(function() {
        $(this).editable({
            ajaxOptions: {
                type: 'PATCH',
                dataType: 'json',
                contentType: 'application/json',
                headers: {
                    'X-CSRFToken': $('[name=csrfmiddlewaretoken]').val()
                }
            },
            params: function(params) {
                var data = {}
                data['name'] = params.value;
                return JSON.stringify(data);
            }
        });
    });
    $('.newdriver_name').each(function() {
        $(this).editable({
            ajaxOptions: {
                type: 'POST',
                dataType: 'json',
                contentType: 'application/json',
                headers: {
                    'X-CSRFToken': $('[name=csrfmiddlewaretoken]').val()
                }
            },
            params: function(params) {
                var data = {}
                data['user'] = params.value;
                data['vehicle'] = params.pk;
                return JSON.stringify(data);
            },
            success: function(response, newValue) {
                location.reload(true);
            }
        });
    });
    $('.newvehicle_name').each(function() {
        $(this).editable({
            ajaxOptions: {
                type: 'POST',
                dataType: 'json',
                contentType: 'application/json',
                headers: {
                    'X-CSRFToken': $('[name=csrfmiddlewaretoken]').val()
                }
            },
            params: function(params) {
                var data = {}
                data['name'] = params.value;
                return JSON.stringify(data);
            },
            success: function(response, newValue) {
                location.reload(true);
            },
            error: function(response,newValue) {
                if (response.responseJSON.name) {
                    return 'Det finns redan ett fordon med det namnet'
                }
            }
        });
    });

});
