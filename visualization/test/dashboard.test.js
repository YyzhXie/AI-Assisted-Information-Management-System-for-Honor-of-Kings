import assert from "node:assert/strict";
import { describe, it } from "node:test";
import { buildDashboard, loadGameData } from "../server.js";

const data = await loadGameData();
const dashboard = buildDashboard(data);

describe("Node.js visualization dashboard data", () => {
  it("keeps the public payload free of admin accounts and passwords", () => {
    const payload = JSON.stringify(dashboard);
    assert.equal(payload.includes("admin123"), false);
    assert.equal(payload.includes("coach123"), false);
    assert.equal(payload.includes("系统管理员"), false);
    assert.equal(payload.includes("战术教练"), false);
    assert.equal(payload.includes('"password"'), false);
  });

  it("uses the same top win-rate player as the Java accuracy report", () => {
    assert.equal(dashboard.rankings.winRate[0].id, "P001");
    assert.equal(dashboard.rankings.winRate[0].displayName, "阿离同学");
    assert.equal(dashboard.rankings.winRate[0].winRate.toFixed(2), "70.00");
  });

  it("keeps the external operation equipment in Hou Yi recommendations", () => {
    const houYi = dashboard.heroes.find((hero) => hero.id === "h003");
    assert.ok(houYi);
    assert.deepEqual(houYi.recommendedEquipment.map((item) => item.name), ["破军", "无尽战刃", "日渊"]);
  });

  it("keeps empty team t004 stable for long-term operations", () => {
    const emptyTeam = dashboard.teams.find((team) => team.id === "t004");
    assert.equal(emptyTeam.name, "云梦试训队");
    assert.equal(emptyTeam.memberCount, 0);
    assert.equal(emptyTeam.winRate, 0);
  });
});
