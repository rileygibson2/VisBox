package visbox.logger;

import java.lang.reflect.Field;

import visbox.VBMain;

public class Logger {
    public static final String RESET  = "\u001B[0m";
    public static final String BLACK  = "\u001B[30m";
    public static final String RED    = "\u001B[31m";
    public static final String GREEN  = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";
    public static final String BLUE   = "\u001B[34m";
    public static final String PURPLE = "\u001B[35m";
    public static final String CYAN   = "\u001B[36m";
    public static final String WHITE  = "\u001B[37m";
    
    private static final StackWalker WALKER = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
    private static boolean verbose = false;
    
    public static void setVerbose(boolean v) {verbose = v;}

    public static boolean verbose() {return verbose;}
    
    public static void info(String m) {
        Class<?> caller = getCaller();
        String col = getColorFor(caller);
        if (col==null) {col = LogColorEnum.GREEN.ansi;}
        System.out.println(col+"["+caller.getSimpleName()+"]: "+m+LogColorEnum.RESET.ansi);
    }
    
    private static void debug(String callerName, String m) {
        System.out.println(LogColorEnum.YELLOW.ansi+"[DEBUG] ["+callerName+"]: "+m+LogColorEnum.RESET.ansi);
    }

    public static void debug(String m) {debug(getCaller().getSimpleName(), m);}

    public static void debugAt(long t, String m) {
        if (VBMain.getGlobalTick()==t) debug(getCaller().getSimpleName(), m);
    }

    public static void debugAtInterval(long i, String m) {
        if (VBMain.getGlobalTick()%i==0) debug(getCaller().getSimpleName(), m);
    }
    
    public static void error(String m) {
        System.err.println(LogColorEnum.RED.ansi+"[ERROR] ["+getCaller().getSimpleName()+"]: "+m+LogColorEnum.RESET.ansi);
    }

    public static void warn(String m) {
        System.err.println(LogColorEnum.YELLOW.ansi+"[WARNING] ["+getCaller().getSimpleName()+"]: "+m+LogColorEnum.RESET.ansi);
    }
    
    public static void ln() {System.out.println();}
    
    private static String getColorFor(Class<?> cls) {
        LogColor annotation = cls.getAnnotation(LogColor.class);
        if (annotation != null) {
            return annotation.value().ansi;
        }
        return null;
    }
    
    
    private static Class<?> getCaller() {
        return WALKER.walk(frames -> frames.skip(2).findFirst().get().getDeclaringClass());
    }
}