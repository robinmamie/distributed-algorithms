package cs451.logger;

public class Logger {

    private static final boolean LOG_ACTIVE = true;
    private static final long START_TIME = System.nanoTime();

    private final String className;
    private final boolean specificLogActive;

    private long counter = 0;
    private long lastLog = START_TIME;

    public Logger(Object o, boolean specificLogActive) {
        this.className = o.getClass().getSimpleName();
        this.specificLogActive = specificLogActive;
    }

    public Logger(Object o) {
        this(o, true);
    }

    private double getMillisDif(long now, long millis) {
        return Math.round((now - millis) / 1e3) / 1e3;

    }

    private double getMillisDif(long millis) {
        return getMillisDif(System.nanoTime(), millis);
    }

    private double getMillisDifAndReset(long millis) {
        lastLog = System.nanoTime();
        return getMillisDif(lastLog, millis);
    }

    public void log(String comment) {
        if (LOG_ACTIVE && specificLogActive) {
            counter += 1;
            System.err.println(this + ": " + comment + "\n -- time since last log: [" + getMillisDifAndReset(lastLog) + "ms] (count: " + counter + ")");
        }
    }

    @Override
    public String toString() {
        return "[" + getMillisDif(START_TIME) + "ms] " + className;
    }
}
