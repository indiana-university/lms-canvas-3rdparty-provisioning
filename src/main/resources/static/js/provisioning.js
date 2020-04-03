$(document).ready(function(){

    // Listen for a custom "modalOpen" event
    document.addEventListener('modalOpen', event => {
        if (event.detail.name() === 'modal-preview') {
            var selectedFrom = $('#senderUsername option:selected').text();
            var subject = $('#msgSubject').val();
            var body = $('#msgBody').val();

            $('#modalFrom').text(selectedFrom);
            $('#modalSubject').text(subject);
            $('#modalBody').text(body);
        }
    }, false);

    $('#uploadSubmit').click(function() {
        var button = $(this);
        button.addClass("rvt-button--loading");
        button.prop('disabled', true);
        button.attr('aria-busy', "true");
        button.append('<div class="rvt-loader rvt-loader--xs" aria-label="Content loading"></div>');
        $('#uploadForm').submit();
    });

    $('#notificationSubmit').click(function() {
        var button = $(this);
        button.addClass("rvt-button--loading");
        button.prop('disabled', true);
        button.attr('aria-busy', "true");
        button.append('<div class="rvt-loader rvt-loader--xs" aria-label="Content loading"></div>');

        //Disable other buttons
        $('#notificationPreview').prop('disabled', true);
        $('#notificationCancel').prop('disabled', true);

        $('#notificationForm').submit();
    });

    $('#uploadForm').change(function(){
        //Make sure a dept option is selected
        var deptVal = $('#deptDropdown option:selected').val();
        //This should evaluate to true if it's undefined or blank
        var deptEmpty = !deptVal;

        //And at least one file has been selected
        var filesVal = $('#deptFileUpload').val();
        var filesEmpty = !filesVal;

        var disabled = deptEmpty || filesEmpty;
        $('#uploadSubmit').prop('disabled', disabled);
    });

    $('#notificationForm').change(function(){
        //Make sure a sender option is selected
        var senderVal = $('#senderUsername option:selected').val();
        //This should evaluate to true if it's undefined or blank
        var senderEmpty = !senderVal;

        //And subject
        var subjectVal = $('#msgSubject').val();
        var subjectEmpty = !subjectVal;

        //And body
        var bodyVal = $('#msgBody').val();
        var bodyEmpty = !bodyVal;

        var disabled = senderEmpty || subjectEmpty || bodyEmpty;
        $('#notificationSubmit').prop('disabled', disabled);
    });

});