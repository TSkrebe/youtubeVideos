package main.java.sample;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashSet;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by titas on 15.10.5.
 */
public class Controler implements Initializable{

    @FXML
    private TextArea videoIds;
    @FXML
    private Button searchButton;
    @FXML
    private TextField channelName;
    @FXML
    private TextField infoField;

    private Set<String> set = new HashSet<>();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

    }

    public void onButtonSearch(){
        set.clear();
        infoField.setText("");
        ExecutorService ex = Executors.newSingleThreadExecutor();
        ex.submit(new LoaderClass());
        System.out.print("bye");

    }

    //extracts videos from html
    private void extractMoreVideos(String part) {
        Elements elems = Jsoup.parse(part).select("a.yt-uix-sessionlink");
        for (Element e : elems) {
            set.add(e.attr("href"));
        }
    }

    //read json from url
    private String readUrl(String url) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(new URL(url).openStream()));
        String line;
        StringBuilder object = new StringBuilder();
        while ((line = reader.readLine()) != null) {
            object.append(line);
        }
        reader.close();
        return object.toString();
    }


    private void findVideosFromMainPage(Document jsoup) {
        String classAttribute = "a.yt-uix-sessionlink.yt-uix-tile-link.spf-link.yt-ui-ellipsis.yt-ui-ellipsis-2";
        Elements elems = jsoup.select(classAttribute);
        for (Element e : elems) {
            set.add(e.attr("href"));
        }
    }


    private class LoaderClass implements Runnable{


        @Override
        public void run() {


            String channel = channelName.getText().trim();
            String url = String.format("https://www.youtube.com/user/%s/videos", channel);
            Document jsoup = null;
            try {
                jsoup = Jsoup.parse(new URL(url), 5 * 1000);
            } catch (IOException e) {
                infoField.setText("Cannot connect to such channel");
                return;
            }

            findVideosFromMainPage(jsoup);

            Element button = jsoup.getElementsByAttribute("data-uix-load-more-href").first();

            // while there are more videos
            while (button != null) {


                // get ajax url
                String ajaxCall = button.attr("data-uix-load-more-href");

                // read url
                String jsonObject = null;
                try {
                    jsonObject = readUrl("https://youtube.com/" + ajaxCall);
                } catch (IOException e) {
                    infoField.setText("Something happened while loading data...\nIds find: " + set.size());
                    return;
                }

                // from string to json object
                JsonObject gson = new JsonParser().parse(jsonObject).getAsJsonObject();

                // if empty means no more video
                String newButton = gson.get("load_more_widget_html").getAsString();

                //new button
                button = Jsoup.parse(newButton).getElementsByTag("button").first();

                //videos from json response
                extractMoreVideos(gson.get("content_html").getAsString());
               /* for (String s : set) {

                    Platform.runLater(() -> {
                     //   videoIds.appendText(s + "\n");

                    });
                }*/
                infoField.setText("Number of videos: " + set.size());


            }
        }
    }

}
