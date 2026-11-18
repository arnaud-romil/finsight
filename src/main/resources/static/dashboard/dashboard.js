"use strict";

const state = { symbols: [], selectedIds: new Set(), series: [], hiddenSeries: new Set(), chart: null };
const palette = ["#6ee7c3", "#60a5fa", "#f9a8d4", "#fbbf24", "#a78bfa", "#fb7185", "#38bdf8"];

const elements = {
    symbolSearch: document.querySelector("#symbolSearch"),
    symbolOptions: document.querySelector("#symbolOptions"),
    selectionCount: document.querySelector("#selectionCount"),
    fromDate: document.querySelector("#fromDate"),
    toDate: document.querySelector("#toDate"),
    compareButton: document.querySelector("#compareButton"),
    reloadButton: document.querySelector("#reloadButton"),
    status: document.querySelector("#status"),
    canvas: document.querySelector("#comparisonChart"),
    empty: document.querySelector("#chartEmpty"),
    legend: document.querySelector("#legend"),
    ignored: document.querySelector("#ignoredSymbols"),
    summaryBody: document.querySelector("#summaryBody")
};

async function fetchJson(url) {
    const response = await fetch(url, { headers: { Accept: "application/json" } });
    if (!response.ok) {
        let message = `Erreur HTTP ${response.status}`;
        try { message = (await response.json()).error || message; } catch (_) { }
        throw new Error(message);
    }
    return response.json();
}

function setStatus(message = "", isError = false) {
    elements.status.hidden = !message;
    elements.status.className = isError ? "status error" : "status";
    elements.status.textContent = message;
}

function displaySymbolOptions(filter = "") {
    const query = filter.trim().toLowerCase();
    const matches = state.symbols.filter(symbol =>
        [symbol.code, symbol.name, symbol.exchange, symbol.currency]
            .some(value => String(value).toLowerCase().includes(query)));

    elements.symbolOptions.replaceChildren();
    for (const symbol of matches) {
        const selected = state.selectedIds.has(symbol.id);
        const label = document.createElement("label");
        label.className = `symbol-option${selected ? " selected" : ""}`;
        label.innerHTML = `
      <input type="checkbox" value="${symbol.id}" ${selected ? "checked" : ""}>
      <strong>${escapeHtml(symbol.code)} — ${escapeHtml(symbol.name)}</strong>
      <small>${escapeHtml(symbol.exchange)} · ${escapeHtml(symbol.currency)}</small>`;
        const checkbox = label.querySelector("input");
        checkbox.addEventListener("change", () => toggleSymbol(symbol.id, checkbox.checked));
        elements.symbolOptions.append(label);
    }
}

function toggleSymbol(id, selected) {
    if (selected && state.selectedIds.size >= 50) {
        setStatus("Maximum 50 instruments par comparaison.", true);
        displaySymbolOptions(elements.symbolSearch.value);
        return;
    }
    selected ? state.selectedIds.add(id) : state.selectedIds.delete(id);
    elements.selectionCount.textContent = `${state.selectedIds.size} sélectionné${state.selectedIds.size > 1 ? "s" : ""}`;
    displaySymbolOptions(elements.symbolSearch.value);
    setStatus("");
}

function escapeHtml(value) {
    const span = document.createElement("span");
    span.textContent = value ?? "";
    return span.innerHTML;
}

async function loadSymbols() {
    setStatus("Chargement des instruments…");
    try {
        state.symbols = await fetchJson("/api/symbols");
        if (state.selectedIds.size === 0 && state.symbols.length) {
            state.selectedIds = new Set(state.symbols.slice(0, 2).map(symbol => symbol.id));
        }
        elements.selectionCount.textContent = `${state.selectedIds.size} sélectionné${state.selectedIds.size > 1 ? "s" : ""}`;
        displaySymbolOptions();
        setStatus("");
    } catch (error) {
        setStatus(error.message, true);
    }
}

function validateRequest() {
    if (state.selectedIds.size === 0) throw new Error("Sélectionnez au moins un instrument.");
    if (state.selectedIds.size > 50) throw new Error("Maximum 50 instruments par comparaison.");
    if (!elements.fromDate.value || !elements.toDate.value) throw new Error("Renseignez les deux dates.");
    if (elements.fromDate.value > elements.toDate.value) throw new Error("La date de début doit précéder la date de fin.");
}

async function compare() {
    try {
        validateRequest();
        elements.compareButton.disabled = true;
        setStatus("Chargement de la comparaison…");

        const params = new URLSearchParams();
        params.set("symbolIds", [...state.selectedIds].join(","));
        params.set("from", elements.fromDate.value);
        params.set("to", elements.toDate.value);

        const response = await fetchJson(`/api/comparisons/base-100?${params}`);
        state.series = response.series;
        state.hiddenSeries.clear();
        render(response);
        setStatus(`${response.series.length} série${response.series.length > 1 ? "s" : ""} chargée${response.series.length > 1 ? "s" : ""}.`);
    } catch (error) {
        setStatus(error.message, true);
    } finally {
        elements.compareButton.disabled = false;
    }
}

function destroyChart() {
    if (state.chart) {
        state.chart.destroy();
        state.chart = null;
    }
}

function render(response) {
    destroyChart();
    renderLegend();
    renderIgnored(response.ignoredSymbolIds, response.series);
    renderSummary(response.series);

    elements.empty.hidden = response.series.length > 0;
    if (response.series.length === 0) return;

    const context = elements.canvas.getContext("2d");
    const datasets = response.series.map((series, index) => ({
        label: `${series.symbolCode} — ${series.name}`,
        symbolId: series.symbolId,
        borderColor: palette[index % palette.length],
        backgroundColor: palette[index % palette.length],
        borderWidth: 2,
        pointRadius: 0,
        pointHoverRadius: 4,
        tension: 0.15,
        data: series.points.map(point => ({ x: point.date, y: Number(point.base100), adjustedClose: point.adjustedClose }))
    }));

    state.chart = new window.Chart(context, {
        type: "line",
        data: { datasets },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            animation: false,
            interaction: { mode: "nearest", intersect: false, axis: "x" },
            scales: {
                x: { type: "category", grid: { color: "rgba(154,171,194,.10)" }, ticks: { color: "#9aabc2", maxRotation: 45, autoSkip: true, maxTicksLimit: 12 } },
                y: { beginAtZero: false, grid: { color: "rgba(154,171,194,.10)" }, ticks: { color: "#9aabc2", callback: value => Number(value).toFixed(0) } }
            },
            plugins: {
                legend: { display: false },
                tooltip: {
                    callbacks: {
                        title: items => items.length ? items[0].raw.x : "",
                        label: context => {
                            const series = response.series.find(item => item.symbolId === context.dataset.symbolId);
                            return `${series.symbolCode}: ${Number(context.raw.y).toFixed(6)} (cours ajusté ${context.raw.adjustedClose} ${series.currency})`;
                        }
                    }
                }
            }
        }
    });
}

function renderLegend() {
    elements.legend.replaceChildren();
    state.series.forEach((series, index) => {
        const item = document.createElement("button");
        item.type = "button";
        item.className = `legend-item${state.hiddenSeries.has(series.symbolId) ? " hidden-series" : ""}`;
        item.innerHTML = `<span class="legend-dot" style="background:${palette[index % palette.length]}"></span>${escapeHtml(series.symbolCode)}`;
        item.setAttribute("aria-pressed", String(!state.hiddenSeries.has(series.symbolId)));
        item.addEventListener("click", () => toggleDataset(series.symbolId));
        elements.legend.append(item);
    });
}

function toggleDataset(symbolId) {
    state.hiddenSeries.has(symbolId) ? state.hiddenSeries.delete(symbolId) : state.hiddenSeries.add(symbolId);
    if (state.chart) {
        state.chart.data.datasets.forEach(dataset => {
            if (dataset.symbolId === symbolId) dataset.hidden = state.hiddenSeries.has(symbolId);
        });
        state.chart.update();
    }
    renderLegend();
}

function renderIgnored(ids, series) {
    if (!ids || ids.length === 0) { elements.ignored.hidden = true; return; }
    const names = ids.map(id => {
        const known = state.symbols.find(symbol => symbol.id === id);
        return known ? known.code : `ID ${id}`;
    });
    elements.ignored.hidden = false;
    elements.ignored.textContent = `Instruments ignorés (sans cours ou baseline invalide) : ${names.join(", ")}`;
}

function renderSummary(series) {
    elements.summaryBody.replaceChildren();
    if (series.length === 0) {
        elements.summaryBody.innerHTML = `<tr><td colspan="6" class="muted">Aucune série disponible pour cette période.</td></tr>`;
        return;
    }
    for (const item of series) {
        const first = item.points[0];
        const last = item.points.at(-1);
        const change = last && first ? (Number(last.base100) / Number(first.base100) - 1) * 100 : 0;
        const row = document.createElement("tr");
        row.innerHTML = `
      <td><strong>${escapeHtml(item.symbolCode)}</strong><br><span class="muted">${escapeHtml(item.name)}</span></td>
      <td>${escapeHtml(item.currency)}</td>
      <td>${Number(item.baseline).toFixed(6)}</td>
      <td>${first ? `${first.date}<br>${Number(first.base100).toFixed(6)}` : "—"}</td>
      <td>${last ? `${last.date}<br>${Number(last.base100).toFixed(6)}` : "—"}</td>
      <td>${first && last ? `${change >= 0 ? "+" : ""}${change.toFixed(2)} %` : "—"}</td>`;
        elements.summaryBody.append(row);
    }
}

function loadChartLibrary() {
    if (window.Chart) return Promise.resolve();
    return new Promise((resolve, reject) => {
        const script = document.createElement("script");
        script.src = "https://cdn.jsdelivr.net/npm/chart.js@4.4.7/dist/chart.umd.min.js";
        script.onload = resolve;
        script.onerror = () => reject(new Error("Impossible de charger la bibliothèque graphique."));
        document.head.append(script);
    });
}

elements.symbolSearch.addEventListener("input", event => displaySymbolOptions(event.target.value));
elements.compareButton.addEventListener("click", compare);
elements.reloadButton.addEventListener("click", () => loadSymbols().then(() => compare()));

loadChartLibrary()
    .then(loadSymbols)
    .then(() => compare())
    .catch(error => setStatus(error.message, true));
