package org.apache.coyote.http11;

import java.io.ByteArrayOutputStream;
import java.util.zip.GZIPOutputStream;

import junit.framework.TestCase;

import org.apache.coyote.Response;
import org.apache.coyote.http11.filters.GzipOutputFilter;
import org.apache.tomcat.util.buf.ByteChunk;

/**
 * Test case to demonstrate the interaction between gzip and flushing in the
 * output filter.
 */
public class TestGzipOutputFilter extends TestCase {

    /**
     * Test the interaction betwen gzip and flushing. The idea is to: 1. create
     * a internal output buffer, response, and attach an active gzipoutputfilter
     * to the output buffer 2. set the output stream of the internal buffer to
     * be a ByteArrayOutputStream so we can inspect the output bytes 3. write a
     * chunk out using the gzipoutputfilter and invoke a flush on the
     * InternalOutputBuffer 4. read from the ByteArrayOutputStream to find out
     * what's being written out (flushed) 5. find out what's expected by wrting
     * to GZIPOutputStream and close it (to force flushing) 6. Compare the size
     * of the two arrays, they should be close (instead of one being much
     * shorter than the other one)
     * 
     * @throws Exception
     */
    public void testFlushingWithGzip() throws Exception {
        // set up response, InternalOutputBuffer, and ByteArrayOutputStream
        Response res = new Response();
        InternalOutputBuffer iob = new InternalOutputBuffer(res);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        iob.setOutputStream(bos);
        res.setOutputBuffer(iob);

        // set up GzipOutputFilter to attach to the InternalOutputBuffer
        GzipOutputFilter gf = new GzipOutputFilter();
        iob.addFilter(gf);
        iob.addActiveFilter(gf);

        // write a chunk out
        ByteChunk chunk = new ByteChunk(1024);
        byte[] d = "Hello there tomcat developers, there is a bug in JDK".getBytes();
        chunk.append(d, 0, d.length);
        iob.doWrite(chunk, res);

        // flush the InternalOutputBuffer
        iob.flush();

        // read from the ByteArrayOutputStream to find out what's being written
        // out (flushed)
        byte[] dataFound = bos.toByteArray();

        // find out what's expected by wrting to GZIPOutputStream and close it
        // (to force flushing)
        ByteArrayOutputStream gbos = new ByteArrayOutputStream(1024);
        GZIPOutputStream gos = new GZIPOutputStream(gbos);
        gos.write(d);
        gos.close();

        // read the expected data
        byte[] dataExpected = gbos.toByteArray();

        // most of the data should have been flushed out
        assertTrue(dataFound.length >= (dataExpected.length - 20));
    }
}