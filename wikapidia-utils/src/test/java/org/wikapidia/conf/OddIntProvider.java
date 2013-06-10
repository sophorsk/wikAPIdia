package org.wikapidia.conf;

import com.typesafe.config.Config;

import static org.junit.Assert.assertEquals;

public class OddIntProvider extends Provider<Integer> {
    private int count = 1;

    /**
     * Creates a new provider instance.
     * Concrete implementations must only use this two-argument constructor.
     *
     * @param configurator
     * @param config
     */
    public OddIntProvider(Configurator configurator, Configuration config) throws ConfigurationException {
        super(configurator, config);
    }

    @Override
    public Class getType() {
        return Integer.class;
    }

    @Override
    public Integer get(String name, Class klass, Config config) throws ConfigurationException {
        if (!config.getString("type").equals("odd")) {
            return null;
        }
        assertEquals(klass, Integer.class);
        int result = count;
        count += 2;
        return result;
    }
}