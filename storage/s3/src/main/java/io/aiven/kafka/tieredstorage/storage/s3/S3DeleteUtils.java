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
import java.util.Base64;
import java.util.List;

import io.aiven.kafka.tieredstorage.storage.ObjectKey;

/**
 * Utility class for S3 delete operations.
 * Provides methods to compute Content-MD5 headers for Tencent COS compatibility.
 * See: Aiven-Open/tiered-storage-for-apache-kafka#694
 */
class S3DeleteUtils {

    /**
     * Computes the Content-MD5 header value for a batch delete request.
     * Generates the delete XML body, serializes to UTF-8, computes MD5 hash, and Base64 encodes.
     *
     * @param keys the object keys to delete
     * @return Base64-encoded MD5 hash of the delete XML body
     */
    static String computeDeleteObjectsContentMd5(final List<ObjectKey> keys) {
        final String deleteXml = generateDeleteXml(keys);
        final byte[] xmlBytes = deleteXml.getBytes(StandardCharsets.UTF_8);
        
        try {
            final MessageDigest md5 = MessageDigest.getInstance("MD5");
            final byte[] hashBytes = md5.digest(xmlBytes);
            return Base64.getEncoder().encodeToString(hashBytes);
        } catch (final NoSuchAlgorithmException e) {
            // MD5 should always be available
            throw new RuntimeException("MD5 algorithm not available", e);
        }
    }

    /**
     * Generates the XML body for a batch delete request.
     * Escapes XML special characters in object keys.
     *
     * @param keys the object keys to delete
     * @return XML string for the delete request
     */
    private static String generateDeleteXml(final List<ObjectKey> keys) {
        final StringBuilder xml = new StringBuilder();
        xml.append("<Delete>");
        
        for (final ObjectKey key : keys) {
            xml.append("<Object><Key>");
            xml.append(escapeXml(key.value()));
            xml.append("</Key></Object>");
        }
        
        xml.append("</Delete>");
        return xml.toString();
    }

    /**
     * Escapes XML special characters in a string.
     *
     * @param value the string to escape
     * @return escaped string safe for XML
     */
    private static String escapeXml(final String value) {
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;");
    }
}