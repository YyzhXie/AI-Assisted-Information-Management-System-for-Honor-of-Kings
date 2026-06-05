import { createServer } from "node:http";
import { readFile } from "node:fs/promises";
import { extname, join, normalize, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const currentFile = fileURLToPath(import.meta.url);
const __dirname = fileURLToPath(new URL(".", import.meta.url));
const projectRoot = resolve(__dirname, "..");
const dataFile = join(projectRoot, "data", "game-data.json");
const publicDir = join(__dirname, "public");
const port = Number.parseInt(process.env.PORT || "3000", 10);

const mimeTypes = {
  ".html": "text/html; charset=utf-8",
  ".css": "text/css; charset=utf-8",
  ".js": "text/javascript; charset=utf-8",
  ".json": "application/json; charset=utf-8",
  ".ico": "image/x-icon"
};

export async function loadGameData(filePath = dataFile) {
  const raw = await readFile(filePath, "utf8");
  return JSON.parse(raw);
}

export function buildDashboard(data) {
  const teamById = new Map(data.teams.map((team) => [team.id, team]));
  const heroById = new Map(data.heroes.map((hero) => [hero.id, hero]));
  const equipmentById = new Map(data.equipment.map((item) => [item.id, item]));

  const players = data.players.map((player) => {
    const totalMatches = player.wins + player.losses;
    const winRate = totalMatches === 0 ? 0 : (player.wins * 100) / totalMatches;
    return {
      id: player.id,
      username: player.username,
      displayName: player.displayName,
      level: player.level,
      wins: player.wins,
      losses: player.losses,
      totalMatches,
      winRate,
      teamId: player.teamId,
      teamName: teamById.get(player.teamId)?.name || "暂无战队",
      heroes: player.heroIds.map((heroId) => ({
        id: heroId,
        name: heroById.get(heroId)?.name || heroId,
        type: heroById.get(heroId)?.type || "UNKNOWN"
      }))
    };
  });

  const matches = data.matches
    .map((match) => ({
      id: match.id,
      date: match.date,
      teamAId: match.teamAId,
      teamAName: teamById.get(match.teamAId)?.name || match.teamAId,
      teamBId: match.teamBId,
      teamBName: teamById.get(match.teamBId)?.name || match.teamBId,
      winnerTeamId: match.winnerTeamId,
      winnerTeamName: teamById.get(match.winnerTeamId)?.name || match.winnerTeamId,
      playerHeroChoices: match.playerHeroChoices
    }))
    .sort((a, b) => b.date.localeCompare(a.date));

  const teams = data.teams.map((team) => {
    const members = players.filter((player) => team.memberIds.includes(player.id));
    const teamMatches = matches.filter((match) => match.teamAId === team.id || match.teamBId === team.id);
    const wins = teamMatches.filter((match) => match.winnerTeamId === team.id).length;
    const averageLevel = average(members.map((player) => player.level));
    const winRate = teamMatches.length === 0 ? 0 : (wins * 100) / teamMatches.length;
    const topPlayer = [...members].sort(playerRankingComparator)[0];

    return {
      id: team.id,
      name: team.name,
      memberCount: members.length,
      members: members.map((player) => ({
        id: player.id,
        displayName: player.displayName,
        level: player.level,
        winRate: player.winRate
      })),
      averageLevel,
      totalMatches: teamMatches.length,
      wins,
      losses: teamMatches.length - wins,
      winRate,
      topPlayer: topPlayer
        ? { id: topPlayer.id, displayName: topPlayer.displayName, winRate: topPlayer.winRate }
        : null
    };
  });

  const equipmentUsage = new Map();
  for (const hero of data.heroes) {
    for (const equipmentId of hero.compatibleEquipmentIds) {
      equipmentUsage.set(equipmentId, (equipmentUsage.get(equipmentId) || 0) + 1);
    }
  }

  const equipment = data.equipment
    .map((item) => {
      const usageCount = equipmentUsage.get(item.id) || 0;
      return {
        id: item.id,
        name: item.name,
        type: item.type,
        rating: item.rating,
        attributeDescription: item.attributeDescription,
        usageCount,
        score: item.rating + usageCount * 3.5
      };
    })
    .sort((a, b) => b.score - a.score || a.id.localeCompare(b.id));

  const heroes = data.heroes.map((hero) => {
    const owners = players.filter((player) => player.heroes.some((ownedHero) => ownedHero.id === hero.id));
    const compatibleEquipment = hero.compatibleEquipmentIds.map((equipmentId) => {
      const item = equipmentById.get(equipmentId);
      return {
        id: equipmentId,
        name: item?.name || equipmentId,
        type: item?.type || "UNKNOWN",
        rating: item?.rating || 0
      };
    });

    return {
      id: hero.id,
      name: hero.name,
      type: hero.type,
      attack: hero.attack,
      defense: hero.defense,
      skillPower: hero.skillPower,
      owners: owners.map((player) => ({ id: player.id, displayName: player.displayName })),
      compatibleEquipment,
      recommendedEquipment: recommendEquipment(hero, compatibleEquipment)
    };
  });

  const heroTypes = Object.entries(
    heroes.reduce((counts, hero) => {
      counts[hero.type] = (counts[hero.type] || 0) + 1;
      return counts;
    }, {})
  ).map(([type, count]) => ({ type, count }));

  const rankings = {
    winRate: [...players].sort(playerRankingComparator).slice(0, 10),
    level: [...players]
      .sort((a, b) => b.level - a.level || b.winRate - a.winRate || b.totalMatches - a.totalMatches || a.id.localeCompare(b.id))
      .slice(0, 10),
    matches: [...players]
      .sort((a, b) => b.totalMatches - a.totalMatches || b.level - a.level || b.winRate - a.winRate || a.id.localeCompare(b.id))
      .slice(0, 10)
  };

  return {
    generatedAt: new Date().toISOString(),
    source: "data/game-data.json",
    summary: {
      playerCount: players.length,
      teamCount: teams.length,
      heroCount: heroes.length,
      equipmentCount: equipment.length,
      matchCount: matches.length,
      averagePlayerLevel: average(players.map((player) => player.level)),
      averageWinRate: average(players.map((player) => player.winRate))
    },
    players,
    teams,
    heroes,
    equipment,
    matches,
    rankings,
    heroTypes
  };
}

export function createDashboardServer() {
  return createServer(async (request, response) => {
    try {
      const url = new URL(request.url || "/", `http://${request.headers.host}`);

      if (url.pathname === "/api/dashboard") {
        const data = await loadGameData();
        sendJson(response, buildDashboard(data));
        return;
      }

      if (url.pathname === "/api/health") {
        sendJson(response, { ok: true, service: "honor-ai-visualization" });
        return;
      }

      const pathname = url.pathname === "/" ? "/index.html" : decodeURIComponent(url.pathname);
      const filePath = normalize(join(publicDir, pathname));
      if (!filePath.startsWith(publicDir)) {
        sendText(response, 403, "Forbidden");
        return;
      }

      const body = await readFile(filePath);
      response.writeHead(200, { "Content-Type": mimeTypes[extname(filePath)] || "application/octet-stream" });
      response.end(body);
    } catch (error) {
      if (error.code === "ENOENT") {
        sendText(response, 404, "Not found");
        return;
      }
      console.error(error);
      sendJson(response, { error: "Dashboard server error" }, 500);
    }
  });
}

function playerRankingComparator(a, b) {
  return b.winRate - a.winRate || b.level - a.level || b.totalMatches - a.totalMatches || a.id.localeCompare(b.id);
}

function recommendEquipment(hero, compatibleEquipment) {
  const preferredTypes = {
    TANK: ["DEFENSE", "MOVEMENT"],
    WARRIOR: ["ATTACK", "DEFENSE"],
    ASSASSIN: ["ATTACK", "JUNGLE"],
    MAGE: ["MAGIC"],
    MARKSMAN: ["ATTACK", "MOVEMENT"],
    SUPPORT: ["SUPPORT", "DEFENSE"]
  };
  const preferred = preferredTypes[hero.type] || [];
  return [...compatibleEquipment]
    .sort((a, b) => {
      const aPreferred = preferred.includes(a.type) ? 1 : 0;
      const bPreferred = preferred.includes(b.type) ? 1 : 0;
      return bPreferred - aPreferred || b.rating - a.rating || a.id.localeCompare(b.id);
    })
    .slice(0, 3);
}

function average(values) {
  if (values.length === 0) {
    return 0;
  }
  return values.reduce((sum, value) => sum + value, 0) / values.length;
}

function sendJson(response, body, statusCode = 200) {
  response.writeHead(statusCode, { "Content-Type": "application/json; charset=utf-8" });
  response.end(JSON.stringify(body));
}

function sendText(response, statusCode, text) {
  response.writeHead(statusCode, { "Content-Type": "text/plain; charset=utf-8" });
  response.end(text);
}

if (currentFile === resolve(process.argv[1] || "")) {
  createDashboardServer().listen(port, () => {
    console.log(`Node.js可视化服务已启动: http://localhost:${port}`);
  });
}
