console.log('Maldive front code executing');

var initialMd5 = '';

function generateMd5() {
  console.log('Calculating form hash...');
  
  var value = $('.maldive-md5').map(function(index, value) {
    return $(value).val();
  }).get();
  console.log(value);
  return md5(value);
}

$(document).ready(function() {
  $('.submit').hide();
  initialMd5 = generateMd5();
});


function checkForChanges() {
  console.log("Checking for changes");
  if(initialMd5 !== generateMd5()) {
    console.log('Form has changes');
    $('.submit').show();
  } else {
    $('.submit').hide();
  }
}
