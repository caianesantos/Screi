'use strict';
const form = document.getElementById('wizard');
const steps = [...form.querySelectorAll('[data-step]')];
const previous = document.getElementById('previous');
const next = document.getElementById('next');
const submit = document.getElementById('submit');
let current = 0;
function showStep(index) {
  current = index;
  steps.forEach((step, i) => { step.hidden = i !== current; });
  document.querySelectorAll('.steps li').forEach((item, i) => {
    item.classList.toggle('active', i === current);
    if (i === current) item.setAttribute('aria-current', 'step'); else item.removeAttribute('aria-current');
  });
  previous.hidden = current === 0;
  next.hidden = current === steps.length - 1;
  submit.hidden = current !== steps.length - 1;
}
function validateStep() {
  for (const input of steps[current].querySelectorAll('input')) {
    if (!input.checkValidity()) { input.reportValidity(); return false; }
  }
  return true;
}
form.noValidate = true;
previous.addEventListener('click', () => showStep(current - 1));
next.addEventListener('click', () => { if (validateStep()) showStep(current + 1); });
form.addEventListener('submit', event => {
  for (let i = 0; i < steps.length; i++) {
    const invalid = [...steps[i].querySelectorAll('input')].find(input => !input.checkValidity());
    if (invalid) { event.preventDefault(); showStep(i); invalid.reportValidity(); return; }
  }
  submit.disabled = true;
});
const serverError = [...steps].findIndex(step => [...step.querySelectorAll('.text-danger')].some(el => el.textContent.trim()));
showStep(serverError >= 0 ? serverError : 0);
const cep = document.getElementById('responsavel-cep');
const feedback = document.getElementById('cep-feedback');
let request;
cep.addEventListener('input', async () => {
  cep.value = cep.value.replace(/\D/g, '').slice(0, 8);
  if (request) request.abort();
  feedback.textContent = '';
  if (cep.value.length !== 8) return;
  request = new AbortController();
  const value = cep.value;
  feedback.textContent = 'Consultando CEP…';
  try {
    const response = await fetch('/responsavel/cep/' + value, { signal: request.signal });
    const data = await response.json();
    if (cep.value !== value) return;
    if (!response.ok) throw new Error(data.mensagem || 'Consulta indisponível. Preencha manualmente.');
    for (const [field, key] of Object.entries({logradouro:'logradouro', bairro:'bairro', cidade:'localidade', estado:'uf'})) {
      document.getElementById('responsavel-' + field).value = data[key] || '';
    }
    feedback.textContent = 'Endereço localizado. Confira e informe o número.';
  } catch (error) {
    if (error.name !== 'AbortError' && cep.value === value) feedback.textContent = error.message;
  }
});
