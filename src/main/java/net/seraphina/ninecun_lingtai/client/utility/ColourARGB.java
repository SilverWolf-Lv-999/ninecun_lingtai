package net.seraphina.ninecun_lingtai.client.utility;

public class ColourARGB extends Colour {
    public ColourARGB(int colour) {
        super(colour >> 16 & 255, colour >> 8 & 255, colour & 255, colour >> 24 & 255);
    }

    public ColourARGB(int a, int r, int g, int b) {
        super(r, g, b, a);
    }

    public ColourARGB(ColourARGB colour) {
        super(colour);
    }

    public static int pack(Colour colour) {
        return (colour.a & 255) << 24 | (colour.r & 255) << 16 | (colour.g & 255) << 8 | colour.b & 255;
    }

    public ColourARGB copy() {
        return new ColourARGB(this);
    }

    public Colour set(int colour) {
        return this.set(new ColourARGB(colour));
    }

    public int pack() {
        return pack(this);
    }

    public float[] packArray() {
        return new float[]{(float) (this.a & 255) / 255.0F, (float) (this.r & 255) / 255.0F, (float) (this.g & 255) / 255.0F, (float) (this.b & 255) / 255.0F};
    }
}
