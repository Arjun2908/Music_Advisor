package advisor;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;

public class Main {
    private static String apiServerPath = "https://api.spotify.com/";
    private static final String categoriesURL = apiServerPath + "v1/browse/categories";
    private static final String releasesURL = apiServerPath + "v1/browse/new-releases";
    private static final String featuredURL = apiServerPath + "v1/browse/featured-playlists";
    private static String authCode = "";
    private static String token = "";
    private static String authorizationServerPath = "https://accounts.spotify.com";
    private static final String clientID = "810044b048274bd898bcf60c6ad2ced3";

    public static void createServer() throws IOException, InterruptedException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.start();
        System.out.println("use this link to request the access code:");
        String redirectURI = "http://localhost:8080";
        System.out.printf("%s/authorize?client_id=%s&redirect_uri=%s&response_type=code", authorizationServerPath, clientID, redirectURI);
        System.out.println();
        System.out.println("waiting for code...");
        server.createContext("/",
                exchange -> {
                    String query = exchange.getRequestURI().getQuery();
                    String result;
                    if (query != null && query.contains("code")) {
                        authCode = query.substring(5);
                        result = "Got the code. Return back to your program.";
                    } else {
                        result = "Not found authorization code. Try again.";
                    }
                    exchange.sendResponseHeaders(200, result.length());
                    exchange.getResponseBody().write(result.getBytes());
                    exchange.getResponseBody().close();
                    System.out.println(result);
                }
        );
        while (authCode.equals("")) {
            Thread.sleep(10);
        }
        server.stop(10);
    }

    public static String createRequest(String requestedFeatureURL) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder().build();
        HttpRequest request = HttpRequest.newBuilder()
                .header("Authorization", "Bearer " + token)
                .uri(URI.create(requestedFeatureURL))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }

    public static void getToken() throws IOException, InterruptedException {
        System.out.println("making http request for access_token...");
        final String clientSecret = "381a0be0ba5b481f82a4807a91f4009f";
        HttpRequest requestAccessToken = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(
                        "client_id=" + clientID
                                + "&client_secret=" + clientSecret
                                + "&grant_type=" + "authorization_code"
                                + "&code=" + authCode
                                + "&redirect_uri=" + "http%3A%2F%2Flocalhost%3A8080"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .uri(URI.create("https://accounts.spotify.com/api/token"))
                .build();
        HttpClient client = HttpClient.newBuilder().build();
        HttpResponse<String> responseWithAccessToken = client.send(requestAccessToken, HttpResponse.BodyHandlers.ofString());
    }


    private static void getNewReleases() throws IOException, InterruptedException {
        String verboseJson = createRequest(releasesURL);
        JsonObject albums = JsonParser.parseString(verboseJson).getAsJsonObject().get("albums").getAsJsonObject();
        albums.get("items").getAsJsonArray().forEach(item -> {
            var album = item.getAsJsonObject();
            var name = album.get("name").getAsString();
            var url = album.get("external_urls").getAsJsonObject().get("spotify").getAsString();
            var artists = new ArrayList<>();
            album.get("artists").getAsJsonArray().forEach(artist -> {
                artists.add(artist.getAsJsonObject().get("name").getAsString());
                System.out.println(name);
                System.out.println(artists);
                System.out.println(url);
                System.out.println();
            });

        });
    }

    private static void getCategoryNames() throws IOException, InterruptedException {
        var categories = createRequest(categoriesURL);
        JsonParser.parseString(categories).getAsJsonObject().getAsJsonObject().get("categories").getAsJsonObject().get("items").getAsJsonArray().forEach(item -> {
            var category = item.getAsJsonObject();
            var categoryName = category.get("name").getAsString();
            System.out.println(categoryName);
        });
    }
    private static String getCategoryIdByCategoryName(String categoryName) throws IOException, InterruptedException {
        var categories = createRequest(categoriesURL);
        var items = JsonParser.parseString(categories).getAsJsonObject().get("categories").getAsJsonObject().get("items").getAsJsonArray();
        for(JsonElement item: items){
            var name = item.getAsJsonObject().get("name").getAsString();
            if(categoryName.equals(name)){
                return item.getAsJsonObject().get("id").getAsString();
            }
        }
        return null;
    }
    private static void getFeatures() throws IOException, InterruptedException {
        var features = createRequest(featuredURL);
        var items = JsonParser.parseString(features).getAsJsonObject().get("playlists").getAsJsonObject().get("items").getAsJsonArray();
        items.forEach(item -> {
            var bar = item.getAsJsonObject();
            System.out.println(bar.get("name").getAsString());
            System.out.println(bar.get("owner").getAsJsonObject().get("external_urls").getAsJsonObject().get("spotify").getAsString());
            System.out.println();
        });
    }
    private static void getPlaylists(String categoryName) throws IOException, InterruptedException {
        String categoryID = getCategoryIdByCategoryName(categoryName);
        if(categoryID == null){
            System.out.println("Unknown category name.");
            return;
        }
        String playlistsURL = apiServerPath + "v1/browse/categories/" + categoryID + "/playlists";
        var playlists = JsonParser.parseString(createRequest(playlistsURL)).getAsJsonObject();

        var items = playlists.get("playlists").getAsJsonObject().get("items").getAsJsonArray();
        items.forEach(item -> {
            var foo = item.getAsJsonObject();
            System.out.println(foo.get("name"));
            System.out.println(foo.get("external_urls").getAsJsonObject().get("spotify").getAsString());
            System.out.println();
        });


    }

    public static void main(String[] args) throws IOException, InterruptedException {
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-access")) {
                authorizationServerPath = args[i + 1];
            }
            if (args[i].equals("-resource")) {
                apiServerPath = args[i + 1];
            }
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        boolean auth = false;
        boolean bool = true;
        while (bool) {
            String option = reader.readLine().trim();
            String command = option.contains(" ") ? option.substring(0, option.indexOf(" ")) : option;
            switch (command) {
                case "new":
                    if (auth) {
                        getNewReleases();
                    } else {
                        System.out.println("Please, provide access for application.\n");
                    }
                    break;

                case "featured":
                    if (auth) {
                        getFeatures();
                    } else {
                        System.out.println("Please, provide access for application.\n");
                    }
                    break;
                case "categories":
                    if (auth) {
                        getCategoryNames();
                    } else {
                        System.out.println("Please, provide access for application.\n");
                    }
                    break;
                case "playlists":
                    String category = option.substring(option.indexOf(" ") + 1);
                    if (auth) {
                        getPlaylists(category);
                    } else {
                        System.out.println("Please, provide access for application.\n");
                    }
                    break;
                case "auth":
                    createServer();
                    getToken();
                    System.out.println(token);
                    System.out.println("---SUCCESS---");
                    auth = true;
                    break;
                case "exit":
                    System.out.println("---GOODBYE!---");
                    bool = false;
                    break;

            }
        }
    }
}