// Aplica el modo oscuro guardado en este navegador antes de que se pinte la página (evita el destello claro).
// Por defecto la aplicación es clara. Es un archivo aparte porque la CSP solo permite scripts del propio dominio.
try {
  if (localStorage.getItem('ueb-horarios-tema') === 'oscuro') document.documentElement.setAttribute('data-theme', 'dark');
} catch (e) {}
