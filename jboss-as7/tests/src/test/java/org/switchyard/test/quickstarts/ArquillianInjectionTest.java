/*
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,  
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.switchyard.test.quickstarts;

import java.io.IOException;
import java.net.URL;

import javax.inject.Inject;
import javax.naming.InitialContext;

import junit.framework.Assert;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.connector.util.ConnectorServices;
import org.jboss.jca.core.spi.rar.ResourceAdapterRepository;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.UrlAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.switchyard.quickstarts.jca.inflow.GreetingService;
import org.switchyard.quickstarts.jca.inflow.GreetingServiceBean;
import org.switchyard.test.ArquillianUtil;
import org.switchyard.common.type.Classes;
import org.switchyard.test.quickstarts.util.ResourceDeployer;

@RunWith(Arquillian.class)
public class ArquillianInjectionTest {

    private static final String QUEUE = "JCAInflowGreetingServiceQueue";

    @Inject
    public ServiceContainer serviceContainer;
    @ArquillianResource
    public InitialContext context;
    @Inject
    public GreetingServiceBean service;
    
    @Deployment(order = 2)
    public static JavaArchive createTestDeployment() throws IOException {
        URL deploymentStructureUrl = Classes.getResource("test-deployment-structure.xml");
        return ShrinkWrap.create(JavaArchive.class)
                         .addClass(GreetingService.class)
                         .addClass(GreetingServiceBean.class)
                         .addAsManifestResource(new UrlAsset(deploymentStructureUrl), "jboss-deployment-structure.xml");
    }

    @Deployment(order = 1, testable = false, name = "apps")
    public static JavaArchive createDeployment() throws IOException {
        ResourceDeployer.addQueue(QUEUE);
        ResourceDeployer.addPropertiesUser();
        return ArquillianUtil.createJarQSDeployment("switchyard-jca-inflow-hornetq");
    }

    @After
    public void after() throws Exception {
        ResourceDeployer.removeQueue(QUEUE);
    }

    @Test
    public void testResourceInjection() throws Exception {
        Assert.assertNotNull(serviceContainer);
        ServiceController<?> controller = serviceContainer.getService(ConnectorServices.RA_REPOSITORY_SERVICE);
        Assert.assertNotNull(controller);
        Object raRepository = controller.getValue();
        Assert.assertTrue(raRepository.getClass().getName(), raRepository instanceof ResourceAdapterRepository); 
        Assert.assertNotNull(context);
        Object factory = context.lookup("java:/ConnectionFactory");
        Assert.assertNotNull(factory);
        Assert.assertTrue(factory.getClass().getName(), factory instanceof javax.jms.ConnectionFactory);
        Assert.assertNotNull(service);
    }
}
