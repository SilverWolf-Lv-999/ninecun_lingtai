package net.seraphina.ninecun_lingtai.transform;

import net.minecraftforge.fml.loading.ImmediateWindowProvider;
import net.seraphina.ninecun_lingtai.utility.launch.LaunchPluginUtility;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Optional;
import java.util.function.*;

@SuppressWarnings("all")
public final class SeraIWP implements ImmediateWindowProvider {
    public static final Logger LOGGER = LogManager.getLogger();

    @Override
    public String name() {
        return "sera_iwp";
    }

    @Override
    public Runnable initialize(String[] arguments) {
        return ()->{
            LOGGER.info("SeraIWP initializing...");
            LaunchPluginUtility.clearPlugins();
        };
    }

    @Override
    public void updateFramebufferSize(IntConsumer width, IntConsumer height) {

    }

    @Override
    public long setupMinecraftWindow(IntSupplier width, IntSupplier height, Supplier<String> title, LongSupplier monitor) {
        return 0;
    }

    @Override
    public boolean positionWindow(Optional<Object> monitor, IntConsumer widthSetter, IntConsumer heightSetter, IntConsumer xSetter, IntConsumer ySetter) {
        return false;
    }

    @Override
    public <T> Supplier<T> loadingOverlay(Supplier<?> mc, Supplier<?> ri, Consumer<Optional<Throwable>> ex, boolean fade) {
        return null;
    }

    @Override
    public void updateModuleReads(ModuleLayer layer) {

    }

    @Override
    public void periodicTick() {

    }

    @Override
    public String getGLVersion() {
        return "";
    }
}
