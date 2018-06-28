/**
 * CAT Core support plugin project.
 *
 * Copyright (C) 2016 New York City Department of Health and Mental Hygiene, Bureau of Immunization
 * Contributions by HLN Consulting, LLC
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version. You should have received a copy of the GNU Lesser
 * General Public License along with this program. If not, see <http://www.gnu.org/licenses/> for more
 * details.
 *
 * The above-named contributors (HLN Consulting, LLC) are also licensed by the New York City
 * Department of Health and Mental Hygiene, Bureau of Immunization to have (without restriction,
 * limitation, and warranty) complete irrevocable access and rights to this project.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; THE
 * SOFTWARE IS PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING,
 * BUT NOT LIMITED TO, WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE COPYRIGHT HOLDERS, IF ANY, OR DEVELOPERS BE LIABLE FOR
 * ANY CLAIM, DAMAGES, OR OTHER LIABILITY OF ANY KIND, ARISING FROM, OUT OF, OR IN CONNECTION WITH
 * THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 * For more information about this software, see https://www.hln.com/services/open-source/ or send
 * correspondence to ice@hln.com.
 */
package org.cdsframework.rs.base;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.cdsframework.rs.core.support.CatResourceConfig;
import org.cdsframework.rs.util.ConfigServiceUtils;
import org.cdsframework.util.LogUtils;

/**
 *
 * @author sdn
 */
public abstract class BaseConfigService {

    protected final LogUtils logger;
    private final Class<? extends BasePluginConfig> pluginConfigClass;
    private final String pluginRoot;
    private static final Map<Class<? extends BasePluginConfig>, BasePluginConfig> PLUGIN_CONFIG_MAP = new HashMap<>();
    private static final Map<Class<? extends BasePluginConfig>, String> BASE_URI_MAP = new HashMap<>();

    public BaseConfigService(Class<? extends BasePluginConfig> pluginConfigClass, String pluginRoot) {
        logger = LogUtils.getLogger(getClass());
        this.pluginConfigClass = pluginConfigClass;
        this.pluginRoot = pluginRoot;
        logger.info("initialized with pluginConfigClass: ", pluginConfigClass, "; pluginRoot: ", pluginRoot);
        initializeMain();
    }

    /**
     * Get the value of pluginConfigClass
     *
     * @return the value of pluginConfigClass
     */
    public Class<? extends BasePluginConfig> getPluginConfigClass() {
        return pluginConfigClass;
    }

    /**
     * Get the value of pluginRoot
     *
     * @return the value of pluginRoot
     */
    public String getPluginRoot() {
        return pluginRoot;
    }

    /**
     * Get the value of BASE_CRUD_URI
     *
     * @return the value of BASE_CRUD_URI
     */
    public String getBaseCrudUri() {
        final String METHODNAME = "getBaseCrudUri ";
        String pluginBaseUri = BaseConfigService.getBaseCrudUriStatic(pluginConfigClass, pluginRoot);
        logger.info(METHODNAME, "pluginBaseUri=", pluginBaseUri);
        return pluginBaseUri;
    }

    private static String getBaseCrudUriStatic(Class<? extends BasePluginConfig> pluginConfigClass, String pluginRoot) {
        String pluginBaseUri = BASE_URI_MAP.get(pluginConfigClass);
        if (pluginBaseUri == null) {
            pluginBaseUri = ConfigServiceUtils.getBaseCrudUriFromPluginRoot(pluginRoot);
            BASE_URI_MAP.put(pluginConfigClass, pluginBaseUri);
        }
        return pluginBaseUri;
    }

    /**
     * Get the value of PLUGIN_CONFIG
     *
     * @return the value of PLUGIN_CONFIG
     */
    public BasePluginConfig getPluginConfig() {
        final String METHODNAME = "getPluginConfig ";
        BasePluginConfig pluginConfig = null;
        try {
            pluginConfig = BaseConfigService.getPluginConfigStatic(pluginConfigClass, getBaseCrudUri());
        } catch (InvocationTargetException | IllegalAccessException | InstantiationException | NoSuchMethodException e) {
            logger.error(e);
        }
        return pluginConfig;
    }

    private static BasePluginConfig getPluginConfigStatic(Class<? extends BasePluginConfig> pluginConfigClass, String baseCrudUri)
            throws NoSuchMethodException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        BasePluginConfig pluginConfigInstance = PLUGIN_CONFIG_MAP.get(pluginConfigClass);
        if (pluginConfigInstance == null) {
            pluginConfigInstance = pluginConfigClass.getConstructor(String.class).newInstance(baseCrudUri);
            PLUGIN_CONFIG_MAP.put(pluginConfigClass, pluginConfigInstance);
        }
        return pluginConfigInstance;
    }

    public final void initializeMain() {
        final String METHODNAME = "initializeMain ";
        initialize();
    }

    public void initialize() {
    }

    public CatResourceConfig configMain(String resource) {
        final String METHODNAME = "configMain ";
        Map<String, CatResourceConfig> configurationMap = getPluginConfig().getConfigurationMap();
        logger.info(METHODNAME, "configurationMap=", configurationMap);
        CatResourceConfig config = configurationMap.get(resource);
        logger.info(METHODNAME, "config=", config);
        return config;
    }

    public List<String> getConfigsMain() {
        final String METHODNAME = "getConfigsMain ";
        Map<String, CatResourceConfig> configurationMap = getPluginConfig().getConfigurationMap();
        logger.info(METHODNAME, "configurationMap=", configurationMap);
        return new ArrayList<>(configurationMap.keySet());
    }

}
