$(document).ready(function() {
    $('[data-toggle="tooltip"]').tooltip();
    //toggle `popup` / `inline` mode
    $.fn.editable.defaults.mode = 'inline';
    $.fn.editable.defaults.emptytext = 'Inget';

    $('.kilometer').each(function() {
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
                data['odometer'] = params.value;
                return JSON.stringify(data);
            }
        });
    });

    $('.vehicle').each(function() {
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
                data['vehicle'] = location.protocol
                    + '//' + location.host
                    + "/api/vehicle/"
                    + params.value + '/';
                return JSON.stringify(data);
            }
        });
    });

    $('.streetaddress').each(function() {
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
                data['where'] = params.value;
                return JSON.stringify(data);
            }
        });
    });

    $('.datetime').each(function() {
        $(this).editable({
            mode: 'popup',
            format: 'YYYY-MM-DDTHH:mm',    
            viewformat: 'YYYY-MM-DD HH:mm',    
            template: 'YYYY-MM-DD HH:mm',    
            combodate: {
                minuteStep: 1,
                minYear: 2015,
                maxYear: 2019
            },
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
                data['when'] = params.value + ':00';
                return JSON.stringify(data);
            }
        });
    });

    $('.type').each(function() {
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
                data['type'] = params.value;
                return JSON.stringify(data);
            },
            success: function(response, newValue) {
                location.reload(true);
            }
        });
    });

    $('.reason').each(function() {
        $(this).editable({
            escape: true,
            ajaxOptions: {
                type: 'PATCH',
                dataType: 'json',
                contentType: 'application/json',
                headers: {
                    'X-CSRFToken': $('[name=csrfmiddlewaretoken]').val()
                }
            },
            typeahead: {
                local: JSON.parse($('meta[name=why_typeahead]').attr('content'))
            },
            params: function(params) {
                var data = {}
                data['why'] = params.value;
                return JSON.stringify(data);
            },
            success: function(response, newValue) {
                location.reload(true);
            }
        });
    });
});
