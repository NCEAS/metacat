package edu.ucsb.nceas.metacat.admin.upgrade.solr;

import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.withSettings;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import edu.ucsb.nceas.LeanTestUtils;
import edu.ucsb.nceas.metacat.properties.PropertyService;

/**
 * Test the class of SolrJvmVersionFinder
 * @author tao
 *
 */
public class SolrJvmVersionFinderIT {

    private static final String ADMIN_INFO = """
<?xml version="1.0" encoding="UTF-8"?>
    <response>
    <lst name="responseHeader">
      <int name="status">0</int>
      <int name="QTime">14</int>
    </lst>
    <str name="mode">std</str>
    <str name="solr_home">/Users/tao/devtools/solr-8.11.1/server/solr</str>
    <lst name="lucene">
      <str name="solr-spec-version">8.11.1</str>
      <str name="solr-impl-version">8.11.1 0b002b11819df70783e83ef36b42ed1223c14b50 - janhoy -
                      2021-12-14 13:50:55</str>
      <str name="lucene-spec-version">8.11.1</str>
      <str name="lucene-impl-version">8.11.1 0b002b11819df70783e83ef36b42ed1223c14b50 - janhoy -
                      2021-12-14 13:46:43</str>
    </lst>
    <lst name="jvm">
      <str name="version">1.8.0_351 25.351-b10</str>
      <str name="name">Oracle Corporation Java HotSpot(TM) 64-Bit Server VM</str>
      <lst name="spec">
        <str name="vendor">Oracle Corporation</str>
        <str name="name">Java Platform API Specification</str>
        <str name="version">1.8</str>
      </lst>
      <lst name="jre">
        <str name="vendor">Oracle Corporation</str>
        <str name="version">1.8.0_351</str>
      </lst>
      <lst name="vm">
        <str name="vendor">Oracle Corporation</str>
        <str name="name">Java HotSpot(TM) 64-Bit Server VM</str>
        <str name="version">25.351-b10</str>
      </lst>
      <int name="processors">10</int>
      <lst name="memory">
        <str name="free">373 MB</str>
        <str name="total">512 MB</str>
        <str name="max">512 MB</str>
        <str name="used">139 MB (%27.1)</str>
        <lst name="raw">
          <long name="free">391116504</long>
          <long name="total">536870912</long>
          <long name="max">536870912</long>
          <long name="used">145754408</long>
          <double name="used%">27.14887410402298</double>
        </lst>
      </lst>
      <lst name="jmx">
        <str name="bootclasspath">/Library/Java/JavaVirtualMachines/jdk1.8.0_351.jdk/Contents/Home
        /jre/lib/resources.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_351.jdk/Contents/Home
        /jre/lib/rt.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_351.jdk/Contents/Home/jre/lib
        /jsse.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_351.jdk/Contents/Home/jre/lib
        /jce.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_351.jdk/Contents/Home/jre/lib
        /charsets.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_351.jdk/Contents/Home/jre
        /lib/jfr.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_351.jdk/Contents/Home
        /jre/classes</str>
        <str name="classpath">start.jar</str>
        <arr name="commandLineArgs">
          <str>-Xms512m</str>
          <str>-Xmx512m</str>
          <str>-XX:+UseG1GC</str>
          <str>-XX:+PerfDisableSharedMem</str>
          <str>-XX:+ParallelRefProcEnabled</str>
          <str>-XX:MaxGCPauseMillis=250</str>
          <str>-XX:+UseLargePages</str>
          <str>-XX:+AlwaysPreTouch</str>
          <str>-XX:+ExplicitGCInvokesConcurrent</str>
          <str>-verbose:gc</str>
          <str>-XX:+PrintHeapAtGC</str>
          <str>-XX:+PrintGCDetails</str>
          <str>-XX:+PrintGCDateStamps</str>
          <str>-XX:+PrintGCTimeStamps</str>
          <str>-XX:+PrintTenuringDistribution</str>
          <str>-XX:+PrintGCApplicationStoppedTime</str>
          <str>-Xloggc:/Users/tao/devtools/solr-8.11.1/server/logs/solr_gc.log</str>
          <str>-XX:+UseGCLogFileRotation</str>
          <str>-XX:NumberOfGCLogFiles=9</str>
          <str>-XX:GCLogFileSize=20M</str>
          <str>-Dsolr.jetty.inetaccess.includes=</str>
          <str>-Dsolr.jetty.inetaccess.excludes=</str>
          <str>-Dsolr.log.dir=/Users/tao/devtools/solr-8.11.1/server/logs</str>
          <str>-Djetty.port=8983</str>
          <str>-DSTOP.PORT=7983</str>
          <str>-DSTOP.KEY=solrrocks</str>
          <str>-Duser.timezone=UTC</str>
          <str>-XX:-OmitStackTraceInFastThrow</str>
          <str>-XX:OnOutOfMemoryError=/Users/tao/devtools/solr-8.11.1/bin/oom_solr.sh 8983
                      /Users/tao/devtools/solr-8.11.1/server/logs</str>
          <str>-Djetty.home=/Users/tao/devtools/solr-8.11.1/server</str>
          <str>-Dsolr.solr.home=/Users/tao/devtools/solr-8.11.1/server/solr</str>
          <str>-Dsolr.data.home=</str>
          <str>-Dsolr.install.dir=/Users/tao/devtools/solr-8.11.1</str>
          <str>-Dsolr.default.confdir=/Users/tao/devtools/solr-8.11.1/server/solr/configsets
                      /_default/conf</str>
          <str>-Dlog4j2.formatMsgNoLookups=true</str>
          <str>-Dsolr.allowPaths=*</str>
          <str>-Xss256k</str>
          <str>-Dsolr.log.muteconsole</str>
        </arr>
        <date name="startTime">2024-02-17T06:41:21.521Z</date>
        <long name="upTimeMS">28315040</long>
      </lst>
    </lst>
    <lst name="security">
      <bool name="tls">false</bool>
    </lst>
    <lst name="system">
      <str name="name">Mac OS X</str>
      <str name="arch">x86_64</str>
      <int name="availableProcessors">10</int>
      <double name="systemLoadAverage">3.4716796875</double>
      <str name="version">12.7</str>
      <long name="committedVirtualMemorySize">38231347200</long>
      <long name="freePhysicalMemorySize">318541824</long>
      <long name="freeSwapSpaceSize">1564147712</long>
      <double name="processCpuLoad">0.2</double>
      <long name="processCpuTime">175042461000</long>
      <double name="systemCpuLoad">NaN</double>
      <long name="totalPhysicalMemorySize">34359738368</long>
      <long name="totalSwapSpaceSize">2147483648</long>
      <long name="maxFileDescriptorCount">10240</long>
      <long name="openFileDescriptorCount">246</long>
    </lst>
    </response>""";

    private String solrBaseUrl = null;

    /**
     * Setup
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {
        LeanTestUtils.initializePropertyService(LeanTestUtils.PropertiesMode.UNIT_TEST);
        solrBaseUrl = PropertyService.getProperty("solr.baseURL");
    }


    /**
     * Test the find method
     * @throws Exception
     */
    @Test
    public void testFind() throws Exception {
        SolrJvmVersionFinder mockVersionFinder = Mockito.mock(SolrJvmVersionFinder.class,
                withSettings().useConstructor(solrBaseUrl).defaultAnswer(CALLS_REAL_METHODS));
        Mockito.doReturn(ADMIN_INFO).when(mockVersionFinder).getSolrAdminInfo();
        String version = mockVersionFinder.find();
        assertEquals("The jvm version " + version + " should be 1.8.0_351.", "1.8.0_351", version);
    }

    /**
     * Live test to get the solr admin information
     * @throws Exception
     */
    @Test
    public void testGetSolrAdminInfo() throws Exception {
        SolrJvmVersionFinder versionFinder = new SolrJvmVersionFinder(solrBaseUrl);
        String result = versionFinder.getSolrAdminInfo();
        assertTrue("The result should have the jre keyword.", result.contains("jre"));
        assertTrue("The result should have the jre keyword.", result.contains("version"));
        assertTrue("The result should have the jre keyword.", result.contains("lucene"));
    }

}
