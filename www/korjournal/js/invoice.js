$(document).ready(function() {
    $('[data-toggle="tooltip"]').tooltip();
    $.fn.editable.defaults.mode = 'inline';
    $.fn.editable.defaults.emptytext = 'Inget';

    $('.customer_name').each(function() {
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
            params: function(params) {
                var data = {}
                data['customer_name'] = params.value;
                data['link_id'] = $('[name=invoice_link_id]').val();
                return JSON.stringify(data);
            }
        });
    });
       $('.customer_address').each(function() {
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
            params: function(params) {
                var data = {}
                data['customer_address'] = params.value;
                data['link_id'] = $('[name=invoice_link_id]').val();
                return JSON.stringify(data);
            }
        });
    });
});
