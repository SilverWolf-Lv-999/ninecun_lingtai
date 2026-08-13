package net.seraphina.ninecun_lingtai.api.alchemy;

public record AlchemyBrewScore(
        double fireScore,
        double environmentScore,
        double realmScore,
        double rootScore,
        double soulScore,
        double totalScore,
        double successRate
) {
}
