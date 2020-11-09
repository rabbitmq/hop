package com.rabbitmq.http.client;

import com.rabbitmq.http.client.domain.VhostInfo;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class HttpLayerTest {

  @Parameterized.Parameter public TestConfiguration testConfiguration;

  static ClassLoader isolatedClassLoader(List<String> toFilter) {
    String classpath = System.getProperty("java.class.path");

    Collection<URL> urls =
        Arrays.stream(classpath.split(":"))
            .filter(entry -> !toFilter.stream().anyMatch(filter -> entry.contains(filter)))
            .map(
                entry -> {
                  try {
                    return new URL(
                        "file:"
                            + entry
                            + (entry.endsWith("/") || entry.endsWith(".jar") ? "" : "/"));
                  } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                  }
                })
            .collect(Collectors.toList());

    return new URLClassLoader(
        urls.toArray(new URL[] {}), ClassLoader.getSystemClassLoader().getParent());
  }

  static String url() {
    return "http://guest:guest@127.0.0.1:" + managementPort() + "/api/";
  }

  static int managementPort() {
    return System.getProperty("rabbitmq.management.port") == null
        ? 15672
        : Integer.valueOf(System.getProperty("rabbitmq.management.port"));
  }

  @Parameterized.Parameters
  public static Object[] testConfigurations() {
    return new Object[] {
      new TestConfiguration(OkHttpSmokeApplication.class, Arrays.asList("httpclient"), false),
      new TestConfiguration(HttpComponentsSmokeApplication.class, Arrays.asList("okhttp"), false),
      new TestConfiguration(
          JdkSmokeApplication.class, Arrays.asList("okhttp", "httpclient"), false),
      new TestConfiguration(OkHttpSmokeApplication.class, Arrays.asList("okhttp"), true),
      new TestConfiguration(
          HttpComponentsSmokeApplication.class, Arrays.asList("httpclient"), true),
    };
  }

  @Test
  public void httpLayer() throws Exception {
    ClassLoader classLoader = isolatedClassLoader(testConfiguration.toFilter);

    Class<?> testClass = classLoader.loadClass(testConfiguration.testClass.getName());
    Method testMethod = testClass.getMethod("test");
    if (testConfiguration.shouldFail) {
      try {
        testMethod.invoke(null);
        Assert.fail();
      } catch (Exception e) {
        // OK
      }
    } else {
      testMethod.invoke(null);
    }
  }

  public static class OkHttpSmokeApplication {

    public static void test() throws Exception {
      Client client =
          new Client(
              new ClientParameters()
                  .url(url())
                  .restTemplateConfigurator(new OkHttpRestTemplateConfigurator()));
      List<VhostInfo> vhosts = client.getVhosts();
      if (vhosts.isEmpty() || !vhosts.get(0).getName().equals("/")) {
        throw new IllegalStateException();
      }
    }
  }

  public static class HttpComponentsSmokeApplication {

    public static void test() throws Exception {
      Client client =
          new Client(
              new ClientParameters()
                  .url(url())
                  .restTemplateConfigurator(new HttpComponentsRestTemplateConfigurator()));
      List<VhostInfo> vhosts = client.getVhosts();
      if (vhosts.isEmpty() || !vhosts.get(0).getName().equals("/")) {
        throw new IllegalStateException();
      }
    }
  }

  public static class JdkSmokeApplication {

    public static void test() throws Exception {
      Client client =
          new Client(
              new ClientParameters()
                  .url(url())
                  .restTemplateConfigurator(new SimpleRestTemplateConfigurator()));
      List<VhostInfo> vhosts = client.getVhosts();
      if (vhosts.isEmpty() || !vhosts.get(0).getName().equals("/")) {
        throw new IllegalStateException();
      }
    }
  }

  static class TestConfiguration {

    private final Class<?> testClass;
    private final List<String> toFilter;
    private final boolean shouldFail;

    TestConfiguration(Class<?> testClass, List<String> toFilter, boolean shouldFail) {
      this.toFilter = toFilter;
      this.testClass = testClass;
      this.shouldFail = shouldFail;
    }
  }
}
