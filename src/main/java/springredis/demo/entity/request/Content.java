package springredis.demo.entity.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.regex.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Content {
    @JsonProperty("from")
    private Sender sender;

    @JsonProperty("subject")
    private String subject;
    private String link;

    @JsonProperty("html")
    private String html;

    public void setHtml(String content, String link) {
        String regex = "(?i)(http|https)://[^\\s<>\"']+";
        Pattern pattern = Pattern.compile(regex);

        Matcher matcher = pattern.matcher(content);
        StringBuilder htmlBuilder = new StringBuilder();

        int lastIndex = 0;
        while (matcher.find()) {
            int startIndex = matcher.start();
            int endIndex = matcher.end();

            String plainText = content.substring(lastIndex, startIndex);
            String hyperlink = content.substring(startIndex, endIndex);

            htmlBuilder.append(plainText);
            htmlBuilder.append("<a href='");
            htmlBuilder.append(hyperlink);
            htmlBuilder.append("'>");
            htmlBuilder.append(hyperlink);
            htmlBuilder.append("</a><br><br>");

            lastIndex = endIndex;
        }

        htmlBuilder.append(content.substring(lastIndex));
        htmlBuilder.append("<br><br><a data-msys-unsubscribe=\"1\" href=\"");
        htmlBuilder.append(link);
        htmlBuilder.append("\">Unsubscribe</a>");

        this.html = htmlBuilder.toString();
    }



//    public void setHtml(String content, String link) {
////        String regex = "(http|https)://[^\\s<>]+";
//        String regex = "(http|https)://[^\s<>\"']+";
//        Pattern pattern = Pattern.compile(regex);
//        Matcher matcher = pattern.matcher(content);
//
//        StringBuilder htmlBuilder = new StringBuilder();
//
//        int lastIndex = 0;
//        while (matcher.find()) {
//            int startIndex = matcher.start();
//            int endIndex = matcher.end();
//
//            String plainText = content.substring(lastIndex, startIndex);
//            String hyperlink = content.substring(startIndex, endIndex);
//
////            htmlBuilder.append(plainText);
////            htmlBuilder.append(hyperlink);
////            htmlBuilder.append("<br><br>");
////
//            htmlBuilder.append(plainText);
//            htmlBuilder.append("<a href='");
//            htmlBuilder.append(hyperlink);
//            htmlBuilder.append("'>");
//            htmlBuilder.append(hyperlink);
//            htmlBuilder.append("</a><br><br>");
//
//            lastIndex = endIndex;
//        }
//
//        htmlBuilder.append(content.substring(lastIndex));
//        htmlBuilder.append("<br><br><a data-msys-unsubscribe=\"1\" href=\"");
//        htmlBuilder.append(link);
//        htmlBuilder.append("\">Unsubscribe</a>");
//
//        this.html = htmlBuilder.toString();
//
//    }

    @JsonProperty("text")
    private String text;

    @JsonProperty("content")
    private String content;

    @Override
    public String toString() {
        return "Content{" +
                "sender=" + sender +
                ", subject='" + subject + '\'' +
                ", html='" + html + '\'' +
                ", text='" + text + '\'' +
                ", content='" + content + '\'' +
                '}';
    }
}