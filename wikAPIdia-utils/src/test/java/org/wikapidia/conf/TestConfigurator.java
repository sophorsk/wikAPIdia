package org.wikapidia.conf;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class TestConfigurator {
    public static final String INTMAKER_PATH =  "some.path.intMaker";

    @Test
    public void testSimple() throws ConfigurationException {
        // Should pick up configuration in reference.conf
        Configurator conf = new Configurator(new Configuration());
        Integer i = conf.get(Integer.class, "foo");
        assertEquals(i, 42);
        Integer j = conf.get(Integer.class, "bar");
        assertEquals(j, 23);
        Integer k = conf.get(Integer.class, "baz");
        assertEquals(k, 0);
        Integer l = conf.get(Integer.class, "biff");
        assertEquals(l, 1);
    }

    @Test
    public void testSpecificFile() throws ConfigurationException, IOException {
        File tmp = File.createTempFile("myconf", ".conf", null);
        tmp.deleteOnExit();
        FileUtils.write(tmp,
                "providers : { some.path.intMaker += org.wikapidia.conf.OddIntProvider }\n" +
                "some.path.intMaker : { aaa : { type : odd } }\n" +
                "some.path.intMaker : { bar : { value : 99 } }\n" +
                "some.path.intMaker : { bbb : { type : odd } }\n"
            );
        Configurator conf = new Configurator(new Configuration(tmp));

        Integer i = conf.get(Integer.class, "foo");
        assertEquals(i, 42);
        Integer j = conf.get(Integer.class, "bar");
        assertEquals(j, 99);
        Integer k = conf.get(Integer.class, "baz");
        assertEquals(k, 0);
        Integer l = conf.get(Integer.class, "biff");
        assertEquals(l, 1);

        Integer m = conf.get(Integer.class, "aaa");
        assertEquals(m, 1);
        Integer n = conf.get(Integer.class, "bbb");
        assertEquals(n, 3);

        assertEquals(conf.getConf().get().getInt("some.path.intMaker.bar.value"), 99);

        tmp.delete();
    }

    @Test
    public void testOverrideVariables() throws ConfigurationException, IOException {
        File tmp = File.createTempFile("myconf", ".conf", null);
        tmp.deleteOnExit();
        FileUtils.write(tmp, "constants.x : 9\n");
        Configurator conf = new Configurator(new Configuration(tmp));

        assertEquals(conf.getConf().get().getInt("constants.x"), 9);
        Integer i = conf.get(Integer.class, "foo");
        assertEquals(i, 92);
        Integer j = conf.get(Integer.class, "bar");
        assertEquals(j, 23);
        Integer k = conf.get(Integer.class, "baz");
        assertEquals(k, 0);
        Integer l = conf.get(Integer.class, "biff");
        assertEquals(l, 1);

        tmp.delete();
    }

    @Test
    public void testOverrideMap() throws ConfigurationException, IOException {
        File tmp = File.createTempFile("myconf", ".conf", null);
        tmp.deleteOnExit();
        FileUtils.write(tmp, "constants.x : 9\n");
        Map<String, Object> overrides = new HashMap<String, Object>();
        overrides.put("foo", Arrays.asList("a", "b", "c"));
        overrides.put("constants.x", 8);
        Configurator conf = new Configurator(new Configuration(overrides, tmp));

        assertEquals(conf.getConf().get().getInt("constants.x"), 8);
        Integer i = conf.get(Integer.class, "foo");
        assertEquals(i, 82);
        Integer j = conf.get(Integer.class, "bar");
        assertEquals(j, 23);
        Integer k = conf.get(Integer.class, "baz");
        assertEquals(k, 0);
        Integer l = conf.get(Integer.class, "biff");
        assertEquals(l, 1);
        assertEquals(conf.getConf().get().getStringList("foo"), Arrays.asList("a", "b", "c"));

        tmp.delete();
    }
}
