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

import software.amazon.awssdk.services.s3.model.ObjectIdentifier;

/**
 * Utility class for S3 delete operations.
 */
class S3DeleteUtils {

    /**
     * Computes the Content-MD5 header value for a DeleteObjects request.
     * 
     * @param objectIdentifiers List of object identifiers to be deleted
     * @return Base64-encoded MD5 hash of the XML request body
     */
    static String computeDeleteObjectsContentMd5(final List<ObjectIdentifier> objectIdentifiers) {
        final String xmlBody = generateDeleteXml(objectIdentifiers);
        final byte[] xmlBytes = xmlBody.getBytes(StandardCharsets.UTF_8);
        final byte[] md5Hash = computeMd5(xmlBytes);
        return Base64.getEncoder().encodeToString(md5Hash);
    }

    /**
     * Generates the XML body for a DeleteObjects request.
     */
    private static String generateDeleteXml(final List<ObjectIdentifier> objectIdentifiers) {
        final StringBuilder xml = new StringBuilder();
        xml.append("<Delete>");
        
        for (final ObjectIdentifier identifier : objectIdentifiers) {
            xml.append("<Object>");
            xml.append("<Key>").append(escapeXml(identifier.key())).append("</Key>");
            xml.append("</Object>");
        }
        
        xml.append("</Delete>");
        return xml.toString();
    }

    /**
     * Escapes XML special characters in a string.
     */
    private static String escapeXml(final String input) {
        if (input == null) {
            return null;
        }
        
        return input.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&apos;");
    }

    /**
     * Computes MD5 hash of the given bytes.
     */
    private static byte[] computeMd5(final byte[] data) {
        try {
            final MessageDigest md5 = MessageDigest.getInstance("MD5");
            return md5.digest(data);
        } catch (final NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not available", e);
        }
    }
}