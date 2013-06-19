/*
 *  Copyright (C) 2011 Michele Roncalli <roncallim at gmail dot com>
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package RSLBench.Helpers;

/**
 * Color-enabled logger.
 * <p/>
 * Logging functions are relayed to the robocup rescue simulator's logger.
 * @author Marc Pujol <mpujol@iiia.csic.es>
 */
public class Logger {
    //font attributes

    String FT_BOLD = "\033[1m";
    final static String FT_UNDERLINE = "\033[4m";
    //background color
    public final static String BG_RED = "\033[41m";
    public final static String BG_GREEN = "\033[42m";
    public final static String BG_YELLOW = "\033[43m";
    public final static String BG_LIGHTBLUE = "\033[44m";
    public final static String BG_MAGENTA = "\033[45m";
    public final static String BG_BLUE = "\033[46m";
    public final static String BG_WHITE = "\033[47m";
    // foreground color
    public final static String FG_BLACK = "\033[30m";
    public final static String FG_RED = "\033[31m";
    public final static String FG_GREEN = "\033[32m";
    public final static String FG_YELLOW = "\033[33m";
    public final static String FG_LIGHTBLUE = "\033[34m";
    public final static String FG_MAGENTA = "\033[35m";
    public final static String FG_BLUE = "\033[36m";
    public final static String FG_WHITE = "\033[37m";
    public final static String FG_NORM = "\033[0m";

    private Logger() {
    }

    /**
     * Set the log context for this thread and all child threads.
     *
     * @param context The new log context.
     */
    public static void setLogContext(String context) {
        rescuecore2.log.Logger.setLogContext(context);
    }

    /**
     * Push a log context onto the stack.
     *
     * @param context The new log context.
     */
    public static void pushLogContext(String context) {
        rescuecore2.log.Logger.pushLogContext(context);
    }

    /**
     * Pop a log context from the stack.
     */
    public static void popLogContext() {
        rescuecore2.log.Logger.popLogContext();
    }

    /**
     * Push an item onto the nested diagnostic context.
     *
     * @param s The item to push.
     */
    public static void pushNDC(String s) {
        rescuecore2.log.Logger.pushNDC(s);
    }

    /**
     * Pop an item from the nested diagnostic context.
     */
    public static void popNDC() {
        rescuecore2.log.Logger.popNDC();
    }

    /**
     * Log a trace level message.
     *
     * @param msg The message to log.
     */
    public static void trace(String msg) {
        rescuecore2.log.Logger.trace(msg);
    }

    /**
     * Log a trace level message along with a throwable.
     *
     * @param msg The message to log.
     * @param t The throwable stack trace to log.
     */
    public static void trace(String msg, Throwable t) {
        rescuecore2.log.Logger.trace(msg, t);
    }

    /**
     * Log a debug level message.
     *
     * @param msg The message to log.
     */
    public static void debug(String msg) {
        rescuecore2.log.Logger.debug(msg);
    }

    /**
     * Log a debug level message.
     *
     * @param msg The message to log.
     */
    public static void debugColor(String msg, String color) {
        msg = color + msg + FG_NORM;
        rescuecore2.log.Logger.debug(msg);
    }

    public static void errC(String msg) {
        msg = FG_RED + msg + FG_NORM;
        rescuecore2.log.Logger.debug(msg);
    }

    public static void warnC(String msg) {
        msg = FG_BLUE + msg + FG_NORM;
        rescuecore2.log.Logger.debug(msg);
    }

    public static void infoC(String msg) {
        msg = FG_GREEN + msg + FG_NORM;
        rescuecore2.log.Logger.debug(msg);
    }

    /**
     * Log a debug level message along with a throwable.
     *
     * @param msg The message to log.
     * @param t The throwable stack trace to log.
     */
    public static void debug(String msg, Throwable t) {
        rescuecore2.log.Logger.debug(msg, t);
    }

    /**
     * Log an info level message.
     *
     * @param msg The message to log.
     */
    public static void info(String msg) {
        rescuecore2.log.Logger.info(msg);
    }

    /**
     * Log an info level message along with a throwable.
     *
     * @param msg The message to log.
     * @param t The throwable stack trace to log.
     */
    public static void info(String msg, Throwable t) {
        rescuecore2.log.Logger.info(msg, t);
    }

    /**
     * Log a warn level message.
     *
     * @param msg The message to log.
     */
    public static void warn(String msg) {
        rescuecore2.log.Logger.warn(msg);
    }

    /**
     * Log a warn level message along with a throwable.
     *
     * @param msg The message to log.
     * @param t The throwable stack trace to log.
     */
    public static void warn(String msg, Throwable t) {
        rescuecore2.log.Logger.warn(msg, t);
    }

    /**
     * Log an error level message.
     *
     * @param msg The message to log.
     */
    public static void error(String msg) {
        rescuecore2.log.Logger.error(msg);
    }

    /**
     * Log an error level message along with a throwable.
     *
     * @param msg The message to log.
     * @param t The throwable stack trace to log.
     */
    public static void error(String msg, Throwable t) {
        rescuecore2.log.Logger.error(msg, t);
    }

    /**
     * Log a fatal level message.
     *
     * @param msg The message to log.
     */
    public static void fatal(String msg) {
        rescuecore2.log.Logger.fatal(msg);
    }

    /**
     * Log a fatal level message along with a throwable.
     *
     * @param msg The message to log.
     * @param t The throwable stack trace to log.
     */
    public static void fatal(String msg, Throwable t) {
        rescuecore2.log.Logger.fatal(msg, t);
    }
}
