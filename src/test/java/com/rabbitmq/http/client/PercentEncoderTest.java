// Copyright (c) 2024 Broadcom. All Rights Reserved.
// The term "Broadcom" refers to Broadcom Inc. and/or its subsidiaries.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
// If you have any questions regarding licensing, please contact us at
// info@rabbitmq.com.
package com.rabbitmq.http.client;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class PercentEncoderTest {

  @ParameterizedTest
  @CsvSource({
    "test,test",
    "foo bar,foo%20bar",
    "foo%bar,foo%25bar",
    "foo/bar,foo%2Fbar",
    "foo+bar,foo+bar",
    "/foo/bar,%2Ffoo%2Fbar"
  })
  void encodePathSegmentTest(String segment, String expected) {
    assertThat(PercentEncoder.encodePathSegment(segment)).isEqualTo(expected);
  }

  @ParameterizedTest
  @CsvSource({"test,test", "foobar,foobar", "foo bar,foo%20bar", "foo&bar,foo%26bar"})
  void encodeParamTest(String param, String expected) {
    assertThat(PercentEncoder.encodeParameter(param)).isEqualTo(expected);
  }

}
