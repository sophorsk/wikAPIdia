package org.wikapidia.conf;

import com.typesafe.config.Config;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.clapper.util.classutil.*;
import org.wikapidia.utils.JvmUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.logging.Logger;

/**
 * Binds together providers for a collection of components. A component is uniquely
 * identified by two elements:
 *
 * 1. A superclass or interface (e.g. DataSource).
 * 2. A name (e.g. 'foo').
 *
 * So there can be multiple instances of the same component type as long as they are
 * uniquely named.
 *
 * The configurator scans the class path for all classes that extend
 * org.wikapidia.conf.Provider. This configurator will instantiate the provider and ask it
 * what class it provides (Provider.getType) and what path of the configuration it handles
 * along (Provider.getPath()).
 *
 * For example, let's say that there are two different providers for DataSource:
 * MySqlDataSourceProvider and H2DataSourceProvider. Both their Provider.getType()
 * methods must return javax.sql.DataSource and both their Provider.getPath() methods
 * must return "dao.dataSource".
 *
 * Given the following config:
 *
 *      ... some top-level elements...
 *
 *      'dao' :
 *          'dataSource' : {
 *              'foo' : { ... config params for foo impl ... },
 *              'bar' : { ... config params for bar impl ... },
 *          }
 *
 * If a client requests the local page dao named 'foo', the configurator iterates
 * through the two providers passing '{ ... config params for foo .. }' until
 * a provider accepts and generates the requested DataSource.
 *
 * A special optional key named 'default' has a value corresponding to the name of
 * implementation that should be used for the version of get() that does not take
 * a name. For example, if the dataSource hashtable above had entry 'default' : 'bar',
 * the 'bar' entry would be used by default if no name was supplied to the get()
 * method.
 *
 * All generated components are considered singletons. Once a named component is
 * generated once, it is cached and reused for future requests.
 */
public class Configurator {
    private static final Logger LOG = Logger.getLogger(Configurator.class.getName());

    public static String PROVIDER_PATH = "providers";

    public static final int MAX_FILE_SIZE = 8 * 1024 * 1024;   // 8MB

    private final Configuration conf;

    /**
     * A collection of providers for a particular type of component (e.g. LocalPageDao).
     */
    private class ProviderSet {
        Class type = null;          // component's superclass or interface
        String path;                // path for config elements of the components
        List<Provider> providers;   // list of providers for the component.

        ProviderSet(Class klass, String path) {
            this.type = klass;
            this.path = path;
            this.providers = new ArrayList<Provider>();
        }
    }

    public Configuration getConf() {
        return conf;
    }

    /**
     * Providers for each component.
     */
    private final Map<Class, ProviderSet> providers = new HashMap<Class, ProviderSet>();

    /**
     * Named instances of each component.
     */
    private final Map<Class, Map<String, Object>> components = new HashMap<Class, Map<String, Object>>();

    /**
     * Constructs a new configuration object with the specified configuration.
     * @param conf
     */
    public Configurator(Configuration conf) throws ConfigurationException {
        this.conf = conf;
        registerProviders();
    }

    /**
     * Registers all class that extend Providers
     * @throws ConfigurationException
     */
    private void registerProviders() throws ConfigurationException {
        String classPath = JvmUtils.getClassPath();

        // map from class names to file they are registered in.
        Set<String> visited = new HashSet<String>();    // files already scanned
        Map<String, File> registered = new HashMap<String, File>();

        for (String entry : classPath.split(":")) {
            File file = new File(entry);
            if (file.length() > MAX_FILE_SIZE) {
                LOG.fine("skipping looking for providers in large file " + file);
                continue;
            }
            String canonical = FilenameUtils.normalize(file.getAbsolutePath());
            if (visited.contains(canonical)) {
                LOG.fine("skipping looking for providers in duplicate classpath entry " + canonical);
                continue;
            }
            visited.add(canonical);

            ClassFinder finder = new ClassFinder();
            finder.add(new File(entry));

            ClassFilter filter = new ProviderFilter();
            Collection<ClassInfo> foundClasses = new ArrayList<ClassInfo>();
            finder.findClasses (foundClasses,filter);

            for (ClassInfo classInfo : foundClasses) {
                if (registered.containsKey(classInfo.getClassName())) {
                    LOG.warning("class " + classInfo.getClassName() +
                            " found in " + file +
                            " but previously found in " + registered.get(classInfo.getClassName()) +
                            ". Skipping!");
                } else {
                    LOG.fine("registering component " + classInfo);
                    registerProvider(classInfo.getClassName());
                    registered.put(classInfo.getClassName(), file);
                }
            }
        }

        int total = 0;
        for (Class c : providers.keySet()) {
            ProviderSet pset = providers.get(c);
            total += pset.providers.size();
            LOG.fine("installed " + pset.providers.size() + " configurators for " + pset.type);
        }
        LOG.info("configurator installed " + total + " providers for " +
                providers.size() + " classes");
    }



    /**
     * Instantiates providers for the component.
     * @param providerClass The name of the provider that should be instaniated
     * @return
     * @throws ConfigurationException
     */
    private void registerProvider(String providerClass) throws ConfigurationException {
        try {
            Class<Provider> klass = (Class<Provider>) Class.forName(providerClass);
            Provider provider = ConstructorUtils.invokeConstructor(klass, this, conf);

            Class type = provider.getType();
            String path = provider.getPath();
            ProviderSet pset = providers.get(type);
            if (pset == null) {
                pset = new ProviderSet(type, path);
                providers.put(type, pset);
                components.put(type, new HashMap<String, Object>());
            }
            if (pset.type != type) {
                throw new IllegalStateException();
            }
            if (!pset.path.equals(path)) {
                throw new ConfigurationException(
                        "inconsistent component path declared for provider " + klass +
                        " that provides type " + type +
                        " expected path " + pset.path +
                        ", found path " + path);
            }
            pset.providers.add(provider);
        } catch (ClassNotFoundException e) {
            throw new ConfigurationException("error when loading provider " + providerClass, e);
        } catch (InvocationTargetException e) {
            throw new ConfigurationException("error when loading provider " + providerClass, e);
        } catch (NoSuchMethodException e) {
            throw new ConfigurationException("error when loading provider " + providerClass, e);
        } catch (InstantiationException e) {
            throw new ConfigurationException("error when loading provider " + providerClass, e);
        } catch (IllegalAccessException e) {
            throw new ConfigurationException("error when loading provider " + providerClass, e);
        }
    }


    /**
     * @see #get(Class, String, boolean)
     *
     * @param klass
     * @param name
     * @param <T>
     * @return
     * @throws ConfigurationException
     */
    public <T> T get(Class<T> klass, String name) throws ConfigurationException {
        return get(klass, name, true);
    }

    /**
     * Get a specific named instance of the component with the specified class.
     *
     * @param klass The generic interface or superclass, not the specific implementation.
     * @param name The name of the class as it appears in the config file. If name is null,
     *             the configurator tries to guess by looking for a "default" entry in
     *             the config that provides the name for a default implementation or, if
     *             there is exactly one implementation returning it. Otherwise, if name is
     *             null it throws an error.
     * @param tryCache If true, and the provider scope is singleton will query / populate the
     *                 cache for the named object.
     * @return The requested component.
     */
    public <T> T get(Class<T> klass, String name, boolean tryCache) throws ConfigurationException {
        if (!providers.containsKey(klass)) {
            throw new ConfigurationException("No registered providers for components with class " + klass);
        }
        ProviderSet pset = providers.get(klass);

        // If name is "default", treat it as null for default option
        if (name != null && name.equalsIgnoreCase("default")) {
            name = null;
        }

        // If name is null, check to see if there is a default entry or only one option.
        if (name == null) {
            if (!conf.get().hasPath(pset.path)) {
                throw new ConfigurationException("Configuration path " + pset.path + " does not exist");
            }
            Config config = conf.get().getConfig(pset.path);
            if (config.hasPath("default")) {
                name = config.getString("default");
            } else if (config.root().keySet().size() == 1) {
                name = config.root().keySet().iterator().next();
            } else {
                throw new IllegalArgumentException(
                        "Ambiguous request for nameless component with type " + klass +
                                " the configuration dictionary at path " + pset.path +
                                " must either have a 'default' key specifying the name " +
                                " of the default implementation or exactly one element. " +
                                "Available provider implementations are: " + Arrays.toString(pset.providers.toArray())
                );
            }
        }

        String path = pset.path + "." + name;
        if (!conf.get().hasPath(path)) {
            throw new ConfigurationException("Configuration path " + path + " does not exist");
        }
        Config config = conf.get().getConfig(path);
        Map<String, Object> cache = components.get(klass);

        synchronized (cache) {
            if (tryCache && cache.containsKey(name)) {
                return (T) cache.get(name);
            } else {
                Pair<Provider, T> pair = constructInternal(klass, name, config);
                if (tryCache && pair.getLeft().getScope() == Provider.Scope.SINGLETON) {
                    cache.put(name, pair.getRight());
                }
                return pair.getRight();
            }
        }
    }

    /**
     * Constructs an instance of the specified class with the passed
     * in config. This bypasses the cache and the configuration object.
     *
     * @param klass The class being created.
     * @param name  An arbitrary name for the object. Can be null.
     * @param conf The configuration for the object.
     * @param <T>
     * @return The object
     */
    public <T> T construct(Class<T> klass, String name, Config conf) throws ConfigurationException {
        return constructInternal(klass, name, conf).getRight();
    }

    private <T> Pair<Provider, T> constructInternal(Class<T> klass, String name, Config conf) throws ConfigurationException {
        if (!providers.containsKey(klass)) {
            throw new ConfigurationException("No registered providers for components with class " + klass);
        }
        List<Provider> pset = providers.get(klass).providers;
        for (Provider p : pset) {
            Object o = p.get(name, conf);
            if (o != null) {
                return Pair.of(p, (T) o);
            }
        }
        throw new ConfigurationException(
                "None of the " + pset.size() + " providers claimed ownership of component " +
                        "with class " + klass +
                        ", name '" + name +
                        "' and configuration '" + conf + "'"
        );
    }

    /**
     * Get a specific named instance of the component with the specified class.
     * This method can only be used when there is exactly one instance of the component.
     *
     * @param klass The generic interface or superclass, not the specific implementation.
     * @return The requested component.
     */
    public <T> T get(Class<T> klass) throws ConfigurationException {
        return get(klass, null);
    }

}
