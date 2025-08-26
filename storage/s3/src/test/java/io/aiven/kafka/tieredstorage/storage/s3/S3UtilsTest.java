/*
 * Copyright 2023 Aiven Oy
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

package io.aiven.kafka.tieredstorage.storage.s3;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

import software.amazon.awssdk.services.s3.model.ObjectIdentifier;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class S3UtilsTest {

    @Test
    void computeDeleteObjectsContentMd5_singleObject() throws NoSuchAlgorithmException {
        final List<ObjectIdentifier> objects = Arrays.asList(
            ObjectIdentifier.builder().key("test-key").build()
        );
        
        final String result = S3Utils.computeDeleteObjectsContentMd5(objects);
        
        // Expected XML: <Delete><Object><Key>test-key</Key></Object></Delete>
        final String expectedXml = "<Delete><Object><Key>test-key</Key></Object></Delete>";
        final byte[] xmlBytes = expectedXml.getBytes(StandardCharsets.UTF_8);
        final MessageDigest md = MessageDigest.getInstance("MD5");
        final byte[] digest = md.digest(xmlBytes);
        final String expected = Base64.getEncoder().encodeToString(digest);
        
        assertThat(result).isEqualTo(expected);
    }

    @Test
    void computeDeleteObjectsContentMd5_multipleObjects() throws NoSuchAlgorithmException {
        final List<ObjectIdentifier> objects = Arrays.asList(
            ObjectIdentifier.builder().key("key1").build(),
            ObjectIdentifier.builder().key("key2").build()
        );
        
        final String result = S3Utils.computeDeleteObjectsContentMd5(objects);
        
        // Expected XML: <Delete><Object><Key>key1</Key></Object><Object><Key>key2</Key></Object></Delete>
        final String expectedXml = "<Delete><Object><Key>key1</Key></Object><Object><Key>key2</Key></Object></Delete>";
        final byte[] xmlBytes = expectedXml.getBytes(StandardCharsets.UTF_8);
        final MessageDigest md = MessageDigest.getInstance("MD5");
        final byte[] digest = md.digest(xmlBytes);
        final String expected = Base64.getEncoder().encodeToString(digest);
        
        assertThat(result).isEqualTo(expected);
    }

    @Test
    void computeDeleteObjectsContentMd5_withXmlEscaping() throws NoSuchAlgorithmException {
        final List<ObjectIdentifier> objects = Arrays.asList(
            ObjectIdentifier.builder().key("key<with>&'\"special>chars").build()
        );
        
        final String result = S3Utils.computeDeleteObjectsContentMd5(objects);
        
        // Expected XML with escaped characters: 
        // <Delete><Object><Key>key&lt;with&gt;&amp;&apos;&quot;special&gt;chars</Key></Object></Delete>
        final String expectedXml = "<Delete><Object><Key>key&lt;with&gt;&amp;&apos;&quot;special&gt;chars</Key></Object></Delete>";
        final byte[] xmlBytes = expectedXml.getBytes(StandardCharsets.UTF_8);
        final MessageDigest md = MessageDigest.getInstance("MD5");
        final byte[] digest = md.digest(xmlBytes);
        final String expected = Base64.getEncoder().encodeToString(digest);
        
        assertThat(result).isEqualTo(expected);
    }

    @Test
    void computeDeleteObjectsContentMd5_emptyList() throws NoSuchAlgorithmException {
        final List<ObjectIdentifier> objects = Arrays.asList();
        
        final String result = S3Utils.computeDeleteObjectsContentMd5(objects);
        
        // Expected XML: <Delete></Delete>
        final String expectedXml = "<Delete></Delete>";
        final byte[] xmlBytes = expectedXml.getBytes(StandardCharsets.UTF_8);
        final MessageDigest md = MessageDigest.getInstance("MD5");
        final byte[] digest = md.digest(xmlBytes);
        final String expected = Base64.getEncoder().encodeToString(digest);
        
        assertThat(result).isEqualTo(expected);
    }

    @Test
    void computeDeleteObjectsContentMd5_nullKey() throws NoSuchAlgorithmException {
        final List<ObjectIdentifier> objects = Arrays.asList(
            ObjectIdentifier.builder().key(null).build()
        );
        
        final String result = S3Utils.computeDeleteObjectsContentMd5(objects);
        
        // Expected XML with empty key: <Delete><Object><Key></Key></Object></Delete>
        final String expectedXml = "<Delete><Object><Key></Key></Object></Delete>";
        final byte[] xmlBytes = expectedXml.getBytes(StandardCharsets.UTF_8);
        final MessageDigest md = MessageDigest.getInstance("MD5");
        final byte[] digest = md.digest(xmlBytes);
        final String expected = Base64.getEncoder().encodeToString(digest);
        
        assertThat(result).isEqualTo(expected);
    }
}