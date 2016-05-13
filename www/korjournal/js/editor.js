$(document).ready(function() {
    //toggle `popup` / `inline` mode
    $.fn.editable.defaults.mode = 'inline';
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
});
