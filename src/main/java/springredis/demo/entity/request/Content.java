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

    @JsonProperty("text")
    private String text;

//    @JsonProperty("address")
//    private String address;

    @JsonProperty("type")
    private String type;

    @JsonProperty("content")
    private String content;

    public void setHtml(String content, String link) {
        String regex = "(?i)(http|https)://[^\\s<>\"']+";
        Pattern pattern = Pattern.compile(regex);

        Matcher matcher = pattern.matcher(content);
        StringBuilder htmlBuilder = new StringBuilder();

        int lastIndex = 0;
        while (matcher.find()) {
            int startIndex = matcher.start();
            int endIndex = matcher.end();

            String hyperlink = content.substring(startIndex, endIndex);

            // Append only the hyperlink as an HTML link
            htmlBuilder.append("<a href='");
            htmlBuilder.append(hyperlink);
            htmlBuilder.append("'>");
            htmlBuilder.append(hyperlink);
            htmlBuilder.append("</a><br><br>");

            lastIndex = endIndex;
        }

        // Append any remaining content after the last hyperlink
        htmlBuilder.append(content.substring(lastIndex));

        htmlBuilder.append("<br><br><a data-msys-unsubscribe=\"1\" href=\"");
        htmlBuilder.append(link);
        htmlBuilder.append("\">Unsubscribe</a>");

        this.html = htmlBuilder.toString();
    }

    public void setText(String content, String link) {

        // Append the plain text content
        this.html = "<span>" +
                escapeHtml(content) +
                "</span>" +

                // Append the unsubscribe link
                "<br><br><a data-msys-unsubscribe=\"1\" href=\"" +
                link +
                "\">Unsubscribe</a>";
    }


    private String escapeHtml(String content) {
        return content
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }


//    public void setText(String content, String link) {
//
//        // Append the plain text content
//
//        this.html = content +
//
//                // Append the unsubscribe link
//                "<br><br><a data-msys-unsubscribe=\"1\" href=\"" +
//                link +
//                "\">Unsubscribe</a>";
//    }

//    public void setHtml(String content, String link) {
//        String regex = "(?i)(http|https)://[^\\s<>\"']+";
//        Pattern pattern = Pattern.compile(regex);
//
//        Matcher matcher = pattern.matcher(content);
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
//        if (lastIndex < content.length()) {
//            String remainingText = content.substring(lastIndex);
//            htmlBuilder.append(remainingText);
//        }
//
//        return htmlBuilder.toString();
//    }


    @Override
    public String toString() {
        return "Content{" +
                "sender=" + sender +
                ", subject='" + subject + '\'' +
                ", html='" + html + '\'' +
                ", text='" + text + '\'' +
//                ", address='" + address + '\'' +
                ", type='" + type + '\'' +
                ", content='" + content + '\'' +
                '}';
    }
}