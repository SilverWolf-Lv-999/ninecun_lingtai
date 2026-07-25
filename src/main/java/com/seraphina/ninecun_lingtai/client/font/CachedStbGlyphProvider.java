package com.seraphina.ninecun_lingtai.client.font;

import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.blaze3d.font.GlyphProvider;
import com.mojang.blaze3d.font.SheetGlyphInfo;
import com.mojang.blaze3d.platform.NativeImage;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.stb.STBTruetype;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.function.Function;

public final class CachedStbGlyphProvider implements GlyphProvider {
    private final float oversample;
    private final float pointScale;
    private final float ascent;
    private final Int2ObjectMap<GlyphInfo> glyphCache = new Int2ObjectOpenHashMap<>();
    private final IntSet missingGlyphs = new IntOpenHashSet();

    private ByteBuffer fontMemory;
    private STBTTFontinfo fontInfo;
    private IntSet supportedGlyphs;

    private CachedStbGlyphProvider(
            ByteBuffer fontMemory,
            STBTTFontinfo fontInfo,
            float fontSize,
            float oversample
    ) {
        this.fontMemory = fontMemory;
        this.fontInfo = fontInfo;
        this.oversample = oversample;
        this.pointScale = STBTruetype.stbtt_ScaleForPixelHeight(fontInfo, fontSize * oversample);

        int[] ascent = new int[1];
        STBTruetype.stbtt_GetFontVMetrics(fontInfo, ascent, new int[1], new int[1]);
        this.ascent = ascent[0] * this.pointScale;
    }

    public static CachedStbGlyphProvider load(Path fontPath, float fontSize, float oversample) throws IOException {
        long fontFileSize = Files.size(fontPath);
        if (fontFileSize <= 0 || fontFileSize > Integer.MAX_VALUE) {
            throw new IOException("Invalid font file size: " + fontFileSize);
        }

        ByteBuffer fontMemory = MemoryUtil.memAlloc((int) fontFileSize);
        STBTTFontinfo fontInfo = STBTTFontinfo.malloc();
        boolean initialized = false;
        try (FileChannel fileChannel = FileChannel.open(fontPath, StandardOpenOption.READ)) {
            while (fontMemory.hasRemaining()) {
                int readLength = fileChannel.read(fontMemory);
                if (readLength < 0) {
                    break;
                }
            }
            if (fontMemory.hasRemaining()) {
                throw new IOException("Unable to read the complete font file: " + fontPath);
            }
            fontMemory.flip();
            if (!STBTruetype.stbtt_InitFont(fontInfo, fontMemory)) {
                throw new IOException("STB failed to initialize font: " + fontPath);
            }
            initialized = true;
            return new CachedStbGlyphProvider(fontMemory, fontInfo, fontSize, oversample);
        } finally {
            if (!initialized) {
                fontInfo.free();
                MemoryUtil.memFree(fontMemory);
            }
        }
    }

    @Override
    public synchronized GlyphInfo getGlyph(int codePoint) {
        validateOpen();
        if (!Character.isValidCodePoint(codePoint) || missingGlyphs.contains(codePoint)) {
            return null;
        }
        if (glyphCache.containsKey(codePoint)) {
            return glyphCache.get(codePoint);
        }

        int glyphIndex = STBTruetype.stbtt_FindGlyphIndex(fontInfo, codePoint);
        if (glyphIndex == 0) {
            missingGlyphs.add(codePoint);
            return null;
        }

        int[] advanceWidth = new int[1];
        int[] leftSideBearing = new int[1];
        int[] bitmapLeft = new int[1];
        int[] bitmapTop = new int[1];
        int[] bitmapRight = new int[1];
        int[] bitmapBottom = new int[1];
        STBTruetype.stbtt_GetGlyphHMetrics(fontInfo, glyphIndex, advanceWidth, leftSideBearing);
        STBTruetype.stbtt_GetGlyphBitmapBoxSubpixel(
                fontInfo,
                glyphIndex,
                pointScale,
                pointScale,
                0.0F,
                0.0F,
                bitmapLeft,
                bitmapTop,
                bitmapRight,
                bitmapBottom
        );

        float advance = advanceWidth[0] * pointScale / oversample;
        int pixelWidth = bitmapRight[0] - bitmapLeft[0];
        int pixelHeight = bitmapBottom[0] - bitmapTop[0];
        GlyphInfo glyphInfo;
        if (pixelWidth <= 0 || pixelHeight <= 0) {
            glyphInfo = new SpaceGlyph(advance);
        } else {
            float bearingX = (leftSideBearing[0] * pointScale + bitmapLeft[0]) / oversample;
            float bearingY = (ascent + bitmapTop[0]) / oversample;
            glyphInfo = new CachedGlyph(
                    glyphIndex,
                    pixelWidth,
                    pixelHeight,
                    bearingX,
                    bearingY,
                    advance
            );
        }
        glyphCache.put(codePoint, glyphInfo);
        return glyphInfo;
    }

    @Override
    public synchronized IntSet getSupportedGlyphs() {
        validateOpen();
        if (supportedGlyphs != null) {
            return supportedGlyphs;
        }

        IntSet discoveredGlyphs = new IntOpenHashSet();
        for (int codePoint = Character.MIN_CODE_POINT; codePoint <= Character.MAX_VALUE; codePoint++) {
            if (!Character.isSurrogate((char) codePoint)
                    && STBTruetype.stbtt_FindGlyphIndex(fontInfo, codePoint) != 0) {
                discoveredGlyphs.add(codePoint);
            }
        }
        supportedGlyphs = discoveredGlyphs;
        return supportedGlyphs;
    }

    @Override
    public synchronized void close() {
        glyphCache.clear();
        missingGlyphs.clear();
        supportedGlyphs = null;
        if (fontInfo != null) {
            fontInfo.free();
            fontInfo = null;
        }
        if (fontMemory != null) {
            MemoryUtil.memFree(fontMemory);
            fontMemory = null;
        }
    }

    private synchronized byte[] rasterizeGlyph(int glyphIndex, int pixelWidth, int pixelHeight) {
        validateOpen();
        ByteBuffer bitmap = MemoryUtil.memAlloc(pixelWidth * pixelHeight);
        try {
            STBTruetype.stbtt_MakeGlyphBitmapSubpixel(
                    fontInfo,
                    bitmap,
                    pixelWidth,
                    pixelHeight,
                    pixelWidth,
                    pointScale,
                    pointScale,
                    0.0F,
                    0.0F,
                    glyphIndex
            );
            byte[] pixels = new byte[pixelWidth * pixelHeight];
            bitmap.get(0, pixels);
            return pixels;
        } finally {
            MemoryUtil.memFree(bitmap);
        }
    }

    private void validateOpen() {
        if (fontMemory == null || fontInfo == null) {
            throw new IllegalStateException("Font provider is already closed");
        }
    }

    private static final class SpaceGlyph implements GlyphInfo.SpaceGlyphInfo {
        private final float advance;

        private SpaceGlyph(float advance) {
            this.advance = advance;
        }

        @Override
        public float getAdvance() {
            return advance;
        }
    }

    private final class CachedGlyph implements GlyphInfo {
        private final int glyphIndex;
        private final int pixelWidth;
        private final int pixelHeight;
        private final float bearingX;
        private final float bearingY;
        private final float advance;

        private byte[] pixelCache;

        private CachedGlyph(
                int glyphIndex,
                int pixelWidth,
                int pixelHeight,
                float bearingX,
                float bearingY,
                float advance
        ) {
            this.glyphIndex = glyphIndex;
            this.pixelWidth = pixelWidth;
            this.pixelHeight = pixelHeight;
            this.bearingX = bearingX;
            this.bearingY = bearingY;
            this.advance = advance;
        }

        @Override
        public float getAdvance() {
            return advance;
        }

        @Override
        public BakedGlyph bake(Function<SheetGlyphInfo, BakedGlyph> glyphStitcher) {
            return glyphStitcher.apply(new CachedSheetGlyph());
        }

        private synchronized byte[] getPixels() {
            if (pixelCache == null) {
                pixelCache = rasterizeGlyph(glyphIndex, pixelWidth, pixelHeight);
            }
            return pixelCache;
        }

        private final class CachedSheetGlyph implements SheetGlyphInfo {
            @Override
            public int getPixelWidth() {
                return pixelWidth;
            }

            @Override
            public int getPixelHeight() {
                return pixelHeight;
            }

            @Override
            public void upload(int xOffset, int yOffset) {
                byte[] pixels = getPixels();
                NativeImage image = new NativeImage(
                        NativeImage.Format.LUMINANCE,
                        pixelWidth,
                        pixelHeight,
                        false
                );
                for (int pixelY = 0; pixelY < pixelHeight; pixelY++) {
                    for (int pixelX = 0; pixelX < pixelWidth; pixelX++) {
                        image.setPixelLuminance(
                                pixelX,
                                pixelY,
                                pixels[pixelY * pixelWidth + pixelX]
                        );
                    }
                }
                image.upload(0, xOffset, yOffset, 0, 0, pixelWidth, pixelHeight, false, true);
            }

            @Override
            public boolean isColored() {
                return false;
            }

            @Override
            public float getOversample() {
                return oversample;
            }

            @Override
            public float getBearingX() {
                return bearingX;
            }

            @Override
            public float getBearingY() {
                return bearingY;
            }
        }
    }
}
