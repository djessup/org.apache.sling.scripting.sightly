/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 ******************************************************************************/

package org.apache.sling.scripting.sightly.impl.engine.extension.use;

import javax.script.Bindings;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.scripting.sightly.ResourceResolution;
import org.apache.sling.scripting.sightly.SightlyException;
import org.apache.sling.scripting.sightly.impl.engine.SightlyScriptEngineFactory;
import org.apache.sling.scripting.sightly.impl.engine.UnitLoader;
import org.apache.sling.scripting.sightly.impl.engine.runtime.RenderContextImpl;
import org.apache.sling.scripting.sightly.impl.engine.runtime.RenderUnit;
import org.apache.sling.scripting.sightly.render.RenderContext;
import org.apache.sling.scripting.sightly.use.ProviderOutcome;
import org.apache.sling.scripting.sightly.use.UseProvider;
import org.osgi.framework.Constants;

/**
 * Interprets identifiers as paths to other Sightly templates
 */
@Component(
        metatype = true,
        label = "Apache Sling Scripting Sightly Render Unit Use Provider",
        description = "The Render Unit Use Provider is responsible for instantiating Sightly templates through the Use-API."
)
@Service(UseProvider.class)
@Properties({
        @Property(
                name = Constants.SERVICE_RANKING,
                label = "Service Ranking",
                description = "The Service Ranking value acts as the priority with which this Use Provider is queried to return an " +
                        "Use-object. A higher value represents a higher priority.",
                intValue = 100,
                propertyPrivate = false
        )
})
public class RenderUnitProvider implements UseProvider {

    @Reference
    private UnitLoader unitLoader = null;

    @Override
    public ProviderOutcome provide(String identifier, RenderContext renderContext, Bindings arguments) {
        if (identifier.endsWith("." + SightlyScriptEngineFactory.EXTENSION)) {
            Bindings globalBindings = renderContext.getBindings();
            SlingHttpServletRequest request = (SlingHttpServletRequest) globalBindings.get(SlingBindings.REQUEST);
            Resource resource = ResourceResolution.getResourceForRequest(renderContext.getScriptResourceResolver(), request);
            Resource renderUnitResource = ResourceResolution.getResourceFromSearchPath(resource, identifier);
            if (renderUnitResource == null) {
                String resourceSuperType = resource.getResourceSuperType();
                StringBuilder errorMessage = new StringBuilder("Cannot find resource ");
                errorMessage.append(identifier).append(" for base path ").append(resource.getPath());
                if (StringUtils.isNotEmpty(resourceSuperType)) {
                    errorMessage.append(" with resource super type ").append(resourceSuperType);
                }
                errorMessage.append(".");
                return ProviderOutcome.failure(new SightlyException(errorMessage.toString()));
            }
            RenderUnit renderUnit = unitLoader.createUnit(renderUnitResource, globalBindings, (RenderContextImpl) renderContext);
            return ProviderOutcome.success(renderUnit);
        }
        return ProviderOutcome.failure();
    }
}
