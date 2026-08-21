package net.seraphina.ninecun_lingtai.client.render.cosmic;

import net.seraphina.ninecun_lingtai.client.render.cosmic.core.CustomVertexElements;
import com.mojang.blaze3d.vertex.BufferBuilder;
import org.joml.Matrix4f;

public class IBufferBuilder extends BufferBuilder {
    public IBufferBuilder(int pCapacity) {
        super(pCapacity);
    }
    public IBufferBuilder progress(Matrix4f tinyMatrix){
        if (this.currentElement().getUsage() != CustomVertexElements.TINY_MATRIX){
            throw new RuntimeException();
        }
        this.putFloat(0, tinyMatrix.m00());
        this.putFloat(4, tinyMatrix.m01());
        this.putFloat(8, tinyMatrix.m02());
        this.putFloat(12, tinyMatrix.m03());
        this.putFloat(16, tinyMatrix.m10());
        this.putFloat(20, tinyMatrix.m11());
        this.putFloat(24, tinyMatrix.m12());
        this.putFloat(28, tinyMatrix.m13());
        this.putFloat(32, tinyMatrix.m20());
        this.putFloat(36, tinyMatrix.m21());
        this.putFloat(40, tinyMatrix.m22());
        this.putFloat(44, tinyMatrix.m23());
        this.putFloat(48, tinyMatrix.m30());
        this.putFloat(52, tinyMatrix.m31());
        this.putFloat(56, tinyMatrix.m32());
        this.putFloat(60, tinyMatrix.m33());
        this.nextElement();
        return this;
    }
}
