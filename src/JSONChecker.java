import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;

import java.io.*;
import java.net.URI;
import java.util.Arrays;
import java.util.Scanner;

public class JSONChecker {
    private static final String INPUT_DIR = "testcases/in/";
    private static final String OUTPUT_DIR = "testcases/out/";
    private static final String YOURS_DIR = "testcases/yours/";


    private String url;

    public JSONChecker(String url) {
        this.url = url;
    }

    public static void writeOutput(String file, String response) {
        try {
            PrintWriter out = new PrintWriter(new FileOutputStream((YOURS_DIR + file)));

            out.println(response);
            out.close();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {

        // uncomment this if you want to see the debug messages of HttpClient
        /*
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.SimpleLog");
        System.setProperty("org.apache.commons.logging.simplelog.showdatetime", "true");
        System.setProperty("org.apache.commons.logging.simplelog.log.httpclient.wire", "debug");
        System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http", "debug");
        */

        String url;


        if (args.length != 1) {
            System.out.println("java JSONCheckerTest <URL>");
            System.out.println("e.g. java JSONCheckerTest http://2013-G8T8.appspot.com/seisfun/");
            return;
        } else {
            url = args[0];
        }

        if (!url.endsWith("/")) {
            url += "/";
        }

        JSONChecker checker = new JSONChecker(url);

        // delete the files in the yours directory
        checker.deleteFilesInYoursDirectory();

        File f = new File("testcases/in");

        File[] directories = f.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return (name.matches("[0-9]+.*"));
            }
        });

        // sort the directories so that the test cases will be read in order
        // using the number in front of the file name
        Arrays.sort(directories);

        int total = 0;
        int numPassed = 0;
        for (int i = 0; i < directories.length; i++) {
            String name = directories[i].getName();

            //
            int posOfDash = name.indexOf("-");

            int testCaseNum = Integer.parseInt(name.substring(0, posOfDash));
            int posOfDot = name.indexOf(".");

            if (directories[i].getName().endsWith(".zip")) {
                total++;

                name = name.substring(0, posOfDot);

                if (checker.bootstrap(directories[i].getName(), name)) {
                    numPassed++;
                    System.out.println("Test Case " + testCaseNum + " passed");
                } else {
                    System.out.println("Test Case " + testCaseNum + " failed");
                }
            } else {
                total++;
                String call = name.substring(posOfDash + 1, posOfDot);

                if (checker.check(call, name)) {
                    numPassed++;
                    System.out.println("Test Case " + testCaseNum + " passed");
                } else {
                    System.out.println("Test Case " + testCaseNum + " failed");
                }
            }
        }
        System.out.println("Total: " + numPassed + "/" + total);

    }

    public boolean assertEquals(String studentAns, String correctAns) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode studentAnsNode = mapper.readTree(studentAns);
            JsonNode correctAnsNode = mapper.readTree(correctAns);

            return studentAnsNode.equals(correctAnsNode);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return false;

    }

    public String readFile(String ans) {
        String data = "";
        try {
            File result = new File(ans);
            Scanner sc = new Scanner(new FileInputStream(result));

            while (sc.hasNext()) {
                data += sc.nextLine();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return data;
    }

    public boolean bootstrap(String dataFile, String call) {
        try {
            File f;
            f = new File(INPUT_DIR + dataFile);

            HttpClient client = new DefaultHttpClient();
            client.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);

            HttpPost post = new HttpPost(url + "bootstrap");

            MultipartEntity entity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);

            // For File parameters
            entity.addPart("bootstrap-file", new FileBody( f, "application/zip"));

            //String startDate = readFile(INPUT_DIR + call + ".zip.txt");
            // For usual String parameters
            //entity.addPart("start-date", new StringBody(startDate, "text/plain",
            //        Charset.forName("UTF-8")));

            post.setEntity(entity);

            // Here we go!
            String response = EntityUtils.toString(client.execute(post).getEntity(), "UTF-8");

            // use the same name as the answer
            writeOutput(call + ".txt", response);
            client.getConnectionManager().shutdown();
//
//            System.out.println("--->" + OUTPUT_DIR + call + ".txt");
            String answer = readFile(OUTPUT_DIR + call + ".txt");
//
//            System.out.println("-->" + answer);
//            System.out.println(response);
            return assertEquals(response, answer);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return false;
    }

    // call - the URL (dump, retrieve etc)
    public boolean check(String call, String filename) {
        try {

            HttpClient client = new DefaultHttpClient();

            HttpParams httpParams = client.getParams();
            HttpConnectionParams.setConnectionTimeout(httpParams, 50000);
            HttpConnectionParams.setSoTimeout(httpParams, 50000);

            client.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);

            URIBuilder builder = new URIBuilder(new URI(url + call));
            //builder.setPath(call);

            String input = readFile(INPUT_DIR + filename);
            if (input != null) {
                builder.setParameter("r", input);
            }
            URI uri = builder.build();

            //System.out.println(uri.toString());
            HttpGet httpget = new HttpGet(uri);

            // System.out.println(client.execute(httpget).getEntity());
            String response = EntityUtils.toString(client.execute(httpget).getEntity(), "UTF-8");

            client.getConnectionManager().shutdown();


            String answer = readFile(OUTPUT_DIR + filename);
            writeOutput(filename, response);

            //  System.out.println(response);
            //  System.out.println(answer);
            return assertEquals(response, answer);

        } catch (Throwable e) {
            e.printStackTrace();
        }
        return false;
    }

    public void deleteFilesInYoursDirectory() {
        File dir = new File("testcases/yours");
        File[] subdirs = dir.listFiles();

        for (File f : subdirs) {
            f.delete();
        }
    }

}
