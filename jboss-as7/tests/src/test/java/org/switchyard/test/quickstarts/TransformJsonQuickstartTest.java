/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.switchyard.test.quickstarts;

import org.hornetq.api.core.client.ClientConsumer;
import org.hornetq.api.core.client.ClientMessage;
import org.hornetq.api.core.client.ClientProducer;
import org.hornetq.api.core.client.ClientSession;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.switchyard.test.ArquillianUtil;
import org.switchyard.test.mixins.HornetQMixIn;
import org.switchyard.test.quickstarts.util.ResourceDeployer;

/**
 *
 * @author Magesh Kumar B <mageshbk@jboss.com> (C) 2011 Red Hat Inc.
 */
@RunWith(Arquillian.class)
public class TransformJsonQuickstartTest {

    private static final String JMS_PREFIX = "jms.queue.";
    private static final String REQUEST_QUEUE = "OrderServiceQueue";
    private static final String RESPONSE_QUEUE = "StoreResponseQueue";
    private static final String USER = "guest";
    private static final String PASSWD = "guestp";
    
    @Deployment(testable = false)
    public static JavaArchive createDeployment() throws Exception {
        ResourceDeployer.addQueue(REQUEST_QUEUE);
        ResourceDeployer.addQueue(RESPONSE_QUEUE);
        ResourceDeployer.addPropertiesUser(USER, PASSWD);
        return ArquillianUtil.createJarQSDeployment("switchyard-quickstart-transform-json");
    }

    @Test
    public void testDeployment() throws Exception {
        HornetQMixIn hqMixIn = new HornetQMixIn(false)
                                    .setUser(USER)
                                    .setPassword(PASSWD);
        hqMixIn.initialize();

        try {
            ClientSession session = hqMixIn.getClientSession();
            ClientMessage message = hqMixIn.createMessage(ORDER_JSON);
            ClientProducer producer = session.createProducer(JMS_PREFIX+REQUEST_QUEUE);
            producer.send(message);
            HornetQMixIn.closeClientProducer(producer);
            HornetQMixIn.closeSession(session);

            session = hqMixIn.createClientSession();
            ClientConsumer consumer = session.createConsumer(JMS_PREFIX+RESPONSE_QUEUE);
            message = consumer.receive(3000);
            Assert.assertNotNull(message);
            Assert.assertEquals(ORDER_ACK_JSON, hqMixIn.readObjectFromMessage(message));

            message.acknowledge();
            HornetQMixIn.closeClientConsumer(consumer);
            HornetQMixIn.closeSession(session);
        } finally {
            hqMixIn.uninitialize();
            ResourceDeployer.removeQueue(REQUEST_QUEUE);
            ResourceDeployer.removeQueue(RESPONSE_QUEUE);
        }
    }

    private static final String ORDER_JSON =
            "{\"orderId\":\"PO-19838-XYZ\",\"itemId\":\"BUTTER\",\"quantity\":100}";
    private static final String ORDER_ACK_JSON =
            "{\"status\":\"Order Accepted\",\"orderId\":\"PO-19838-XYZ\",\"accepted\":true}";
}
