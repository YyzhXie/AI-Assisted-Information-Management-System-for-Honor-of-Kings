const state = {
  data: null,
  activePlayerId: null,
  activeTeamId: null,
  activeView: "overview"
};

const colors = ["#1f9a8a", "#2e73b8", "#d95f4f", "#d19428", "#4f8c45", "#7a6bb0", "#c45f8c"];

const formatPercent = (value) => `${value.toFixed(2)}%`;
const formatNumber = (value) => Number.isInteger(value) ? String(value) : value.toFixed(2);

const byId = (id) => document.getElementById(id);

async function boot() {
  const response = await fetch("/api/dashboard");
  if (!response.ok) {
    throw new Error("无法加载可视化数据");
  }
  state.data = await response.json();
  state.activePlayerId = state.data.players[0]?.id || null;
  state.activeTeamId = state.data.teams[0]?.id || null;

  bindTabs();
  bindControls();
  renderAll();
}

function bindTabs() {
  document.querySelectorAll(".tab").forEach((button) => {
    button.addEventListener("click", () => {
      state.activeView = button.dataset.view;
      document.querySelectorAll(".tab").forEach((tab) => tab.classList.toggle("is-active", tab === button));
      document.querySelectorAll(".view").forEach((view) => view.classList.toggle("is-active", view.id === `${state.activeView}View`));
      resizeCharts();
    });
  });
}

function bindControls() {
  byId("playerSearch").addEventListener("input", renderPlayers);
  byId("rankingMode").addEventListener("change", renderPlayers);
  byId("heroSearch").addEventListener("input", renderHeroes);
  byId("equipmentSearch").addEventListener("input", renderEquipment);
  byId("matchTeamFilter").addEventListener("change", renderMatches);
  window.addEventListener("resize", debounce(resizeCharts, 120));
}

function renderAll() {
  const { generatedAt, source } = state.data;
  byId("dataSource").textContent = source;
  byId("generatedAt").textContent = new Date(generatedAt).toLocaleString("zh-CN");

  renderSummary();
  renderOverview();
  renderPlayers();
  renderTeams();
  renderHeroes();
  renderEquipment();
  renderMatchFilter();
  renderMatches();
}

function renderSummary() {
  const metrics = [
    ["玩家", state.data.summary.playerCount],
    ["战队", state.data.summary.teamCount],
    ["英雄", state.data.summary.heroCount],
    ["装备", state.data.summary.equipmentCount],
    ["对战记录", state.data.summary.matchCount],
    ["平均等级", state.data.summary.averagePlayerLevel.toFixed(2)]
  ];

  byId("summaryGrid").innerHTML = metrics
    .map(([label, value]) => `<article class="metric"><span>${label}</span><strong>${value}</strong></article>`)
    .join("");
}

function renderOverview() {
  drawBarChart(byId("winRateChart"), {
    labels: state.data.rankings.winRate.slice(0, 8).map((player) => player.displayName),
    values: state.data.rankings.winRate.slice(0, 8).map((player) => player.winRate),
    suffix: "%",
    color: "#1f9a8a"
  });

  drawDonutChart(byId("heroTypeChart"), {
    labels: state.data.heroTypes.map((item) => item.type),
    values: state.data.heroTypes.map((item) => item.count)
  });

  drawBarChart(byId("equipmentChart"), {
    labels: state.data.equipment.slice(0, 8).map((item) => item.name),
    values: state.data.equipment.slice(0, 8).map((item) => item.score),
    suffix: "",
    color: "#d19428"
  });

  byId("teamBars").innerHTML = state.data.teams.map((team) => `
    <div class="team-row">
      <strong>${team.name}</strong>
      <div class="bar-track" title="胜率 ${formatPercent(team.winRate)}">
        <div class="bar-fill" style="width: ${Math.max(team.winRate, 2)}%"></div>
      </div>
      <span class="subtle">${formatPercent(team.winRate)} · ${team.memberCount}人</span>
    </div>
  `).join("");
}

function renderPlayers() {
  const query = byId("playerSearch").value.trim().toLowerCase();
  const mode = byId("rankingMode").value;
  const rankedIds = new Set(state.data.rankings[mode].map((player) => player.id));
  const players = state.data.players
    .filter((player) => {
      if (!query) {
        return true;
      }
      return [player.id, player.username, player.displayName].some((value) => value.toLowerCase().includes(query));
    })
    .sort((a, b) => {
      const aRanked = rankedIds.has(a.id) ? 1 : 0;
      const bRanked = rankedIds.has(b.id) ? 1 : 0;
      return bRanked - aRanked || b.winRate - a.winRate || b.level - a.level || a.id.localeCompare(b.id);
    });

  if (players.length === 0) {
    renderEmpty(byId("playerList"));
    byId("playerDetail").innerHTML = "";
    return;
  }

  if (!players.some((player) => player.id === state.activePlayerId)) {
    state.activePlayerId = players[0].id;
  }

  byId("playerList").innerHTML = players.map((player) => `
    <button class="list-button ${player.id === state.activePlayerId ? "is-active" : ""}" data-player-id="${player.id}" type="button">
      <strong>${player.displayName} (${player.id})</strong>
      <span>${player.teamName} · 等级 ${player.level} · 胜率 ${formatPercent(player.winRate)}</span>
    </button>
  `).join("");

  byId("playerList").querySelectorAll("[data-player-id]").forEach((button) => {
    button.addEventListener("click", () => {
      state.activePlayerId = button.dataset.playerId;
      renderPlayers();
    });
  });

  renderPlayerDetail(state.data.players.find((player) => player.id === state.activePlayerId));
}

function renderPlayerDetail(player) {
  const matches = state.data.matches
    .filter((match) => Object.prototype.hasOwnProperty.call(match.playerHeroChoices, player.id))
    .slice(0, 5);

  byId("playerDetail").innerHTML = `
    <h2>${player.displayName}</h2>
    <p class="subtle">${player.id} · ${player.teamName} · 用户名 ${player.username}</p>
    <div class="detail-grid">
      ${miniStat("等级", player.level)}
      ${miniStat("胜率", formatPercent(player.winRate))}
      ${miniStat("胜场", player.wins)}
      ${miniStat("总场次", player.totalMatches)}
    </div>
    <h3>拥有英雄</h3>
    <div class="chip-row">${player.heroes.map((hero) => `<span class="chip">${hero.name} · ${hero.type}</span>`).join("")}</div>
    <h3>最近对战</h3>
    <div class="timeline compact">
      ${matches.map((match) => {
        const hero = state.data.heroes.find((item) => item.id === match.playerHeroChoices[player.id]);
        const result = match.winnerTeamId === player.teamId ? "胜利" : "失败";
        const opponent = match.teamAId === player.teamId ? match.teamBName : match.teamAName;
        return `<div class="stat-line"><span>${match.date} 对 ${opponent}</span><strong>${result} · ${hero?.name || "未知英雄"}</strong></div>`;
      }).join("")}
    </div>
  `;
}

function renderTeams() {
  byId("teamList").innerHTML = state.data.teams.map((team) => `
    <button class="list-button ${team.id === state.activeTeamId ? "is-active" : ""}" data-team-id="${team.id}" type="button">
      <strong>${team.name}</strong>
      <span>${team.memberCount} 人 · 胜率 ${formatPercent(team.winRate)} · 平均等级 ${team.averageLevel.toFixed(2)}</span>
    </button>
  `).join("");

  byId("teamList").querySelectorAll("[data-team-id]").forEach((button) => {
    button.addEventListener("click", () => {
      state.activeTeamId = button.dataset.teamId;
      renderTeams();
    });
  });

  const team = state.data.teams.find((item) => item.id === state.activeTeamId);
  byId("teamDetail").innerHTML = `
    <h2>${team.name}</h2>
    <p class="subtle">${team.id} · ${team.memberCount === 0 ? "空成员战队" : "正式战队"}</p>
    <div class="detail-grid">
      ${miniStat("成员数", team.memberCount)}
      ${miniStat("胜率", formatPercent(team.winRate))}
      ${miniStat("平均等级", team.averageLevel.toFixed(2))}
      ${miniStat("对战记录", team.totalMatches)}
    </div>
    <h3>顶尖玩家</h3>
    <p>${team.topPlayer ? `${team.topPlayer.displayName} (${team.topPlayer.id}) · ${formatPercent(team.topPlayer.winRate)}` : "暂无"}</p>
    <h3>成员</h3>
    <div class="chip-row">
      ${team.members.length === 0 ? '<span class="chip">暂无成员</span>' : team.members.map((member) => `<span class="chip">${member.displayName} · ${member.id}</span>`).join("")}
    </div>
  `;
}

function renderHeroes() {
  const query = byId("heroSearch").value.trim().toLowerCase();
  const heroes = state.data.heroes.filter((hero) => [hero.id, hero.name, hero.type].some((value) => value.toLowerCase().includes(query)));
  const container = byId("heroGrid");
  if (heroes.length === 0) {
    renderEmpty(container);
    return;
  }
  container.innerHTML = heroes.map((hero) => `
    <article class="item-card">
      <span class="type-badge">${hero.type}</span>
      <h3>${hero.name} (${hero.id})</h3>
      <div class="stat-line"><span>攻击</span><strong>${hero.attack}</strong></div>
      <div class="stat-line"><span>防御</span><strong>${hero.defense}</strong></div>
      <div class="stat-line"><span>技能</span><strong>${hero.skillPower}</strong></div>
      <p class="subtle">推荐：${hero.recommendedEquipment.map((item) => item.name).join("、") || "暂无"}</p>
      <p class="subtle">拥有者：${hero.owners.map((owner) => owner.displayName).join("、") || "暂无"}</p>
    </article>
  `).join("");
}

function renderEquipment() {
  const query = byId("equipmentSearch").value.trim().toLowerCase();
  const equipment = state.data.equipment.filter((item) => [item.id, item.name, item.type].some((value) => value.toLowerCase().includes(query)));
  const container = byId("equipmentGrid");
  if (equipment.length === 0) {
    renderEmpty(container);
    return;
  }
  container.innerHTML = equipment.map((item) => `
    <article class="item-card">
      <span class="type-badge">${item.type}</span>
      <h3>${item.name} (${item.id})</h3>
      <div class="stat-line"><span>基础评分</span><strong>${item.rating.toFixed(1)}</strong></div>
      <div class="stat-line"><span>兼容英雄</span><strong>${item.usageCount}</strong></div>
      <div class="stat-line"><span>综合分数</span><strong>${item.score.toFixed(2)}</strong></div>
      <p class="subtle">${item.attributeDescription}</p>
    </article>
  `).join("");
}

function renderMatchFilter() {
  const select = byId("matchTeamFilter");
  select.innerHTML = '<option value="all">全部战队</option>' + state.data.teams
    .map((team) => `<option value="${team.id}">${team.name}</option>`)
    .join("");
}

function renderMatches() {
  const teamId = byId("matchTeamFilter").value;
  const matches = state.data.matches.filter((match) => teamId === "all" || match.teamAId === teamId || match.teamBId === teamId);
  const container = byId("matchTimeline");
  if (matches.length === 0) {
    renderEmpty(container);
    return;
  }
  container.innerHTML = matches.map((match) => `
    <article class="match-card">
      <strong>${match.date}</strong>
      <div>
        <h3>${match.teamAName} vs ${match.teamBName}</h3>
        <span class="subtle">记录ID ${match.id}</span>
      </div>
      <span class="winner">${match.winnerTeamName} 胜利</span>
    </article>
  `).join("");
}

function miniStat(label, value) {
  return `<div class="mini-stat"><span>${label}</span><strong>${value}</strong></div>`;
}

function renderEmpty(container) {
  container.innerHTML = byId("emptyStateTemplate").innerHTML;
}

function drawBarChart(canvas, config) {
  const { context, width, height } = setupCanvas(canvas);
  context.clearRect(0, 0, width, height);

  const padding = { left: 112, right: 24, top: 16, bottom: 24 };
  const chartWidth = width - padding.left - padding.right;
  const rowHeight = (height - padding.top - padding.bottom) / config.values.length;
  const max = Math.max(...config.values, 1);

  context.font = "13px Microsoft YaHei, Arial";
  context.textBaseline = "middle";

  config.values.forEach((value, index) => {
    const y = padding.top + index * rowHeight + rowHeight * 0.5;
    const barWidth = (value / max) * chartWidth;

    context.fillStyle = "#607080";
    context.textAlign = "right";
    context.fillText(trimLabel(config.labels[index], 7), padding.left - 12, y);

    context.fillStyle = "#e7edf2";
    roundRect(context, padding.left, y - 8, chartWidth, 16, 8);
    context.fill();

    context.fillStyle = config.color;
    roundRect(context, padding.left, y - 8, barWidth, 16, 8);
    context.fill();

    context.fillStyle = "#17202a";
    context.textAlign = "left";
    context.fillText(`${formatNumber(value)}${config.suffix}`, padding.left + barWidth + 8, y);
  });
}

function drawDonutChart(canvas, config) {
  const { context, width, height } = setupCanvas(canvas);
  context.clearRect(0, 0, width, height);

  const total = config.values.reduce((sum, value) => sum + value, 0);
  const radius = Math.min(width, height) * 0.32;
  const centerX = width * 0.38;
  const centerY = height * 0.5;
  let startAngle = -Math.PI / 2;

  config.values.forEach((value, index) => {
    const slice = (value / total) * Math.PI * 2;
    context.beginPath();
    context.moveTo(centerX, centerY);
    context.arc(centerX, centerY, radius, startAngle, startAngle + slice);
    context.closePath();
    context.fillStyle = colors[index % colors.length];
    context.fill();
    startAngle += slice;
  });

  context.globalCompositeOperation = "destination-out";
  context.beginPath();
  context.arc(centerX, centerY, radius * 0.58, 0, Math.PI * 2);
  context.fill();
  context.globalCompositeOperation = "source-over";

  context.font = "13px Microsoft YaHei, Arial";
  context.textBaseline = "middle";
  config.labels.forEach((label, index) => {
    const x = width * 0.72;
    const y = 30 + index * 28;
    context.fillStyle = colors[index % colors.length];
    roundRect(context, x, y - 8, 16, 16, 4);
    context.fill();
    context.fillStyle = "#17202a";
    context.fillText(`${label} · ${config.values[index]}`, x + 24, y);
  });
}

function setupCanvas(canvas) {
  const ratio = window.devicePixelRatio || 1;
  const rect = canvas.getBoundingClientRect();
  const width = Math.max(1, rect.width);
  const height = Math.max(1, Number(canvas.getAttribute("height")));
  canvas.width = Math.floor(width * ratio);
  canvas.height = Math.floor(height * ratio);
  canvas.style.height = `${height}px`;
  const context = canvas.getContext("2d");
  context.setTransform(ratio, 0, 0, ratio, 0, 0);
  return { context, width, height };
}

function resizeCharts() {
  if (!state.data) {
    return;
  }
  renderOverview();
}

function roundRect(context, x, y, width, height, radius) {
  const safeRadius = Math.min(radius, width / 2, height / 2);
  context.beginPath();
  context.moveTo(x + safeRadius, y);
  context.arcTo(x + width, y, x + width, y + height, safeRadius);
  context.arcTo(x + width, y + height, x, y + height, safeRadius);
  context.arcTo(x, y + height, x, y, safeRadius);
  context.arcTo(x, y, x + width, y, safeRadius);
  context.closePath();
}

function trimLabel(label, maxLength) {
  return label.length > maxLength ? `${label.slice(0, maxLength - 1)}…` : label;
}

function debounce(fn, wait) {
  let timer;
  return (...args) => {
    clearTimeout(timer);
    timer = setTimeout(() => fn(...args), wait);
  };
}

boot().catch((error) => {
  document.body.innerHTML = `<main><div class="empty-state">${error.message}</div></main>`;
});
