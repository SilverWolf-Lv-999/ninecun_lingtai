package net.seraphina.ninecun_lingtai.utility.natives;

import com.sun.jna.Function;
import com.sun.jna.Pointer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.seraphina.ninecun_lingtai.utility.system.SysUtility;

@OnlyIn(Dist.CLIENT)
public final class FunctionUtility {
    private static final int WINDOWS_CLOSE_MESSAGE = 0x0010;

    public static void forceCloseWindow(long windowHandle) {
        if (windowHandle == 0L) {
            throw new IllegalArgumentException("Window handle must not be zero");
        }

        if (SysUtility.isWindows()) {
            forceCloseWindowsWindow(windowHandle);
        } else if (SysUtility.isMac()) {
            forceCloseMacWindow(windowHandle);
        } else if (SysUtility.isLinux()) {
            forceCloseLinuxWindow(windowHandle);
        } else {
            throw new UnsupportedOperationException(
                    "Force closing windows is not supported on this operating system"
            );
        }
    }

    private static void forceCloseWindowsWindow(long windowHandle) {
        Pointer nativeWindowHandle = Pointer.createConstant(windowHandle);
        Function isWindow = Function.getFunction("user32", "IsWindow");
        if (isWindow.invokeInt(new Object[]{nativeWindowHandle}) == 0) {
            throw new IllegalArgumentException("The supplied Windows window handle is invalid");
        }

        Function sendMessage = Function.getFunction("user32", "SendMessageW");
        sendMessage.invokeLong(new Object[]{
                nativeWindowHandle,
                WINDOWS_CLOSE_MESSAGE,
                null,
                null
        });

        Function destroyWindow = Function.getFunction("user32", "DestroyWindow");
        if (isWindow.invokeInt(new Object[]{nativeWindowHandle}) != 0
                && destroyWindow.invokeInt(new Object[]{nativeWindowHandle}) == 0
                && isWindow.invokeInt(new Object[]{nativeWindowHandle}) != 0) {
            throw new IllegalStateException("user32!DestroyWindow failed");
        }
    }

    private static void forceCloseMacWindow(long windowHandle) {
        Function registerSelector = Function.getFunction(
                "/usr/lib/libobjc.A.dylib",
                "sel_registerName"
        );
        Pointer closeSelector = registerSelector.invokePointer(new Object[]{"close"});

        Function sendMessage = Function.getFunction(
                "/usr/lib/libobjc.A.dylib",
                "objc_msgSend"
        );
        sendMessage.invokeVoid(new Object[]{
                Pointer.createConstant(windowHandle),
                closeSelector
        });
    }

    private static void forceCloseLinuxWindow(long windowHandle) {
        Function openDisplay = Function.getFunction("X11", "XOpenDisplay");
        Pointer display = openDisplay.invokePointer(new Object[]{null});
        if (display == null) {
            throw new IllegalStateException("X11!XOpenDisplay failed");
        }

        try {
            Function destroyWindow = Function.getFunction("X11", "XDestroyWindow");
            if (destroyWindow.invokeInt(new Object[]{display, windowHandle}) == 0) {
                throw new IllegalStateException("X11!XDestroyWindow failed");
            }

            Function sync = Function.getFunction("X11", "XSync");
            sync.invokeInt(new Object[]{display, 0});
        } finally {
            Function closeDisplay = Function.getFunction("X11", "XCloseDisplay");
            closeDisplay.invokeInt(new Object[]{display});
        }
    }
}
