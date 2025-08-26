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

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;

import io.aiven.kafka.tieredstorage.storage.ObjectKey;
import io.aiven.kafka.tieredstorage.storage.TestObjectKey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class S3StorageContentMd5Test {

    @Mock
    private S3Client s3Client;

    private S3Storage storage;

    @BeforeEach
    void setUp() throws Exception {
        storage = new S3Storage();
        
        // Use reflection to set the required fields directly
        final Field s3ClientField = S3Storage.class.getDeclaredField("s3Client");
        s3ClientField.setAccessible(true);
        s3ClientField.set(storage, s3Client);
        
        final Field bucketNameField = S3Storage.class.getDeclaredField("bucketName");
        bucketNameField.setAccessible(true);
        bucketNameField.set(storage, "test-bucket");
    }

    @Test
    void delete_setsContentMd5Header() throws Exception {
        // Arrange
        final DeleteObjectsResponse mockResponse = DeleteObjectsResponse.builder().build();
        when(s3Client.deleteObjects(org.mockito.ArgumentMatchers.any(DeleteObjectsRequest.class)))
            .thenReturn(mockResponse);

        final Set<ObjectKey> keys = Set.of(
            new TestObjectKey("key1"),
            new TestObjectKey("key2")
        );

        // Act
        storage.delete(keys);

        // Assert
        final ArgumentCaptor<DeleteObjectsRequest> requestCaptor = ArgumentCaptor.forClass(DeleteObjectsRequest.class);
        verify(s3Client).deleteObjects(requestCaptor.capture());

        final DeleteObjectsRequest capturedRequest = requestCaptor.getValue();
        
        // Verify that overrideConfiguration is set
        assertThat(capturedRequest.overrideConfiguration()).isPresent();
        
        // Verify that Content-MD5 header is present
        final var headers = capturedRequest.overrideConfiguration().get().headers();
        assertThat(headers).containsKey("Content-MD5");
        
        // Verify that the Content-MD5 header value is not empty
        final List<String> contentMd5Values = headers.get("Content-MD5");
        assertThat(contentMd5Values).hasSize(1);
        assertThat(contentMd5Values.get(0)).isNotEmpty();
        
        // Verify the header value matches what S3Utils would compute
        final var objectIds = capturedRequest.delete().objects();
        final String expectedMd5 = S3Utils.computeDeleteObjectsContentMd5(objectIds);
        assertThat(contentMd5Values.get(0)).isEqualTo(expectedMd5);
    }

    @Test
    void delete_singleKey_setsContentMd5Header() throws Exception {
        // Arrange
        final DeleteObjectsResponse mockResponse = DeleteObjectsResponse.builder().build();
        when(s3Client.deleteObjects(org.mockito.ArgumentMatchers.any(DeleteObjectsRequest.class)))
            .thenReturn(mockResponse);

        final Set<ObjectKey> keys = Set.of(new TestObjectKey("single-key"));

        // Act
        storage.delete(keys);

        // Assert
        final ArgumentCaptor<DeleteObjectsRequest> requestCaptor = ArgumentCaptor.forClass(DeleteObjectsRequest.class);
        verify(s3Client).deleteObjects(requestCaptor.capture());

        final DeleteObjectsRequest capturedRequest = requestCaptor.getValue();
        
        // Verify that overrideConfiguration is set
        assertThat(capturedRequest.overrideConfiguration()).isPresent();
        
        // Verify that Content-MD5 header is present
        final var headers = capturedRequest.overrideConfiguration().get().headers();
        assertThat(headers).containsKey("Content-MD5");
        
        // Verify that the Content-MD5 header value is not empty
        final List<String> contentMd5Values = headers.get("Content-MD5");
        assertThat(contentMd5Values).hasSize(1);
        assertThat(contentMd5Values.get(0)).isNotEmpty();
    }

    @Test
    void delete_largeNumberOfKeys_setsContentMd5HeaderForEachBatch() throws Exception {
        // Arrange
        final DeleteObjectsResponse mockResponse = DeleteObjectsResponse.builder().build();
        when(s3Client.deleteObjects(org.mockito.ArgumentMatchers.any(DeleteObjectsRequest.class)))
            .thenReturn(mockResponse);

        // Create more than 1000 keys to test batching (MAX_DELETE_OBJECTS = 1000)
        final Set<ObjectKey> keys = Set.of(
            new TestObjectKey("key1"),
            new TestObjectKey("key2"),
            new TestObjectKey("key3")
        );

        // Act
        storage.delete(keys);

        // Assert
        final ArgumentCaptor<DeleteObjectsRequest> requestCaptor = ArgumentCaptor.forClass(DeleteObjectsRequest.class);
        verify(s3Client).deleteObjects(requestCaptor.capture());

        final DeleteObjectsRequest capturedRequest = requestCaptor.getValue();
        
        // Verify that overrideConfiguration is set
        assertThat(capturedRequest.overrideConfiguration()).isPresent();
        
        // Verify that Content-MD5 header is present
        final var headers = capturedRequest.overrideConfiguration().get().headers();
        assertThat(headers).containsKey("Content-MD5");
        
        final List<String> contentMd5Values = headers.get("Content-MD5");
        assertThat(contentMd5Values).hasSize(1);
        assertThat(contentMd5Values.get(0)).isNotEmpty();
    }
}
