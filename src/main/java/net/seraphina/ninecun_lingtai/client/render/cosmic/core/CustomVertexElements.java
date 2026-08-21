package net.seraphina.ninecun_lingtai.client.render.cosmic.core;

import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;

public class CustomVertexElements {
    public static VertexFormatElement.Usage TINY_MATRIX;
    public static VertexFormatElement ELEMENT_TINY_MAT;
    private static VertexFormat CIL;
    public static VertexFormat CIL() {
        if (CIL == null) {
            CIL = new VertexFormat(ImmutableMap.<String, VertexFormatElement>builder().put("Position", DefaultVertexFormat.ELEMENT_POSITION).put("Color", DefaultVertexFormat.ELEMENT_COLOR)/*.put("UV0", DefaultVertexFormat.ELEMENT_UV0).put("TinyMatrix", CustomVertexElements.ELEMENT_TINY_MAT)*/.put("Padding", DefaultVertexFormat.ELEMENT_PADDING).build());
        }
        return CIL;
    }
}
