package naamakahvi.naamakahviclient;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

public class Client {
    private String host;
    private int port;
    private IStation station;

    private static class Station implements IStation {
        private String name;

        Station(String name) {
            this.name = name;
        }

        public String getName() {
            return this.name;
        }
    }

    /**
     * Gets all stations as a json object from server using the specified host name and port 
     * number and makes a list of IStation objects. The server response should contain fields:
     * "status" - a string
     * "stations" - an array of station names
     * 
     * @param host server host name
     * @param port server port number
     * @return list of stations
     */
    public static List<IStation> listStations(String host, int port) throws ClientException {
        try {
            JsonObject obj = new Client(host, port, null).doGet("/list_stations/");

            if (obj.get("status").getAsString().equalsIgnoreCase("ok")) {
                List<IStation> ans = new ArrayList();
                for (JsonElement e : obj.get("stations").getAsJsonArray()) {
                    ans.add(new Station(e.getAsString()));
                }
                return ans;
            } else {
                throw new GeneralClientException("Could not fetch list of stations");
            }
        } catch (Exception e) {
            throw new GeneralClientException(e.toString());
        }
    }

    /**
     * Creates a new Client object that uses the specified host name and
     * port number and is located at the given station.
     * 
     * @param host server host name
     * @param port server port number
     * @param station coffee syndicate location
     */
    public Client(String host, int port, IStation station) {
        this.host = host;
        this.port = port;
        this.station = station;
    }

    private String[] jsonArrayToStringArray(JsonArray jsonArray) {
        String[] ans = new String[jsonArray.size()];

        for (int i = 0; i < jsonArray.size(); i++) {
            ans[i] = jsonArray.get(i).getAsString();
        }

        return ans;
    }

    private JsonObject responseToJson(HttpResponse response) throws IOException {
        String s = Util.readStream(response.getEntity().getContent());
        return new JsonParser().parse(s).getAsJsonObject();
    }

    private URI buildURI(String path) throws Exception {
        return new URI("http://" + this.host + ":" + this.port + path);
    }

    private JsonObject doPost(String path, String... params) throws Exception {
        if ((params.length % 2) != 0) {
            throw new IllegalArgumentException("Odd number of parameters");
        }

        final URI uri = buildURI(path);
        final HttpClient c = new DefaultHttpClient();
        final HttpPost post = new HttpPost(uri);
        final List<NameValuePair> plist = new ArrayList<NameValuePair>();

        for (int i = 0; i < params.length; i += 2) {
            plist.add(new BasicNameValuePair(params[i], params[i + 1]));
        }

        post.setEntity(new UrlEncodedFormEntity(plist, "UTF-8"));
        post.addHeader("Content-Type", "application/x-www-form-urlencoded");

        final HttpResponse resp = c.execute(post);
        final int status = resp.getStatusLine().getStatusCode();

        if (status == 200) {
            return responseToJson(resp);
        } else {
            throw new GeneralClientException("Server returned HTTP-status code " + status);
        }
    }

    private JsonObject doGet(String path, String... params) throws Exception {
        if ((params.length % 2) != 0) {
            throw new IllegalArgumentException("Odd number of parameters");
        }

        final HttpClient c = new DefaultHttpClient();

        String suri = "http://" + this.host + ":" + this.port + path + "?";

        for (int i = 0; i < params.length; i += 2) {
            suri = suri + params[i] + "=" + params[i + 1] + "&";
        }

        final URI uri = new URI(suri);
        final HttpGet get = new HttpGet(uri);

        get.addHeader("Content-Type", "application/x-www-form-urlencoded");

        final HttpResponse resp = c.execute(get);
        final int status = resp.getStatusLine().getStatusCode();

        if (status == 200) {
            return responseToJson(resp);
        } else {
            throw new GeneralClientException("Server returned HTTP-status code " + status);
        }
    }

    /**
     * Gets all usernames from server and creates a string array. The server response should
     * be a json object that contains fields:
     * "status" - a string, "ok" when the operation is successful
     * "usernames" - an array of usernames
     * 
     * @return list of all usernames
     */
    public String[] listUsernames() throws ClientException {
        try {
            JsonObject obj = doGet("/list_usernames/");
            if (obj.get("status").getAsString().equalsIgnoreCase("ok")) {
                JsonArray jarr = obj.get("usernames").getAsJsonArray();
                return jsonArrayToStringArray(jarr);
            } else {
                throw new GeneralClientException("asdf");
            }
        } catch (Exception e) {
            throw new GeneralClientException(e.getMessage());
        }
    }

    /**
     * Sends new user's user data and optional image data to server and creates a User object 
     * that represents the registered user. The server response should be a json object that
     * contains fields:
     * "status" - a string, "ok" when the operation is successful
     * "username" - 
     * "given" -
     * "family" - 
     * 
     * @param username new user's username
     * @param givenName new user's given name
     * @param familyName new user's family name
     * @param imagedata image of the new user (optional)
     * @return the newly registered user
     */
    public IUser registerUser(String username, String givenName,
            String familyName) throws RegistrationException {
        try {
            JsonObject obj = doPost("/register/",
                    "username", username,
                    "given", givenName,
                    "family", familyName);

            if (obj.get("status").getAsString().equalsIgnoreCase("ok")) {
                obj.remove("status");
                User responseUser = new Gson().fromJson(obj, User.class);
                if (!responseUser.getUserName().equals(username)) {
                    throw new RegistrationException("username returned from server doesn't match given username");
                }

                return responseUser;
            } else {
                throw new RegistrationException("Registration failed: Try another username");
            }
        } catch (RegistrationException e) {
            throw e;
        } catch (Exception e) {
            throw new RegistrationException(e.getClass().toString() + ": " + e.toString());
        }
    }

    /**
     * Sends an image of a user to server. 
     * 
     * @param username 
     * @param imagedata the image data
     */
    public void addImage(String username, byte[] imagedata) throws GeneralClientException {
        try {
            HttpClient httpClient = new DefaultHttpClient();
            HttpPost post = new HttpPost(buildURI("/upload/"));

            MultipartEntity entity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
            entity.addPart("file", new ByteArrayBody(imagedata, "snapshot.jpg", "image/jpeg"));
            entity.addPart("username", new StringBody(username, "text/plain", Charset.forName("UTF-8")));
            post.setEntity(entity);
            HttpResponse response = httpClient.execute(post);
            JsonObject jsonResponse = responseToJson(response);
            String status = jsonResponse.get("status").getAsString();
            if (!status.equalsIgnoreCase("ok")) {
                throw new GeneralClientException(status);
            }
        } catch (Exception ex) {
            throw new GeneralClientException(ex.getMessage());
        }
    }

    /**
     * Gets user data related to given username from server and creates a User instance.
     * The server response should have a json object that contains fields:
     * "status" - a string, "ok" when the operation is successful
     * "username" - the user's username as a string
     * "given" - the user's given name as a string
     * "family" - the user's family name as a string
     * 
     * @param username the username
     * @return the user instance
     */
    public IUser authenticateText(String username) throws AuthenticationException {
        try {
            JsonObject obj = doPost("/authenticate_text/",
                    "username", username);

            if (obj.get("status").getAsString().equalsIgnoreCase("ok")) {
                obj.remove("status");
                User responseUser = new Gson().fromJson(obj, User.class);
                if (!responseUser.getUserName().equals(username)) {
                    throw new AuthenticationException("username returned from server doesn't match given username");
                }

                return responseUser;
            } else {
                throw new AuthenticationException("Authentication failed");
            }
        } catch (AuthenticationException e) {
            throw e;
        } catch (Exception e) {
            throw new AuthenticationException(e.toString());
        }
    }

    static class GeneralClientException extends ClientException {
        public GeneralClientException(String s) {
            super(s);
        }
    }

    private List<BuyableProduct> jsonToBuyableProductList(JsonArray ar) {
        List<BuyableProduct> ans = new ArrayList();
        for (JsonElement e : ar) {
            JsonObject product = e.getAsJsonObject();
            String productName = product.get("product_name").getAsString();
            double productPrice = product.get("product_price").getAsDouble();
            int productId = product.get("product_id").getAsInt();
            double productSize = product.get("product_size").getAsDouble();
            ans.add(new BuyableProduct(productId, productName, productPrice, productSize));
        }
        return ans;
    }

    private List<RawProduct> jsonToRawProductList(JsonArray ar) {
        List<RawProduct> ans = new ArrayList();
        for (JsonElement e : ar) {
            JsonObject product = e.getAsJsonObject();
            String productName = product.get("product_name").getAsString();
            double productPrice = product.get("product_price").getAsDouble();
            int productId = product.get("product_id").getAsInt();
            ans.add(new RawProduct(productName, productPrice, productId));
        }
        return ans;
    }


    /**
     * Fetches all buyable products from server and makes a list of BuyableProduct objects from them.
     * 
     * @return list of all buyable products
     */
    public List<BuyableProduct> listBuyableProducts() throws ClientException {
        try {
            JsonObject obj = doGet("/list_buyable_products/",
                    "station_name", this.station.getName());
            if (obj.get("status").getAsString().equalsIgnoreCase("ok")) {
                return jsonToBuyableProductList(obj.get("buyable_products").getAsJsonArray());
            } else {
                throw new GeneralClientException("Could not fetch list of buyable products");
            }
        } catch (Exception e) {
            throw new GeneralClientException(e.getMessage());
        }
    }

    /**
     * Fetches default buyable products that are shown on main view from server and makes
     * a list of IProduct objects from them.
     * 
     * @return list of default products
     */
    public List<BuyableProduct> listDefaultProducts() throws ClientException {
        try {
            JsonObject obj = doGet("/list_default_products/",
                    "station_name", this.station.getName());
            if (obj.get("status").getAsString().equalsIgnoreCase("ok")) {
                return jsonToBuyableProductList(obj.get("default_products").getAsJsonArray());
            } else {
                throw new GeneralClientException("Could not fetch list of default products");
            }
        } catch (Exception e) {
            e.printStackTrace(System.out);
            throw new GeneralClientException(e.getMessage());
        }
    }

    /**
     * 
     * 
     * @param user the buying user
     * @param product bought product
     * @param amount amount of bought product
     */
    public void buyProduct(IUser user, BuyableProduct product, int amount) throws ClientException {
        try {
            JsonObject obj = doPost("/buy_product/",
                    "product_id", Integer.toString(product.getId()),
                    "amount", "" + amount,
                    "username", user.getUserName());

            if (obj.get("status").getAsString().equalsIgnoreCase("ok")) {
                return;
            } else {
                throw new GeneralClientException("Buying the product failed");
            }
        } catch (Exception e) {
            throw new GeneralClientException(e.toString());
        }
    }

    /**
     * 
     * 
     * @return list of raw product objects
     */
    public List<RawProduct> listRawProducts() throws ClientException {
        try {
            JsonObject obj = doGet("/list_raw_products/",
                    "station_name", this.station.getName());
            if (obj.get("status").getAsString().equalsIgnoreCase("ok")) {
                return jsonToRawProductList(obj.get("raw_products").getAsJsonArray());
            } else {
                throw new GeneralClientException("Could not fetch list of raw products");
            }
        } catch (Exception e) {
            throw new GeneralClientException(e.toString());
        }
    }

    /**
     * 
     * 
     * @param user the user bringing the product
     * @param product the product to be brought
     * @param amount the amount of the brought product
     */
    public void bringProduct(IUser user, IProduct product, int amount) throws ClientException {
        try {
            JsonObject obj = doPost("/bring_product/",
                    "product_name", product.getName(),
                    "station_name", this.station.getName(),
                    "amount", "" + amount,
                    "username", user.getUserName());

            if (obj.get("status").getAsString().equalsIgnoreCase("ok")) {
                return;
            } else {
                throw new GeneralClientException("Bringing the product failed");
            }
        } catch (Exception e) {
            throw new GeneralClientException(e.toString());
        }
    }

    /**
     * Sends a HTTP post request to server containing a field:
     * "file" - the image data for identification
     * 
     * The server response should have a json object that contains fields:
     * "status" - a string, "ok" when the operation is successful
     * "idlist" - an ordered list of usernames that match the sent image, the best match first
     * 
     * @param imagedata the image
     * @return list of identified usernames 
     */
    public String[] identifyImage(byte[] imagedata) throws ClientException {
        try {
            HttpClient httpClient = new DefaultHttpClient();
            HttpPost post = new HttpPost(buildURI("/identify/"));

            MultipartEntity entity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
            entity.addPart("file", new ByteArrayBody(imagedata, "snapshot.jpg", "image/jpeg"));
            post.setEntity(entity);
            HttpResponse response = httpClient.execute(post);

            JsonObject jsonResponse = responseToJson(response);
            String status = jsonResponse.get("status").getAsString();
            if (status.equalsIgnoreCase("ok")) {
                JsonArray jarr = jsonResponse.get("idlist").getAsJsonArray();

                return jsonArrayToStringArray(jarr);
            } else {
                throw new AuthenticationException("Failed to identify user");
            }
        } catch (Exception ex) {
            throw new AuthenticationException(ex.toString());
        }
    }

    private List<SaldoItem> jsonToSaldoList(JsonArray ar) {
        List<SaldoItem> ans = new ArrayList();
        for (JsonElement e : ar) {
            JsonObject saldoitem = e.getAsJsonObject();
            String group_name = saldoitem.get("group_name").getAsString();
            double saldo = saldoitem.get("saldo").getAsDouble();
            ans.add(new SaldoItem(group_name, saldo));
        }
        return ans;
    }

    public List<SaldoItem> listUserSaldos(IUser user) throws ClientException {
        try {
            JsonObject obj = doGet("/list_user_saldos/",
                    "username", user.getUserName());

            String status = obj.get("status").getAsString();

            if (status.equalsIgnoreCase("ok")) {
                return jsonToSaldoList(obj.get("saldo_list").getAsJsonArray());
            } else {
                throw new GeneralClientException("Failed to get user saldos: " + status);
            }
        } catch (Exception e) {
            throw new GeneralClientException(e.toString());
        }
    }
    //    public static void main(String[] args) throws AuthenticationException, GeneralClientException, RegistrationException {
    //        Client c = new Client("naama.zerg.fi", 5001, null);
    //       // IUser u = c.registerUser("afdsafds", "asd", "as", new File("3.pgm"));
    //       // System.out.println("registered user " + u.getUserName());
    //        String[] identifyImage = c.identifyImage(new File("1.pgm"));
    //        for (String name : identifyImage) {
    //            System.out.println(name);
    //        }
    //    }
}