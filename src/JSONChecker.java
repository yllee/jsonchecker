import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.io.*;
import java.util.*;

public class JSONChecker {
    private static final String INPUT_DIR = "testcases/in/";
    private static final String OUTPUT_DIR = "testcases/out/";
    private static final String YOURS_DIR = "testcases/yours/";


    private String url;
    private String token;


    public JSONChecker(String url) {
        this.url = url;
        this.token = "";
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

        String url = "http://app-2014is203g8t8.rhcloud.com/json";


        if (args.length != 1) {
            System.out.println("java JSONChecker <URL>");
            System.out.println("e.g. java JSONChecker http://app-2014is203g8t8.rhcloud.com/json/");
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

        File[] directories = f.listFiles(new FileFilter() {
            @Override
            public boolean accept(File dir) {
                return !dir.isDirectory() && dir.getName().matches("[0-9]+.*");
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
                String call = name.substring(posOfDash + 1, posOfDot);
                String filenameWithoutExt = name.substring(0, posOfDot);

                if (checker.bootstrap(directories[i].getName(), call, filenameWithoutExt)) {
                    numPassed++;
                    System.out.println("Test Case " + testCaseNum + " passed");
                } else {
                    System.out.println("Test Case " + testCaseNum + " failed");
                }
            } else {
                String call = name.substring(posOfDash + 1, posOfDot);

                boolean testResult = false;
                if (call.equals("authenticate")) {
                    testResult = checker.postCheck(call, name);
                } else {
                    testResult = checker.getCheck(call, name);
                }
                if (testResult) {
                    numPassed++;
                    System.out.println("Test Case " + testCaseNum + " passed");
                } else {
                    System.out.println("Test Case " + testCaseNum + " failed");
                }
            }

            total++;
        }
        System.out.println("Total: " + numPassed + "/" + total);

    }

    public boolean assertAuthenticateEquals(String studentAns, String correctAns) {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode studentAnsNode = null;
        JsonNode correctAnsNode = null;

        try {
            studentAnsNode = mapper.readTree(studentAns);
            correctAnsNode = mapper.readTree(correctAns);

            if (studentAnsNode.get("status").asText().equals("success")) {
                // currently check that it contains two attributes,
                // response status matches the answer status,
                // and the other attribute is called token.
                if ((studentAnsNode.size() == 2)
                        && studentAnsNode.get("status").equals(correctAnsNode.get("status"))
                        && (correctAnsNode.get("token") != null)) {
                    // not really OO
                    // store the received token inside the instance variable
                    // for use in subsequent requests
                    token = studentAnsNode.get("token").asText();
                    return true;
                } else {
                    return false;
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }

        // response is not successful, still compare it with the answer
        return studentAnsNode != null && studentAnsNode.equals(correctAnsNode);

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

    public Properties readInputFile(String path) {
        Properties p = new Properties();
        try {
            p.load(new FileInputStream(new File(path)));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return p;
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

    public boolean bootstrap(String dataFile, String call, String filename) {
        try {
            File f;
            f = new File(INPUT_DIR + dataFile);

            CloseableHttpClient httpclient = HttpClients.createDefault();

            HttpPost httppost = new HttpPost(url + call);

            FileBody bin = new FileBody(f);

            HttpEntity reqEntity = MultipartEntityBuilder.create()
                    .addPart("bootstrap-file", bin)
                    .addTextBody("token", token)
                    .build();


            httppost.setEntity(reqEntity);

            System.out.println("executing request " + httppost.getRequestLine());
            CloseableHttpResponse response = httpclient.execute(httppost);


            HttpEntity resEntity = response.getEntity();


            String teamResponse = EntityUtils.toString(resEntity);
            EntityUtils.consume(resEntity);

            // use the same name as the answer
            writeOutput(filename + ".txt", teamResponse);
//
//            System.out.println("--->" + OUTPUT_DIR + call + ".txt");
            String answer = readFile(OUTPUT_DIR + filename + ".txt");

            return assertEquals(teamResponse, answer);

        } catch (Throwable e) {
            e.printStackTrace();
        }
        return false;
    }

    public String sendGet(String call, Properties params) {
        String result = "";

        CloseableHttpClient client = HttpClientBuilder.create().build();
        CloseableHttpResponse response = null;


        try {
            URIBuilder builder = new URIBuilder(call);

            // convert the Properties object to an array for iterating thru it :(
            for (String key : params.keySet().toArray(new String[0])) {
                builder.addParameter(key, params.getProperty(key));
            }

            builder.addParameter("token", token);

            HttpGet httpGet = new HttpGet(builder.toString());
            response = client.execute(httpGet);


            HttpEntity entity = response.getEntity();
            result = EntityUtils.toString(entity);

            EntityUtils.consume(entity);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (response != null) {
                try {
                    response.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }


    public String sendPost(String call, Properties params) {
        String result = "";

        CloseableHttpClient client = HttpClientBuilder.create().build();
        CloseableHttpResponse response = null;
        try {
            HttpPost httpPost = new HttpPost(call);

            List<NameValuePair> nvps = new ArrayList<NameValuePair>();

            // convert the Properties object to an array for iterating thru it :(
            for (String key : params.keySet().toArray(new String[0])) {
                nvps.add(new BasicNameValuePair(key, params.getProperty(key)));
            }

            httpPost.setEntity(new UrlEncodedFormEntity(nvps));
            response = client.execute(httpPost);


            HttpEntity entity = response.getEntity();

            result = EntityUtils.toString(entity);

            EntityUtils.consume(entity);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (response != null) {
                try {
                    response.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }

    // call - the URL (dump, retrieve etc)
    public boolean postCheck(String call, String filename) {
        try {
            Properties props = readInputFile(INPUT_DIR + filename);

            String response = sendPost(url + call, props);

            String answer = readFile(OUTPUT_DIR + filename);
            writeOutput(filename, response);


            return assertAuthenticateEquals(response, answer);

        } catch (Throwable e) {
            e.printStackTrace();
        }
        return false;
    }

    // call - the URL (dump, retrieve etc)
    // call - the URL (dump, retrieve etc)
    public boolean getCheck(String call, String filename) {
        try {
            Properties props = readInputFile(INPUT_DIR + filename);

            String response = sendGet(url + call, props);

            String answer = readFile(OUTPUT_DIR + filename);
            writeOutput(filename, response);

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
