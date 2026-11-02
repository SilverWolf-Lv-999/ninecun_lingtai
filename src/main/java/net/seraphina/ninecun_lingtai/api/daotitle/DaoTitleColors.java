package net.seraphina.ninecun_lingtai.api.daotitle;

public record DaoTitleColors(int firstColor, int secondColor) {
    public DaoTitleColors {
        firstColor = firstColor & 0xFFFFFF;
        secondColor = secondColor & 0xFFFFFF;
    }

    public static DaoTitleColors defaults() {
        return new DaoTitleColors(0xF4D58D, 0xFFF8D6);
    }
}
