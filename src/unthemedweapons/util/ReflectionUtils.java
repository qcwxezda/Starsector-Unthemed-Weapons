package unthemedweapons.util;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.CoreUIAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ReflectionUtils {

    public static Object getField(Object o, String fieldName) {
        try {
            return getFieldNoCatch(o, fieldName);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Object getFieldNoCatch(Object o, String fieldName) throws NoSuchFieldException, IllegalAccessException {
        if (o == null) return null;
        Field field = o.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(o);
    }

    public static void setField(Object o, String fieldName, Object value) {
        if (o == null) return;
        try {
            Field field = o.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(o, value);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static UIPanelAPI getCoreUI() {
        CampaignUIAPI campaignUI = Global.getSector().getCampaignUI();
        InteractionDialogAPI dialog = campaignUI.getCurrentInteractionDialog();

        CoreUIAPI core;
        if (dialog == null) {
            core = (CoreUIAPI) ReflectionUtils.getField(campaignUI, "core");
        }
        else {
            core = (CoreUIAPI) ReflectionUtils.invokeMethod(dialog, "getCoreUI");
        }
        return core == null ? null : (UIPanelAPI) core;
    }

    public static Object invokeMethod(Object o, String methodName, Object... args) {
        return invokeMethodExt(o, methodName, false, args);
    }

    public static Object invokeMethodExt(Object o, String methodName, boolean isDeclaredAndHidden, Object... args) {
        try {
            return invokeMethodNoCatchExt(o, methodName, isDeclaredAndHidden, args);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Object invokeMethodExtWithClasses(Object o, String methodName, boolean isDeclaredAndHidden, Class<?>[] classes, Object[] args) {
        try {
            return invokeMethodNoCatchExtWithClasses(o, methodName, isDeclaredAndHidden, classes, args);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Object invokeMethodNoCatch(Object o, String methodName, Object... args) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        return invokeMethodNoCatchExt(o, methodName, false, args);
    }

    public static Object invokeMethodNoCatchExt(Object o, String methodName, boolean isDeclaredAndHidden, Object... args) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        if (o == null) return null;
        Class<?>[] argClasses = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++) {
            argClasses[i] = args[i].getClass();
            // unbox
            if (argClasses[i] == Integer.class) {
                argClasses[i] = int.class;
            } else if (argClasses[i] == Boolean.class) {
                argClasses[i] = boolean.class;
            } else if (argClasses[i] == Float.class) {
                argClasses[i] = float.class;
            }
        }
        Method method = isDeclaredAndHidden ? o.getClass().getDeclaredMethod(methodName, argClasses) : o.getClass().getMethod(methodName, argClasses);
        if (isDeclaredAndHidden) {
            method.setAccessible(true);
        }
        return method.invoke(o, args);
    }

    public static Object invokeMethodNoCatchExtWithClasses(Object o, String methodName, boolean isDeclaredAndHidden, Class<?>[] classes, Object[] args) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        Method method = isDeclaredAndHidden ? o.getClass().getDeclaredMethod(methodName, classes) : o.getClass().getMethod(methodName, classes);
        if (isDeclaredAndHidden) {
            method.setAccessible(true);
        }
        return method.invoke(o, args);
    }
}
