package service;

import java.util.List;

public record CombatSimulationReport(String teamAId, String teamAName, String teamBId, String teamBName,
                                     String winnerTeamId, String winnerTeamName,
                                     int teamAFinalHealth, int teamBFinalHealth,
                                     double teamABasePower, double teamBBasePower,
                                     String teamALineup, String teamBLineup,
                                     String environmentSummary, List<CombatRoundReport> rounds) {

    public String formatReport() {
        StringBuilder builder = new StringBuilder();
        builder.append("=== 对战模拟报告 ===\n");
        builder.append("说明: 本次模拟不写入对战记录，不影响排行榜和历史数据。\n");
        builder.append(String.format("队伍A: %s(%s) 基础战力: %.2f%n", teamAName, teamAId, teamABasePower));
        builder.append(String.format("队伍B: %s(%s) 基础战力: %.2f%n", teamBName, teamBId, teamBBasePower));
        builder.append("队伍A阵容:\n").append(teamALineup).append('\n');
        builder.append("队伍B阵容:\n").append(teamBLineup).append('\n');
        builder.append("随机环境: ").append(environmentSummary).append('\n');
        for (CombatRoundReport round : rounds) {
            builder.append(String.format("第%d回合: %s造成%d伤害，%s造成%d伤害，剩余生命 %s:%d / %s:%d。%s%n",
                    round.round(), teamAName, round.teamADamage(), teamBName, round.teamBDamage(),
                    teamAId, round.teamAHealth(), teamBId, round.teamBHealth(), round.eventSummary()));
        }
        builder.append(String.format("模拟胜者: %s(%s)%n", winnerTeamName, winnerTeamId));
        builder.append(String.format("最终生命值: %s %d，%s %d%n", teamAId, teamAFinalHealth, teamBId, teamBFinalHealth));
        return builder.toString();
    }
}
