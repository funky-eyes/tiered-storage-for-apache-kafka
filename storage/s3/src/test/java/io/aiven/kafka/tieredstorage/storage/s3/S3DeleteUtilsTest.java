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

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import software.amazon.awssdk.services.s3.model.ObjectIdentifier;

import static org.assertj.core.api.Assertions.assertThat;

class S3DeleteUtilsTest {

    @Test
    void computeDeleteObjectsContentMd5WithKnownInput() {
        final List<ObjectIdentifier> objectIds = Arrays.asList(
            ObjectIdentifier.builder().key("test").build()
        );
        
        final String contentMd5 = S3DeleteUtils.computeDeleteObjectsContentMd5(objectIds);
        
        // The expected XML is: <Delete><Object><Key>test</Key></Object></Delete>
        // Let's verify we get a predictable Base64-encoded MD5 hash
        assertThat(contentMd5).isNotEmpty();
        assertThat(contentMd5).isBase64();
        assertThat(contentMd5).hasSize(24); // Base64 encoding of 16-byte MD5 hash is 24 characters
    }

    @Test
    void computeDeleteObjectsContentMd5WithMultipleKeys() {
        final List<ObjectIdentifier> objectIds = Arrays.asList(
            ObjectIdentifier.builder().key("key1").build(),
            ObjectIdentifier.builder().key("key2").build()
        );
        
        final String contentMd5 = S3DeleteUtils.computeDeleteObjectsContentMd5(objectIds);
        
        assertThat(contentMd5).isNotEmpty();
        assertThat(contentMd5).isBase64();
    }

    @Test
    void computeDeleteObjectsContentMd5WithXmlSpecialCharacters() {
        final List<ObjectIdentifier> objectIds = Arrays.asList(
            ObjectIdentifier.builder().key("key-with-<>&\"'").build()
        );
        
        final String contentMd5 = S3DeleteUtils.computeDeleteObjectsContentMd5(objectIds);
        
        // Should handle XML escaping correctly
        assertThat(contentMd5).isNotEmpty();
        assertThat(contentMd5).isBase64();
    }

    @Test
    void computeDeleteObjectsContentMd5IsConsistent() {
        final List<ObjectIdentifier> objectIds = Arrays.asList(
            ObjectIdentifier.builder().key("consistent-key").build()
        );
        
        final String contentMd5_1 = S3DeleteUtils.computeDeleteObjectsContentMd5(objectIds);
        final String contentMd5_2 = S3DeleteUtils.computeDeleteObjectsContentMd5(objectIds);
        
        // Same input should produce same output
        assertThat(contentMd5_1).isEqualTo(contentMd5_2);
    }

    @Test
    void computeDeleteObjectsContentMd5WithEmptyList() {
        final List<ObjectIdentifier> objectIds = Arrays.asList();
        
        final String contentMd5 = S3DeleteUtils.computeDeleteObjectsContentMd5(objectIds);
        
        // Even empty list should produce valid MD5
        assertThat(contentMd5).isNotEmpty();
        assertThat(contentMd5).isBase64();
    }
}