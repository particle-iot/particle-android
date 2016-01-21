var inputElement = document.getElementById('iccid');
inputElement.value = '%s';
var e = new Event('change');
e.target = inputElement;
inputElement.dispatchEvent(e);
