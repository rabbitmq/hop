/*
 * Copyright 2025 the original author or authors.
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
package com.rabbitmq.http.client;

abstract class TestUtils {

  private TestUtils() {}

  /**
   * https://stackoverflow.com/questions/6701948/efficient-way-to-compare-version-strings-in-java
   */
  static Integer compareVersions(String str1, String str2) {
    String[] vals1 = str1.split("\\.");
    String[] vals2 = str2.split("\\.");
    int i = 0;
    // set index to first non-equal ordinal or length of shortest version string
    while (i < vals1.length && i < vals2.length && vals1[i].equals(vals2[i])) {
      i++;
    }
    // compare first non-equal ordinal number
    if (i < vals1.length && i < vals2.length) {
      if (vals1[i].indexOf('-') != -1) {
        vals1[i] = vals1[i].substring(0, vals1[i].indexOf('-'));
      }
      if (vals2[i].indexOf('-') != -1) {
        vals2[i] = vals2[i].substring(0, vals2[i].indexOf('-'));
      }
      int diff = Integer.valueOf(vals1[i]).compareTo(Integer.valueOf(vals2[i]));
      return Integer.signum(diff);
    }
    // the strings are equal or one string is a substring of the other
    // e.g. "1.2.3" = "1.2.3" or "1.2.3" < "1.2.3.4"
    else {
      return Integer.signum(vals1.length - vals2.length);
    }
  }

  static boolean isVersion36orLater(String currentVersion) {
    return checkVersionOrLater(currentVersion, "3.6.0");
  }

  static boolean isVersion37orLater(String currentVersion) {
    return checkVersionOrLater(currentVersion, "3.7.0");
  }

  static boolean isVersion38orLater(String currentVersion) {
    return checkVersionOrLater(currentVersion, "3.8.0");
  }

  static boolean isVersion310orLater(String currentVersion) {
    return checkVersionOrLater(currentVersion, "3.10.0");
  }

  private static boolean checkVersionOrLater(String currentVersion, String expectedVersion) {
    String v = currentVersion.replaceAll("\\+.*$", "");
    try {
      return v.equals("0.0.0") || compareVersions(v, expectedVersion) >= 0;
    } catch (Exception e) {
      return false;
    }
  }
}
