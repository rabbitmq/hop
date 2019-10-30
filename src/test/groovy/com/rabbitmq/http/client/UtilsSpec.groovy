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

import spock.lang.Specification


class UtilsSpec extends Specification {

  def "extract username and password, no decoding needed"() {
    when: "username and password are extracted from http://mylogin:mypassword@localhost:15672"
    def usernamePassword = Utils.extractUsernamePassword("http://mylogin:mypassword@localhost:15672")

    then: "username and password are extracted properly"
    usernamePassword[0] == "mylogin"
    usernamePassword[1] == "mypassword"
  }

  def "extract username and password, no user info in the URL"() {
    when: "there is no user info in the URL"
    Utils.extractUsernamePassword("http://localhost:15672")

    then: "an exception is thrown"
    thrown(IllegalArgumentException)
  }

  def "extract username and password, decoding needed"() {
    when: "username and password are extracted from https://test+user:test%40password@myrabbithost/api/"
    def usernamePassword = Utils.extractUsernamePassword("https://test+user:test%40password@myrabbithost/api/")

    then: "username and password are extracted and decoded properly"
    usernamePassword[0] == "test user"
    usernamePassword[1] == "test@password"
  }

  def "URL without credentials, credentials not encoded"() {
    when: "credentials do not need encoding in the URL"
    def urlWithoutCredentials = Utils.urlWithoutCredentials("http://mylogin:mypassword@localhost:15672")

    then: "credentials are properly removed from the URL"
    urlWithoutCredentials == "http://localhost:15672"
  }

  def "URL without credentials, credentials need to be encoded"() {
    when: "credentials need encoding in the URL"
    def urlWithoutCredentials = Utils.urlWithoutCredentials("https://test+user:test%40password@myrabbithost/api/")

    then: "credentials are properly removed from the URL"
    urlWithoutCredentials == "https://myrabbithost/api/"
  }

}
