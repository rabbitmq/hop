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
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class HttpLayerTest {

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

  static Stream<TestConfiguration> httpLayer() {
    return Stream.of(
        new TestConfiguration(OkHttpSmokeApplication.class, Arrays.asList("httpclient"), false),
        new TestConfiguration(HttpComponentsSmokeApplication.class, Arrays.asList("okhttp"), false),
        new TestConfiguration(
            JdkSmokeApplication.class, Arrays.asList("okhttp", "httpclient"), false),
        new TestConfiguration(OkHttpSmokeApplication.class, Arrays.asList("okhttp"), true),
        new TestConfiguration(
            HttpComponentsSmokeApplication.class, Arrays.asList("httpclient"), true),
        new TestConfiguration(
            JdkHttpClientSmokeApplication.class, Arrays.asList("okhttp", "httpclient", "spring"), false)
        );
  }

  @ParameterizedTest
  @MethodSource
  void httpLayer(TestConfiguration testConfiguration) throws Exception {
    ClassLoader classLoader = isolatedClassLoader(testConfiguration.toFilter);

    Class<?> testClass = classLoader.loadClass(testConfiguration.testClass.getName());
    Method testMethod = testClass.getMethod("test");
    if (testConfiguration.shouldFail) {
      try {
        testMethod.invoke(null);
        Assertions.fail();
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

  public static class JdkHttpClientSmokeApplication {

    public static void test() throws Exception {
      Client client =
          new Client(
              new ClientParameters()
                  .url(url()));
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
