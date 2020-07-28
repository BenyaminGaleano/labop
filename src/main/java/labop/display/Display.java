package labop.display;

import labop.config.ConfigParser;

public class Display {
    public static final String RESET = System.getProperty("os.name").toLowerCase().contains("windows") ? ""
            : "\u001B[0m";
    public static final String RED = System.getProperty("os.name").toLowerCase().contains("windows") ? ""
            : "\u001B[31m";
    public static final String GREEN = System.getProperty("os.name").toLowerCase().contains("windows") ? ""
            : "\u001B[32m";
    public static final String BLUE = System.getProperty("os.name").toLowerCase().contains("windows") ? ""
            : "\u001B[34m";
    public static final String YELLOW = System.getProperty("os.name").toLowerCase().contains("windows") ? ""
            : "\u001B[33m";

    public final ConfigParser config;

    private final String error;
    private final String normal;
    private final String information;
    private final String warning;

    public Display(ConfigParser config){
        this.config = config;
        this.error = "["+config.get("error", String.class) +"]: ";
        this.normal = "["+config.get("normal", String.class) +"]: ";
        this.information = "["+config.get("information", String.class) +"]: ";
        this.warning = "["+config.get("warning", String.class) +"]: ";
    }

    public void show(final Object info, final String color) {
        if (color == null) {
            System.out.print(info);
            return;
        }
        System.out.print(color.toString() + info + RESET);
    }

    public void error(final Object info) {
        show(error, RED);
        show(info + "\n\n", null);
    }

    public void msg(final Object info) {
        show(normal, GREEN);
        show(info + "\n\n", null);
    }

    public void inf(final Object info) {
        show(information, BLUE);
        show(info + "\n\n", null);
    }

    public void warning(final Object info) {
        show(warning, YELLOW);
        show(info + "\n\n", null);
    }
}