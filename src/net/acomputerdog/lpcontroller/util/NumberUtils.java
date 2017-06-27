package net.acomputerdog.lpcontroller.util;

import net.acomputerdog.lpcontroller.ex.ResponseFormatException;

public class NumberUtils {
    public static long parseAxisLoc(String locStr) {
        try {
            int decIdx = locStr.indexOf('.');

            if (decIdx >= 0) {
                long mmS;
                if (decIdx > 0) {
                    mmS = Long.parseLong(locStr.substring(0, decIdx));
                } else {
                    mmS = 0;
                }

                long umS;
                if (decIdx < locStr.length() - 1) {
                    umS = Long.parseLong(locStr.substring(decIdx + 1));
                } else {
                    umS = 0;
                }

                return (mmS * 1000L) + umS;
            } else {
                return Long.parseLong(locStr);
            }
        } catch (NumberFormatException e) {
            throw new ResponseFormatException("Malformed number in input: '" + locStr + "'");
        }
    }
}
