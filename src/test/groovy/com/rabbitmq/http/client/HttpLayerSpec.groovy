/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.rabbitmq.http.client

import com.rabbitmq.http.client.domain.VhostInfo
import spock.lang.Specification
import spock.lang.Unroll

import java.lang.reflect.Method
import java.util.stream.Collectors


class HttpLayerSpec extends Specification {

    // HTTP Components is mandatory in those tests because
    // Groovy loads SSLConnectionSocketFactory from Client,
    // even though it is not used
    static TestConfiguration[] testConfigurations() {
        [
                new TestConfiguration(OkHttpSmokeApplication.class, []),
                new TestConfiguration(HttpComponentsSmokeApplication.class, ["okhttp"]),
                new TestConfiguration(JdkSmokeApplication.class, ["okhttp"]),
        ]
    }

    @Unroll
    def "use client with HTTP layer in isolated classloader"() {
        when: "client is used with HTTP layer in isolated environment"
        ClassLoader classLoader = isolatedClassLoader(testConfiguration.toFilter)
        Class<?> testClass = classLoader.loadClass(testConfiguration.testClass.getName())
        Method testMethod = testClass.getMethod("test")
        testMethod.invoke(null)

        then: "the execution should succeed"

        where:
        testConfiguration << testConfigurations()
    }

    static String url() {
        return "http://guest:guest@127.0.0.1:" + managementPort() + "/api/"
    }

    static int managementPort() {
        return System.getProperty("rabbitmq.management.port") == null ?
                15672 :
                Integer.valueOf(System.getProperty("rabbitmq.management.port"))
    }

    static ClassLoader isolatedClassLoader(List<String> toFilter) {
        String classpath = System.getProperty("java.class.path")
        Collection<URL> urls =
                Arrays.stream(classpath.split(":"))
                        .filter({ entry -> !toFilter.any { filter -> entry.contains(filter) } })
                        .map(
                                {
                                    try {
                                        return new URL("file:" + it +
                                                (it.endsWith("/") || it.endsWith(".jar") ? "" : "/"))
                                    } catch (MalformedURLException e) {
                                        throw new RuntimeException(e)
                                    }
                                })
                        .collect(Collectors.toList())

        new URLClassLoader(
                urls as URL[],
                ClassLoader.getSystemClassLoader().getParent()
        ) {

            @Override
            Class<?> loadClass(String name) throws ClassNotFoundException {
                if (name.equals("org.apache.http.conn.ssl.SSLConnectionSocketFactory")) {
                    try {
                        return super.loadClass(name)
                    } catch (ClassNotFoundException e) {
                        println "passe"
                        return new Object() {}.getClass()
                    }
                } else {
                    return super.loadClass(name)
                }
            }
        }
    }

    static class OkHttpSmokeApplication {

        static void test() throws Exception {
            Client client = new Client(new ClientParameters().url(url())
                    .restTemplateConfigurator(new OkHttpRestTemplateConfigurator()))
            List<VhostInfo> vhosts = client.getVhosts()
            if (vhosts.isEmpty() || !vhosts.get(0).getName().equals("/")) {
                throw new IllegalStateException()
            }
        }
    }

    static class HttpComponentsSmokeApplication {

        static void test() throws Exception {
            Client client = new Client(new ClientParameters().url(url())
                    .restTemplateConfigurator(new HttpComponentsRestTemplateConfigurator()))
            List<VhostInfo> vhosts = client.getVhosts()
            if (vhosts.isEmpty() || !vhosts.get(0).getName().equals("/")) {
                throw new IllegalStateException()
            }
        }
    }

    static class JdkSmokeApplication {

        static boolean test() {
            Client client = new Client(new ClientParameters()
                    .url(url())
                    .restTemplateConfigurator(new SimpleRestTemplateConfigurator())
            )
            List<VhostInfo> vhosts = client.getVhosts()
            if (vhosts.isEmpty() || !vhosts.get(0).getName().equals("/")) {
                throw new IllegalStateException()
            }
        }
    }

    static class TestConfiguration {

        private final Class<?> testClass
        private final List<String> toFilter

        TestConfiguration(Class<?> testClass, List<String> toFilter) {
            this.toFilter = toFilter
            this.testClass = testClass
        }
    }

}
