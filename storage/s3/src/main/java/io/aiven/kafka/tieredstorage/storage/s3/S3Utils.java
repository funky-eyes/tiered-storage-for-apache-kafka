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

import software.amazon.awssdk.services.s3.model.ObjectIdentifier;

/**
 * Utility class for S3 operations.
 */
class S3Utils {

    /**
     * Computes Content-MD5 header value for DeleteObjects request.
     * This is required for compatibility with Tencent Cloud COS.
     * 
     * @param objectIdentifiers list of object identifiers to delete
     * @return Base64 encoded MD5 hash of the Delete XML request body
     */
    static String computeDeleteObjectsContentMd5(final List<ObjectIdentifier> objectIdentifiers) {
        final StringBuilder sb = new StringBuilder();
        sb.append("<Delete>");
        for (final ObjectIdentifier id : objectIdentifiers) {
            sb.append("<Object><Key>")
              .append(escapeXml(id.key()))
              .append("</Key></Object>");
        }
        sb.append("</Delete>");
        
        final byte[] xmlBytes = sb.toString().getBytes(StandardCharsets.UTF_8);
        
        try {
            final MessageDigest md = MessageDigest.getInstance("MD5");
            final byte[] digest = md.digest(xmlBytes);
            return Base64.getEncoder().encodeToString(digest);
        } catch (final NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not available", e);
        }
    }

    /**
     * Escapes XML special characters in a string.
     * 
     * @param s string to escape
     * @return escaped string
     */
    private static String escapeXml(final String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("'", "&apos;")
                .replace("\"", "&quot;");
    }
}