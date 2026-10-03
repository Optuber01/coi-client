package dev.ua.ikeepcalm.coi.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The mod's one logger: the prefix is its name, so it cannot drift between call sites.
 */
public class CoiLog {

    public static final Logger LOG = LoggerFactory.getLogger("COI Client");

    private CoiLog() {
    }
}
