/*
 * Copyright (C) 2017 Alex Stockinger
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fi.testee.util.nopostconstruct;

import org.jboss.weld.bootstrap.spi.BeanDiscoveryMode;
import org.jboss.weld.bootstrap.spi.BeansXml;
import org.jboss.weld.bootstrap.spi.Metadata;
import org.jboss.weld.bootstrap.spi.Scanning;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class WrappedBeansXml implements BeansXml {
    private BeansXml beansXml;

    public WrappedBeansXml(final BeansXml beansXml) {
        this.beansXml = beansXml;
    }

    @Override
    public List<Metadata<String>> getEnabledAlternativeStereotypes() {
        return beansXml.getEnabledAlternativeStereotypes();
    }

    @Override
    public List<Metadata<String>> getEnabledAlternativeClasses() {
        return beansXml.getEnabledAlternativeClasses();
    }

    @Override
    public List<Metadata<String>> getEnabledDecorators() {
        return beansXml.getEnabledDecorators();
    }

    @Override
    public List<Metadata<String>> getEnabledInterceptors() {
        final List<Metadata<String>> ret = new ArrayList<>(beansXml.getEnabledInterceptors());
        ret.add(new Metadata<String>() {
            @Override
            public String getValue() {
                return NoPostConstructInterceptor.class.getName();
            }

            @Override
            public String getLocation() {
                return beansXml.getUrl().toString();
            }
        });
        return ret;
    }

    @Override
    public Scanning getScanning() {
        return beansXml.getScanning();
    }

    @Override
    public URL getUrl() {
        return beansXml.getUrl();
    }

    @Override
    public BeanDiscoveryMode getBeanDiscoveryMode() {
        return beansXml.getBeanDiscoveryMode();
    }

    @Override
    public String getVersion() {
        return beansXml.getVersion();
    }
}
