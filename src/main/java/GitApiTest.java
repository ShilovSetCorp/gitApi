
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class GitApiTest {

    private static String username = "epamskillsearch";
    private static String token = "49878fcc4d4292e7d74fef15bf114a99aa396fa8";
    private static String uriString = "https://" + username + ":" + token + "@api.github.com/search/users?q=followers:%3E%3D1000&per_page=100";///"https://" + username + ":" + token + "@api.github.com/search/users?q=followers:%3E%3D1000";
    private static int cnt = 0;
    private static File file = new File("resultUsersssqwe.csv");

    public static void main(String[] args) {
        String uriStringPaginate;
        final CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpUriRequest httpGet = new HttpGet(uriString);  //https://github.com/search?q=followers%3A%3E%3D1000&type=Users");///"https://api.github.com/users");

        try (CloseableHttpResponse response = httpClient.execute(httpGet)){
            cnt++;
            String page = getPage(response);
            System.out.println(page);
            HttpEntity entity = response.getEntity();
            putUsersToCSV(entity, file);
          //  Files.write(Paths.get("output.txt"), EntityUtils.toString(entity).getBytes());

            while (!"".equals(page)){
                uriStringPaginate = uriString + "&" + page;
                if (cnt >= 9){
                    System.out.println("Pause");
                    TimeUnit.MINUTES.sleep(1);
                    cnt = 0;
                }
                httpGet = new HttpGet(uriStringPaginate);
                CloseableHttpResponse responsePage = httpClient.execute(httpGet);
                cnt++;
                page = getPage(responsePage);
                System.out.println(page);
                entity = responsePage.getEntity();
                putUsersToCSV(entity, file);
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static String getPage(HttpResponse response){
        return Arrays.stream(response.getAllHeaders())
                .flatMap(h -> Arrays.stream(h.getElements()))
                .filter(he -> (null != he.getParameterByName("rel")) && ("next".equals(he.getParameterByName("rel").getValue())))
                .map(he -> {String s = he.getValue().substring(he.getValue().lastIndexOf('&')+1, he.getValue().length()-1);
                    return s;})
                .findAny().orElseGet(String::new);
    }

    private static void putUsersToCSV(HttpEntity entity, File file) throws IOException {
        ArrayList<String[]> users = new ArrayList<>();
        JSONObject jsonObject = new JSONObject(EntityUtils.toString(entity));
        JSONArray usersJsonArray = jsonObject.getJSONArray("items");
        for(int i = 0; i < usersJsonArray.length(); i++) {
            String[] user = new String[5];
            JSONObject jo = usersJsonArray.getJSONObject(i);
            user[0] = jo.get("id").toString();
            user[1] = jo.get("login").toString();
            user[2] = jo.get("type").toString();
            user[3] = jo.get("organizations_url").toString();
            user[4] = jo.get("subscriptions_url").toString();
            users.add(user);
        }

        try (PrintWriter pw = new PrintWriter(new FileOutputStream(file), true)) {
            users.stream()
                    .map(GitApiTest::convertToCSV)
                    .forEach(pw::println);
            pw.flush();
        }
    }

    private static String convertToCSV(String[] data) {
        return String.join(",", data);
    }
}
