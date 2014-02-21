/**
 * PODD is an OWL ontology database used for scientific project management
 * 
 * Copyright (C) 2009-2013 The University Of Queensland
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Affero General Public License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 */
package com.github.podd.utils.test;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.ConcurrentMap;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.github.podd.utils.PoddDigestUtils;

public class PoddDigestUtilsTest
{
    @Rule
    public TemporaryFolder tempDir = new TemporaryFolder();
    
    private Path testDir;
    
    @Before
    public void setUp() throws Exception
    {
        testDir = tempDir.newFolder("podddigestutils").toPath();
    }
    
    @After
    public void tearDown() throws Exception
    {
        testDir = null;
    }
    
    @Test
    public final void testGetDigestsEmpty() throws Exception
    {
        Path emptyFile = testDir.resolve("emptyfile.txt");
        Files.createFile(emptyFile);
        
        Assert.assertTrue(Files.exists(emptyFile));
        Assert.assertEquals(0, Files.size(emptyFile));
        
        ConcurrentMap<Path, ConcurrentMap<String, String>> digests =
                PoddDigestUtils.getDigests(Arrays.asList(emptyFile));
        
        Assert.assertEquals(1, digests.size());
        
        Assert.assertTrue(digests.containsKey(emptyFile));
        
        ConcurrentMap<String, String> emptyFileDigests = digests.get(emptyFile);
        
        Assert.assertEquals(2, emptyFileDigests.size());
        
        Assert.assertTrue(emptyFileDigests.containsKey("MD5"));
        Assert.assertTrue(emptyFileDigests.containsKey("SHA-1"));
        
        Assert.assertEquals("d41d8cd98f00b204e9800998ecf8427e", emptyFileDigests.get("MD5"));
        Assert.assertEquals("da39a3ee5e6b4b0d3255bfef95601890afd80709", emptyFileDigests.get("SHA-1"));
    }
    
    @Test
    public final void testGetDigestsShort() throws Exception
    {
        Path shortFile = testDir.resolve("shortfile.txt");
        Files.copy(
                new ByteArrayInputStream("The quick brown fox jumps over the lazy dog".getBytes(StandardCharsets.UTF_8)),
                shortFile);
        
        Assert.assertTrue(Files.exists(shortFile));
        Assert.assertEquals(43, Files.size(shortFile));
        
        ConcurrentMap<Path, ConcurrentMap<String, String>> digests =
                PoddDigestUtils.getDigests(Arrays.asList(shortFile));
        
        Assert.assertEquals(1, digests.size());
        
        Assert.assertTrue(digests.containsKey(shortFile));
        
        ConcurrentMap<String, String> shortFileDifests = digests.get(shortFile);
        
        Assert.assertEquals(2, shortFileDifests.size());
        
        Assert.assertTrue(shortFileDifests.containsKey("MD5"));
        Assert.assertTrue(shortFileDifests.containsKey("SHA-1"));
        
        Assert.assertEquals("9e107d9d372bb6826bd81d3542a419d6", shortFileDifests.get("MD5"));
        Assert.assertEquals("2fd4e1c67a2d28fced849ee1bb76e7391b93eb12", shortFileDifests.get("SHA-1"));
    }
    
}
