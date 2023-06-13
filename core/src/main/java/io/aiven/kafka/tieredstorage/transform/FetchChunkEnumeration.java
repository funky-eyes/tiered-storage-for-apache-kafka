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

package io.aiven.kafka.tieredstorage.transform;

import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.Enumeration;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

import org.apache.kafka.server.log.remote.storage.RemoteLogSegmentMetadata;

import io.aiven.kafka.tieredstorage.Chunk;
import io.aiven.kafka.tieredstorage.ChunkManager;
import io.aiven.kafka.tieredstorage.manifest.SegmentManifest;
import io.aiven.kafka.tieredstorage.manifest.index.ChunkIndex;
import io.aiven.kafka.tieredstorage.storage.BytesRange;
import io.aiven.kafka.tieredstorage.storage.StorageBackendException;

import org.apache.commons.io.input.BoundedInputStream;

public class FetchChunkEnumeration implements Enumeration<InputStream> {
    private final ChunkManager chunkManager;
    private final RemoteLogSegmentMetadata remoteLogSegmentMetadata;
    private final SegmentManifest manifest;
    private final BytesRange range;
    final int startChunkId;
    final int lastChunkId;
    private final ChunkIndex chunkIndex;
    int currentChunkId;

    /**
     *
     * @param chunkManager provides chunk input to fetch from
     * @param remoteLogSegmentMetadata required by chunkManager
     * @param manifest provides to index to build response from
     * @param range original offset range start/end position
     */
    public FetchChunkEnumeration(final ChunkManager chunkManager,
                                 final RemoteLogSegmentMetadata remoteLogSegmentMetadata,
                                 final SegmentManifest manifest,
                                 final BytesRange range) {
        this.chunkManager = Objects.requireNonNull(chunkManager, "chunkManager cannot be null");
        this.remoteLogSegmentMetadata =
            Objects.requireNonNull(remoteLogSegmentMetadata, "remoteLogSegmentMetadata cannot be null");
        this.manifest = Objects.requireNonNull(manifest, "manifest cannot be null");
        this.range = Objects.requireNonNull(range, "range cannot be null");

        this.chunkIndex = manifest.chunkIndex();

        final Chunk firstChunk = getFirstChunk(range.from);
        startChunkId = firstChunk.id;
        currentChunkId = startChunkId;
        final Chunk lastChunk = getLastChunk(range.to);
        lastChunkId = lastChunk.id;
    }

    private Chunk getFirstChunk(final int fromPosition) {
        final Chunk firstChunk = chunkIndex.findChunkForOriginalOffset(fromPosition);
        if (firstChunk == null) {
            throw new IllegalArgumentException("Invalid start position "
                + fromPosition + " in segment " + remoteLogSegmentMetadata);
        }
        return firstChunk;
    }

    private Chunk getLastChunk(final int endPosition) {
        final Chunk chunkForOriginalOffset = chunkIndex.findChunkForOriginalOffset(endPosition);
        if (chunkForOriginalOffset == null) {
            final List<Chunk> chunks = chunkIndex.chunks();
            return chunks.get(chunks.size() - 1);
        } else {
            return chunkForOriginalOffset;
        }
    }

    @Override
    public boolean hasMoreElements() {
        return currentChunkId <= lastChunkId;
    }

    @Override
    public InputStream nextElement() {
        if (!hasMoreElements()) {
            throw new NoSuchElementException();
        }

        InputStream chunkContent = getChunkContent(currentChunkId);

        final Chunk currentChunk = chunkIndex.chunks().get(currentChunkId);
        final int chunkStartPosition = currentChunk.originalPosition;
        final boolean isAtFirstChunk = currentChunkId == startChunkId;
        final boolean isAtLastChunk = currentChunkId == lastChunkId;
        final boolean isSingleChunk = isAtFirstChunk && isAtLastChunk;
        if (isSingleChunk) {
            final int toSkip = range.from - chunkStartPosition;
            try {
                chunkContent.skip(toSkip);
                final int chunkSize = range.size();
                chunkContent = new BoundedInputStream(chunkContent, chunkSize);
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            if (isAtFirstChunk) {
                final int toSkip = range.from - chunkStartPosition;
                try {
                    chunkContent.skip(toSkip);
                } catch (final IOException e) {
                    throw new RuntimeException(e);
                }
            }
            if (isAtLastChunk) {
                final int chunkSize = range.to - chunkStartPosition + 1;
                chunkContent = new BoundedInputStream(chunkContent, chunkSize);
            }
        }

        currentChunkId += 1;
        return chunkContent;
    }

    private InputStream getChunkContent(final int chunkId) {
        try {
            return chunkManager.getChunk(remoteLogSegmentMetadata, manifest, chunkId);
        } catch (final StorageBackendException e) {
            throw new RuntimeException(e);
        }
    }

    public InputStream toInputStream() {
        return new SequenceInputStream(this);
    }
}