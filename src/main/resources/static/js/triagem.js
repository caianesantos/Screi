'use strict';
const statusField = document.getElementById('status');
const justificativa = document.getElementById('justificativa');
function updateRequired() { justificativa.required = statusField.value === 'COM_PENDENCIA'; }
statusField.addEventListener('change', updateRequired);
updateRequired();
