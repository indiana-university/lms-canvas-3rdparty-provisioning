/*-
 * #%L
 * lms-lti-3rdpartyprovisioning
 * %%
 * Copyright (C) 2015 - 2022 Indiana University
 * %%
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the Indiana University nor the names of its contributors
 *    may be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
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

    $('#notificationForm').on('input change selectionchange', function(){
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
