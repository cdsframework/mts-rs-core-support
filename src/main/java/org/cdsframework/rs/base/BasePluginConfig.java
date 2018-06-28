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

import java.util.HashMap;
import java.util.Map;
import org.cdsframework.base.BaseDTO;
import org.cdsframework.rs.core.support.CatResourceConfig;
import org.cdsframework.rs.util.CatResourceConfigUtils;
import org.cdsframework.util.LogUtils;

/**
 *
 * @author sdn
 */
public abstract class BasePluginConfig {

    private final Map<String, CatResourceConfig> configurationMap = new HashMap<>();
    protected final LogUtils logger;
    private final String baseCrudUri;

    public BasePluginConfig(String baseCrudUri) {
        logger = LogUtils.getLogger(getClass());
        this.baseCrudUri = baseCrudUri;
    }

    public final void registerConfig(Class<? extends BaseDTO> resourceClass, CatResourceConfig config) {
        final String METHODNAME = "registerConfig ";
        config.setCrudRsUri(baseCrudUri + "/" + config.getResourceName());
        config.initialize();
        CatResourceConfigUtils.postProcessCatResourceConfig(config);
        logger.info(METHODNAME, "registering resourceName: ", config.getResourceName(), " - ", config);
        configurationMap.put(config.getResourceName(), config);
    }

    public String getBaseCrudUri() {
        return baseCrudUri;
    }

    /**
     * Get the value of configurationMap
     *
     * @return the value of configurationMap
     */
    public final Map<String, CatResourceConfig> getConfigurationMap() {
        return configurationMap;
    }

}
