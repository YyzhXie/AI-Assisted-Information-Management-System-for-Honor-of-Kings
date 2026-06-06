package service;

public record CombatRoundReport(int round, int teamADamage, int teamBDamage,
                                int teamAHealth, int teamBHealth, String eventSummary) {
}
