package unthemedweapons;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.listeners.ListenerManagerAPI;
import com.fs.starfarer.launcher.ModManager;
import unthemedweapons.campaign.RefitTabListenerAndScript;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.net.URL;
import java.net.URLClassLoader;

@SuppressWarnings("unused")
public class ModPlugin extends BaseModPlugin {

    @Override
    public void onApplicationLoad() {
        ModManager manager = ModManager.getInstance();
        if (manager.isModEnabled("unthemedweapons")) {
            throw new RuntimeException("Both Unthemed Weapons Collection and Unthemed Weapons Utilities are enabled! Disable one or the other.");
        }
    }

    @Override
    public void onGameLoad(boolean newGame) {
        ListenerManagerAPI listeners = Global.getSector().getListenerManager();
        try {
            Class<?> cls = getClassLoader().loadClass("unthemedweapons.campaign.RefitTabListenerAndScript");
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            MethodHandle mh = lookup.findConstructor(cls, MethodType.methodType(void.class));
            EveryFrameScript refitModifier = (EveryFrameScript) mh.invoke();
            Global.getSector().addTransientScript(refitModifier);
            if (!listeners.hasListenerOfClass(RefitTabListenerAndScript.class)) {
                listeners.addListener(refitModifier, true);
            }
        } catch (Throwable e) {
            throw new RuntimeException("Failed to add refit tab listener", e);
        }
    }

    private static final String[] reflectionWhitelist = new String[] {
            "unthemedweapons.campaign.RefitTabListenerAndScript",
            "unthemedweapons.util.ReflectionUtils",
            "unthemedweapons.util.DynamicWeaponStats"
    };

    public static ReflectionEnabledClassLoader getClassLoader() {
        URL url = ModPlugin.class.getProtectionDomain().getCodeSource().getLocation();
        return new ReflectionEnabledClassLoader(url, ModPlugin.class.getClassLoader());
    }

    public static class ReflectionEnabledClassLoader extends URLClassLoader {

        public ReflectionEnabledClassLoader(URL url, ClassLoader parent) {
            super(new URL[] {url}, parent);
        }

        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            if (name.startsWith("java.lang.reflect")) {
                return ClassLoader.getSystemClassLoader().loadClass(name);
            }
            return super.loadClass(name);
        }

        @Override
        public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            Class<?> c = findLoadedClass(name);
            if (c != null) {
                return c;
            }
            // Be the defining classloader for all classes in the reflection whitelist
            // For classes defined by this loader, classes in java.lang.reflect will be loaded directly
            // by the system classloader, without the intermediate delegations.
            for (String str : reflectionWhitelist) {
                if (name.startsWith(str)) {
                    return findClass(name);
                }
            }
            return super.loadClass(name, resolve);
        }
    }
}
