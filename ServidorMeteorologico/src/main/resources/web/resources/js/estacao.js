/* Front da Estação Climática.
   Mostra as leituras gravadas no banco pelo coletor, lidas pela API do ServidorMeteorologico:
     GET api/leituras/ultima · GET api/leituras?limite=N · GET api/resumo                       */
var MAX_PONTOS = 60, grafico, cfg;
var TX = '#4A3F4F', NUVEM = '#D9D4EC', CINZA = '#ADA7C0', GOTA = '#7FBFA5', SOL = '#F3B266', RAIO = '#E99055';

/* ---------- Ícones (nuvenzinhas) ---------- */
function rosto() {
  return '<circle cx="58" cy="68" r="2.6" fill="' + TX + '"/><circle cx="76" cy="68" r="2.6" fill="' + TX + '"/>' +
         '<path d="M62 74q5 5 10 0" stroke="' + TX + '" stroke-width="2.4" fill="none" stroke-linecap="round"/>';
}
function nuvem(cor, x, y, s) {
  return '<g transform="translate(' + x + ' ' + y + ') scale(' + s + ')"><circle cx="44" cy="64" r="18" fill="' + cor + '"/>' +
    '<circle cx="66" cy="50" r="24" fill="' + cor + '"/><circle cx="88" cy="66" r="16" fill="' + cor + '"/>' +
    '<rect x="28" y="62" width="72" height="26" rx="13" fill="' + cor + '"/>' + rosto() + '</g>';
}
function sol(x, y, s) {
  var r = '';
  for (var i = 0; i < 8; i++) {
    var a = i * Math.PI / 4, c = Math.cos(a), n = Math.sin(a);
    r += '<line x1="' + (60 + c * 36) + '" y1="' + (60 + n * 36) + '" x2="' + (60 + c * 47) + '" y2="' + (60 + n * 47) +
         '" stroke="' + RAIO + '" stroke-width="6" stroke-linecap="round"/>';
  }
  return '<g transform="translate(' + x + ' ' + y + ') scale(' + s + ')"><g class="raios">' + r + '</g><circle cx="60" cy="60" r="26" fill="' + SOL + '"/>' +
         '<g transform="translate(-7 -8)">' + rosto() + '</g></g>';
}
function gota(x, y) { return '<line class="gota" x1="' + x + '" y1="' + y + '" x2="' + (x - 5) + '" y2="' + (y + 13) + '" stroke="' + GOTA + '" stroke-width="5" stroke-linecap="round"/>'; }
function floco(x, y) {
  var l = '';
  [0, 60, 120].forEach(function (g) { l += '<line class="floco" x1="' + x + '" y1="' + (y - 6) + '" x2="' + x + '" y2="' + (y + 6) + '" transform="rotate(' + g + ' ' + x + ' ' + y + ')" stroke="' + GOTA + '" stroke-width="2.6" stroke-linecap="round"/>'; });
  return l;
}
function linhas() {
  return [96, 104, 112].map(function (y, i) { return '<line x1="' + (30 + i * 4) + '" y1="' + y + '" x2="' + (92 - i * 4) + '" y2="' + y + '" stroke="' + CINZA + '" stroke-width="5" stroke-linecap="round"/>'; }).join('');
}
var ICONES = {
  sol: sol(0, 0, 1),
  parcial: sol(28, -10, .68) + nuvem(NUVEM, -4, 14, .95),
  nublado: nuvem(NUVEM, 0, 4, 1),
  neblina: nuvem(CINZA, 0, -12, 1) + linhas(),
  chuva: nuvem(NUVEM, 0, -10, 1) + gota(46, 86) + gota(64, 92) + gota(82, 86),
  chuvaForte: nuvem(NUVEM, 0, -12, 1) + gota(38, 84) + gota(52, 94) + gota(66, 84) + gota(80, 94) + gota(94, 84),
  tempestade: nuvem(CINZA, 0, -12, 1) + '<polygon class="raio" points="64,78 52,100 62,100 56,116 78,90 67,90 74,78" fill="' + SOL + '"/>' + gota(40, 90) + gota(92, 90),
  neve: nuvem(NUVEM, 0, -10, 1) + floco(46, 94) + floco(66, 102) + floco(86, 94)
};
var ROTULOS = { sol: 'Ensolarado', parcial: 'Parcialmente nublado', nublado: 'Nublado', neblina: 'Neblina',
                chuva: 'Chuva fraca', chuvaForte: 'Chuva forte', tempestade: 'Tempestade', neve: 'Neve' };
function svgIcone(k) { return '<svg viewBox="0 0 120 120" role="img" aria-label="' + ROTULOS[k] + '">' + ICONES[k] + '</svg>'; }

/* Só usada se a leitura vier sem código WMO: deduz a condição pelos valores medidos. */
function condicao(l) {
  if (l.chovendo) { if (l.velocidadeVento > 25) return 'tempestade'; return l.umidade > 85 ? 'chuvaForte' : 'chuva'; }
  if (l.umidade >= 88 && l.velocidadeVento < 5) return 'neblina';
  if (l.umidade > 72) return 'nublado';
  return l.umidade > 55 ? 'parcial' : 'sol';
}

/* ---------- Preferências (página Configurações, guardadas neste navegador) ---------- */
var SERIES = ['temperatura', 'umidade', 'precipitacao', 'vento'];
function configPadrao() { return { atualizacao: 10, unidade: 'C', series: { temperatura: true, umidade: true, precipitacao: true, vento: true } }; }
function lerConfig() {
  var c = configPadrao(), salva = null;
  try { salva = JSON.parse(localStorage.getItem('estacaoConfig')); } catch (e) {}
  if (salva) {
    var seg = parseInt(salva.atualizacao, 10);
    if (seg >= 5 && seg <= 300) c.atualizacao = seg;
    if (salva.unidade === 'F') c.unidade = 'F';
    if (salva.series) SERIES.forEach(function (s) { if (typeof salva.series[s] === 'boolean') c.series[s] = salva.series[s]; });
  }
  return c;
}

/* ---------- Formatação ---------- */
function definir(id, t) { var e = document.getElementById(id); if (e) e.textContent = t; }
function num(v, casas) { return v == null ? '--' : v.toLocaleString('pt-BR', { minimumFractionDigits: casas, maximumFractionDigits: casas }); }
function temp(c) { return c == null ? null : (cfg.unidade === 'F' ? c * 9 / 5 + 32 : c); }
function rumo(graus) { return graus == null ? '' : ['N', 'NE', 'L', 'SE', 'S', 'SO', 'O', 'NO'][Math.round(graus / 45) % 8]; }
function duracao(s) {
  if (s < 90) return Math.round(s) + ' s';
  if (s < 5400) return Math.round(s / 60) + ' min';
  if (s < 172800) return Math.round(s / 3600) + ' h';
  return Math.round(s / 86400) + ' dias';
}
function estado(tipo, texto) {
  var p = document.getElementById('ponto'); if (p) p.className = 'ponto ' + tipo;
  definir('estado', texto);
}

/* ---------- Comunicação com o servidor ---------- */
function buscar(caminho) {
  return fetch('api/' + caminho, { cache: 'no-store' }).then(function (r) {
    return r.json().catch(function () { return {}; }).then(function (corpo) {
      if (!r.ok) { var e = new Error(corpo.erro || ('Erro HTTP ' + r.status)); e.status = r.status; throw e; }
      return corpo;
    });
  });
}

function atualizar() {
  return Promise.all([buscar('leituras/ultima'), buscar('leituras?limite=' + MAX_PONTOS), buscar('resumo')])
    .then(function (r) { mostrarLeitura(r[0]); mostrarGrafico(r[1]); mostrarResumo(r[2]); })
    .catch(function (e) {
      if (e.status === 404) estado('alerta', 'Nenhuma leitura no banco ainda. Inicie o coletor (aplicação 1).');
      else if (e.status) estado('erro', e.message);
      else estado('erro', 'Sem conexão com o servidor. O ServidorMeteorologico está rodando?');
    });
}

/* Agenda a próxima consulta só depois que a atual termina: duas nunca se sobrepõem. */
function ciclo() { atualizar().then(function () { setTimeout(ciclo, cfg.atualizacao * 1000); }); }

/* ---------- Exibição ---------- */
function mostrarLeitura(l) {
  var quando = new Date(l.dataHoraColeta), k = ICONES[l.icone] ? l.icone : condicao(l), u = '°' + cfg.unidade;
  definir('temp', Math.round(temp(l.temperatura))); definir('unidadeTemp', u);
  definir('cond', l.condicao || ROTULOS[k]);
  definir('sensacao', l.sensacaoTermica == null ? '' : 'Sensação térmica ' + num(temp(l.sensacaoTermica), 0) + ' ' + u);
  definir('umidade', l.umidade + '%');
  definir('pressao', num(l.pressao, 1) + ' hPa');
  definir('vento', num(l.velocidadeVento, 1) + ' km/h ' + rumo(l.direcaoVento));
  definir('chuva', l.chovendo ? num(l.precipitacao, 1) + ' mm' : 'Sem chuva');
  definir('hora', quando.toLocaleTimeString('pt-BR'));
  definir('data', quando.toLocaleDateString('pt-BR', { weekday: 'long', day: '2-digit', month: 'long' }));

  var selo = document.getElementById('origem');
  if (selo) { selo.hidden = l.origem !== 'SIMULADOR'; selo.textContent = 'dados simulados'; }

  var ic = document.getElementById('icone');
  if (ic && ic.getAttribute('data-k') !== k) { ic.setAttribute('data-k', k); ic.innerHTML = svgIcone(k); }

  // Se a última leitura é antiga, o coletor parou: avisar em vez de fingir que está ao vivo
  var atraso = (Date.now() - quando.getTime()) / 1000, fonte = l.origem === 'SIMULADOR' ? 'simulador' : 'Open-Meteo';
  if (atraso > 30) estado('alerta', 'Coletor parado? A última leitura (nº ' + l.id + ') foi há ' + duracao(atraso) + '.');
  else estado('ok', 'Ao vivo · leitura nº ' + l.id + ' · fonte: ' + fonte + ' · página atualiza a cada ' + cfg.atualizacao + ' s');
}

function mostrarGrafico(lista) {
  if (!grafico) return;
  var d = grafico.data;
  d.labels = lista.map(function (l) { return new Date(l.dataHoraColeta).toLocaleTimeString('pt-BR'); });
  d.datasets[0].data = lista.map(function (l) { return temp(l.temperatura); });
  d.datasets[1].data = lista.map(function (l) { return l.umidade; });
  d.datasets[2].data = lista.map(function (l) { return l.velocidadeVento; });
  d.datasets[3].data = lista.map(function (l) { return l.precipitacao; });
  grafico.update();
  definir('tituloGrafico', 'Variáveis em domínio do tempo: últimas ' + lista.length + ' leituras do banco');
}

function mostrarResumo(r) {
  definir('resumoChuva', r.leiturasComChuva + ' de ' + r.leituras);
  definir('resumoTemp', r.leituras === 0 ? 'Nenhuma leitura hoje'
    : 'Hoje: mín ' + num(temp(r.temperaturaMinima), 1) + ' °' + cfg.unidade + ' · máx ' + num(temp(r.temperaturaMaxima), 1) + ' °' + cfg.unidade);
}

/* ---------- Gráfico ---------- */
function criarGrafico() {
  var cv = document.getElementById('grafico'); if (!cv) return;
  if (typeof Chart === 'undefined') { estado('erro', 'Não foi possível carregar o Chart.js (sem internet?). Os valores continuam sendo exibidos.'); return; }
  function serie(n, c, eixo, fill, chave) { return { label: n, data: [], borderColor: c, backgroundColor: c + '1A', fill: fill, tension: .25, pointRadius: 0, pointHoverRadius: 4, pointHitRadius: 10, borderWidth: 2, borderJoinStyle: 'round', yAxisID: eixo, hidden: !cfg.series[chave] }; }
  grafico = new Chart(cv, { type: 'line',
    data: { labels: [], datasets: [serie('Temperatura (°' + cfg.unidade + ')', '#CD5C5C', 'y', true, 'temperatura'), serie('Umidade (%)', '#7FBFA5', 'y1', false, 'umidade'),
                                   serie('Vento (km/h)', '#F3B266', 'y', false, 'vento'), serie('Precipitação (mm)', '#E99055', 'y', false, 'precipitacao')] },
    options: { responsive: true, maintainAspectRatio: false, animation: false, interaction: { mode: 'index', intersect: false },
      plugins: { legend: { position: 'bottom', labels: { usePointStyle: true, font: { family: 'Dongle' } } } },
      scales: { y: { grid: { color: '#F3ECEF' } }, y1: { position: 'right', min: 0, max: 100, grid: { display: false } },
                x: { grid: { display: false }, ticks: { maxTicksLimit: 6 } } } } });
}

/* ---------- Página de configurações ---------- */
function preencherForm(c) {
  document.getElementById('atualizacao').value = c.atualizacao;
  document.getElementById('unidade').value = c.unidade;
  SERIES.forEach(function (s) { document.getElementById('serie-' + s).checked = c.series[s]; });
}
function salvarConfig() {
  var seg = parseInt(document.getElementById('atualizacao').value, 10), c = configPadrao();
  c.atualizacao = seg >= 5 && seg <= 300 ? seg : 10;
  c.unidade = document.getElementById('unidade').value;
  SERIES.forEach(function (s) { c.series[s] = document.getElementById('serie-' + s).checked; });
  var a = document.getElementById('aviso'), ok = true;
  try { localStorage.setItem('estacaoConfig', JSON.stringify(c)); } catch (e) { ok = false; }
  preencherForm(c);
  if (a) { a.textContent = ok ? 'Configurações salvas.' : 'Este navegador bloqueou o armazenamento local; as preferências não foram salvas.'; a.style.display = 'block'; }
  return false;
}
function restaurarConfig() { preencherForm(configPadrao()); salvarConfig(); }

window.addEventListener('DOMContentLoaded', function () {
  cfg = lerConfig();
  if (document.getElementById('formConfig')) { preencherForm(cfg); return; }
  if (!document.getElementById('temp')) return;
  if (location.protocol === 'file:') { estado('erro', 'Abra a página pelo servidor: http://localhost:8080 (não direto do arquivo).'); return; }
  criarGrafico(); ciclo();
});
