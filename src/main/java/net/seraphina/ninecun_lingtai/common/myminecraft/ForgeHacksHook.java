package net.seraphina.ninecun_lingtai.common.myminecraft;

import net.minecraftforge.fml.unsafe.UnsafeHacks;
import net.seraphina.ninecun_lingtai.utility.JDKUtility;

import java.lang.reflect.Field;

@SuppressWarnings("all")
public class ForgeHacksHook extends UnsafeHacks {
    public static <T> T getField(Field field, Object object)
    {
        final long l = JDKUtility.UNSAFE.objectFieldOffset(field);
        return (T) JDKUtility.UNSAFE.getObject(object, l);
    }
}
