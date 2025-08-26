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

import io.aiven.kafka.tieredstorage.storage.ObjectKey;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class S3DeleteUtilsTest {

    @Test
    void testComputeDeleteObjectsContentMd5SingleKey() throws NoSuchAlgorithmException {
        final List<ObjectKey> keys = Arrays.asList(
            new TestObjectKey("test-key-1")
        );
        
        final String result = S3DeleteUtils.computeDeleteObjectsContentMd5(keys);
        
        assertNotNull(result);
        
        // Verify the result by manually computing the expected value
        final String expectedXml = "<Delete><Object><Key>test-key-1</Key></Object></Delete>";
        final byte[] xmlBytes = expectedXml.getBytes(StandardCharsets.UTF_8);
        final MessageDigest md5 = MessageDigest.getInstance("MD5");
        final byte[] hashBytes = md5.digest(xmlBytes);
        final String expected = Base64.getEncoder().encodeToString(hashBytes);
        
        assertEquals(expected, result);
    }

    @Test
    void testComputeDeleteObjectsContentMd5MultipleKeys() throws NoSuchAlgorithmException {
        final List<ObjectKey> keys = Arrays.asList(
            new TestObjectKey("key1"),
            new TestObjectKey("key2"),
            new TestObjectKey("key3")
        );
        
        final String result = S3DeleteUtils.computeDeleteObjectsContentMd5(keys);
        
        assertNotNull(result);
        
        // Verify the result by manually computing the expected value
        final String expectedXml = "<Delete><Object><Key>key1</Key></Object><Object><Key>key2</Key></Object><Object><Key>key3</Key></Object></Delete>";
        final byte[] xmlBytes = expectedXml.getBytes(StandardCharsets.UTF_8);
        final MessageDigest md5 = MessageDigest.getInstance("MD5");
        final byte[] hashBytes = md5.digest(xmlBytes);
        final String expected = Base64.getEncoder().encodeToString(hashBytes);
        
        assertEquals(expected, result);
    }

    @Test
    void testComputeDeleteObjectsContentMd5WithXmlEscaping() throws NoSuchAlgorithmException {
        final List<ObjectKey> keys = Arrays.asList(
            new TestObjectKey("key-with-&-ampersand"),
            new TestObjectKey("key-with-<-less-than"),
            new TestObjectKey("key-with->-greater-than"),
            new TestObjectKey("key-with-\"-quote"),
            new TestObjectKey("key-with-'-apostrophe")
        );
        
        final String result = S3DeleteUtils.computeDeleteObjectsContentMd5(keys);
        
        assertNotNull(result);
        
        // Verify the result by manually computing the expected value with escaped XML
        final String expectedXml = "<Delete>" +
            "<Object><Key>key-with-&amp;-ampersand</Key></Object>" +
            "<Object><Key>key-with-&lt;-less-than</Key></Object>" +
            "<Object><Key>key-with-&gt;-greater-than</Key></Object>" +
            "<Object><Key>key-with-&quot;-quote</Key></Object>" +
            "<Object><Key>key-with-&apos;-apostrophe</Key></Object>" +
            "</Delete>";
        final byte[] xmlBytes = expectedXml.getBytes(StandardCharsets.UTF_8);
        final MessageDigest md5 = MessageDigest.getInstance("MD5");
        final byte[] hashBytes = md5.digest(xmlBytes);
        final String expected = Base64.getEncoder().encodeToString(hashBytes);
        
        assertEquals(expected, result);
    }

    @Test
    void testComputeDeleteObjectsContentMd5EmptyList() throws NoSuchAlgorithmException {
        final List<ObjectKey> keys = Arrays.asList();
        
        final String result = S3DeleteUtils.computeDeleteObjectsContentMd5(keys);
        
        assertNotNull(result);
        
        // Verify the result by manually computing the expected value
        final String expectedXml = "<Delete></Delete>";
        final byte[] xmlBytes = expectedXml.getBytes(StandardCharsets.UTF_8);
        final MessageDigest md5 = MessageDigest.getInstance("MD5");
        final byte[] hashBytes = md5.digest(xmlBytes);
        final String expected = Base64.getEncoder().encodeToString(hashBytes);
        
        assertEquals(expected, result);
    }

    // Test helper class implementing ObjectKey
    private static class TestObjectKey implements ObjectKey {
        private final String value;

        public TestObjectKey(final String value) {
            this.value = value;
        }

        @Override
        public String value() {
            return value;
        }
    }
}